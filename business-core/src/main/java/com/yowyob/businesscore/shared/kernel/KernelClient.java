package com.yowyob.businesscore.shared.kernel;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * ⚠️ STUB DE SOCLE (interface) — présent uniquement pour le mode standalone.
 * Si le vrai socle est disponible, SUPPRIME ce fichier et importe le KernelClient du socle.
 *
 * Le socle pose automatiquement X-Client-Id + X-Api-Key + Authorization: Bearer.
 * Les variantes *ForOrganization ajoutent X-Organization-Id.
 */
public interface KernelClient {

    <T> Mono<T> post(String path, Object body, Class<T> type);

    <T> Mono<T> get(String path, Class<T> type);

    <T> Mono<T> postForOrganization(UUID organizationId, String path, Object body, Class<T> type);

    <T> Mono<T> getForOrganization(UUID organizationId, String path, Class<T> type);
}
