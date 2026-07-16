package com.yowyob.businesscore.adapter.in.rest.billing;

import com.yowyob.businesscore.application.billing.PlanService.ResultatUpgrade;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat d'un changement de plan")
public record UpgradeResponse(
        @Schema(description = "Plan effectif après l'opération", example = "PRO") String plan,
        @Schema(description = "Issue du paiement", example = "CONFIRME",
                allowableValues = {"CONFIRME", "EN_ATTENTE"}) String statut,
        @Schema(description = "URL de paiement à finaliser (cas EN_ATTENTE, sinon null)") String urlPaiement,
        @Schema(description = "Référence de paiement") String reference
) {

    public static UpgradeResponse depuis(ResultatUpgrade r) {
        return new UpgradeResponse(r.plan(), r.statut(), r.urlPaiement(), r.reference());
    }
}
