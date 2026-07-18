package com.yowyob.businesscore.adapter.in.rest.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demande de changement de plan")
public record UpgradePlanRequest(
        @Schema(description = "Code du plan cible", example = "PRO", allowableValues = {"FREE", "PRO", "ENTERPRISE"})
        @NotBlank(message = "le plan cible est obligatoire") String targetPlan,

        @Schema(description = "Numéro mobile money du payeur (Orange/MTN Money) qui règle l'upgrade",
                example = "692162333")
        @NotBlank(message = "le numéro mobile money du payeur est obligatoire") String payerReference
) {
}
