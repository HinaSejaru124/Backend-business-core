package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.shared.TypeEtape;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Étape d'une saga d'opération")
public record EtapeRequest(
        @Schema(description = "Ordre d'exécution (0-based)", example = "0")
        @NotNull @PositiveOrZero Integer ordre,
        @Schema(description = "Type d'étape du catalogue", example = "ENREGISTRER_VENTE")
        @NotNull TypeEtape typeEtape
) {
}
