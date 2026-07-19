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
                        déclare des Types Métier (le modèle), crée des Applications (les instances),
                        et déclenche des Opérations. Le Business Core orchestre le kernel en façade.

                        ## Authentification
                        L'API identifie le **développeur** et délègue l'**utilisateur** au kernel :
                        - **JWT Bearer** (`Authorize`, via `POST /v1/auth/login`) : obligatoire sur toutes les
                          routes protégées qui appellent le kernel (applications, opérations, etc.).
                        - **Headers `X-BC-*`** (Try it out) : identifient la clé API du développeur sur les
                          routes d'intégration (`/v1/business-types`, `/v1/businesses`, opérations…).
                          Recommandés pour le backend du dev (suivi d'usage, `X-BC-On-Behalf-Of`).
                        - **Console développeur** (`/v1/api-keys`, `/v1/dashboard`, `/v1/auth/me`) : JWT seul.

                        Le Business Core transmet au kernel : credentials plateforme (`X-Client-Id`/`X-Api-Key`
                        serveur) + Bearer utilisateur + `X-Tenant-Id`.

                        ## Tests E2E
                        Après login : `Authorization: Bearer $JWT` sur tout le parcours métier.
                        Optionnel : ajouter `X-BC-Client-Id` / `X-BC-Api-Key` sur les routes d'intégration.

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
                @Tag(name = "Applications", description = "Instances de métier (niveau Application)"),
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
        description = "JWT kernel obtenu via POST /v1/auth/login. Obligatoire pour les appels kernel (applications, opérations…)."
)
public class OpenApiConfig {
}
