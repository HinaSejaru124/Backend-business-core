package com.yowyob.businesscore.adapter.in.rest.actor;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Corps de {@code PUT /v1/businesses/{businessId}/actors/{actorId}}. */
public record ModifierActeurRequete(@NotNull UUID roleMetierId) {
}
