# Audit de réalisation — Authentification déléguée (Miguel)

> Journal d'audit du travail d'authentification.
> Branche : `feat-auth-miguel` (partie de `develop2`, qui reste intacte — le chef d'équipe fera les merge).
> Commits : `02da7ab` (feature) + `5c09ccc` (migration vers le Resource Server standard).
> Date : 2026-06-28. Référence : `docs/Guide_Special_Auth.md`.

---

## 1. Mission

Brancher le Business Core sur l'**authentification du kernel** (fournisseur d'identité central OIDC/OAuth2) :
le BC affiche son propre écran de login, **délègue la vérification du mot de passe au kernel** (jamais
stocké), reçoit un **JWT signé**, le **vérifie localement** (JWKS) pour construire le `BusinessContext`,
et **re-transmet** ce JWT au kernel sur les appels métier. **v1 = mono-tenant** (tenant en config).

---

## 2. Le bon modèle (corrige les malentendus de l'ancien doc)

`Guide_Special_Auth.md` fait autorité. Points clés vérifiés dans la spec kernel (`kernel1-api-docs.json`) :
- ❌ Pas de flux « à la Google » : `/oauth2/authorize` **n'existe pas** → on n'utilise PAS l'Authorization Code.
- ✅ Cœur du flux = `POST /api/auth/login {principal, password}` (+ en-têtes `X-Client-Id`/`X-Api-Key`/`X-Tenant-Id`).
- ✅ Le kernel renvoie `accessToken` (JWT RS256) + `authorities[]` + `organizations[]`.
- ✅ Vérification locale via `GET /.well-known/jwks.json` (clé publique RS256).
- ⚠️ **Pas de refresh token** pour l'instant → re-login à expiration (~15 min). `/oauth2/token` = token-exchange uniquement.
- ✅ OWNER = présence de `organizations:write` dans les permissions (acte d'attribution humain, hors BC).

> L'ancien `AUTHENTIFICATION-DELEGUEE (2).md` bâtissait à tort sur Authorization Code + refresh token +
> discover/select comme voie principale : abandonné. La philosophie (déléguer au kernel) reste la même.

---

## 3. Décisions

| # | Décision | Raison |
|---|----------|--------|
| 1 | v1 **mono-tenant** (tenant en config `businesscore.auth.tenant-id`) | plus rapide ; multi-tenant (discover/select) en v2 sans rien casser |
| 2 | **Pas de stockage de refresh token** | le kernel n'en fournit pas (re-login) ; décision « colonne developer_account » abandonnée |
| 3 | Identité d'app = **une ClientApplication BC** (config), token utilisateur = **JWT délégué** | conforme au modèle « identity provider centralisé » |
| 4 | Vérification JWT via le **Resource Server standard Spring** (au lieu de nimbus maison) | moins de code sécurité custom, conventions, rotation des clés gérée par Spring ; perf identique (même moteur nimbus) |
| 5 | Clé Business Core (`X-BC-*`) **conservée en repli** | non-cassant (provisioning / transitoire) |

---

## 4. Ce qui a été construit

### Entrée — le BC accepte et vérifie les JWT kernel (`adapter/in/security`)
- `SecurityConfig` : `oauth2ResourceServer().jwt()` + bean `ReactiveJwtDecoder` (`NimbusReactiveJwtDecoder`,
  JWKS + RS256 + `exp` + `iss`) ; `/v1/auth/login` public ; clé BC en repli.
- `BusinessContextJwtConverter` : `Jwt` → `BusinessContext` (claims `tid`=tenant, `actor`=acteur,
  `permissions`=rôles), token brut conservé en credentials.
- `JwtAuthenticationToken` : porte le `BusinessContext` (principal) + le token brut (credentials).
- `KernelTokenWebFilter` + `KernelTokenHolder` : propagent le JWT brut dans le Reactor Context.

### Sortie — login + re-transmission du token (`adapter/out/kernel/auth`, `KernelClient`)
- Port `AuthentifierUtilisateur` + records `ResultatLogin`, `OrganisationAccessible`.
- `KernelAuthAdapter` : `POST /api/auth/login` en **app-only** (X-Client-Id/X-Api-Key/X-Tenant-Id, sans Bearer),
  désenveloppe `data`, détecte OWNER.
- `KernelClient` : **flux délégué** (Bearer = JWT utilisateur courant + `X-Tenant-Id` + identité app BC)
  avec **repli machine** (client-credentials) inchangé — non-cassant.

### REST + use case (`adapter/in/rest/auth`, `application/usecase/auth`)
- `AuthController` : `POST /v1/auth/login` (public), `GET /v1/auth/me` (protégé).
- `AuthentificationService` (use case) ; DTO `LoginRequest`, `LoginResponse` (+ `OrganisationDto`), `MeResponse`.

### Config (`infrastructure/config`)
- `AuthProperties` (`businesscore.auth.tenant-id/issuer/jwks-uri`) ; `application.yml` enrichi.
- `pom.xml` : `spring-boot-starter-oauth2-resource-server` (~1,1 Mo, nimbus transitif géré par le BOM).

---

## 5. Endpoints exposés

| Verbe | Chemin | Accès | Rôle |
|---|---|---|---|
| POST | `/v1/auth/login` | public | connexion → JWT kernel + permissions + organisations |
| GET | `/v1/auth/me` | protégé (Bearer) | identité courante + `owner` |

Sur les autres routes `/v1/**` : `Authorization: Bearer <JWT kernel>` → vérifié (JWKS) → `BusinessContext`.

---

## 6. Comment l'authentification change le projet

- Le **tenant** vient du token (claim `tid`) — plus de tenant deviné.
- Les appels kernel re-portent le **token de l'utilisateur** (Bearer délégué) + `X-Tenant-Id`, au lieu d'un
  token machine. Le reste (adapters métier, `ResolveurContexteKernel`, désenveloppage) **n'a pas bougé** —
  l'architecture hexagonale a isolé le changement.
- Vérification JWT **locale et stateless** (clé publique en cache) → **passe à l'échelle** (pas d'appel
  réseau ni BD par requête).

---

## 7. Vérification

| Étape | Résultat |
|---|---|
| `mvn compile` (sources principales) | ✅ |
| Tests d'auth (WireMock + JWKS signé) | ✅ `KernelAuthAdapterTest` (2), `JwtVerificationStandardTest` (2), `KernelClientDelegationTest` (1) |
| Non-régression (vente legacy + moteur opérations) | ✅ 16 tests verts ensemble |
| Tests Testcontainers (contexte Spring) | ⏸️ exécutés en CI (Docker) |

Migration nimbus-maison → Resource Server standard : 16 tests toujours verts.

---

## 8. État Git

- Branche **`feat-auth-miguel`** = **tout le projet** (descend de `develop2`) + auth (`02da7ab`, `5c09ccc`).
- **`develop2` intacte** — jamais modifiée, jamais poussée (les merge sont faits par le chef d'équipe).

---

## 9. Reste à faire / points d'attention

1. **Multi-tenant** (v2) : `identify` → `discover-contexts` → `select-context` (l'architecture est prête).
2. **Questions DevOps** : `KERNEL_CLIENT_ID/SECRET` de prod (allowedServices), parcours du **premier sign-up**
   (nouveau dev → tenant initial → OWNER), qui réalise le **frontend** (écran de login).
3. **Refresh** : revoir si/quand le kernel l'expose (aujourd'hui re-login).
4. `mvn verify` complet **avec Docker** (RLS + chargement de contexte) avant merge.
5. Configurer `businesscore.auth.tenant-id` / `issuer` / `jwks-uri` selon l'environnement.
