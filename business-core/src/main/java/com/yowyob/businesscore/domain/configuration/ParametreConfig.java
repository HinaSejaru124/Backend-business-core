/**
 * Brique 7 — Configuration (Dev 2).
 * Paramètres de réglage à deux niveaux (défaut Type / surcharge Entreprise).
 */
package com.yowyob.businesscore.domain.configuration;
import com.yowyob.businesscore.application.error.ProblemException;

import java.util.UUID;

/**
 * Brique 7 — Configuration.
 *
 * Paramètre de réglage d'un métier. Sépare "quoi faire" (Règle)
 * de "avec quelle valeur" (Configuration).
 *
 * Exemple :
 *   Règle       → "plafonner la remise"
 *   Config      → cle="remise_max", valeur="20"
 *
 * Deux niveaux :
 *   - Niveau TYPE    : défaut défini par le développeur sur une VersionType
 *   - Niveau ENTREPRISE : surcharge possible, sauf si verrouille=true
 *
 * Règle : un seul des deux FK est rempli à la fois
 *   (soit versionTypeId, soit entrepriseId — jamais les deux, jamais aucun).
 */
public record ParametreConfig(
        UUID id,
        UUID tenantId,       // isolation RLS
        UUID versionTypeId,  // rempli si niveau TYPE     (nullable)
        UUID entrepriseId,   // rempli si niveau ENTREPRISE (nullable)
        String cle,          // ex: "devise", "taux_tva", "remise_max"
        String valeur,       // ex: "XAF", "19.25", "20"
        boolean verrouille   // true = l'entreprise ne peut pas surcharger
) {

    // ─── Constructeur compact ─────────────────────────────────────────────
    public ParametreConfig {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (cle == null || cle.isBlank())
            throw new IllegalArgumentException("cle est obligatoire");
        if (valeur == null)
            throw new IllegalArgumentException("valeur est obligatoire (utiliser '' pour vide)");

        // Exactement un des deux FK doit être rempli
        boolean aVersionType  = versionTypeId != null;
        boolean aEntreprise   = entrepriseId  != null;
        if (aVersionType == aEntreprise) {
            throw new IllegalArgumentException(
                "Exactement un des deux FK doit être rempli : " +
                "versionTypeId XOR entrepriseId."
            );
        }

        cle = cle.trim().toLowerCase();
    }

    // ─── Fabriques ────────────────────────────────────────────────────────

    /**
     * Crée un paramètre au niveau TYPE (valeur par défaut).
     * Le développeur définit ce paramètre sur une version de son type métier.
     *
     * @param verrouille  si true, les entreprises ne pourront PAS surcharger ce paramètre
     */
    public static ParametreConfig pourType(UUID tenantId, UUID versionTypeId,String cle, String valeur,boolean verrouille) {
        return new ParametreConfig(
                UUID.randomUUID(),
                tenantId,
                versionTypeId,
                null,           // pas d'entreprise
                cle,
                valeur,
                verrouille
        );
    }

    /**
     * Crée un paramètre au niveau ENTREPRISE (surcharge locale).
     * Appelé uniquement si le paramètre correspondant au niveau TYPE
     * n'est PAS verrouillé — le use case vérifie ça avant d'appeler cette fabrique.
     */
    public static ParametreConfig pourEntreprise(UUID tenantId, UUID entrepriseId,String cle, String valeur) {
        return new ParametreConfig(
                UUID.randomUUID(),
                tenantId,
                null,           // pas de versionType
                entrepriseId,
                cle,
                valeur,
                false           // une surcharge d'entreprise n'est jamais verrouillée
        );
    }

    // ─── Gardes métier ────────────────────────────────────────────────────

    /**
     * Vérifie que ce paramètre peut être surchargé par une entreprise.
     * Lancé par le use case avant de créer un ParametreConfig niveau ENTREPRISE.
     */
    public void verifierSurchargeable() {
        if (this.verrouille) {
            throw ProblemException.conflict(
                "Le paramètre '" + this.cle + "' est verrouillé par le Type Métier " +
                "et ne peut pas être surchargé par une entreprise."
            ).violatedRule("PARAMETRE_VERROUILLE");
        }
    }

    /**
     * Vérifie l'appartenance au tenant.
     */
    public void verifierAppartenance(UUID autreTenantId) {
        if (!this.tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden(
                "Ce paramètre n'appartient pas à votre tenant."
            );
        }
    }

    /** Indique si ce paramètre est au niveau TYPE. */
    public boolean estNiveauType() {
        return this.versionTypeId != null;
    }

    /** Indique si ce paramètre est au niveau ENTREPRISE. */
    public boolean estNiveauEntreprise() {
        return this.entrepriseId != null;
    }

    /** Libellé lisible pour les logs. */
    public String libelle() {
        String niveau = estNiveauType() ? "type" : "entreprise";
        String verrou = verrouille ? " [verrouillé]" : "";
        return cle + "=" + valeur + " (" + niveau + ")" + verrou;
    }
}