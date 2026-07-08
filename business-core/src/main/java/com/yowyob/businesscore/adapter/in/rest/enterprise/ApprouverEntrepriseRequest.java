package com.yowyob.businesscore.adapter.in.rest.enterprise;

/**
 * Corps optionnel de {@code POST /v1/businesses/{businessId}/approve}.
 * Si {@code reason} est absent ou vide, le socle envoie {@code "Approbation initiale"} au kernel.
 */
public record ApprouverEntrepriseRequest(String reason) {
}
