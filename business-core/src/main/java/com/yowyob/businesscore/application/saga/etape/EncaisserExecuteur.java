package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.internal.PorteMonnaieGenerique;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Étape {@code ENCAISSER} — enregistre l'échange de valeur via le port interne
 * {@link PorteMonnaieGenerique} (implémentation monétaire de départ). Lit le montant/devise posés par
 * l'étape de vente ; sans montant à encaisser, l'étape est neutre.
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
        return porteMonnaie.enregistrerEchange(montant, devise)
                .map(mouvementId -> contexte.avec(ClesContexte.MOUVEMENT_ID, mouvementId));
    }
}
