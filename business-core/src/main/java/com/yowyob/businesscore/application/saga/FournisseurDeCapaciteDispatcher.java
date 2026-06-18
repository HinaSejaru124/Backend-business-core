package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.internal.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatcher socle des fournisseurs de capacité : indexe les stratégies {@link FournisseurDeCapacite}
 * par leur {@link TypeCapacite}. La brique Offre (Dev 3) ajoute ses implémentations ; la liste
 * injectée peut être vide au stade socle.
 */
@Component
public class FournisseurDeCapaciteDispatcher {

    private final Map<TypeCapacite, FournisseurDeCapacite> parType = new EnumMap<>(TypeCapacite.class);

    public FournisseurDeCapaciteDispatcher(List<FournisseurDeCapacite> fournisseurs) {
        for (FournisseurDeCapacite fournisseur : fournisseurs) {
            parType.put(fournisseur.typeSupporte(), fournisseur);
        }
    }

    public Mono<Void> activer(TypeCapacite type, UUID offreId) {
        FournisseurDeCapacite fournisseur = parType.get(type);
        if (fournisseur == null) {
            return Mono.error(ProblemException.unprocessable("Capacité non encore supportée : " + type));
        }
        return fournisseur.activer(offreId);
    }
}
