package com.yowyob.businesscore.domain.port.in;

/**
 * Clé Business Core émise à l'inscription d'un développeur.
 * {@code apiKey} (le secret) n'est affiché qu'une seule fois.
 */
public record ApiKeyEmise(String clientId, String apiKey, String plan) {
}
