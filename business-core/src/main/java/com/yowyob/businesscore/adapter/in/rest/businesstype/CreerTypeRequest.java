package com.yowyob.businesscore.adapter.in.rest.businesstype;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Création d'un type métier en BROUILLON")
public record CreerTypeRequest(
        @Schema(description = "Code unique du type", example = "RETAIL")
        @NotBlank(message = "code est obligatoire") String code,
        @Schema(description = "Libellé", example = "Commerce de détail")
        @NotBlank(message = "nom est obligatoire") String nom,
        @Schema(description = "Code domaine kernel (optionnel)", example = "COMMERCE")
        String domainCode,
        @Schema(description = "Nom du domaine kernel (optionnel)", example = "Commerce")
        String domainNom
) {}
