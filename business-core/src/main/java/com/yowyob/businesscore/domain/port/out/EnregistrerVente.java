package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — façade financière unifiée d'un échange de valeur.
 * Mappe (plusieurs appels) : POST /api/sales/orders ; POST /api/sales/orders/{id}/confirm ;
 * GET /api/cashier/bills/{id}. Un port, plusieurs cores : c'est la façade.
 */
public interface EnregistrerVente {

    Mono<VenteEnregistree> enregistrer(UUID offreId, int quantite, UUID beneficiaireId);
}
