package com.yowyob.businesscore.application.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.UUID;

/**
 * Lecture non vérifiante des claims d'un JWT (payload déjà validé ailleurs ou token fraîchement émis).
 */
public final class JwtClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtClaims() {
    }

    public static UUID tid(String accessToken) {
        return parseUuid(texte(accessToken, "tid"));
    }

    public static String sub(String accessToken) {
        return texte(accessToken, "sub");
    }

    public static String email(String accessToken) {
        String email = texte(accessToken, "email");
        if (email != null) {
            return email;
        }
        return texte(accessToken, "principal");
    }

    private static String texte(String accessToken, String claim) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = MAPPER.readTree(decoded);
            JsonNode node = payload.get(claim);
            if (node == null || node.isNull()) {
                return null;
            }
            String valeur = node.asText(null);
            return (valeur == null || valeur.isBlank()) ? null : valeur;
        } catch (Exception ex) {
            return null;
        }
    }

    private static UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
