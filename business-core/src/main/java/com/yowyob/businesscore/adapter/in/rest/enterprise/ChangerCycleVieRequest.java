package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.shared.CycleVie;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code PUT /v1/businesses/{businessId}/lifecycle} (aligné OpenAPI {@code LifecycleChange}).
 */
public record ChangerCycleVieRequest(@NotNull CycleVie cycleVie) {
}
