package com.yowyob.businesscore.adapter.out.kernel.organization;

import java.util.List;
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

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Crée l'organisation réelle d'une entreprise dans le kernel.
 *
 * <p>Chaîne de provisionnement orchestrée par {@link com.yowyob.businesscore.application.usecase.enterprise.EntrepriseService}
 * (une nouvelle entreprise BC = une nouvelle organisation kernel) :
 * <ol>
 *   <li><b>Résoudre le business actor propriétaire</b> — {@link #creerOrganisation} (étapes 1–2),</li>
 *   <li><b>Approuver l'organisation</b> — {@link #approuverOrganisation},</li>
 *   <li><b>Souscrire les services</b> — {@link #souscrireServices},</li>
 *   <li><b>Créer l'agence principale</b> — {@link #creerAgence}.</li>
 * </ol>
 *
 * <p>Cas traités à chaque étape :
 * <table>
 *   <tr><th>Étape</th><th>Situation</th><th>Action</th></tr>
 *   <tr><td>1 — Actor</td><td>{@code GET /api/actors/me} renvoie un profil</td>
 *       <td>Onboarding déjà fait → récupérer l'id, passer à l'étape 2</td></tr>
 *   <tr><td>1 — Actor</td><td>{@code GET /api/actors/me} → 404</td>
 *       <td>Onboarding absent → {@code POST /api/actors/onboarding}</td></tr>
 *   <tr><td>1 — Actor</td><td>{@code POST onboarding} → 409 (concurrence)</td>
 *       <td>Profil créé entre-temps → {@code GET /api/actors/me}</td></tr>
 *   <tr><td>2 — Organisation</td><td>Nouvelle entreprise BC</td>
 *       <td>Toujours {@code POST /api/organizations} (une org par entreprise)</td></tr>
 *   <tr><td>3 — Approbation</td><td>Org fraîchement créée</td>
 *       <td>{@code POST /api/organizations/{id}/approve} (id dans l'URL, pas {@code X-Organization-Id})</td></tr>
 *   <tr><td>3 — Approbation</td><td>Déjà approuvée (409)</td>
 *       <td>Ignorer et continuer (idempotent)</td></tr>
 *   <tr><td>4 — Services</td><td>Service déjà souscrit (409)</td>
 *       <td>Ignorer et continuer (idempotent)</td></tr>
 *   <tr><td>5 — Agence</td><td>Org approuvée et services souscrits</td>
 *       <td>{@code POST /api/organizations/{id}/agencies}</td></tr>
 * </table>
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
        return resoudreBusinessActor(nom)
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
                : nom.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "")
                        .substring(0, Math.min(nom.length(), 8))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);

        return kernel.post(
                        "/api/organizations/" + organizationId + "/agencies",
                        Map.of("code", code, "name", nom != null ? nom : "Agence principale"),
                        KernelId.class)
                .map(kernelId -> kernelId.id());
    }

    @Override
    public Mono<Void> changerCycleVieKernel(UUID organizationId, CycleVie cible) {
        String action = switch (cible) {
            case SUSPENDUE -> "suspend";
            case FERMEE -> "close";
            case ACTIVE -> "reopen";
        };
        return kernel.post(
                        "/api/organizations/" + organizationId + "/" + action,
                        null, Void.class)
                .then();
    }

    @Override
    public Mono<Void> approuverOrganisation(UUID organizationId, String reason) {
        String motif = (reason == null || reason.isBlank()) ? "Approbation initiale" : reason.trim();
        // L'id est déjà dans le chemin : X-Organization-Id sur une org fraîche provoque 401 kernel.
        return kernel.post(
                        "/api/organizations/" + organizationId + "/approve",
                        Map.of("reason", motif),
                        Void.class)
                .onErrorResume(this::estDejaApprouvee, e -> Mono.empty())
                .then();
    }

    @Override
    public Mono<UUID> trouverAgencePrincipale(UUID organizationId) {
        return kernel.get(
                        "/api/organizations/" + organizationId + "/agencies",
                        AgenceItem[].class)
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
        return resoudreOrdreServices()
                .flatMapMany(ordered -> Flux.fromIterable(ordered)
                        .concatMap(code -> kernel.post(
                                        "/api/organizations/" + organizationId + "/services",
                                        Map.of("serviceCode", code),
                                        Void.class)
                                .onErrorResume(this::estDejaSouscrit, e -> Mono.empty())))
                .then();
    }

    /** Lit le catalogue kernel et trie les services selon {@code requiredDependencies}. */
    private Mono<List<String>> resoudreOrdreServices() {
        List<String> codes = properties.organizationServices();
        return kernel.get("/api/organizations/services/catalog", Object.class)
                .map(OrganizationServiceOrder::parserCatalogue)
                .map(catalog -> OrganizationServiceOrder.ordonner(codes, catalog))
                .defaultIfEmpty(codes)
                .onErrorReturn(codes);
    }

    /**
     * Étape 1 — résout le business actor propriétaire : onboarding déjà fait → {@code GET /me},
     * sinon → {@code POST /onboarding}.
     */
    private Mono<UUID> resoudreBusinessActor(String nom) {
        return recupererBusinessActorCourant()
                .switchIfEmpty(onboarderBusinessActor(nom)
                        .onErrorResume(this::estActorDejaOnboarde,
                                e -> recupererBusinessActorCourant()));
    }

    private Mono<UUID> recupererBusinessActorCourant() {
        return kernel.get("/api/actors/me", Map.class)
                .map(this::extraireId)
                .onErrorResume(this::estActorAbsent, e -> Mono.empty());
    }

    private Mono<UUID> onboarderBusinessActor(String nom) {
        CreerBusinessActorRequest requete = new CreerBusinessActorRequest(
                nom, properties.businessActorRole(), true);
        return kernel.post("/api/actors/onboarding", requete, Map.class)
                .map(this::extraireId);
    }

    private boolean estActorAbsent(Throwable e) {
        return codeHttp(e) == 404;
    }

    private boolean estActorDejaOnboarde(Throwable e) {
        return codeHttp(e) == 409;
    }

    private boolean estDejaSouscrit(Throwable e) {
        return codeHttp(e) == 409;
    }

    private boolean estDejaApprouvee(Throwable e) {
        return codeHttp(e) == 409;
    }

    private static int codeHttp(Throwable e) {
        if (e instanceof WebClientResponseException ex) {
            return ex.getStatusCode().value();
        }
        if (Exceptions.isRetryExhausted(e) && e.getCause() instanceof WebClientResponseException ex2) {
            return ex2.getStatusCode().value();
        }
        return -1;
    }

    private UUID extraireId(Map<?, ?> reponse) {
        Object charge = reponse.containsKey("data") ? reponse.get("data") : reponse;
        Object id = charge instanceof Map<?, ?> m ? m.get("id") : reponse.get("id");
        if (id == null) {
            throw new IllegalStateException("Réponse kernel sans id");
        }
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
