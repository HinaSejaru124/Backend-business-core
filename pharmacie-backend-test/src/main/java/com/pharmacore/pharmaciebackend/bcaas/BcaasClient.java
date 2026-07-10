package com.pharmacore.pharmaciebackend.bcaas;

import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Point d'accès unique à Business Core. Authentification par clé API (X-BC-Client-Id/X-BC-Api-Key,
 * posée une fois pour toutes dans {@link com.pharmacore.pharmaciebackend.config.PharmacoreConfig}).
 * Toute erreur RFC 7807 est traduite en {@link BcaasException} exploitable par les couches au-dessus.
 */
@Component
public class BcaasClient {

    private final RestClient client;
    private final BcaasProperties properties;

    public BcaasClient(RestClient bcaasRestClient, BcaasProperties properties) {
        this.client = bcaasRestClient;
        this.properties = properties;
    }

    /**
     * Déclare une Offre (médicament) sous la version de Type configurée
     * ({@code POST /v1/business-types/{typeId}/versions/{n}/offers}).
     */
    public OffreDtos.OffreReponse declarerOffre(String nom, BigDecimal prix, boolean stockable) {
        var requete = new OffreDtos.DeclarerOffreRequete(
                nom, "FIXE", prix, stockable ? Set.of("STOCKABLE") : Set.of());
        String path = "/v1/business-types/%s/versions/%d/offers"
                .formatted(properties.typeId(), properties.versionNumber());
        try {
            return client.post()
                    .uri(path)
                    .body(requete)
                    .retrieve()
                    .body(OffreDtos.OffreReponse.class);
        } catch (RestClientResponseException e) {
            throw traduire(e);
        }
    }

    private BcaasException traduire(RestClientResponseException e) {
        try {
            ProblemDetail pd = e.getResponseBodyAs(ProblemDetail.class);
            if (pd != null) {
                String violatedRule = stringProp(pd, "violatedRule");
                String requiredAction = stringProp(pd, "requiredAction");
                String requiredDocument = stringProp(pd, "requiredDocument");
                return new BcaasException(e.getStatusCode().value(), pd.getTitle(), pd.getDetail(),
                        violatedRule, requiredAction, requiredDocument);
            }
        } catch (Exception ignored) {
            // corps non RFC 7807 (ex. 401 sans corps JSON) : on retombe sur le message brut ci-dessous.
        }
        return new BcaasException(e.getStatusCode().value(), "Erreur Business Core",
                e.getResponseBodyAsString(), null, null, null);
    }

    private String stringProp(ProblemDetail pd, String key) {
        Object v = pd.getProperties() != null ? pd.getProperties().get(key) : null;
        return v != null ? v.toString() : null;
    }
}
