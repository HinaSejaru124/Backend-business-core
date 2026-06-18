package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.shared.Declencheur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Corps de {@code POST /v1/business-types/{typeId}/versions/{n}/operations} (aligné OpenAPI
 * {@code CreateOperation}). {@code declencheurRegles} et {@code differe} sont des extensions optionnelles
 * (par défaut : AVANT_OPERATION, immédiat).
 */
public record CreerOperationRequest(
        @NotBlank String nom,
        String roleDeclencheur,
        Declencheur declencheurRegles,
        Boolean differe,
        @NotEmpty @Valid List<EtapeRequest> etapes
) {
}
