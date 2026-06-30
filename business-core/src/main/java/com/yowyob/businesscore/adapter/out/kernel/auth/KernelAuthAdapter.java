package com.yowyob.businesscore.adapter.out.kernel.auth;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.OrganisationAccessible;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Adapter d'authentification : implémente {@link AuthentifierUtilisateur} via {@code POST /api/auth/login}
 * du kernel, en mode <b>app-only</b> (en-têtes d'identité de l'application BC {@code X-Client-Id} /
 * {@code X-Api-Key} + {@code X-Tenant-Id}, <b>sans</b> Bearer — il n'y a pas encore de token utilisateur).
 *
 * <p>Le kernel vérifie le mot de passe et renvoie un JWT signé ; le BC ne stocke jamais de mot de passe.
 * La réponse est enveloppée ({@code {success, data, errorCode}}) : on lit {@code data}.
 */
@Component
public class KernelAuthAdapter implements AuthentifierUtilisateur {

    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_API_KEY = "X-Api-Key";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final WebClient kernelWebClient;
    private final KernelProperties kernelProperties;
    private final AuthProperties authProperties;

    public KernelAuthAdapter(@Qualifier("kernelWebClient") WebClient kernelWebClient,
                             KernelProperties kernelProperties,
                             AuthProperties authProperties) {
        this.kernelWebClient = kernelWebClient;
        this.kernelProperties = kernelProperties;
        this.authProperties = authProperties;
    }

    @Override
    public Mono<ResultatLogin> login(String principal, String motDePasse) {
        Map<String, Object> corps = Map.of("principal", principal, "password", motDePasse);
        return kernelWebClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    h.set(HEADER_CLIENT_ID, kernelProperties.clientId());
                    h.set(HEADER_API_KEY, kernelProperties.clientSecret());
                    if (authProperties.aUnTenantParDefaut()) {
                        h.set(HEADER_TENANT_ID, authProperties.tenantId());
                    }
                })
                .bodyValue(corps)
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 403,
                        reponse -> Mono.error(new ProblemException(
                                HttpStatus.UNAUTHORIZED, "Authentification refusée",
                                "Identifiant ou mot de passe invalide.")))
                .bodyToMono(Map.class)
                .flatMap(this::versResultat);
    }

    @SuppressWarnings("unchecked")
    private Mono<ResultatLogin> versResultat(Map<?, ?> corps) {
        Object charge = corps;
        if (corps.containsKey("success") && corps.containsKey("data")) {
            Object errorCode = corps.get("errorCode");
            if (errorCode != null) {
                return Mono.error(new ProblemException(HttpStatus.UNAUTHORIZED, "Authentification refusée",
                        String.valueOf(corps.get("message"))));
            }
            charge = corps.get("data");
        }
        if (!(charge instanceof Map<?, ?> data)) {
            return Mono.error(new ProblemException(HttpStatus.BAD_GATEWAY, "Réponse kernel invalide",
                    "Réponse de login inattendue du kernel."));
        }
        String accessToken = texte(data.get("accessToken"));
        if (accessToken == null) {
            return Mono.error(new ProblemException(HttpStatus.BAD_GATEWAY, "Réponse kernel invalide",
                    "Réponse de login sans accessToken."));
        }
        long expires = data.get("expiresInSeconds") instanceof Number n ? n.longValue() : 900L;
        return Mono.just(new ResultatLogin(
                accessToken,
                expires,
                texteListe(data.get("authorities")),
                organisations(data.get("organizations")),
                texte(data.get("tenantId")),
                texte(data.get("actorId"))));
    }

    private List<OrganisationAccessible> organisations(Object brut) {
        if (!(brut instanceof List<?> liste)) {
            return List.of();
        }
        return liste.stream()
                .filter(o -> o instanceof Map)
                .map(o -> (Map<?, ?>) o)
                .map(m -> new OrganisationAccessible(
                        texte(m.get("organizationId")),
                        texte(m.get("organizationCode")),
                        texte(m.get("displayName")),
                        texteListe(m.get("services"))))
                .toList();
    }

    private List<String> texteListe(Object brut) {
        if (!(brut instanceof List<?> liste)) {
            return List.of();
        }
        return liste.stream().map(this::texte).filter(s -> s != null).toList();
    }

    private String texte(Object valeur) {
        return valeur == null ? null : valeur.toString();
    }
}
