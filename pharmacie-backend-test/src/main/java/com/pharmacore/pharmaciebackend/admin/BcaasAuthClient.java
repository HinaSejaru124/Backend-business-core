package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Authentification du titulaire auprès de Business Core ({@code POST /v1/auth/login}, route publique).
 *
 * <p>Business Core délègue la vérification de l'identité au Kernel et renvoie un JWT court. On n'extrait
 * ici que ce qui sert PharmaCore : le jeton, sa durée de validité, et l'indicateur {@code owner}
 * (présence de {@code organizations:write} — utile pour savoir si ce compte peut provisionner des
 * entreprises). Aucune identité ni mot de passe n'est stocké.
 */
@Component
public class BcaasAuthClient {

    private final RestClient client;

    public BcaasAuthClient(RestClient bcaasAnonRestClient) {
        this.client = bcaasAnonRestClient;
    }

    /** Résultat de login réduit à ce dont PharmaCore a besoin. {@code actorId} vient de GET /v1/auth/me. */
    public record ResultatLogin(String accessToken, long expiresInSeconds, boolean owner, String actorId) {
    }

    public ResultatLogin login(String principal, String password) {
        try {
            Map<?, ?> corps = client.post()
                    .uri("/v1/auth/login")
                    .body(Map.of("principal", principal, "password", password))
                    .retrieve()
                    .body(Map.class);
            if (corps == null) {
                throw new BcaasException(502, "Réponse invalide",
                        "Business Core n'a pas renvoyé de session.", null, null, null);
            }
            String token = corps.get("accessToken") instanceof String s ? s : null;
            if (token == null) {
                throw new BcaasException(502, "Réponse invalide",
                        "Session de connexion sans accessToken.", null, null, null);
            }
            long expires = corps.get("expiresInSeconds") instanceof Number n ? n.longValue() : 900L;
            boolean owner = corps.get("owner") instanceof Boolean b && b;
            String actorId = actorIdPour(token);
            return new ResultatLogin(token, expires, owner, actorId);
        } catch (RestClientResponseException e) {
            throw traduire(e);
        }
    }

    /**
     * Identité kernel du titulaire connecté ({@code GET /v1/auth/me}, claim {@code actor} du JWT) — sert
     * à le rattacher lui-même comme acteur métier (ex. CAISSIER) dans {@link ModeleProvisioningService},
     * sans jamais décoder le JWT nous-mêmes : Business Core fait autorité sur ce que le jeton contient.
     */
    private String actorIdPour(String jwt) {
        Map<?, ?> moi = client.get()
                .uri("/v1/auth/me")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .body(Map.class);
        return moi != null && moi.get("actorId") instanceof String s ? s : null;
    }

    /** Relaie l'erreur RFC 7807 de Business Core (message exact plutôt qu'une erreur générique). */
    private BcaasException traduire(RestClientResponseException e) {
        try {
            ProblemDetail pd = e.getResponseBodyAs(ProblemDetail.class);
            if (pd != null) {
                return new BcaasException(e.getStatusCode().value(), pd.getTitle(), pd.getDetail(),
                        null, null, null);
            }
        } catch (Exception ignored) {
            // corps non RFC 7807
        }
        return new BcaasException(e.getStatusCode().value(), "Connexion refusée",
                e.getResponseBodyAsString(), null, null, null);
    }
}
