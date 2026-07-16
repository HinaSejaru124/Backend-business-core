package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Étape {@code EMETTRE_EVENEMENT} — publie un événement métier (port {@link PublierEvenement}, bus
 * Kafka du socle) reflétant l'exécution. Utile au découplage et aux opérations différées.
 */
@Component
public class EmettreEvenementExecuteur implements ExecuteurDEtape {

    private static final String TYPE_EVENEMENT = "operation.executee";

    private final PublierEvenement publierEvenement;

    public EmettreEvenementExecuteur(PublierEvenement publierEvenement) {
        this.publierEvenement = publierEvenement;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.EMETTRE_EVENEMENT;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        Map<String, Object> charge = new HashMap<>();
        ajouter(charge, contexte, ClesContexte.OPERATION_NOM);
        ajouter(charge, contexte, ClesContexte.ENTREPRISE_ID);
        ajouter(charge, contexte, ClesContexte.TRANSACTION_KERNEL_ID);
        ajouter(charge, contexte, ClesContexte.MONTANT);
        ajouter(charge, contexte, ClesContexte.DEVISE);
        return publierEvenement.publier(TYPE_EVENEMENT, charge).thenReturn(contexte);
    }

    private void ajouter(Map<String, Object> charge, ContexteEtape contexte, String cle) {
        Object valeur = contexte.get(cle);
        if (valeur != null) {
            charge.put(cle, valeur.toString());
        }
    }
}
