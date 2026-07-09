package com.yowyob.businesscore.application.error;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception métier traduite en réponse {@code application/problem+json} (RFC 7807) par le handler
 * global du socle. Porte les extensions métier {@code violatedRule}, {@code requiredAction},
 * {@code requiredDocument} (sémantique des effets de règle).
 *
 * <p>Les features lèvent cette exception ; le format de sortie est garanti par le socle.
 */
public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final Map<String, Object> extensions = new HashMap<>();

    public ProblemException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return getMessage();
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public ProblemException with(String key, Object value) {
        if (value != null) {
            extensions.put(key, value);
        }
        return this;
    }

    public ProblemException violatedRule(String rule) {
        return with("violatedRule", rule);
    }

    public ProblemException requiredAction(String action) {
        return with("requiredAction", action);
    }

    public ProblemException requiredDocument(String document) {
        return with("requiredDocument", document);
    }

    // Fabriques courantes
    public static ProblemException notFound(String detail) {
        return new ProblemException(HttpStatus.NOT_FOUND, "Ressource introuvable", detail);
    }

    public static ProblemException conflict(String detail) {
        return new ProblemException(HttpStatus.CONFLICT, "Conflit", detail);
    }

    public static ProblemException unprocessable(String detail) {
        return new ProblemException(HttpStatus.UNPROCESSABLE_CONTENT, "Règle métier violée", detail);
    }

    public static ProblemException forbidden(String detail) {
        return new ProblemException(HttpStatus.FORBIDDEN, "Accès refusé", detail);
    }

    public static ProblemException badRequest(String detail) {
        return new ProblemException(HttpStatus.BAD_REQUEST, "Requête invalide", detail);
    }

    /** Échec d'appel au kernel (transport ou auth inter-services). */
    public static ProblemException badGateway(String detail) {
        return new ProblemException(HttpStatus.BAD_GATEWAY, "Service kernel indisponible", detail);
    }
}
