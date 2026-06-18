package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.operation.OperationAvecEtapes;

import java.util.List;
import java.util.UUID;

/**
 * Réponse d'une opération déclarée (aligné OpenAPI {@code Operation}).
 */
public record OperationResponse(
        UUID id,
        String nom,
        String roleDeclencheur,
        String declencheurRegles,
        boolean differe,
        List<EtapeResponse> etapes
) {

    public static OperationResponse depuis(OperationAvecEtapes operation) {
        return new OperationResponse(
                operation.definition().id(),
                operation.definition().nom(),
                operation.definition().roleDeclencheur(),
                operation.definition().declencheurRegles().name(),
                operation.definition().differe(),
                operation.etapes().stream().map(EtapeResponse::depuis).toList());
    }

    public record EtapeResponse(int ordre, String typeEtape) {
        public static EtapeResponse depuis(com.yowyob.businesscore.domain.operation.EtapeOperation etape) {
            return new EtapeResponse(etape.ordre(), etape.typeEtape().name());
        }
    }
}
