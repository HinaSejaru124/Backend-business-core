package com.yowyob.businesscore.adapter.in.rest.billing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demande de changement de plan")
public record UpgradePlanRequest(
        @Schema(description = "Code du plan cible", example = "PRO", allowableValues = {"FREE", "PRO", "ENTERPRISE"})
        @NotBlank(message = "le plan cible est obligatoire") String targetPlan,

        // Plus de @NotBlank : le numéro n'a de sens qu'en mobile money. Par carte, le porteur saisit ses
        // coordonnées sur la page sécurisée de la passerelle — jamais sur nos serveurs. L'obligation est
        // désormais portée par PlanService, qui ne l'exige que pour le mode MOBILE_MONEY.
        @Schema(description = "Numéro mobile money du payeur (Orange/MTN Money). Obligatoire uniquement si "
                + "paymentMethod vaut MOBILE_MONEY ; inutile pour un paiement par carte.",
                example = "692162333")
        String payerReference,

        // Optionnel, avec repli sur MOBILE_MONEY : les intégrations existantes qui n'envoient pas ce
        // champ continuent de fonctionner exactement comme avant.
        @Schema(description = "Mode de règlement : MOBILE_MONEY (défaut) ou CARD (carte bancaire).",
                example = "MOBILE_MONEY", allowableValues = {"MOBILE_MONEY", "CARD"})
        String paymentMethod
) {
}
