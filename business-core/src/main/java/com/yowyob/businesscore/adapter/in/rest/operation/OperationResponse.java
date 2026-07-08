package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.operation.OperationAvecEtapes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Opération déclarée sur une version de type")
public record OperationResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "vente") String nom,
        @Schema(example = "CAISSIER") String roleDeclencheur,
        @Schema(example = "AVANT_OPERATION") String declencheurRegles,
        @Schema(example = "false") boolean differe,
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

    @Schema(description = "Étape de saga")
    public record EtapeResponse(
            @Schema(example = "0") int ordre,
            @Schema(example = "ENREGISTRER_VENTE") String typeEtape
    ) {
        public static EtapeResponse depuis(com.yowyob.businesscore.domain.operation.EtapeOperation etape) {
            return new EtapeResponse(etape.ordre(), etape.typeEtape().name());
        }
    }
}
