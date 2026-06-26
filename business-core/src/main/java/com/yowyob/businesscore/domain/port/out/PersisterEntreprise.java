package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.shared.CycleVie;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — crée l'organisation réelle d'une entreprise.
 * Mappe : POST /api/organizations ; POST /api/organizations/{orgId}/agencies.
 */
public interface PersisterEntreprise {

    /** Onboarding du business actor propriétaire puis création de l'organisation ; renvoie les deux réfs. */
    Mono<OrganisationProvisionnee> creerOrganisation(String nom);

    Mono<UUID> creerAgence(UUID organizationId, String nom);

    /** Agence principale d'une organisation : GET /api/organizations/{orgId}/agencies (siège, sinon 1ʳᵉ). */
    Mono<UUID> trouverAgencePrincipale(UUID organizationId);

    /**
     * Applique la transition de cycle de vie côté kernel : {@code suspend} (SUSPENDUE), {@code close}
     * (FERMEE), {@code reopen} (réactivation → ACTIVE). POST /api/organizations/{orgId}/{action}.
     */
    Mono<Void> changerCycleVieKernel(UUID organizationId, CycleVie cible);
}
