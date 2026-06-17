package com.yowyob.businesscore.adapter.in.rest.businesstype;

import jakarta.validation.constraints.NotBlank;

/** Corps de POST /v1/business-types/{typeId}/versions/{versionId}/config */
public record DefinirParametreRequest(
        @NotBlank(message = "cle est obligatoire")   String cle,
        @NotBlank(message = "valeur est obligatoire") String valeur,
        boolean verrouille
) {}
