package com.yowyob.businesscore.adapter.out.kernel;

/**
 * Erreur métier renvoyée par le kernel dans l'enveloppe de réponse ({@code errorCode} non nul), y
 * compris sur un HTTP 200 (cf. §0.3 de la référence kernel). Distincte d'une erreur de transport.
 */
public class KernelException extends RuntimeException {

    private final String errorCode;

    public KernelException(String errorCode, String message) {
        super("Erreur kernel [" + errorCode + "] : " + message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
