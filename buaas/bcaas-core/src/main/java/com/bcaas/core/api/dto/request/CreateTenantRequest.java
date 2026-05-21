package com.bcaas.core.api.dto.request;

import com.bcaas.core.tenant.domain.model.TenantPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de création d'un tenant.
 * Valide les données avant qu'elles n'atteignent le domaine.
 * Le domaine ne connaît pas ce DTO.
 */
public record CreateTenantRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String name,

        @NotBlank(message = "Le slug est obligatoire")
        @Pattern(regexp = "^[a-z0-9-]{2,50}$",
                message = "Le slug doit contenir uniquement des lettres minuscules, chiffres et tirets")
        String slug,

        TenantPlan plan,

        String defaultLocale,

        String defaultCurrency,

        String timezone
) {
    public CreateTenantRequest {
        plan = plan != null ? plan : TenantPlan.FREE;
        defaultLocale = defaultLocale != null ? defaultLocale : "fr";
        defaultCurrency = defaultCurrency != null ? defaultCurrency : "XAF";
        timezone = timezone != null ? timezone : "Africa/Douala";
    }
}
