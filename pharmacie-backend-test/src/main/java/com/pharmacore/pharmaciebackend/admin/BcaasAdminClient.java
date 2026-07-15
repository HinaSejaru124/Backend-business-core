package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Appels <b>design-time</b> à Business Core, authentifiés par le JWT du titulaire ({@link AdminSession}
 * via {@code bcaasAdminRestClient}). Point d'entrée unique de la modélisation : consulter/déclarer le
 * type métier, les rôles, les règles et l'opération « Vendre » d'une version.
 *
 * <p>Business Core n'impose pas l'unicité (déclarer deux fois le même rôle crée deux lignes) : c'est
 * l'appelant ({@link ModeleProvisioningService}) qui rend la modélisation idempotente en listant
 * d'abord l'existant.
 */
@Component
public class BcaasAdminClient {

    private final RestClient client;

    public BcaasAdminClient(RestClient bcaasAdminRestClient) {
        this.client = bcaasAdminRestClient;
    }

    // ─── Type métier ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listerTypesMetier() {
        return get("/v1/business-types");
    }

    // ─── Offres / catalogue (brique 2) ───────────────────────────────────────
    // Déclarer une offre est design-time (JWT) — la clé API (caisse) n'y a plus accès depuis la
    // séparation design-time/runtime (cf. FEUILLE-DE-ROUTE.md §0).

    public List<Map<String, Object>> listerOffres(String typeId, int version) {
        return get("/v1/business-types/%s/versions/%d/offers".formatted(typeId, version));
    }

    /** {@code formePrix} FIXE, capacité STOCKABLE (un médicament a un stock). */
    public Map<String, Object> declarerOffre(String typeId, int version, String nom, java.math.BigDecimal prix) {
        return post("/v1/business-types/%s/versions/%d/offers".formatted(typeId, version),
                Map.of("nom", nom, "formePrix", "FIXE", "prix", prix, "capacites", List.of("STOCKABLE")));
    }

    /**
     * Suppression réelle d'une offre — {@code DELETE .../offers/{offerId}}. Business Core refuse
     * (409, règle {@code OFFRE_MAPPEE_PRODUIT}) si un produit kernel est déjà mappé sur cette offre ;
     * l'erreur est relayée telle quelle, pas masquée.
     */
    public void supprimerOffre(String typeId, int version, java.util.UUID offreId) {
        delete("/v1/business-types/%s/versions/%d/offers/%s".formatted(typeId, version, offreId));
    }

    // ─── Rôles (brique 3) ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listerRoles(String typeId, int version) {
        return get("/v1/business-types/%s/versions/%d/roles".formatted(typeId, version));
    }

    public Map<String, Object> declarerRole(String typeId, int version, String code, String categorie) {
        return post("/v1/business-types/%s/versions/%d/roles".formatted(typeId, version),
                Map.of("code", code, "categorie", categorie));
    }

    // ─── Règles (brique 4) ───────────────────────────────────────────────────

    public Map<String, Object> declarerRegle(String typeId, int version, String declencheur,
                                             String condition, String effet, List<String> rolesDeroger) {
        return post("/v1/business-types/%s/versions/%d/rules".formatted(typeId, version),
                Map.of("declencheur", declencheur, "condition", condition, "effet", effet,
                        "rolesAutorisesADeroger", rolesDeroger));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listerRegles(String typeId, int version) {
        return get("/v1/business-types/%s/versions/%d/rules".formatted(typeId, version));
    }

    public void supprimerRegle(String typeId, int version, java.util.UUID ruleId) {
        delete("/v1/business-types/%s/versions/%d/rules/%s".formatted(typeId, version, ruleId));
    }

    // ─── Opérations (brique 5) ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listerOperations(String businessId) {
        return get("/v1/businesses/%s/operations".formatted(businessId));
    }

    /** {@code etapes} : liste ordonnée de {@code {ordre, typeEtape}}. */
    public Map<String, Object> declarerOperation(String typeId, int version, String nom,
                                                 String roleDeclencheur, String declencheurRegles,
                                                 List<Map<String, Object>> etapes) {
        Map<String, Object> body = Map.of(
                "nom", nom,
                "roleDeclencheur", roleDeclencheur,
                "declencheurRegles", declencheurRegles,
                "differe", false,
                "etapes", etapes);
        return post("/v1/business-types/%s/versions/%d/operations".formatted(typeId, version), body);
    }

    // ─── Acteurs (brique 3) ──────────────────────────────────────────────────

    public List<Map<String, Object>> listerActeurs(String businessId) {
        return get("/v1/businesses/%s/actors".formatted(businessId));
    }

    /** OPERATEUR (caissier, pharmacien) : identité kernel déjà connue, {@code identifiantPersonne} nul. */
    public Map<String, Object> rattacherActeurOperateur(String businessId, String roleMetierId,
                                                        String acteurKernelId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("roleMetierId", roleMetierId);
        body.put("acteurKernelId", acteurKernelId);
        return post("/v1/businesses/%s/actors".formatted(businessId), body);
    }

    // ─── Configuration (brique 7) ───────────────────────────────────────────

    public List<Map<String, Object>> listerConfig(String typeId, int version) {
        return get("/v1/business-types/%s/versions/%d/config".formatted(typeId, version));
    }

    public Map<String, Object> definirConfig(String typeId, int version, String cle, String valeur,
                                             boolean verrouille) {
        return post("/v1/business-types/%s/versions/%d/config".formatted(typeId, version),
                Map.of("cle", cle, "valeur", valeur, "verrouille", verrouille));
    }

    // ─── Bas niveau ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T get(String path) {
        try {
            return (T) client.get().uri(path).retrieve().body(Object.class);
        } catch (RestClientResponseException e) {
            throw traduire(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Object body) {
        try {
            return client.post().uri(path).body(body).retrieve().body(Map.class);
        } catch (RestClientResponseException e) {
            throw traduire(e);
        }
    }

    private void delete(String path) {
        try {
            client.delete().uri(path).retrieve().toBodilessEntity();
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
