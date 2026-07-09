package com.yowyob.businesscore.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Business Core API",
                version = "v1",
                description = """
                        API du Business Core — noyau métier générique intégré au kernel RT-Comops.

                        Le Business Core expose un modèle piloté par les métadonnées : le développeur
                        déclare des Types Métier (le modèle), crée des Entreprises (les instances),
                        et déclenche des Opérations. Le Business Core orchestre le kernel en façade.

                        ## Authentification
                        Deux modes à l'exécution (l'un ou l'autre) :
                        - **JWT** : `Authorize` → Bearer, token obtenu via `POST /v1/auth/login`.
                        - **Clé BC (backend M2M)** : en-têtes `X-BC-Client-Id` + `X-BC-Api-Key` dans Try it out
                          (secret stocké côté serveur du développeur — pas dans Authorize).

                        Ni le Client-Id ni l'Api-Key ne figurent dans Authorize : ce sont des en-têtes HTTP.

                        ## Tests E2E
                        Après login, préférer le JWT Bearer sur toutes les routes pour un tenant kernel unique.

                        ## Conventions
                        - Versionnement de l'API dans l'URL /v1, distinct de la version d'un Type Métier.
                        - Opérations synchrones par défaut (`200`), différées en `202` avec une trace de suivi.
                        - Erreurs au format RFC 7807 (Problem Details), enrichies de champs métier.
                        """,
                contact = @Contact(name = "Équipe Business Core")
        ),
        tags = {
                @Tag(name = "Accès", description = "Inscription et gestion des clés d'API"),
                @Tag(name = "Auth", description = "Login délégué kernel et profil utilisateur"),
                @Tag(name = "Types métier", description = "Déclaration du modèle métier (niveau Type)"),
                @Tag(name = "Contenu de version", description = "Offres, rôles, règles, opérations et configuration d'une version"),
                @Tag(name = "Entreprises", description = "Instances de métier (niveau Entreprise)"),
                @Tag(name = "Opérations", description = "Exécution des actes métier"),
                @Tag(name = "Consultation", description = "Transactions et traces"),
                @Tag(name = "Santé", description = "Sondes de disponibilité")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT kernel obtenu via POST /v1/auth/login. Alternative aux en-têtes X-BC-Client-Id / X-BC-Api-Key."
)
public class OpenApiConfig {
}
