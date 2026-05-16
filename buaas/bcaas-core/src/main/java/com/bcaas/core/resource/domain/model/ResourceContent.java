package com.bcaas.core.resource.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Contenu générique d'une ressource.
 *
 * BCaaS stocke le contenu comme un dictionnaire clé-valeur.
 * Chaque application métier définit ses propres clés.
 *
 * BuaaS utilise : title, description, skills, salary, education
 * Transport utilise : origin, destination, price, seats
 *
 * Pattern : Property Bag — flexible sans schéma rigide au niveau du Core.
 */
public record ResourceContent(
        String title,
        String summary,
        Map<String, String> fields,
        String locale
) {
    public ResourceContent {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre de la ressource est obligatoire");
        }
        if (title.length() < 3 || title.length() > 200) {
            throw new IllegalArgumentException(
                "Le titre doit contenir entre 3 et 200 caractères");
        }
        fields = fields != null ? Map.copyOf(fields) : Map.of();
        locale = locale != null ? locale : "fr";
    }

    public static ResourceContent of(String title, String summary, String locale) {
        return new ResourceContent(title, summary, Map.of(), locale);
    }

    public ResourceContent withField(String key, String value) {
        Map<String, String> newFields = new HashMap<>(this.fields);
        newFields.put(key, value);
        return new ResourceContent(title, summary, newFields, locale);
    }

    public String getField(String key) {
        return fields.getOrDefault(key, null);
    }
}
