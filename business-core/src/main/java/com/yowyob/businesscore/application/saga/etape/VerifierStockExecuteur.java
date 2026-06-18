package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.out.VerifierDisponibilite;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Étape {@code VERIFIER_STOCK} — lit le solde de stock de l'offre (via le port {@link VerifierDisponibilite})
 * et le pose dans le contexte. Si une quantité est demandée et que le stock est insuffisant, l'étape
 * échoue (422) — avant tout effet, donc sans compensation.
 */
@Component
public class VerifierStockExecuteur implements ExecuteurDEtape {

    private final VerifierDisponibilite verifierDisponibilite;

    public VerifierStockExecuteur(VerifierDisponibilite verifierDisponibilite) {
        this.verifierDisponibilite = verifierDisponibilite;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.VERIFIER_STOCK;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        UUID offreId = Valeurs.versUuid(contexte.get(ClesContexte.OFFRE_ID));
        if (offreId == null) {
            // Aucune offre ciblée : rien à vérifier.
            return Mono.just(contexte);
        }
        Integer quantite = Valeurs.versEntier(contexte.get(ClesContexte.QUANTITE));
        return verifierDisponibilite.soldeStock(offreId).flatMap(solde -> {
            if (quantite != null && solde < quantite) {
                return Mono.error(ProblemException.unprocessable(
                                "Stock insuffisant : disponible " + solde + ", demandé " + quantite)
                        .violatedRule("STOCK_INSUFFISANT"));
            }
            return Mono.just(contexte.avec(ClesContexte.STOCK, solde));
        });
    }
}
