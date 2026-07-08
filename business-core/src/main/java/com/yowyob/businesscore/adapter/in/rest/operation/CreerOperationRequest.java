package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.shared.Declencheur;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Déclaration d'une opération et de sa saga")
public record CreerOperationRequest(
        @Schema(description = "Nom unique de l'opération", example = "vente")
        @NotBlank String nom,
        @Schema(description = "Code du rôle déclencheur", example = "CAISSIER")
        String roleDeclencheur,
        @Schema(description = "Moment d'évaluation des règles", example = "AVANT_OPERATION")
        Declencheur declencheurRegles,
        @Schema(description = "Exécution différée (202 + trace)", example = "false")
        Boolean differe,
        @Schema(description = "Étapes de la saga, dans l'ordre")
        @NotEmpty @Valid List<EtapeRequest> etapes
) {
}
