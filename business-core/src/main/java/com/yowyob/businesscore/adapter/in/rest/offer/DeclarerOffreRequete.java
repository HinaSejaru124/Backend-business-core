package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

@Schema(description = "Déclaration ou modification d'une offre")
public record DeclarerOffreRequete(
        @Schema(description = "Nom de l'offre", example = "Forfait mensuel")
        @NotBlank String nom,
        @Schema(description = "Forme de tarification", example = "FIXE")
        @NotNull FormePrix formePrix,
        @Schema(description = "Prix (si applicable)", example = "15000.00")
        BigDecimal prix,
        @Schema(description = "Capacités activées", example = "[\"STOCK\",\"FACTURATION\"]")
        Set<TypeCapacite> capacites
) {
}
