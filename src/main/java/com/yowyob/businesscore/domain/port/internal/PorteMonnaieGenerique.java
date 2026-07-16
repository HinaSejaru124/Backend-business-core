package com.yowyob.businesscore.domain.port.internal;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port interne (stratégie) — réalise un échange de valeur générique.
 * Le monétaire est l'implémentation de départ ; troc/don pourront suivre sans toucher au domaine.
 *
 * <p>L'échange règle un <b>bill cashier</b> ({@code billId}) produit par la vente : l'implémentation
 * monétaire enregistre le paiement via {@code POST /api/bills/pay}. {@code businessId} permet à
 * l'adapter de résoudre la caisse ({@code registerId}) et l'organisation via le
 * {@code ResolveurContexteKernel}. Renvoie l'identifiant du paiement créé.
 */
public interface PorteMonnaieGenerique {

    Mono<UUID> enregistrerEchange(UUID billId, BigDecimal montant, String devise, UUID businessId);
}
