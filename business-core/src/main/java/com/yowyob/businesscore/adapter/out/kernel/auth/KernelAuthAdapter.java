package com.yowyob.businesscore.adapter.out.kernel.auth;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.OrganisationAccessible;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import com.yowyob.businesscore.domain.port.out.SignUpResult;
import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;

import reactor.core.publisher.Mono;

/**
 * Adapter d'authentification : implémente {@link AuthentifierUtilisateur} via
 * {@code POST /api/auth/login}
 * du kernel, en mode <b>app-only</b> (en-têtes d'identité de l'application BC
 * {@code X-Client-Id} /
 * {@code X-Api-Key} + {@code X-Tenant-Id}, <b>sans</b> Bearer — il n'y a pas
 * encore de token utilisateur).
 *
 * <p>
 * Le kernel vérifie le mot de passe et renvoie un JWT signé ; le BC ne stocke
 * jamais de mot de passe.
 * La réponse est enveloppée ({@code {success, data, errorCode}}) : on lit
 * {@code data}.
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
    // Étape 1 — discover-contexts (pas besoin de X-Tenant-Id)
    Map<String, Object> discoverBody = Map.of(
            "principal", principal,
            "password", motDePasse
    );
    return kernelWebClient.post()
            .uri("/api/auth/discover-contexts")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> {
                h.set(HEADER_CLIENT_ID, kernelProperties.clientId());
                h.set(HEADER_API_KEY, kernelProperties.clientSecret());
            })
            .bodyValue(discoverBody)
            .retrieve()
            .onStatus(status -> status.value() == 401 || status.value() == 403,
                    reponse -> Mono.error(new ProblemException(
                            HttpStatus.UNAUTHORIZED, "Authentification refusée",
                            "Identifiant ou mot de passe invalide.")))
            .bodyToMono(Map.class)
            .flatMap(discoverReponse -> selectPremierContexte(discoverReponse, principal, motDePasse));
}

@SuppressWarnings("unchecked")
private Mono<ResultatLogin> selectPremierContexte(Map<?, ?> discoverReponse,
                                                   String principal, String motDePasse) {
    Map<?, ?> data = discoverReponse.containsKey("data")
            ? (Map<?, ?>) discoverReponse.get("data") : discoverReponse;

    String selectionToken = texte(data.get("selectionToken"));
    List<?> contexts = data.get("contexts") instanceof List<?> l ? l : List.of();

    if (contexts.isEmpty() || selectionToken == null) {
        return Mono.error(new ProblemException(
                HttpStatus.UNAUTHORIZED, "Authentification refusée",
                "Aucun contexte disponible pour cet utilisateur."));
    }

    // Sélection automatique du premier contexte
    Map<?, ?> premierContexte = (Map<?, ?>) contexts.get(0);
    String contextId = texte(premierContexte.get("contextId"));

    // Récupère l'organizationId si disponible
    List<?> orgs = premierContexte.get("organizations") instanceof List<?> l ? l : List.of();
    String organizationId = orgs.isEmpty() ? null
            : texte(((Map<?, ?>) orgs.get(0)).get("organizationId"));

    // Étape 2 — select-context → JWT final
    Map<String, Object> selectBody = new java.util.HashMap<>();
    selectBody.put("selectionToken", selectionToken);
    selectBody.put("contextId", contextId);
    if (organizationId != null) selectBody.put("organizationId", organizationId);

    return kernelWebClient.post()
            .uri("/api/auth/select-context")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(h -> {
                h.set(HEADER_CLIENT_ID, kernelProperties.clientId());
                h.set(HEADER_API_KEY, kernelProperties.clientSecret());
            })
            .bodyValue(selectBody)
            .retrieve()
            .onStatus(status -> status.value() == 401 || status.value() == 403,
                    reponse -> Mono.error(new ProblemException(
                            HttpStatus.UNAUTHORIZED, "Authentification refusée",
                            "Sélection de contexte refusée.")))
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

    // Le token est dans data.session.accessToken
    Map<?, ?> session = data.get("session") instanceof Map<?, ?> s ? s : data;

    String accessToken = texte(session.get("accessToken"));
    if (accessToken == null) {
        return Mono.error(new ProblemException(HttpStatus.BAD_GATEWAY, "Réponse kernel invalide",
                "Réponse de login sans accessToken."));
    }

    long expires = session.get("expiresInSeconds") instanceof Number n ? n.longValue() : 900L;
    String tenantId = texte(data.get("selectedTenantId"));
    String actorId = texte(session.get("actorId"));

    return Mono.just(new ResultatLogin(
            accessToken,
            expires,
            texteListe(session.get("authorities")),
            organisations(session.get("organizations")),
            tenantId,
            actorId));
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

    @Override
    public Mono<SignUpResult> signUp(String principal, String password,
            String firstName, String lastName) {
                Map<String, Object> corps = new java.util.HashMap<>();
                corps.put("email", principal);        // kernel attend "email" pas "principal"
                corps.put("password", password);
                corps.put("firstName", firstName);
                corps.put("lastName", lastName);
                corps.put("username", principal);     // username = email par défaut
                // corps.put("accountType", "BUSINESS");
                // corps.put("businessType", "RETAIL"); 
        return kernelWebClient.post()
                .uri("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> {
                    h.set(HEADER_CLIENT_ID, kernelProperties.clientId());
                    h.set(HEADER_API_KEY, kernelProperties.clientSecret());
                })
                .bodyValue(corps)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        reponse -> reponse.bodyToMono(String.class).flatMap(body -> {
                            System.err.println("KERNEL 400 body: " + body);
                            return Mono.error(new ProblemException(
                                    HttpStatus.BAD_REQUEST, "Inscription refusée", body));
                        }))
                .bodyToMono(Map.class)
                .map(this::versSignUpResult);
    }

    @SuppressWarnings("unchecked")
    private SignUpResult versSignUpResult(Map<?, ?> corps) {
        Object charge = corps.containsKey("data") ? corps.get("data") : corps;
        Map<?, ?> data = charge instanceof Map<?, ?> m ? m : Map.of();
        String message = corps.get("message") instanceof String s ? s
                : "Compte créé. Vérifiez votre email avant de vous connecter.";
        return new SignUpResult(
                texte(data.get("id")),
                texte(data.get("tenantId")),
                texte(data.get("status")),
                message);
    }
}
