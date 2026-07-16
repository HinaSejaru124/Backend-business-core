package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.internal.PorteMonnaieGenerique;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Étape {@code ENCAISSER} — enregistre l'échange de valeur via le port interne
 * {@link PorteMonnaieGenerique} (implémentation monétaire de départ). Lit le montant/devise et le
 * billId cashier posés par l'étape de vente ; sans montant à encaisser, l'étape est neutre.
 */
@Component
public class EncaisserExecuteur implements ExecuteurDEtape {

    private static final String DEVISE_DEFAUT = "XAF";

    private final PorteMonnaieGenerique porteMonnaie;

    public EncaisserExecuteur(PorteMonnaieGenerique porteMonnaie) {
        this.porteMonnaie = porteMonnaie;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.ENCAISSER;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        BigDecimal montant = Valeurs.versDecimal(contexte.get(ClesContexte.MONTANT));
        if (montant == null) {
            // Aucun montant à encaisser (ex. étape isolée) : neutre.
            return Mono.just(contexte);
        }
        String devise = Valeurs.versTexteOuDefaut(contexte.get(ClesContexte.DEVISE), DEVISE_DEFAUT);
        UUID billId = Valeurs.versUuid(contexte.get(ClesContexte.TRANSACTION_KERNEL_ID));
        if (billId == null) {
            // Le kernel encaisse un bill cashier : sans bill produit par la vente, on ne peut pas encaisser.
            return Mono.error(ProblemException.unprocessable(
                    "Encaissement impossible : aucun bill cashier lié à la vente."));
        }
        UUID businessId = Valeurs.versUuid(contexte.get(ClesContexte.ENTREPRISE_ID));
        return porteMonnaie.enregistrerEchange(billId, montant, devise, businessId)
                .map(paiementId -> contexte.avec(ClesContexte.MOUVEMENT_ID, paiementId));
    }
}
