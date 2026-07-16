package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps optionnel d'approbation kernel (défaut : « Approbation initiale »)")
public record ApprouverEntrepriseRequest(
        @Schema(description = "Motif transmis au kernel", example = "Validation conformité")
        String reason
) {
}
