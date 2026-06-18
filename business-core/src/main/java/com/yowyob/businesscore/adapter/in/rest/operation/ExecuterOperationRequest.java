package com.yowyob.businesscore.adapter.in.rest.operation;

import java.util.Map;

/**
 * Corps de {@code POST /v1/businesses/{businessId}/operations/{name}:execute} (aligné OpenAPI
 * {@code ExecuteOperation}). {@code parametres} porte les entrées métier de l'opération
 * (ex. {@code offreId}, {@code quantite}, {@code beneficiaireId}).
 */
public record ExecuterOperationRequest(Map<String, Object> parametres) {

    public Map<String, Object> parametresOuVide() {
        return parametres == null ? Map.of() : parametres;
    }
}
