package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.EtapeCompensable;
import com.yowyob.businesscore.domain.port.out.EnregistrerVente;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Étape {@code ENREGISTRER_VENTE} — déclenche la façade financière {@link EnregistrerVente}
 * (sales + cashier) et pose dans le contexte l'id de commande (point de compensation), l'id de
 * facture, le montant et la devise produits.
 *
 * <p>Étape <b>compensable</b> : si une étape ultérieure échoue, le moteur rappelle {@link #compenser}
 * qui annule la commande de vente sur le kernel ({@code /cancel}).
 */
@Component
public class EnregistrerVenteExecuteur implements EtapeCompensable {

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
        UUID businessId = Valeurs.versUuid(contexte.get(ClesContexte.ENTREPRISE_ID));

        return enregistrerVente.enregistrer(offreId, quantite, beneficiaireId, businessId)
                .map(vente -> contexte
                        .avec(ClesContexte.COMMANDE_ID, vente.commandeId())
                        // billId cashier : référence de transaction + cible de l'étape ENCAISSER.
                        .avec(ClesContexte.TRANSACTION_KERNEL_ID, vente.billId())
                        .avec(ClesContexte.MONTANT, vente.montant())
                        .avec(ClesContexte.DEVISE, vente.devise()));
    }

    @Override
    public Mono<Void> compenser(ContexteEtape contexte) {
        UUID commandeId = Valeurs.versUuid(contexte.get(ClesContexte.COMMANDE_ID));
        if (commandeId == null) {
            // La vente n'a pas été engagée (pas de commande créée) : rien à annuler.
            return Mono.empty();
        }
        return enregistrerVente.annuler(commandeId);
    }
}
