package com.yowyob.businesscore.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Demande de création d'un produit kernel à partir d'une offre, pour une organisation donnée.
 *
 * <p>Mappe {@code CreateProductRequest} : {@code organizationId}, {@code sku}, {@code name},
 * {@code familyCode}, {@code variantLabel}, {@code unitPrice}, {@code currency} sont obligatoires côté
 * kernel. Les identifiants dérivés ({@code sku}, {@code familyCode}, {@code variantLabel}) ne sont
 * jamais saisis : ils sont fabriqués de façon déterministe par le socle depuis la {@code DefinitionOffre}.
 *
 * @param organizationId organisation kernel propriétaire du produit.
 * @param sku            référence dérivée déterministe (ex. {@code OFFRE-{slug}-{id8}}).
 * @param nom            libellé du produit (= nom de l'offre).
 * @param familyCode     famille produit (défaut {@code GENERAL}).
 * @param variantLabel   variante (défaut {@code STANDARD}).
 * @param unitPrice      prix unitaire (0 si offre sans prix fixe — voir {@code priceless}).
 * @param currency       devise résolue.
 * @param stockable      true si l'offre est STOCKABLE (gestion de stock kernel).
 * @param priceless      true si l'offre n'a pas de prix fixe (SUR_DEVIS / GRATUIT).
 */
public record CreationProduit(UUID organizationId, String sku, String nom, String familyCode,
                              String variantLabel, BigDecimal unitPrice, String currency,
                              boolean stockable, boolean priceless) {
}
