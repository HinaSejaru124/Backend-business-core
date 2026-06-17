package com.yowyob.businesscore.domain.businesstype;

import com.yowyob.businesscore.application.error.ProblemException;

import java.time.Instant;
import java.util.UUID;

/**
 * Version immuable d'un Type Métier.
 *
 * Analogie : une version d'application sur un store.
 * - Une fois publiée, la version est IMMUABLE (RG-03).
 * - Chaque Entreprise mémorise la version avec laquelle elle a été créée
 *   et y reste épinglée jusqu'à migration explicite.
 * - Pour "modifier" un type publié → on crée une NOUVELLE VersionType.
 *
 * Numérotation : 1, 2, 3... auto-incrémentée par le use case.
 */
public record VersionType(
        UUID id,
        UUID tenantId,       // hérité du TypeMetier (isolation RLS)
        UUID typeMetierId,   // FK vers le TypeMetier parent
        int numero,          // numéro de version (1, 2, 3...)
        boolean immuable,    // true une fois publiée — jamais repassé à false
        Instant publieeLe    // null si encore en brouillon de version
) {

    // ─── Constructeur compact — validations ───────────────────────────────
    public VersionType {
        if (typeMetierId == null)
            throw new IllegalArgumentException("typeMetierId est obligatoire");
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (numero < 1)
            throw new IllegalArgumentException("Le numéro de version doit être >= 1");
    }

    // ─── Fabrique statique ────────────────────────────────────────────────

    /**
     * Crée une nouvelle version (pas encore publiée = pas encore immuable).
     *
     * @param typeMetierId  l'ID du TypeMetier parent
     * @param tenantId      le tenant propriétaire (copié du TypeMetier)
     * @param numero        le prochain numéro (dernierNumero + 1, calculé par le use case)
     */
    public static VersionType creer(UUID typeMetierId, UUID tenantId, int numero) {
        return new VersionType(
                UUID.randomUUID(),
                tenantId,
                typeMetierId,
                numero,
                false,   // pas encore immuable
                null     // pas encore publiée
        );
    }

    // ─── Transition : publication ─────────────────────────────────────────

    /**
     * Publie cette version : la rend immuable et horodate la publication.
     *
     * Règle RG-03 : une version publiée ne peut JAMAIS être modifiée.
     * Retourne une NOUVELLE instance (records immuables).
     */
    public VersionType publier(Instant maintenant) {
        if (this.immuable) {
            throw ProblemException.conflict(
                "La version " + this.numero + " est déjà publiée et immuable."
            ).violatedRule("RG-03");
        }
        return new VersionType(
                id,
                tenantId,
                typeMetierId,
                numero,
                true,        // immuable = true, définitif
                maintenant   // horodatage de publication
        );
    }

    // ─── Gardes métier ────────────────────────────────────────────────────

    /**
     * Vérifie que cette version peut encore être modifiée.
     * Utilisé avant toute tentative de mise à jour d'une version.
     */
    public void verifierModifiable() {
        if (this.immuable) {
            throw ProblemException.conflict(
                "La version " + this.numero + " est publiée et ne peut plus être modifiée (RG-03)."
            ).violatedRule("RG-03");
        }
    }

    /**
     * Vérifie l'appartenance au tenant.
     */
    public void verifierAppartenance(UUID autreTenantId) {
        if (!this.tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden(
                "Cette version n'appartient pas à votre tenant."
            );
        }
    }

    /**
     * Libellé lisible pour les logs et les messages d'erreur.
     */
    public String libelle() {
        return "v" + this.numero + (this.immuable ? " [publiée]" : " [brouillon]");
    }
}
