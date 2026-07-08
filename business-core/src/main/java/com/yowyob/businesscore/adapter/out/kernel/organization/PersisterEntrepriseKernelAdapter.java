package com.yowyob.businesscore.adapter.out.kernel.organization;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.OrganisationProvisionnee;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.shared.CycleVie;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Crée l'organisation réelle d'une entreprise dans le kernel.
 *
 * <p>Le kernel exige une <b>chaîne de provisionnement</b> (cf. {@code CreateOrganizationRequest}) : une
 * organisation appartient à un <b>business actor</b> (propriétaire). On enchaîne donc :
 * <ol>
 *   <li>{@code POST /api/actors/onboarding} — crée le business actor propriétaire (rôle OWNER),</li>
 *   <li>{@code POST /api/organizations} — crée l'organisation avec le payload complet exigé
 *       ({@code businessActorId}, {@code code}, {@code service}, {@code shortName}, {@code longName}),</li>
 *   <li>{@code POST /api/organizations/{orgId}/agencies} — crée l'agence principale.</li>
 * </ol>
 * Le {@code service} et le rôle proviennent de {@link KernelProperties} (à aligner sur le catalogue
 * kernel). Tous les appels passent par {@link KernelClient} (auth automatique).
 */
@Component
public class PersisterEntrepriseKernelAdapter implements PersisterEntreprise {

    private final KernelClient kernel;
    private final KernelProperties properties;

    public PersisterEntrepriseKernelAdapter(KernelClient kernel, KernelProperties properties) {
        this.kernel = kernel;
        this.properties = properties;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<OrganisationProvisionnee> creerOrganisation(String nom) {
        return creerBusinessActor(nom)
                .flatMap(businessActorId -> {
                    CreerOrganisationRequest requete = new CreerOrganisationRequest(
                            businessActorId.toString(),
                            genererCode(nom),
                            properties.organizationService(),
                            nom,
                            nom);
                    return kernel.post("/api/organizations", requete, KernelId.class)
                            .map(org -> new OrganisationProvisionnee(businessActorId, org.id()));
                });
    }

    @Override
public Mono<UUID> creerAgence(UUID organizationId, String nom) {
    String code = nom == null ? "AGENCE" 
        : nom.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "").substring(0, Math.min(nom.length(), 8))
          + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    
    return kernel.postForOrganization(
                    "/api/organizations/" + organizationId + "/agencies",
                    Map.of("code", code, "name", nom != null ? nom : "Agence principale"),
                    KernelId.class,
                    organizationId)
            .map(kernelId -> kernelId.id());
}

    @Override
    public Mono<Void> changerCycleVieKernel(UUID organizationId, CycleVie cible) {
        String action = switch (cible) {
            case SUSPENDUE -> "suspend";
            case FERMEE -> "close";
            case ACTIVE -> "reopen";
        };
        return kernel.postForOrganization(
                        "/api/organizations/" + organizationId + "/" + action,
                        null, Void.class, organizationId)
                .then();
    }

    @Override
    public Mono<UUID> trouverAgencePrincipale(UUID organizationId) {
        return kernel.getForOrganization(
                        "/api/organizations/" + organizationId + "/agencies",
                        AgenceItem[].class,
                        organizationId)
                .flatMap(agences -> {
                    for (AgenceItem a : agences) {
                        if (a.isHeadquarter()) {
                            return Mono.just(a.id());
                        }
                    }
                    return agences.length > 0 ? Mono.just(agences[0].id()) : Mono.empty();
                });
    }

    @Override
    public Mono<Void> souscrireServices(UUID organizationId) {
        return Flux.fromIterable(properties.organizationServices())
                .concatMap(code -> kernel.postForOrganization(
                        "/api/organizations/" + organizationId + "/services",
                        Map.of("serviceCode", code),
                        Void.class,
                        organizationId))
                .then();
    }

    /** Crée le business actor propriétaire (prérequis à la création d'organisation). */
    private Mono<UUID> creerBusinessActor(String nom) {
    CreerBusinessActorRequest requete = new CreerBusinessActorRequest(
            nom, properties.businessActorRole(), true);

    return kernel.post("/api/actors/onboarding", requete, Map.class)
            .map(this::extraireId)
            .onErrorResume(e -> {
                boolean est409 = e instanceof WebClientResponseException ex
                        && ex.getStatusCode().value() == 409;

                boolean estRetryExhausted409 = reactor.core.Exceptions.isRetryExhausted(e)
                        && e.getCause() instanceof WebClientResponseException ex2
                        && ex2.getStatusCode().value() == 409;

                if (est409 || estRetryExhausted409) {
                    return kernel.get("/api/actors/me", Map.class)
                            .map(this::extraireId);
                }
                return Mono.error(e);
            });
}

private UUID extraireId(Map<?, ?> reponse) {
    Object charge = reponse.containsKey("data") ? reponse.get("data") : reponse;
    Object id = charge instanceof Map<?, ?> m ? m.get("id") : reponse.get("id");
    if (id == null) throw new IllegalStateException("Réponse kernel sans id");
    return UUID.fromString(id.toString());
}

    /** Code unique exigé par le kernel : slug du nom + suffixe court (évite les collisions). */
    private static String genererCode(String nom) {
        String slug = nom == null ? "" : nom.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (slug.length() > 12) {
            slug = slug.substring(0, 12);
        }
        if (slug.isBlank()) {
            slug = "ORG";
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}

/** Agence kernel (mappe {@code AgencyResponse}) : on n'en retient que l'id et le statut de siège. */
record AgenceItem(UUID id, boolean isHeadquarter) {
}

/** Corps d'onboarding d'un business actor (seul {@code name} est requis côté kernel). */
record CreerBusinessActorRequest(String name, String role, boolean isActive) {
}

/** Corps de création d'organisation : champs requis par {@code CreateOrganizationRequest}. */
record CreerOrganisationRequest(String businessActorId, String code, String service,
                                String shortName, String longName) {
}
