package com.yowyob.businesscore.domain.port.out;

/**
 * Identifiants d'une ClientApplication kernel provisionnée pour un développeur.
 * Le secret n'est exposé qu'au moment de la création/rotation et n'est jamais re-divulgué.
 */
public record KernelClientCredentials(String clientId, String secret) {
}
