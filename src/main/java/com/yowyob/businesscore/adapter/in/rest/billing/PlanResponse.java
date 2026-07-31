package com.yowyob.businesscore.adapter.in.rest.billing;

import com.yowyob.businesscore.application.billing.BillingProperties.PlanDef;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Plan du catalogue")
public record PlanResponse(
        @Schema(example = "PRO") String code,
        @Schema(description = "Nom affiché du forfait (défaut = code)", example = "Professionnel") String libelle,
        @Schema(description = "Quota mensuel de requêtes (-1 = illimité)", example = "100000") long quotaMensuel,
        @Schema(example = "false") boolean illimite,
        @Schema(description = "Prix d'affichage (paiement réel géré par Kernel Core)", example = "0") long prixMensuel,
        @Schema(example = "XAF") String devise,
        // Exposé pour que la console puisse présenter le forfait sous forme de liste d'avantages
        // (requêtes / prix / applications) plutôt que du seul quota. La valeur vient du paramétrage
        // administrateur (businesscore.billing.plans.*.applications-max).
        @Schema(description = "Nombre d'applications autorisées (-1 = illimité)", example = "3")
        long applicationsMax,
        @Schema(example = "false") boolean applicationsIllimitees
) {

    public static PlanResponse depuis(String code, PlanDef def) {
        return new PlanResponse(
                code,
                def.libelle() == null || def.libelle().isBlank() ? code : def.libelle(),
                def.illimite() ? -1 : def.quotaMensuel(),
                def.illimite(),
                def.prixMensuel(),
                def.devise(),
                def.illimiteApplications() ? -1 : def.applicationsMax(),
                def.illimiteApplications());
    }
}
