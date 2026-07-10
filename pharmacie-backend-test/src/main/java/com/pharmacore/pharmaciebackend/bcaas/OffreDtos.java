package com.pharmacore.pharmaciebackend.bcaas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTOs alignés exactement sur {@code DeclarerOffreRequete}/{@code OffreReponse} du backend
 * Business Core (adapter/in/rest/offer) — aucun champ inventé.
 */
public final class OffreDtos {

    private OffreDtos() {}

    public record DeclarerOffreRequete(String nom, String formePrix, BigDecimal prix, Set<String> capacites) {
    }

    public record OffreReponse(UUID id, UUID versionTypeId, String nom, String formePrix,
                               BigDecimal prix, List<CapaciteReponse> capacites) {
    }

    public record CapaciteReponse(UUID id, String type, boolean active) {
    }
}
