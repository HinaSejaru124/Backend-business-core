package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.enterprise.EntrepriseContrat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Contrat technique de l'application (paramètres de communication avec Business Core)")
public record EntrepriseContratResponse(
        UUID entrepriseId,
        @Schema(description = "Clé publique exposable au front terminal (non secrète)") String clePublique,
        String callbackUrl,
        String successUrl,
        String errorUrl,
        String cancelUrl,
        Instant mettreAJourLe
) {

    public static EntrepriseContratResponse depuis(EntrepriseContrat contrat) {
        return new EntrepriseContratResponse(
                contrat.entrepriseId(),
                contrat.clePublique(),
                contrat.callbackUrl(),
                contrat.successUrl(),
                contrat.errorUrl(),
                contrat.cancelUrl(),
                contrat.mettreAJourLe());
    }
}
