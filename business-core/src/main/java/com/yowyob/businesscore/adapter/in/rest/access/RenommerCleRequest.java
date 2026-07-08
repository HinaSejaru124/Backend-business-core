package com.yowyob.businesscore.adapter.in.rest.access;

import jakarta.validation.constraints.NotBlank;

/** Corps de {@code PATCH /v1/api-keys/{id}}. */
public record RenommerCleRequest(@NotBlank(message = "le nom est obligatoire") String name) {
}
