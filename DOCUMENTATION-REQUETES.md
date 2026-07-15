# Documentation des requêtes — KNL Core, Business Core, backend développeur

> Écrit pour accompagner l'onglet **Audit / Requêtes** de la console développeur. Chaque endpoint listé
> ci-dessous a été vérifié dans le vrai code (`business-core`) au moment de l'écriture — aucune requête
> inventée. Voir aussi [EXPLICATION.md](EXPLICATION.md) pour le contexte général de l'architecture.

---

## 1. Les trois catégories de requêtes

Une application développeur (ex. PharmaCore) fait circuler du trafic HTTP à trois niveaux distincts :

```
Backend du développeur  →  Business Core  →  Kernel Core
   (ex. PharmaCore)          (BCaaS)           (RT-Comops)
```

| Catégorie | Sens du trafic | Visible depuis Business Core ? | Facturable ? |
|---|---|---|---|
| **KNL Core** | Business Core → Kernel | Oui (Business Core est l'appelant) | Oui |
| **Business Core** | Backend développeur → Business Core | Oui (Business Core est le serveur) | Oui |
| **Backend développeur** | Frontend/utilisateur → backend du développeur, en local | **Non** | Non (pas de visibilité = pas de facturation) |

Cette troisième catégorie n'est **pas un choix de conception à discuter** — c'est une limite physique :
Business Core ne reçoit jamais ces requêtes, il ne peut donc ni les compter ni les journaliser. C'est
pourquoi l'onglet Audit / Requêtes n'affiche que les deux premières catégories, les deux seules qui
consomment réellement le quota du développeur.

---

## 2. Catégorie KNL Core — Business Core → Kernel

Chaque ligne correspond à un appel réel trouvé dans `business-core/src/main/java/.../adapter/out/kernel/`.

| Endpoint Kernel | Adapter | Rôle | Pourquoi KNL Core |
|---|---|---|---|
| `POST /api/auth/discover-contexts`, `POST /api/auth/select-context` | `KernelAuthAdapter` | Authentifie un titulaire (login délégué) et sélectionne son contexte d'organisation | Business Core relaie l'identité au Kernel, seule source de vérité des comptes |
| `POST /oauth2/token` | `KernelAuthAdapter` | Échange les identifiants d'application Business Core contre un JWT machine (flux `client_credentials`) | Jeton technique utilisé pour tous les appels « machine » suivants |
| `POST /api/auth/sign-up` | `KernelAuthAdapter` | Crée un compte kernel lors de l'inscription développeur | Le compte développeur EST un compte Kernel |
| `POST /api/organizations` | `PersisterEntrepriseKernelAdapter` | Crée l'organisation Kernel associée à une entreprise Business Core | Le Kernel est le registre d'organisations faisant autorité |
| `POST /api/actors`, `POST /api/third-parties` | `ResoudreBeneficiaireKernelAdapter` | Résout ou crée l'acteur/tiers kernel d'un bénéficiaire (client d'une vente) | Les identités de personnes physiques/morales vivent dans le Kernel |
| `GET /api/roles`, `POST /api/roles` | `AppliquerRoleTechniqueKernelAdapter` | Liste/crée les rôles techniques Kernel et les applique à un acteur | Le contrôle d'accès technique (RBAC) est porté par le Kernel |
| `POST /api/organizations/{id}/actors...` | `RattacherAOrganisationKernelAdapter` | Rattache un acteur (ex. opérateur caisse) à une organisation Kernel | Le rattachement organisationnel est une notion Kernel |
| `POST /api/inventory/movements` | `EngagerStockKernelAdapter` | Engage un mouvement de stock (débit à la vente) | Le stock réel est géré par le Kernel, pas par Business Core |
| `GET /api/inventory/...` | `VerifierDisponibiliteKernelAdapter` | Vérifie la disponibilité d'un produit avant vente | Lecture du stock réel, source Kernel |
| `POST /api/sales/orders`, `POST /api/sales/orders/{id}/cancel` | `EnregistrerVenteKernelAdapter` | Enregistre (ou annule) une commande de vente, génère la facture | La transaction financière réelle est actée dans le Kernel |
| `GET /api/cashier/movements` | `LireTransactionsKernelAdapter` | Lit l'historique des mouvements de caisse | Lecture de transactions réelles, source Kernel |
| `POST /api/products` | `CatalogueOffreKernelAdapter` | Crée le produit Kernel correspondant à une offre déclarée | Le catalogue produit réel (avec stock) vit dans le Kernel |
| `POST /api/files` | `StockerDocumentKernelAdapter` | Stocke un document (ex. justificatif) | Le stockage de fichiers est un service Kernel |
| `GET/PUT /api/settings/...` | `DepotDeConfigurationKernelAdapter` | Lit/écrit une configuration au niveau organisation | La configuration multi-organisation est portée par le Kernel |
| `POST /api/audit` | (adapter d'audit) | Journalise un événement d'audit métier côté Kernel | Trace d'audit centralisée au niveau plateforme |

**Constat honnête** : avant ce chantier, aucun de ces appels n'était journalisé individuellement — seuls
les échecs apparaissaient dans les logs applicatifs (`log.warn`). Le nouveau journal `requete_log`
(catégorie `KNL_CORE`) enregistre désormais chaque appel, réussi ou non, avec méthode/endpoint/statut/
durée, horodaté et rattaché au tenant du développeur.

---

## 3. Catégorie Business Core — backend développeur → Business Core

Ce sont les appels que fait le **backend du développeur** (authentifié par clé API `X-BC-Client-Id` /
`X-BC-Api-Key`) vers l'API publique de Business Core. Deux familles cohabitent :

### 3.1 Runtime (clé API — éligibles, comptées au quota)

Routes explicitement ouvertes à l'authentification par clé API (`AuthRouteClassifier`) :

| Route | Rôle |
|---|---|
| `POST /v1/businesses/{id}/actors:login`, `:register` | Authentification d'un acteur terminal (ex. caissier) |
| `GET/POST /v1/businesses/{id}/operations`, `:execute` | Liste et **exécution** des opérations métier déclarées (ex. `Vendre`) — c'est le cœur du runtime |
| `GET /v1/businesses/{id}/traces`, `/traces/{id}` | Consultation des traces d'exécution d'opérations |
| `GET /v1/businesses/{id}/transactions` | Consultation des transactions |
| `GET/POST /v1/businesses/{id}/orders` | Commandes |

Chacun de ces appels est déjà comptabilisé dans le tableau de bord (compteurs Redis
`ApiKeyUsageCompteur`) **et**, depuis ce chantier, journalisé en détail (`requete_log`, catégorie
`BUSINESS_CORE`) par `UsageTrackingWebFilter`.

### 3.2 Design-time (JWT — jamais éligibles à la clé API, donc jamais dans cette catégorie)

Tout le reste de l'API (`/v1/business-types/**`, `/v1/businesses` en création, `/v1/plans`,
`/v1/dashboard`, `/v1/requetes`...) est exclusivement authentifié par **JWT** (le titulaire connecté à
la console, jamais un terminal). Ces appels ne consomment jamais le quota — c'est le développeur qui
modélise son métier depuis la console, pas son application qui tourne en production.

---

## 4. Catégorie backend développeur — invisible depuis Business Core

**Vérifié en direct pendant ce chantier** : `GET /api/medicaments` de PharmaCore (le catalogue affiché
à la caisse) lit sa **propre base Postgres locale** — aucun appel réseau vers Business Core. Le compteur
d'usage Business Core n'a pas bougé après ce test, confirmant qu'il n'a strictement aucune visibilité
sur ce type d'appel.

Ce n'est pas un défaut à corriger : c'est la conséquence directe de la promesse de la plateforme
(« développez votre application comme vous voulez, Business Core ne s'occupe que de votre cœur
métier partagé »). Un développeur reste libre d'avoir son propre cache, ses propres écrans, sa propre
logique d'affichage — tout cela n'a pas à transiter par Business Core, et ne doit donc jamais lui être
facturé.

---

## 5. Alignement architectural

**Avec la logique métier de Business Core** — Aligné. Les deux catégories visibles (KNL Core,
Business Core) correspondent exactement aux deux frontières réelles où Business Core agit : en tant que
**client** du Kernel (KNL Core) et en tant que **serveur** pour les applications développeur
(Business Core). Il n'existe pas de troisième frontière que Business Core pourrait légitimement
observer sans compromettre l'indépendance des backends développeurs.

**Avec l'objectif de simplifier le développement d'applications** — Aligné, avec une nuance à noter :
le fait que le développeur garde un backend totalement invisible de la plateforme est justement ce qui
lui laisse la liberté de choisir sa stack, son cache, ses règles d'affichage — sans dépendance forcée à
Business Core pour tout. La simplification porte sur le cœur métier partagé (offres, règles, opérations),
pas sur la totalité de l'application.

**Avec la politique de développement (facturation)** — Aligné pour le flux runtime (clé API), qui est
désormais entièrement journalisé en détail. **Écart réel identifié** (déjà signalé dans
[EXPLICATION.md](EXPLICATION.md)) : le trafic **design-time** (JWT, modélisation depuis la console)
n'est actuellement pas mesuré du tout, même pas en volume agrégé. Ce n'est pas un problème pour la
facturation actuelle (à raison, ce n'est pas un trafic applicatif en production), mais si la plateforme
veut un jour observer la charge design-time (ex. détection d'abus, capacité serveur), il faudrait
étendre `UsageTrackingWebFilter` équivalent au flux JWT — un chantier distinct, non fait ici.
