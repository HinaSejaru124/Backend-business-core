package com.yowyob.businesscore.domain.enterprise;

import com.yowyob.businesscore.application.error.ProblemException;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Fiche produit d'une Application : identité et branding déclarés par le développeur (description,
 * logo, couleur, support, site web, environnement) — la vitrine d'une plateforme SaaS, à distinguer :
 * <ul>
 *   <li>du {@link EntrepriseContrat} (paramètres techniques d'intégration : callback, clé publique) ;</li>
 *   <li>de la Configuration (brique 7, {@code ParametreConfig} — réglages métier).</li>
 * </ul>
 * Relation 1:1 avec {@link Entreprise} (clé primaire = {@code entrepriseId}).
 */
public record EntrepriseProfil(
        UUID entrepriseId,
        UUID tenantId,
        String description,
        String logoUrl,
        String couleur,
        String supportEmail,
        String siteWebUrl,
        EnvironnementApplication environnement,
        Instant mettreAJourLe
) {
    private static final Pattern COULEUR_HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EntrepriseProfil {
        if (entrepriseId == null)
            throw new IllegalArgumentException("entrepriseId est obligatoire");
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        environnement = environnement == null ? EnvironnementApplication.DEVELOPPEMENT : environnement;
    }

    /** Fabrique : fiche vierge à la création de l'entreprise (environnement par défaut : développement). */
    public static EntrepriseProfil vierge(UUID entrepriseId, UUID tenantId, Instant maintenant) {
        return new EntrepriseProfil(entrepriseId, tenantId, null, null, null, null, null,
                EnvironnementApplication.DEVELOPPEMENT, maintenant);
    }

    /** Remplace les informations générales (les champs absents sont effacés, pas conservés). */
    public EntrepriseProfil avecInformations(String description, String logoUrl, String couleur,
                                             String supportEmail, String siteWebUrl,
                                             EnvironnementApplication environnement, Instant maintenant) {
        verifierUrl(logoUrl);
        verifierUrl(siteWebUrl);
        verifierCouleur(couleur);
        verifierEmail(supportEmail);
        return new EntrepriseProfil(entrepriseId, tenantId, description, logoUrl, couleur,
                supportEmail, siteWebUrl, environnement, maintenant);
    }

    private static void verifierUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw ProblemException.badRequest("URL invalide (http/https requis) : " + url);
        }
    }

    private static void verifierCouleur(String couleur) {
        if (couleur == null || couleur.isBlank()) {
            return;
        }
        if (!COULEUR_HEX.matcher(couleur).matches()) {
            throw ProblemException.badRequest("Couleur invalide (format hexadécimal #RRGGBB attendu) : " + couleur);
        }
    }

    private static void verifierEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            throw ProblemException.badRequest("Adresse e-mail de support invalide : " + email);
        }
    }

    public void verifierAppartenance(UUID autreTenantId) {
        if (!tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden("Cette fiche produit n'appartient pas à votre tenant.");
        }
    }
}
