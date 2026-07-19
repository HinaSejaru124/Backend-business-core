# Refonte — alignement Business Core sur le modèle Organisation → Applications

Résumé de l'alignement conceptuel de Business Core avec la vision du professeur (Organisation Kernel
possédant plusieurs Applications) et avec les capacités réelles du Kernel, vérifiées par inspection de
`docs/kernel1-api.json`. Renommage de façade uniquement — aucune classe de domaine ni table SQL
renommée, aucune régression sur le paiement, la RLS, l'authentification ou les opérations existantes.

## 1. Architecture cible et état actuel

```
Utilisateur
    │
    ▼
Authentification KCore (POST /v1/auth/login)
    │
    ▼
organisations[] (déjà dans la réponse de login — LoginResponse.organizations[] du Kernel)
    │
    ▼
Business Core
    │
    ▼
POST /v1/applications (alias de /v1/businesses)
    organizationId optionnel
    │
    ├── organizationId fourni  → rattachement à l'organisation Kernel existante
    │                             (aucun POST /api/organizations)
    │
    └── organizationId absent → comportement inchangé : provisionnement complet
                                  d'une nouvelle organisation Kernel
    │
    ▼
Application
    ├── Api Key       (une seule active à la fois, scopée à l'application)
    ├── Contrat       (callback/success/error/cancel URLs, clé publique)
    └── Profil        (description, logo, couleur, support email, site web, environnement)
```

## 2. Ce qui a été renommé (façade uniquement)

- **Endpoints** : `/v1/businesses/**` reste l'endpoint canonique ; `/v1/applications/**` a été ajouté
  en alias sur les 6 controllers de **gestion développeur** (`EntrepriseController`,
  `BusinessApiKeyController`, `EntrepriseContratController`, `EntrepriseConfigController`,
  `EntrepriseProfilController`) via `@RequestMapping({"/v1/businesses", "/v1/applications"})`. Les deux
  chemins pointent vers le même controller, donc un comportement strictement identique.
- **Swagger** : tags (`@Tag`), résumés et descriptions d'opérations (`@Operation`), descriptions de
  schémas (`@Schema`) — "Entreprise(s)" → "Application(s)" partout où le mot désignait réellement une
  application cliente.
- **Messages d'erreur RFC 7807** (`ProblemException`) : "Entreprise introuvable" → "Application
  introuvable", et équivalents, dans tous les use cases qui les lèvent.
- **Champs JSON de DTO publics** (renommés directement, sans double transition — décision explicite,
  aucun front n'est cassé par ce choix pour l'instant) :
  - `DashboardResponse` : `nombreEntreprises` → `nombreApplications`, `TopEntreprise` →
    `TopApplication` (son champ `entrepriseId` devient `applicationId`), `ActiviteItem.entrepriseNom` →
    `applicationNom`.
  - `AdminService` : `EntrepriseRow` → `ApplicationDuDeveloperRow` (renommé ainsi pour ne pas entrer en
    collision avec `ApplicationRow`, déjà existant pour la vue globale plateforme, qui a une forme
    différente avec `developerId`/`developerEmail`) ; `DeveloperDetail.entreprises` → `.applications` ;
    `DeveloperRow.nbEntreprises` → `.nbApplications` ; `CleRow.entrepriseId` → `.applicationId`.

## 3. Ce qui n'a volontairement PAS été renommé

- **Classes de domaine Java** : `Entreprise`, `EntrepriseService`, `EntrepriseEntity`,
  `EntrepriseRepository`, `EntrepriseContrat`, `EntrepriseProfil`, etc. restent en français, inchangées.
  Le développeur consommant l'API voit "Application" partout ; le code interne continue de vivre avec
  "Entreprise". Justifié par la surface (119 fichiers référencent ce vocabulaire) pour un gain déjà
  atteint en façade.
- **Tables et colonnes SQL** : `entreprise`, `entreprise_contrat`, `entreprise_profil`, et toutes les
  colonnes `entreprise_id` des tables liées — aucun renommage, aucune migration destructive.
- **`business-types` / `TypeMetier`** : concept différent (le gabarit — "Pharmacie", "Banque" — dont une
  Application est l'instance épinglée à une version). Le mot "Business" y est correct et n'a pas été
  touché.
- **Valeurs de code machine** consommées par un client au même titre qu'un enum : le champ `portee`
  retourné par `RegleMetierResponse`/`ParametreConfigResponse` garde la valeur littérale `"ENTREPRISE"`
  (et `ProblemException.violatedRule("ENTREPRISE_NON_ACTIVE")`), par prudence — seuls leurs textes
  descriptifs `@Schema` ont été clarifiés.
- **`X-BC-*`** (headers de clé API) : "BC" signifie "Business Core", pas l'entité Application — aucun
  risque de confusion à lever ici.

## 4. Nouvelles fonctionnalités additives

- **`EntrepriseProfil`** (fiche produit) : nouvelle table `entreprise_profil` (RLS conforme au socle),
  port `DepotEntrepriseProfil`, service `EntrepriseProfilService`, endpoints
  `GET/PUT /v1/businesses/{id}/profile` (+ alias `/v1/applications/{id}/profile`). Champs : description,
  logoUrl, couleur (validée `#RRGGBB`), supportEmail (validé par regex), siteWebUrl,
  `EnvironnementApplication` (DEVELOPPEMENT/TEST/PRODUCTION). Distincte du contrat technique
  (`EntrepriseContrat`, déjà livré en session précédente : callback/success/error/cancel URLs + clé
  publique) — deux responsabilités séparées, comme recommandé.
- **`organizationId` optionnel sur `POST /v1/businesses`** (et son alias `/v1/applications`) : nouveau
  paramètre nullable dans `CreerEntrepriseRequest`. Fourni → `EntrepriseService.rattacherOrganisationExistante`
  résout uniquement le business actor courant (`PersisterEntreprise.resoudreBusinessActorCourant`,
  nouvelle méthode de port) et l'agence principale de l'organisation ciblée
  (`PersisterEntreprise.trouverAgencePrincipale`, déjà existant) — **aucun** `POST /api/organizations`.
  Absent → comportement historique intégralement conservé (`provisionnerOrganisation`).
- **Admin** : `GET /v1/admin/applications` (vue globale de toutes les applications de la plateforme,
  tous développeurs confondus) ; `DeveloperRow`/`Overview` portent désormais `nbErreursMois` et
  `tempsReponseMoyenMs` (agrégés par une nouvelle requête `RequeteLogRepository.statsParTenant`, fenêtre
  glissante de 30 jours).
- **Dashboard développeur** : `tempsReponseMoyenMs` ajouté à `DashboardResponse`, même fenêtre de 30
  jours que les autres métriques du tableau de bord.

## 5. Limitations connues, dépendantes du Kernel

Inspection exhaustive de `docs/kernel1-api.json` (spec OpenAPI réelle du Kernel) :

| Capacité | Disponible côté Kernel ? |
|---|---|
| Créer une organisation | Oui — `POST /api/organizations` (déjà utilisé) |
| Lister les organisations d'un utilisateur | Oui — `GET /api/organizations/my`, et déjà exposé dans `LoginResponse.organizations[]` au login |
| **Rejoindre une organisation existante (second développeur)** | **Non — aucun endpoint équivalent trouvé** dans tout le spec |
| Concept "Application" (au sens développeur/organisation) | **Non — n'existe pas.** `ClientApplication` (`/api/client-applications`) désigne l'entité OAuth qui représente Business Core lui-même comme client machine du Kernel, pas l'application d'un développeur |

**Conséquence directe** : le paramètre `organizationId` de `POST /v1/businesses` permet de préparer le
rattachement d'une Application à une organisation existante, mais **seulement pour le développeur qui a
déjà accès à cette organisation** (aucune vérification de propriété supplémentaire n'a été ajoutée, car
il n'y a rien côté Kernel à vérifier au-delà de ce qui existe déjà). Tant que le Kernel n'expose pas de
mécanisme permettant à un **second** développeur de rejoindre une organisation qu'il ne possède pas
encore, le modèle "une organisation, plusieurs développeurs" reste théorique côté Business Core — il ne
peut pas être complété unilatéralement de ce côté.

**Question à trancher avec le professeur / l'équipe Kernel**, formulée telle quelle :
> Le Kernel expose la création d'une organisation, mais pas le rattachement d'un développeur à une
> organisation existante détenue par un autre développeur. Devons-nous considérer qu'une organisation
> ne possède qu'un seul développeur propriétaire pour cette version, ou un mécanisme de rattachement
> est-il prévu côté Kernel ?

## 6. Validation

- Build : `mvn clean test` → **BUILD SUCCESS**, **187/187 tests verts** (0 échec, 0 erreur), y compris
  `RlsCoverageGuardTest` et `TenantIsolationRlsTest` (les nouvelles tables `entreprise_contrat` et
  `entreprise_profil` respectent la RLS obligatoire du socle).
- Aucune modification au paiement, à la RLS, à l'authentification KCore ou aux opérations métier
  existantes.
