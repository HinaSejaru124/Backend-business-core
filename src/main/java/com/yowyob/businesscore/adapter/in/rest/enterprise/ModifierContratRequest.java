package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paramètres de communication du contrat technique d'une application")
public record ModifierContratRequest(
        @Schema(description = "URL de callback (notifications d'opération)",
                example = "https://mon-app.example.com/webhooks/business-core") String callbackUrl,
        @Schema(description = "URL de redirection succès", example = "https://mon-app.example.com/success")
        String successUrl,
        @Schema(description = "URL de redirection erreur", example = "https://mon-app.example.com/error")
        String errorUrl,
        @Schema(description = "URL de redirection annulation", example = "https://mon-app.example.com/cancel")
        String cancelUrl
) {
}
