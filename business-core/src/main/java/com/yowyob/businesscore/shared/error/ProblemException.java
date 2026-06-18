package com.yowyob.businesscore.shared.error;

import org.springframework.http.HttpStatus;

/**
 * ⚠️ STUB DE SOCLE — présent uniquement pour le mode standalone.
 * Si le vrai socle est disponible, SUPPRIME ce fichier et importe le ProblemException du socle
 * (le format RFC 7807 y est géré par GlobalProblemHandler).
 */
public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String detail;

    private ProblemException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public static ProblemException notFound(String detail) {
        return new ProblemException(HttpStatus.NOT_FOUND, detail);
    }

    public static ProblemException conflict(String detail) {
        return new ProblemException(HttpStatus.CONFLICT, detail);
    }

    public static ProblemException unprocessable(String detail) {
        return new ProblemException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    public static ProblemException forbidden(String detail) {
        return new ProblemException(HttpStatus.FORBIDDEN, detail);
    }

    public static ProblemException badRequest(String detail) {
        return new ProblemException(HttpStatus.BAD_REQUEST, detail);
    }
}
