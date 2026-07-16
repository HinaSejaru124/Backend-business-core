package com.yowyob.businesscore.application.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * Administrateurs de la plateforme Business Core (console admin).
 *
 * <p>Un administrateur n'est jamais auto-créé (ce n'est pas un client de la plateforme mais son
 * exploitant) : sa liste est fixée par configuration ({@code businesscore.admin.emails}). Un développeur
 * déjà authentifié dont l'e-mail figure ici obtient l'accès admin — aucune inscription ni vérification
 * d'e-mail supplémentaire (cf. AUDIT-PHARMACORE.md, décision « marquer un dev existant comme admin »).
 */
@ConfigurationProperties(prefix = "businesscore.admin")
public record AdminProperties(List<String> emails) {

    public AdminProperties {
        emails = emails == null ? List.of() : emails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    /** Vrai si cet e-mail est déclaré administrateur (comparaison insensible à la casse). */
    public boolean estAdmin(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
