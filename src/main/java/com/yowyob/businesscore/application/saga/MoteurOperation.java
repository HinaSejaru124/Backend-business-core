package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Moteur d'exécution d'une opération — <b>saga orchestrée localement</b> (réactive).
 *
 * <p>Le kernel RT-Comops n'expose <b>pas</b> de moteur Saga : la compensation est pilotée ici. Le
 * moteur chaîne les étapes-types via le {@link ExecuteurDEtapeDispatcher} (chaque type → une stratégie
 * {@code ExecuteurDEtape}) en propageant le {@link ContexteEtape} de proche en proche, et
 * <b>journalise chaque étape réussie</b>. Si une étape échoue, il <b>déroule le journal en ordre
 * inverse</b> (LIFO) et annule chaque effet via {@link ExecuteurDEtapeDispatcher#compenser} — seules
 * les étapes {@code EtapeCompensable} ont un effet à défaire, les autres sont des no-op.
 *
 * <p>La compensation est <i>best-effort</i> : une annulation en échec est journalisée mais ne masque
 * jamais l'erreur d'origine. Le moteur ne lève pas : il renvoie un {@link ResultatMoteur}
 * (succès/échec + contexte) pour laisser la couche application décider de la trace
 * (COMPLETEE / COMPENSEE) et du code HTTP.
 */
@Component
public class MoteurOperation {

    private static final Logger log = LoggerFactory.getLogger(MoteurOperation.class);

    private final ExecuteurDEtapeDispatcher dispatcher;

    public MoteurOperation(ExecuteurDEtapeDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Exécute la séquence d'étapes à partir d'un contexte initial. */
    public Mono<ResultatMoteur> executer(List<TypeEtape> etapes, ContexteEtape contexteInitial) {
        // Journal des étapes réussies (type + contexte produit), empilé pour une compensation LIFO.
        Deque<EtapeJournalisee> journal = new ArrayDeque<>();
        AtomicReference<ContexteEtape> dernier = new AtomicReference<>(contexteInitial);

        Mono<ContexteEtape> chaine = Mono.just(contexteInitial);
        for (TypeEtape etape : etapes) {
            chaine = chaine.flatMap(ctx -> dispatcher.executer(etape, ctx)
                    .doOnNext(ctxApres -> {
                        journal.push(new EtapeJournalisee(etape, ctxApres));
                        dernier.set(ctxApres);
                    }));
        }

        return chaine
                .map(ResultatMoteur::succes)
                .onErrorResume(erreur -> compenser(journal)
                        .thenReturn(ResultatMoteur.echec(dernier.get(), erreur)));
    }

    /** Annule en ordre inverse les effets des étapes déjà réussies (best-effort). */
    private Mono<Void> compenser(Deque<EtapeJournalisee> journal) {
        // L'itération d'un ArrayDeque empilé via push() est LIFO : dernière étape réussie d'abord.
        return Flux.fromIterable(journal)
                .concatMap(etape -> dispatcher.compenser(etape.type(), etape.contexte())
                        .doOnError(e -> log.warn("Compensation de l'étape {} en échec : {}",
                                etape.type(), e.toString()))
                        .onErrorResume(e -> Mono.empty()))
                .then();
    }

    /** Trace minimale d'une étape réussie : son type et le contexte qu'elle a produit. */
    private record EtapeJournalisee(TypeEtape type, ContexteEtape contexte) {
    }
}
