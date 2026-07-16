package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.AppliquerRoleTechnique;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GET /api/roles (recherche par code) → POST /api/roles seulement si absent → POST /api/roles/assignments.
 * Le kernel n'est pas idempotent sur la création (409 {@code ROLE_CODE_DUPLICATE} si le code existe déjà,
 * ex. après une tentative précédente qui a créé le rôle puis échoué sur l'étape suivante) — on cherche
 * donc d'abord l'existant plutôt que de foncer sur la création à chaque appel.
 */
@Component
public class AppliquerRoleTechniqueKernelAdapter implements AppliquerRoleTechnique {

    private final KernelClient kernel;

    public AppliquerRoleTechniqueKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    record RoleKernelDto(UUID id, String code) {}

    @Override
    public Mono<Void> appliquer(UUID actorId, String roleCode) {
        return resoudreOuCreerRole(roleCode)
                .flatMap(roleId -> kernel.post(
                        "/api/roles/assignments",
                        // "scope" est requis par AssignRoleToUserRequest (kernel) ; "TENANT" aligné sur
                        // le scopeType du rôle (défaut kernel pour /api/roles).
                        Map.of("roleId", roleId, "userId", actorId, "scope", "TENANT"),
                        KernelId.class))
                .onErrorResume(ex -> estAssignationDejaExistante(ex) ? Mono.empty() : Mono.error(ex))
                .then();
    }

    private Mono<UUID> resoudreOuCreerRole(String roleCode) {
        return kernel.get("/api/roles", RoleKernelDto[].class)
                .flatMap(roles -> List.of(roles).stream()
                        .filter(r -> roleCode.equals(r.code()))
                        .map(RoleKernelDto::id)
                        .findFirst()
                        .map(Mono::just)
                        .orElseGet(() -> creerRole(roleCode)));
    }

    private Mono<UUID> creerRole(String roleCode) {
        // CreateRoleRequest (kernel) exige code ET name — on réutilise le code comme name, aucun
        // libellé distinct n'existe côté Business Core pour un rôle métier (RoleMetier n'a que code).
        return kernel.post("/api/roles", Map.of("code", roleCode, "name", roleCode), KernelId.class)
                .map(KernelId::id);
    }

    /**
     * Une seconde tentative sur un acteur déjà assigné à ce rôle ne doit pas faire échouer le
     * provisioning. Le kernel signale ce cas de deux façons observées : un 409 propre
     * ({@code ROLE_CODE_DUPLICATE}), ou une violation de contrainte SQL brute remontée en 500
     * ({@code user_role_assignment} a un index unique (tenant, user, role)).
     *
     * <p>On remonte toute la chaîne de causes (pas seulement {@code ex} lui-même) : {@code
     * KernelClient.resilience()} retente les 5xx avant d'abandonner, et Reactor enveloppe alors
     * l'erreur finale dans un {@code RetryExhaustedException}. Pour un {@link WebClientResponseException}
     * (le niveau qui porte réellement l'erreur kernel), le texte utile est dans {@code
     * getResponseBodyAsString()} — {@code getMessage()} ne renvoie que la ligne de statut HTTP
     * générique ("500 Internal Server Error from POST ..."), jamais le corps de la réponse.
     */
    private boolean estAssignationDejaExistante(Throwable ex) {
        for (Throwable courant = ex; courant != null; courant = courant.getCause()) {
            String contenu = courant instanceof WebClientResponseException wcre
                    ? wcre.getResponseBodyAsString()
                    : courant.getMessage();
            if (contenu == null) {
                continue;
            }
            String bas = contenu.toLowerCase(java.util.Locale.ROOT);
            if (bas.contains("duplicate key") || bas.contains("role_code_duplicate")
                    || contenu.contains("409")) {
                return true;
            }
        }
        return false;
    }
}
