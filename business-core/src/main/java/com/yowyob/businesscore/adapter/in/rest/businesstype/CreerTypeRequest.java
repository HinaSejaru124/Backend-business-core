package com.yowyob.businesscore.adapter.in.rest.businesstype;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de POST /v1/business-types.
 * domainCode et domainNom sont optionnels —
 * si fournis, le BusinessDomain kernel est résolu ou créé.
 */
public record CreerTypeRequest(
        @NotBlank(message = "code est obligatoire") String code,
        @NotBlank(message = "nom est obligatoire")  String nom,
        String domainCode,
        String domainNom
) {}
