package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Corps de {@code POST /v1/business-types/{typeId}/versions/{n}/offers}. La version cible provient de
 * l'URL ({@code typeId} + {@code n}) ; elle n'est jamais reprise du payload.
 */
public record DeclarerOffreRequete(
        @NotBlank String nom,
        @NotNull FormePrix formePrix,
        BigDecimal prix,
        Set<TypeCapacite> capacites) {
}
