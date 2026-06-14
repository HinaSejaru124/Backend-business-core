package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatcher socle des exécuteurs d'étapes : indexe les stratégies {@link ExecuteurDEtape} par leur
 * {@link TypeEtape}. Le moteur d'opération (Dev 5) délègue ici ; ajouter une étape = ajouter une
 * implémentation, sans modifier ce dispatcher. La liste injectée peut être vide au stade socle.
 */
@Component
public class ExecuteurDEtapeDispatcher {

    private final Map<TypeEtape, ExecuteurDEtape> parType = new EnumMap<>(TypeEtape.class);

    public ExecuteurDEtapeDispatcher(List<ExecuteurDEtape> executeurs) {
        for (ExecuteurDEtape executeur : executeurs) {
            parType.put(executeur.typeSupporte(), executeur);
        }
    }

    public Mono<ContexteEtape> executer(TypeEtape type, ContexteEtape contexte) {
        ExecuteurDEtape executeur = parType.get(type);
        if (executeur == null) {
            return Mono.error(new IllegalStateException("Aucun ExecuteurDEtape enregistré pour l'étape " + type));
        }
        return executeur.executer(contexte);
    }
}
