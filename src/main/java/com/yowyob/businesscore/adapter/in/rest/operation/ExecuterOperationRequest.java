package com.yowyob.businesscore.adapter.in.rest.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Paramètres d'exécution d'une opération métier")
public record ExecuterOperationRequest(
        @Schema(description = "Entrées métier libres",
                example = "{\"offreId\":\"00000000-0000-0000-0000-000000000000\",\"quantite\":2}")
        Map<String, Object> parametres
) {

    public Map<String, Object> parametresOuVide() {
        return parametres == null ? Map.of() : parametres;
    }
}
