package com.yowyob.businesscore.domain.enterprise;

import com.yowyob.businesscore.application.error.ProblemException;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrat technique entre une Entreprise (application cliente) et Business Core : les paramètres de
 * communication déclarés par le développeur (URLs de callback, clé publique exposable au front terminal).
 *
 * <p>Distinct de la Configuration (brique 7, {@code ParametreConfig}) qui porte des réglages métier
 * (devise, taux, seuils) : ce contrat porte des paramètres d'intégration technique. Relation 1:1 avec
 * {@link Entreprise} (clé primaire = {@code entrepriseId}).
 */
public record EntrepriseContrat(
        UUID entrepriseId,
        UUID tenantId,
        String clePublique,
        String callbackUrl,
        String successUrl,
        String errorUrl,
        String cancelUrl,
        Instant mettreAJourLe
) {

    public EntrepriseContrat {
        if (entrepriseId == null)
            throw new IllegalArgumentException("entrepriseId est obligatoire");
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
    }

    /** Fabrique : contrat vierge à la création de l'entreprise (clé publique dérivée de l'id). */
    public static EntrepriseContrat vierge(UUID entrepriseId, UUID tenantId, Instant maintenant) {
        return new EntrepriseContrat(entrepriseId, tenantId, "pk_" + entrepriseId,
                null, null, null, null, maintenant);
    }

    /** Remplace les paramètres du contrat (les URLs absentes sont effacées, pas conservées). */
    public EntrepriseContrat avecParametres(String callbackUrl, String successUrl, String errorUrl,
                                            String cancelUrl, Instant maintenant) {
        verifierUrl(callbackUrl);
        verifierUrl(successUrl);
        verifierUrl(errorUrl);
        verifierUrl(cancelUrl);
        return new EntrepriseContrat(entrepriseId, tenantId, clePublique,
                callbackUrl, successUrl, errorUrl, cancelUrl, maintenant);
    }

    private static void verifierUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw ProblemException.badRequest("URL invalide (http/https requis) : " + url);
        }
    }

    public void verifierAppartenance(UUID autreTenantId) {
        if (!tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden("Ce contrat n'appartient pas à votre tenant.");
        }
    }
}
