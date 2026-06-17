package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.out.ExecuterWorkflow;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Moteur d'exécution d'une opération (saga orchestrée localement, réactive).
 *
 * <p>Chaîne les étapes-types via le {@link ExecuteurDEtapeDispatcher} (chaque type → une stratégie
 * {@code ExecuteurDEtape}), en propageant le {@link ContexteEtape} de proche en proche. Si une étape
 * échoue, le moteur <b>compense</b> : il annule l'effet déjà engagé sur le kernel (la transaction dont
 * la référence a été posée dans le contexte) via {@link ExecuterWorkflow#compenser}. La compensation
 * est <i>best-effort</i> et ne masque jamais l'erreur d'origine.
 *
 * <p>Ne lève pas : renvoie un {@link ResultatMoteur} (succès/échec + contexte) pour laisser la couche
 * application décider de la trace (COMPLETEE / COMPENSEE) et du code HTTP.
 */
@Component
public class MoteurOperation {

    private static final Logger log = LoggerFactory.getLogger(MoteurOperation.class);

    private final ExecuteurDEtapeDispatcher dispatcher;
    private final ExecuterWorkflow executerWorkflow;

    public MoteurOperation(ExecuteurDEtapeDispatcher dispatcher, ExecuterWorkflow executerWorkflow) {
        this.dispatcher = dispatcher;
        this.executerWorkflow = executerWorkflow;
    }

    /** Exécute la séquence d'étapes à partir d'un contexte initial. */
    public Mono<ResultatMoteur> executer(List<TypeEtape> etapes, ContexteEtape contexteInitial) {
        AtomicReference<ContexteEtape> dernier = new AtomicReference<>(contexteInitial);

        Mono<ContexteEtape> chaine = Mono.just(contexteInitial);
        for (TypeEtape etape : etapes) {
            chaine = chaine.flatMap(ctx -> dispatcher.executer(etape, ctx).doOnNext(dernier::set));
        }

        return chaine
                .map(ResultatMoteur::succes)
                .onErrorResume(erreur -> compenser(dernier.get())
                        .thenReturn(ResultatMoteur.echec(dernier.get(), erreur)));
    }

    /** Annule l'effet kernel déjà engagé, identifié par sa référence de transaction dans le contexte. */
    private Mono<Void> compenser(ContexteEtape contexte) {
        Object transactionKernelId = contexte.get(ClesContexte.TRANSACTION_KERNEL_ID);
        if (transactionKernelId == null) {
            // Rien n'a encore été engagé sur le kernel : aucune compensation nécessaire.
            return Mono.empty();
        }
        return executerWorkflow.compenser(transactionKernelId.toString())
                .doOnError(e -> log.warn("Compensation de la transaction {} en échec : {}",
                        transactionKernelId, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }
}
