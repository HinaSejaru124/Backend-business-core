package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.shared.CycleVie;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Changement de cycle de vie de l'entreprise")
public record ChangerCycleVieRequest(
        @Schema(description = "Nouvel état", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDUE", "FERMEE"})
        @NotNull CycleVie cycleVie
) {
}
