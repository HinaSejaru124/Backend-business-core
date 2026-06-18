package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.out.EnregistrerVente;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Étape {@code ENREGISTRER_VENTE} — déclenche la façade financière {@link EnregistrerVente}
 * (sales + cashier) et pose dans le contexte la référence de transaction kernel (point de
 * compensation), le montant et la devise produits.
 */
@Component
public class EnregistrerVenteExecuteur implements ExecuteurDEtape {

    private final EnregistrerVente enregistrerVente;

    public EnregistrerVenteExecuteur(EnregistrerVente enregistrerVente) {
        this.enregistrerVente = enregistrerVente;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.ENREGISTRER_VENTE;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        UUID offreId = Valeurs.versUuid(contexte.get(ClesContexte.OFFRE_ID));
        if (offreId == null) {
            return Mono.error(ProblemException.badRequest(
                    "offreId est requis pour l'étape ENREGISTRER_VENTE."));
        }
        int quantite = Valeurs.versEntierOuDefaut(contexte.get(ClesContexte.QUANTITE), 1);
        UUID beneficiaireId = Valeurs.versUuid(contexte.get(ClesContexte.BENEFICIAIRE_ID));

        return enregistrerVente.enregistrer(offreId, quantite, beneficiaireId)
                .map(vente -> contexte
                        .avec(ClesContexte.TRANSACTION_KERNEL_ID, vente.transactionKernelId())
                        .avec(ClesContexte.MONTANT, vente.montant())
                        .avec(ClesContexte.DEVISE, vente.devise()));
    }
}
