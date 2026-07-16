package com.yowyob.businesscore.adapter.in.rest.billing;

import com.yowyob.businesscore.application.billing.BillingProperties.PlanDef;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Plan du catalogue")
public record PlanResponse(
        @Schema(example = "PRO") String code,
        @Schema(description = "Quota mensuel de requêtes (-1 = illimité)", example = "100000") long quotaMensuel,
        @Schema(example = "false") boolean illimite,
        @Schema(description = "Prix d'affichage (paiement réel géré par Kernel Core)", example = "0") long prixMensuel,
        @Schema(example = "XAF") String devise
) {

    public static PlanResponse depuis(String code, PlanDef def) {
        return new PlanResponse(
                code,
                def.illimite() ? -1 : def.quotaMensuel(),
                def.illimite(),
                def.prixMensuel(),
                def.devise());
    }
}
