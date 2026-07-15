package com.pharmacore.pharmaciebackend.vente;

import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Appel réel de l'opération {@code Vendre} — {@code POST /v1/businesses/{businessId}/operations/Vendre:execute}
 * (brique 5 + 6), authentifié par la clé API de l'entreprise ({@code bcaasRestClient}) avec
 * {@code X-BC-On-Behalf-Of} renseigné à l'identité kernel de l'acteur connecté à la caisse — c'est ce
 * qui donne à l'opération le rôle déclencheur requis (cf. {@code CaisseSession}).
 *
 * <p>Réponse réelle sur 200 (synchrone, {@code differe:false}) : {@code OperationResultResponse} côté
 * Business Core — {@code statut} ("COMPLETEE"), {@code transactionId} (String), {@code traceId} (UUID),
 * {@code details} (Map, rempli seulement par les étapes atteintes). Toute erreur (422 règle/stock,
 * 502 ENGAGER_STOCK bloqué par le Kernel, etc.) est relayée telle quelle via {@link BcaasException}.
 */
@Component
public class BcaasVenteClient {

    private final RestClient client;
    private final BcaasProperties properties;

    public BcaasVenteClient(RestClient bcaasRestClient, BcaasProperties properties) {
        this.client = bcaasRestClient;
        this.properties = properties;
    }

    public record ResultatVente(String statut, String transactionKernelId, UUID traceId) {}

    @SuppressWarnings("unchecked")
    public ResultatVente executerVendre(String acteurKernelId, UUID idempotencyKey, Map<String, Object> parametres) {
        try {
            Map<String, Object> corps = client.post()
                    .uri("/v1/businesses/{businessId}/operations/Vendre:execute", properties.businessId())
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .header("X-BC-On-Behalf-Of", acteurKernelId)
                    .body(Map.of("parametres", parametres))
                    .retrieve()
                    .body(Map.class);
            if (corps == null) {
                throw new BcaasException(502, "Réponse invalide",
                        "Business Core n'a pas renvoyé de résultat d'exécution.", null, null, null);
            }
            String statut = String.valueOf(corps.get("statut"));
            String transactionKernelId = corps.get("transactionId") != null
                    ? String.valueOf(corps.get("transactionId")) : null;
            UUID traceId = corps.get("traceId") != null ? UUID.fromString(String.valueOf(corps.get("traceId"))) : null;
            return new ResultatVente(statut, transactionKernelId, traceId);
        } catch (RestClientResponseException e) {
            throw traduire(e);
        }
    }

    private BcaasException traduire(RestClientResponseException e) {
        try {
            ProblemDetail pd = e.getResponseBodyAs(ProblemDetail.class);
            if (pd != null) {
                return new BcaasException(e.getStatusCode().value(), pd.getTitle(), pd.getDetail(),
                        stringProp(pd, "violatedRule"), stringProp(pd, "requiredAction"),
                        stringProp(pd, "requiredDocument"));
            }
        } catch (Exception ignored) {
            // corps non RFC 7807
        }
        return new BcaasException(e.getStatusCode().value(), "Erreur Business Core",
                e.getResponseBodyAsString(), null, null, null);
    }

    private String stringProp(ProblemDetail pd, String key) {
        Object v = pd.getProperties() != null ? pd.getProperties().get(key) : null;
        return v != null ? v.toString() : null;
    }
}
