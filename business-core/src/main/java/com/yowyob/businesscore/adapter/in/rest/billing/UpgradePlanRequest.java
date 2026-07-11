package com.yowyob.businesscore.adapter.in.rest.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demande de changement de plan")
public record UpgradePlanRequest(
        @Schema(description = "Code du plan cible", example = "PRO", allowableValues = {"FREE", "PRO", "ENTERPRISE"})
        @NotBlank(message = "le plan cible est obligatoire") String targetPlan
) {
}
