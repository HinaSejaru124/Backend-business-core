# Référence Kernel (IWM Backend) — pour le Business Core

> Tout ce que l'équipe doit savoir pour appeler le kernel **correctement**. Construit à partir de la spec OpenAPI live (`https://kernel-core.yowyob.com/v3/api-docs`) : 967 endpoints, 114 cores. Ce document ne couvre **que** les ~30 cores que le Business Core utilise.
>
> À donner en contexte à ton IA de code avec ton fichier `feat-*.md`.

---

## 0. Règles d'or (à lire absolument)

### 0.1 Authentification — trois en-têtes
Tout appel `/api/**` exige **deux en-têtes** d'application :
- `X-Client-Id` : l'id de la ClientApplication
- `X-Api-Key` : son secret

Les endpoints protégés exigent **en plus** :
- `Authorization: Bearer <accessToken>` (JWT)

Le `KernelClient` du socle pose ces trois en-têtes automatiquement. **N'écris jamais un appel kernel sans passer par lui.**

### 0.2 En-têtes contextuels
- `X-Tenant-Id` : requis au sign-up / login (espace d'isolation racine).
- `X-Organization-Id` : requis pour toute opération liée à une organisation. Utilise `KernelClient.postForOrganization / getForOrganization`.

### 0.3 Toutes les réponses sont enveloppées
Le kernel ne renvoie jamais l'objet directement. Il renvoie une enveloppe :
```json
{ "success": true, "data": { ... }, "message": "...", "errorCode": null, "timestamp": "..." }
```
**Le vrai contenu est dans `data`.** Tes mappings doivent désérialiser `data`, pas la racine. Un `errorCode` non nul signale une erreur métier même avec un HTTP 200.

### 0.4 Le token expire vite
`expiresInSeconds = 900` (15 min). Le socle met le JWT en cache Redis avec ce TTL et le renouvelle. Un `403 Access Denied` en cours d'usage = token périmé → le socle redemande un token.

### 0.5 OAuth2 token = form-urlencoded
`POST /oauth2/token` n'est **pas** du JSON : c'est le standard OAuth2 (`application/x-www-form-urlencoded`). Le socle le gère ; ne le réimplémente pas en JSON.

---

## 1. Accès & Authentification

### client-application-controller
| Méthode | Endpoint | Usage |
|---|---|---|
| POST | `/api/client-applications` | Provisionner une ClientApplication (notre `ProvisionnerAccesDev`) |
| POST | `/api/client-applications/{id}/rotate-secret` | Rotation du secret |
| POST | `/api/client-applications/{id}/revoke` | Révoquer |
| GET | `/api/client-applications/me` | S'auto-décrire |

**Créer (`CreateClientApplicationRequest`)** : `clientId*`, `name*`, `description`, `clientSecret*`, `planCode`, `allowedServices[]`.

### auth-controller (22 endpoints — flux d'identité)
Les plus utiles : `POST /api/auth/sign-up`, `/api/auth/login`, `/api/auth/login/mfa/confirm`, `/api/auth/select-context`, `/api/auth/discover-sign-up-contexts`.

**Sign-up (`PublicSignUpRequest`)** : `firstName*`, `lastName*`, `email*`, `username`, `password`, `phoneNumber`, `accountType` (utiliser `BUSINESS`, pas `PROSPECT`), `businessType`, `tenantId` (ou `signUpSelectionToken`+`contextId`). Mot de passe ≥ 10 car. (Maj+min+chiffre+symbole).

**Login (`LoginRequest`)** : `principal*` (email), `password*`. Réponse `LoginResponse` : `accessToken`, `expiresInSeconds`, `tenantId`, `actorId`, `mfaEnabled`, `authorities[]`, `organizations[]`. Si `202` + `nextStep=CONFIRM_MFA` → confirmer via `/api/auth/login/mfa/confirm`.

### auth-oidc-controller
`POST /oauth2/token` (form-urlencoded), `POST /oauth2/introspect`, `GET /oauth2/userinfo`, `GET /.well-known/openid-configuration`.

---

## 2. Organisation, Agence, Services

> ⚠️ **Piège n°1 corrigé.** Créer une organisation a un **prérequis** et exige bien plus que `{name}`.

### Le flux correct de création d'entreprise (`PersisterEntreprise`)
```
1. POST /api/actors/onboarding   → crée le business actor, récupère businessActorId
2. POST /api/organizations       → crée l'organisation (businessActorId requis)
3. POST /api/organizations/{id}/agencies   → ajoute une agence
4. POST /api/organizations/{id}/services   → souscrit les services nécessaires
```

**Business actor (`BusinessActorRequest`)** : `name*`, `code`, `type`, `role`, `isIndividual`, `isActive`, `businessId`, `niu`, `tradeRegistryNumber`…

**Organisation (`CreateOrganizationRequest`)** : `businessActorId*`, `code*`, `service*`, `shortName*`, `longName*`, + optionnels (`email`, `taxNumber`, `legalForm`, `ceoName`, `capitalShare`, `logoId`…).

### Cycle de vie de l'organisation (à mapper sur notre `CycleVie`)
| Action kernel | Notre CycleVie |
|---|---|
| `POST /api/organizations/{id}/approve` | activation |
| `POST /api/organizations/{id}/suspend` | SUSPENDUE |
| `POST /api/organizations/{id}/close` | FERMEE |
| `POST /api/organizations/{id}/reopen` | réactivation |
| `POST /api/organizations/{id}/transfer/{newOwnerId}` | changement de propriétaire |

> **Recommandation** : ne réinventons pas le cycle de vie ; appuyons notre `Entreprise.cycleVie` sur ces transitions kernel.

### Services souscrits (organization-service-controller)
`GET /api/organizations/services/catalog` (catalogue), `POST /api/organizations/{id}/services` (souscrire : `serviceCode`, `requestQuotaLimit`, `requestQuotaWindowSeconds`). Services : ACCOUNTING, COMMERCIAL, BILLING, CASHIER, HRM…
> Certaines opérations exigent que le service correspondant soit souscrit (ex. inviter un employé → HRM).

---

## 3. Acteurs, Rôles, Tiers (Acteurs métier)

### actor-controller
| Endpoint | Usage |
|---|---|
| `POST /api/actors` | Créer un acteur (`ResoudrePersonne`) |
| `POST /api/actors/onboarding` | Créer un **business actor** (prérequis organisation) |
| `GET/PUT /api/actors/me` | L'acteur courant |

**`CreateActorRequest`** : `firstName*`, `lastName*`, `organizationId`, `name`, `email`, `phoneNumber`, `type`, `profession`, `birthDate`…

### role-controller (`AppliquerRoleTechnique` = 2 appels)
```
POST /api/roles               → créer le rôle technique
POST /api/roles/assignments   → l'assigner à un user
```
**`CreateRoleRequest`** : `code*`, `name*`, `permissions[]*`, `scopeType` (TENANT | ORGANIZATION).
**`AssignRoleToUserRequest`** : `userId*`, `roleId*`, `scope*`, `scopeType`, `scopeId`.
> Codes réservés (ACCOUNTANT, ORGANIZATION_ADMIN…) → `ADMIN_ROLE_PROTECTED`. Utilise un code libre.
> Une permission hors du scope du rôle → `ADMIN_PERMISSION_INVALID`. Aligne `scopeType` et permissions.

### third-party-controller (`ResoudreBeneficiaire`)
> ⚠️ **Piège n°2.** Un tiers n'est **pas autonome** : c'est un rôle commercial attaché à un acteur ou une organisation existante.

```
1. (créer/trouver l'actor)        → partyId
2. POST /api/third-parties        → le déclarer comme tiers
```
**`CreateThirdPartyRequest`** : `organizationId*`, `partyType*` (ACTOR | ORGANIZATION), `partyId*`, `code*`, `name*`, `roles[]*` (ex. CUSTOMER, SUPPLIER), + commercial (`authorizedCreditLimit`, `maxDiscountRate`, `vatSubject`, `payTermType`…).

---

## 4. Produits (Offre)

### product-controller / product-catalog-controller
| Endpoint | Usage |
|---|---|
| `POST /api/products` | Créer un produit (`GererCatalogueOffre`) |
| `GET /api/products/{id}` | Lire |
| `POST /api/products/{id}/prices` | Ajouter un prix |
| `GET /api/products/{id}/prices/effective` | **Prix effectif** (notre lecture de prix) |
| `GET/POST /api/product-categories` | Catégories |

**`CreateProductRequest`** : `organizationId*`, `sku*`, `name*`, `familyCode*`, `variantLabel*`, `unitPrice*`, `currency*`, + `categoryCode`, `barcode`, `cost`, `uom`, `quantity`, `allowedSaleSizes[]`.

> **Traduction de notre Offre** : notre `DefinitionOffre` (riche, capacités activables) → un `CreateProductRequest`. Une offre STOCKABLE crée un produit avec `quantity`/`uom` ; SUR_DEVIS → produit sans prix fixe (gérer `formePrix`). C'est le rôle de l'adapter.

---

## 5. Stock (Disponibilité)

### inventory-controller
| Endpoint | Usage |
|---|---|
| `GET /api/inventory/movements/balance` | Lire le solde (`VerifierDisponibilite`) |
| `POST /api/inventory/movements` | Enregistrer un mouvement |
| `POST /api/inventory/movements/{id}/validate` | **Valider** un mouvement (2ᵉ temps) |

> ⚠️ **Piège n°3.** Lire le solde exige **trois** query params : `organizationId*`, `agencyId*`, `productId*`. Pas seulement le produit.

**`RecordStockMovementRequest`** : `organizationId*`, `agencyId*`, `productId*`, `movementType*`, `quantity*`, `thirdPartyId`, `referenceNumber`, `sourceDocumentType`…

> **Réservation en 2 temps (pour la compensation)** : `record` crée le mouvement, `validate` l'engage. Si une vente échoue avant `validate`, rien à compenser sur le stock. Privilégie ce modèle (cf. doc compensation).

---

## 6. Vente & Encaissement (Transactions)

### sales-controller (`EnregistrerVente`)
| Endpoint | Usage |
|---|---|
| `POST /api/sales/orders` | Créer une commande |
| `POST /api/sales/orders/{id}/confirm` | Confirmer |
| `POST /api/sales/orders/{id}/cancel` | **Annuler (compensation)** |

**`CreateSalesOrderRequest`** : `currency*`, `organizationId`, `agencyId`, `customerThirdPartyId`, `productId`, `quantity`, `unitPrice`, ou `lines[]` (multi-lignes).

### cashier — le paiement est une CHAÎNE
> ⚠️ **Piège n°4 corrigé (le plus important).** `/api/cashier/payments` **n'existe pas**. Le paiement réel :
```
1. POST /api/sessions     → ouvrir une session de caisse (registerId, cashierId, openingAmount, currency)
2. POST /api/bills        → créer la facture (customerId, reference, totalAmount, currency)
3. POST /api/bills/pay    → payer (amount, sessionId, registerId)
```
Lecture : `GET /api/cashier/bills/{id}`, `GET /api/cashier/movements`, `GET /api/cashier/sessions`.
Clôture de session : `POST /api/sessions/{id}/close`, rapports `X`/`Z` (`/api/reports/x|z/{sessionId}`).

> Le cashier core est vaste (registres, sessions, réconciliations, dénominations). Pour notre `PorteMonnaie`, le strict minimum est la chaîne session→bill→pay.

---

## 7. Configuration, Documents, Audit

### operational-policy-controller (`DepotDeConfiguration`)
`GET/PUT /api/settings/organizations/{id}/operational-policy` (niveau organisation)
`GET/PUT /api/settings/organizations/{id}/agencies/{agencyId}/operational-policy` (niveau agence)

### file-controller (`StockerDocument`)
`POST /api/files` (déposer), `GET /api/files/{id}/content` (télécharger), `GET /api/files/{id}/metadata`, `GET /api/files/{id}/review`.

### Audit (`JournaliserAudit`)
`POST /api/audit` (core cashier) — **`CreateAuditEntryRequest`** : `action*`, `targetType*`, `targetId`, `details`.
`GET /api/system-audits/organization` (lecture audit d'org), `/integrity-check`.
Observabilité : `GET /api/observability/outbox/events` (lecture seule — vérifier les événements publiés, **pas** un endpoint de compensation).

---

## 8. Tableau de correction des adapters

Récapitulatif des écarts entre le code actuel et le kernel réel :

| Adapter | Problème | Correction |
|---|---|---|
| `ExecuterWorkflow` (saga) | `/api/sagas` **inexistant** | Supprimé → compensation applicative (voir doc compensation) |
| `PorteMonnaie` | `/api/cashier/payments` **inexistant** | Chaîne session → `/api/bills` → `/api/bills/pay` |
| `PersisterEntreprise` | envoyait `{name}` seul | onboarding actor + 5 champs obligatoires de l'org |
| `GererCatalogueOffre` | champs produit incomplets | `sku`, `familyCode`, `variantLabel`, `unitPrice`, `currency` |
| `VerifierDisponibilite` | manquait org + agence | 3 query params : `organizationId`, `agencyId`, `productId` |
| `ResoudreBeneficiaire` | tiers traité comme autonome | créer l'actor d'abord, puis le tiers (`partyType`+`partyId`) |

---

## 9. Erreurs fréquentes du kernel

| Erreur | Cause | Résolution |
|---|---|---|
| `401` sur `/api/**` | `X-Client-Id`/`X-Api-Key` invalides (dev-* en prod) | ClientApplication de prod |
| `403 Access Denied` en cours | token périmé (15 min) | redemander un token |
| `403` sur `/api/organizations` | pas `organizations:write` (pas OWNER) | devenir OWNER d'abord |
| `ORGANIZATION_CONTEXT_REQUIRED` | en-tête `X-Organization-Id` absent | l'ajouter (`postForOrganization`) |
| `ADMIN_ROLE_PROTECTED` | code de rôle réservé | code libre |
| `ADMIN_PERMISSION_INVALID` | permission hors scope du rôle | aligner `scopeType` + permissions |
| `500` au sign-up | `accountType: PROSPECT` | utiliser `BUSINESS` |
| `AUTH_INVALID_REQUEST` | mot de passe trop faible | ≥ 10 car. Maj+min+chiffre+symbole |
