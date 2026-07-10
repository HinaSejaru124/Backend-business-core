package com.yowyob.businesscore.adapter.out.kernel.auth;

import java.time.Duration;
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
import com.yowyob.businesscore.infrastructure.config.KernelProperties;

import reactor.core.publisher.Mono;

/**
 * Adapter d'authentification : implémente {@link AuthentifierUtilisateur} via
 * discover-contexts / select-context du kernel, en mode <b>app-only</b>
 * (en-têtes d'identité de l'application BC {@code X-Client-Id} / {@code X-Api-Key},
 * <b>sans</b> Bearer — il n'y a pas encore de token utilisateur).
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

    private final WebClient kernelWebClient;
    private final KernelProperties kernelProperties;

    public KernelAuthAdapter(@Qualifier("kernelWebClient") WebClient kernelWebClient,
            KernelProperties kernelProperties) {
        this.kernelWebClient = kernelWebClient;
        this.kernelProperties = kernelProperties;
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
                    reponse -> reponse.bodyToMono(Map.class).defaultIfEmpty(Map.of()).flatMap(corps -> {
                        String message = corps.get("message") instanceof String s
                                ? s : "Identifiant ou mot de passe invalide.";
                        return Mono.error(new ProblemException(
                                HttpStatus.UNAUTHORIZED, "Authentification refusée", message));
                    }))
            .bodyToMono(Map.class)
            .timeout(Duration.ofMillis(kernelProperties.timeoutMs()))
            .flatMap(discoverReponse -> selectPremierContexte(discoverReponse, principal, motDePasse))
            .onErrorMap(ex -> !(ex instanceof ProblemException), ex -> new ProblemException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible",
                    "Le service d'authentification est momentanément indisponible. Réessayez dans quelques instants."));
}

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

    return essayerContexte(contexts, 0, selectionToken, null);
}

/**
 * BUG #3 — essaie les contextes renvoyés par discover-contexts un par un jusqu'à ce que l'un
 * fonctionne, au lieu de ne tenter que {@code contexts.get(0)}.
 *
 * <p>Cause : un même principal (email) peut correspondre à plusieurs contextes/tenants kernel —
 * notamment quand deux comptes développeur distincts ont été créés par erreur pour la même adresse
 * (ex. avant qu'une contrainte d'unicité soit appliquée côté inscription). {@code discover-contexts}
 * renvoie alors tous les contextes trouvés, dans un ordre qui n'est pas garanti stable. L'ancien code
 * ne tentait que le premier : si ce premier contexte précis était inutilisable (ex. email non vérifié
 * sur CE compte-là précisément) alors qu'un autre contexte du même utilisateur était parfaitement
 * valide, la connexion échouait quand même — de façon intermittente et déroutante pour l'utilisateur.
 *
 * <p>Reproduit empiriquement le 2026-07-09 sur un compte de test ayant deux contextes kernel
 * distincts (deux {@code userId} différents pour le même email) : connexion tantôt réussie, tantôt
 * en échec sur "Sélection de contexte refusée", sans aucun changement de notre côté entre les deux
 * tentatives — uniquement l'ordre des contextes renvoyés par le kernel qui variait.
 *
 * <p>En cas d'échec de tous les contextes, on renvoie la toute première erreur rencontrée (la plus
 * probable pour l'utilisateur), pas la dernière.
 */
private Mono<ResultatLogin> essayerContexte(List<?> contexts, int index, String selectionToken,
                                             ProblemException premiereErreur) {
    if (index >= contexts.size()) {
        return Mono.error(premiereErreur != null ? premiereErreur : new ProblemException(
                HttpStatus.UNAUTHORIZED, "Authentification refusée",
                "Aucun contexte utilisable pour cet utilisateur."));
    }

    Map<?, ?> contexte = (Map<?, ?>) contexts.get(index);
    String contextId = texte(contexte.get("contextId"));

    // Récupère l'organizationId si disponible
    List<?> orgs = contexte.get("organizations") instanceof List<?> l ? l : List.of();
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
                    reponse -> reponse.bodyToMono(Map.class).defaultIfEmpty(Map.of()).flatMap(corps ->
                            Mono.error(erreurSelectContexte(corps))))
            .bodyToMono(Map.class)
            .timeout(Duration.ofMillis(kernelProperties.timeoutMs()))
            .flatMap(this::versResultat)
            .onErrorResume(ProblemException.class, ex -> essayerContexte(
                    contexts, index + 1, selectionToken, premiereErreur != null ? premiereErreur : ex));
}

/** Traduit le refus de select-context en message actionnable — notamment l'email non vérifié. */
private ProblemException erreurSelectContexte(Map<?, ?> corps) {
    String errorCode = corps.get("errorCode") instanceof String s ? s : null;
    if ("EMAIL_NOT_VERIFIED".equals(errorCode)) {
        return new ProblemException(HttpStatus.UNAUTHORIZED, "Adresse email non vérifiée",
                "Votre adresse email n'est pas encore vérifiée. Consultez votre boîte mail "
                        + "(et vos spams) et cliquez sur le lien de vérification avant de vous connecter.")
                .with("errorCode", errorCode);
    }
    String message = corps.get("message") instanceof String s ? s : "Sélection de contexte refusée.";
    return new ProblemException(HttpStatus.UNAUTHORIZED, "Authentification refusée", message)
            .with("errorCode", errorCode);
}


private Mono<ResultatLogin> versResultat(Map<?, ?> corps) {
    Object charge = corps;
    if (corps.containsKey("success") && corps.containsKey("data")) {
        Object errorCode = corps.get("errorCode");
        if (errorCode != null) {
            return Mono.error(erreurSelectContexte(corps));
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
                        reponse -> reponse.bodyToMono(Map.class).flatMap(erreur -> {
                            String message = erreur.get("message") instanceof String s
                                    ? s : "Les informations fournies sont invalides.";
                            return Mono.error(new ProblemException(
                                    HttpStatus.BAD_REQUEST, "Inscription refusée", message));
                        }))
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(kernelProperties.timeoutMs()))
                .map(this::versSignUpResult)
                .onErrorMap(ex -> !(ex instanceof ProblemException), ex -> new ProblemException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible",
                        "Le service d'inscription est momentanément indisponible. Réessayez dans quelques instants."));
    }

    
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
