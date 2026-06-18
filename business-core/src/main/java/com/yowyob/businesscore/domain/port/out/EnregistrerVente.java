package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — façade financière unifiée d'un échange de valeur.
 * Mappe (plusieurs appels) : POST /api/sales/orders ; POST /api/sales/orders/{id}/confirm ;
 * GET /api/cashier/bills/{id}. Un port, plusieurs cores : c'est la façade.
 *
 * <p>{@link #annuler(UUID)} est le point de compensation : le kernel n'ayant pas de moteur Saga, le
 * {@code MoteurOperation} annule la vente localement via {@code POST /api/sales/orders/{id}/cancel}.
 */
public interface EnregistrerVente {

    Mono<VenteEnregistree> enregistrer(UUID offreId, int quantite, UUID beneficiaireId);

    /** Annule une commande de vente déjà créée (compensation). Best-effort, idempotent côté kernel. */
    Mono<Void> annuler(UUID commandeId);
}
