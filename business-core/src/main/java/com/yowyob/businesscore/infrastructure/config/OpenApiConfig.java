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
                        Deux surfaces distinctes :
                        - **Console développeur** (`/v1/auth`, `/v1/api-keys`, `/v1/dashboard`) :
                          JWT via `Authorize` (token obtenu via `POST /v1/auth/login`). Pas de headers BC.
                        - **API consommable M2M** (types métier, entreprises, opérations…) :
                          en-têtes `X-BC-Client-Id` + `X-BC-Api-Key` dans Try it out
                          (secret stocké côté serveur du développeur — pas dans Authorize).

                        À l'exécution, JWT et clé BC restent mutuellement exclusifs sur un même appel.

                        ## Tests E2E
                        Après login, utiliser le JWT Bearer sur les routes console dev et le parcours métier.

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
