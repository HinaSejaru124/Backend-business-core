package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.shared.CycleVie;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — provisionnement et cycle de vie d'une organisation kernel.
 *
 * <p>Chaîne auto lors de {@code POST /v1/businesses} (sans {@code organizationId}) :
 * actor → organisation → approbation → services → agence.
 */
public interface PersisterEntreprise {

    /** Résout le business actor puis crée l'organisation ; renvoie les deux références. */
    Mono<OrganisationProvisionnee> creerOrganisation(String nom);

    /**
     * Résout (ou onboarde) le business actor courant, sans créer d'organisation — utilisé quand une
     * Application se rattache à une organisation kernel déjà existante ({@code organizationId} fourni
     * à la création), au lieu du provisionnement complet ({@link #creerOrganisation}).
     */
    Mono<UUID> resoudreBusinessActorCourant(String nom);

    Mono<UUID> creerAgence(UUID organizationId, String nom);

    /** Agence principale d'une organisation : GET /api/organizations/{orgId}/agencies (siège, sinon 1ʳᵉ). */
    Mono<UUID> trouverAgencePrincipale(UUID organizationId);

    /**
     * Applique la transition de cycle de vie côté kernel : {@code suspend} (SUSPENDUE), {@code close}
     * (FERMEE), {@code reopen} (réactivation → ACTIVE). POST /api/organizations/{orgId}/{action}.
     */
    Mono<Void> changerCycleVieKernel(UUID organizationId, CycleVie cible);

    /**
     * Première approbation de gouvernance kernel ({@code POST /api/organizations/{orgId}/approve}).
     * Distinct de {@code reopen} (réactivation après suspend/close).
     */
    Mono<Void> approuverOrganisation(UUID organizationId, String reason);

    /** Souscrit les services kernel requis pour sales, billing, stock et comptabilité. */
    Mono<Void> souscrireServices(UUID organizationId);
}
