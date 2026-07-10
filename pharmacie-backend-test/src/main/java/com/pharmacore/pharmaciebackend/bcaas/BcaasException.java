package com.pharmacore.pharmaciebackend.bcaas;

/**
 * Erreur renvoyée par Business Core, au format RFC 7807 (application/problem+json).
 * Porte les champs métier enrichis (violatedRule/requiredAction/requiredDocument) quand présents,
 * pour que le frontend Pharmacie affiche un message exact plutôt qu'une erreur générique.
 */
public class BcaasException extends RuntimeException {

    private final int status;
    private final String title;
    private final String detail;
    private final String violatedRule;
    private final String requiredAction;
    private final String requiredDocument;

    public BcaasException(int status, String title, String detail,
                          String violatedRule, String requiredAction, String requiredDocument) {
        super(detail != null ? detail : title);
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.violatedRule = violatedRule;
        this.requiredAction = requiredAction;
        this.requiredDocument = requiredDocument;
    }

    public int status() { return status; }
    public String title() { return title; }
    public String detail() { return detail; }
    public String violatedRule() { return violatedRule; }
    public String requiredAction() { return requiredAction; }
    public String requiredDocument() { return requiredDocument; }
}
