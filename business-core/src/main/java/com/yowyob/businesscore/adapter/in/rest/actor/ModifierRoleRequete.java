package com.yowyob.businesscore.adapter.in.rest.actor;

import jakarta.validation.constraints.NotBlank;

/** Corps de {@code PUT /v1/business-types/{typeId}/versions/{n}/roles/{roleId}}. */
public record ModifierRoleRequete(@NotBlank String code) {
}
