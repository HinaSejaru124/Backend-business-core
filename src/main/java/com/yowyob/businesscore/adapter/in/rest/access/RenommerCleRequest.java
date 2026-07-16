package com.yowyob.businesscore.adapter.in.rest.access;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Renommage d'une clé API")
public record RenommerCleRequest(
        @Schema(example = "Staging") @NotBlank(message = "le nom est obligatoire") String name
) {
}
