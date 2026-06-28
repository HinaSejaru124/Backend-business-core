# Authentification déléguée — « se connecter avec le kernel »

> Document de conception. Le DevOps a tranché : le Business Core agit **au nom du développeur** en présentant un JWT de ce dernier (délégation d'identité). Devenir OWNER est un acte **humain et hors-ligne** (le dev rencontre l'admin). Ce document décrit le flux d'authentification déléguée à construire.
>
> **Mise à jour 2** — intègre un **précédent concret** : la plateforme comptabilité (accounting.yowyob.com) a déjà déployé ce flux. Le **discover/select-context devient la voie principale recommandée** (§3bis), pas une simple option : une équipe sœur l'utilise en production. Deux pièges issus de leur expérience sont intégrés (§3bis + §5).
>
> **Mise à jour 1** — intègre le rapport interne **auth-core** (RT-Comops). Le kernel **est** un provider OIDC central, les **permissions sont portées dans le token**, et le flux discover/select-context résout la découverte du tenant.

---

## 1. Le besoin

Le Business Core n'a pas d'autorité propre sur le kernel. Pour créer une organisation (et bien d'autres actions), il doit agir **avec l'identité du développeur**, qui porte le rôle OWNER. Deux contraintes en découlent :

1. Obtenir un JWT du développeur **sans manipuler son mot de passe** (le mot de passe ne doit jamais transiter par le BC).
2. Pouvoir **réémettre** ce JWT après expiration (15 min) **sans** que le développeur se reconnecte à chaque fois.

C'est exactement le problème que résout **OAuth2 Authorization Code + refresh token** — le mécanisme « se connecter avec Google ».

---

## 2. Le principe (OAuth2 délégué)

```
1. Le dev clique « se connecter » sur le Business Core.
2. Le BC le redirige vers la page de connexion du KERNEL.
3. Le dev s'authentifie sur le kernel (login + consentement). Son mot de passe reste chez le kernel.
4. Le kernel redirige vers le BC avec un code d'autorisation.
5. Le BC échange ce code contre { accessToken (court), refreshToken (long) } via /oauth2/token.
6. Le BC stocke le refreshToken (chiffré). Il agit désormais au nom du dev avec l'accessToken.
7. Quand l'accessToken expire → POST /api/auth/refresh { refreshToken } → nouvel accessToken, sans le dev.
```

Le rôle OWNER n'est jamais géré par le BC : le dev devient OWNER hors-ligne (cf. §6). Le BC constate simplement, dans le token obtenu, si les permissions OWNER sont présentes.

---

## 3. Ce que le kernel offre (vérifié dans la spec)

| Élément | Endpoint kernel | État |
|---|---|---|
| Métadonnées OIDC | `GET /.well-known/openid-configuration` | présent |
| Clés de signature | `GET /.well-known/jwks.json` (RS256) | présent |
| Émission de token | `POST /oauth2/token` | présent (form-urlencoded, OAuth2 standard) |
| Introspection | `POST /oauth2/introspect` | présent |
| Infos utilisateur | `GET /oauth2/userinfo` | présent |
| Identify (email-first) | `POST /api/auth/identify { principal }` | présent |
| **Login global** | `POST /api/auth/discover-contexts { principal, password }` | présent → `{ selectionToken, contexts[] }` |
| **Sélection contexte** | `POST /api/auth/select-context { selectionToken, contextId, organizationId? }` | présent → JWT final |
| **Renouvellement** | `POST /api/auth/refresh { refreshToken }` → `RefreshTokenResponse` | présent |
| Création IAM | `POST /api/auth/register` (ClientApplication + user IAM) | présent |
| Autorisation app | `POST /api/client-applications/me/authorize` | présent — **rôle à clarifier (§7)** |

> Le kernel publie une configuration OIDC complète : c'est un serveur d'autorisation OAuth2/OIDC. Reste à confirmer qu'il expose la **page de login/consentement interactive** et qu'il accepte d'enregistrer le BC comme **client OAuth** (redirect URI). Voir §7.

> **Nuance importante** : `LoginResponse` (login simple `/api/auth/login`) ne contient **pas** de `refreshToken` — seulement `accessToken` + `expiresInSeconds`. Le refresh token vient donc du **flux OAuth2** (`/oauth2/token`), pas du login direct. C'est une raison de plus de passer par OAuth2 et non par un login proxifié.

---

## 3bis. Le flux discover/select-context — VOIE PRINCIPALE

> **Précédent concret.** La plateforme comptabilité (accounting.yowyob.com) a déployé exactement ce flux : un composant `login-modal` côté front appelle *son* backend, qui détient les clés Kernel côté serveur et exécute discover/select-context. C'est l'architecture que nous visons. Ce n'est donc plus une option théorique mais une voie éprouvée.

Le développeur se connecte avec son principal + mot de passe, **sans connaître son tenantId** : le kernel résout lui-même les tenants/organisations auxquels le principal a accès.

```
1. (optionnel) POST /api/auth/identify { principal }
   → indique si le principal va vers SIGN_IN_PASSWORD ou SIGN_UP (parcours email-first).

2. POST /api/auth/discover-contexts { principal, password }      ← login GLOBAL, sans tenantId, sans clé API
   → { selectionToken (court), expiresInSeconds, contexts[] }
   Chaque contexte = { contextId, tenantId, organizations[] }.

3. POST /api/auth/select-context { selectionToken, contextId, organizationId? }
   → token FINAL + tenantId du contexte retenu. (Le token porte déjà le tenant dans le claim tid.)

4. À expiration : POST /api/auth/refresh { refreshToken } → nouvel accessToken.
```

> ⚠️ **Détail d'implémentation** : le champ JSON s'appelle **`principal`** (c'est l'e-mail), pas `email`. Certaines docs internes écrivent `{email, password}` par simplification — le kernel attend `{principal, password}`.

### Pourquoi c'est la bonne voie pour nous
- **Multi-tenant réel** : un même e-mail peut exister dans plusieurs tenants → plusieurs contextes. Si **une seule** option → sélection automatique (pas d'écran de choix) ; si **plusieurs** → le dev choisit son espace.
- **Pas besoin de connaître le tenantId** : il se découvre. (Résout définitivement « quand définit-on le tenant ? » → jamais, on le découvre.)
- **Le token porte le tenant** (claim `tid`) ET les permissions (`authorities`). L'en-tête `X-Tenant-Id` reste néanmoins requis par les filtres (redondant mais obligatoire).
- **Le login projette orgs + services** : `LoginResponse.organizations[]` donne `organizationId, organizationCode, shortName, longName, displayName, services[]` — on sait quoi est accessible/souscrit sans appel de plus.

### Deux pièges (vécus par l'équipe comptabilité)
1. **Ne JAMAIS coder le tenant en dur.** Leur bug historique : envoyer `NEXT_PUBLIC_TENANT_ID` (= `11111111-…`, le *tenant plateforme*) dans le login → `401` puis throttle Kernel. Le tenant doit venir du contexte découvert ; une valeur d'environnement n'est qu'un dernier recours pour un déploiement mono-client.
2. **`X-Organization-Id` obligatoire** si le backend l'exige (`require-explicit=true`) : sinon erreur « no organization context ». Propager l'`organizationId` du contexte choisi, pas seulement le tenant.

### Sécurité — le compromis assumé
Dans ce flux, le mot de passe **transite par le Business Core** le temps de l'appel `discover-contexts` (le BC détient les clés Kernel côté serveur et relaie). L'équipe comptabilité a fait ce choix. L'alternative (flux Authorization Code web, §2) évite ce transit mais suppose une page de consentement kernel. **Décision d'équipe** : si le transit serveur-à-serveur est acceptable (le BC est de confiance, en HTTPS), ce flux est plus simple et déjà éprouvé.

### Limite signalée
Pas de catalogue de noms d'affichage des tenants : les `tenantId` sont des **UUID techniques**. Pour l'affichage des contextes au dev, utiliser le `displayName` des organisations (qui existe), pas l'UUID du tenant.

---

## 4. Composants à construire (côté Business Core)

> Tous dans le socle (`adapter/in/security` + `adapter/out/kernel/auth`), car c'est de l'infrastructure d'authentification partagée.

| Composant | Rôle |
|---|---|
| **Point d'entrée OAuth2** (`GET /v1/auth/kernel/login`) | construit l'URL d'autorisation du kernel et redirige le dev (avec `state` anti-CSRF, `redirect_uri`, `scope`). |
| **Callback** (`GET /v1/auth/kernel/callback`) | reçoit le `code` + `state`, vérifie le `state`, échange le code via `/oauth2/token`, récupère access + refresh. |
| **Magasin de tokens délégués** | stocke le `refreshToken` **chiffré** (réutilise `SecretCipher`) par développeur, dans `developer_account` (ou une table dédiée). |
| **Service de renouvellement** | quand l'accessToken est expiré/absent, appelle `/api/auth/refresh` et met à jour le cache. |
| **Intégration KernelClient** | au lieu de `tokenService.tokenPour(clientId, secret)` (client-credentials actuel), poser le `Bearer` = accessToken **délégué** du dev courant. |

### Ce qui change dans l'existant
- **`RegistrationService`** : ne provisionne plus une ClientApplication via `provisionner()`. Le « registration » devient le **rattachement** de l'identité kernel déléguée du dev (déclenché par le callback OAuth2). Le port `ProvisionnerAccesDev` est soit supprimé, soit repensé en « enregistrer l'accès délégué ».
- **`KernelCredentialStore`** : aujourd'hui il rend `{clientId, secret}` (client-credentials). Il devra rendre un **accessToken délégué valide** (en le renouvelant via le refresh token si besoin). C'est le changement le plus profond.
- **`KernelTokenService`** : passe du grant `client_credentials` au grant `refresh_token` (et, à la première fois, `authorization_code`).

> Le reste — `KernelClient`, le chiffrement, le désenveloppage, les adapters métier — **ne change pas**. Ils consomment un token ; seule sa provenance change. C'est l'architecture hexagonale qui protège.

---

## 5. Sécurité (points de vigilance)

- **Le mot de passe du dev ne transite jamais par le BC** : c'est tout l'intérêt du flux délégué. Ne jamais ajouter un login/mot de passe « proxifié » par facilité.
- **`state` anti-CSRF** obligatoire sur le flux Authorization Code (généré au login, vérifié au callback).
- **Refresh token = secret long** : chiffré au repos (AES-GCM via `SecretCipher`), jamais loggé, jamais renvoyé au client.
- **Cache d'accessToken** : court (TTL = `accessExpiresInSeconds`), en mémoire/Redis ; jamais persisté en clair.
- **Révocation** : prévoir d'invalider le refresh token d'un dev (déconnexion, compromission). Vérifier si le kernel a un endpoint de révocation.
- **Multi-tenant** : le token délégué est lié au développeur/tenant courant ; le `KernelCredentialStore` doit résoudre le bon token via le `BusinessContext`, jamais en mélanger deux.

---

## 6. Le parcours « devenir OWNER » (hors BC)

Acte humain, hors-ligne, qui s'insère dans le parcours global :

```
1. Le dev se connecte (OAuth2 §2, ou discover/select-context §3bis) → le BC obtient un token délégué.
2. Le BC lit les permissions DANS le token (claim authorities/permissions) ou via /oauth2/userinfo.
   Si organizations:write absent → le dev n'est pas OWNER.
3. Le BC informe le dev : « Pour créer une organisation, rencontrez l'administrateur pour devenir OWNER. »
4. Le dev rencontre l'admin → l'admin lui attribue OWNER (hors BC).
5. Le dev re-authentifie (nouveau select-context, ou refresh si le kernel ré-résout) → le token porte
   désormais organizations:write.
6. La création d'organisation fonctionne.
```

> **Confirmé par le rapport auth-core** : le JWT porte les permissions résolues (`authorities`), le `tenantId` et l'`organizationId`. Détecter « OWNER » = inspecter ces `authorities`, pas un appel dédié.

---

## 7. Questions DevOps — état après le rapport auth-core

**Résolues par le rapport :**
- ✅ **Le kernel est un provider OIDC/OAuth2 central** (`/.well-known/openid-configuration`, `/oauth2/token`, `userinfo`, `introspect`, JWKS RS256). Le flux délégué est viable.
- ✅ **Les permissions sont dans le token** (`authorities`/permissions + tenantId + organizationId). Détection OWNER = lire le token.
- ✅ **Découverte du tenant** : `discover-contexts`/`select-context` permet le login sans tenantId connu.
- ✅ **Grant `refresh_token`** : `POST /api/auth/refresh { refreshToken }` renouvelle sans le dev.
- ✅ **Projection orgs + services** : `LoginResponse.organizations[]` les fournit directement.

**Encore à trancher :**
1. **Flux web (Authorization Code) ou flux discover/select-context ?** → **Penche fortement vers discover/select-context** : c'est déjà déployé par la plateforme comptabilité, plus simple, et le transit serveur-à-serveur du mot de passe est jugé acceptable par eux. Confirmer que c'est OK pour notre niveau d'exigence sécurité. (Si exigence plus forte un jour → migrer vers Authorization Code.)
2. **Création du tout premier compte/tenant** : le flux discover/select couvre la *connexion* d'un compte existant, pas la création du **premier** tenant d'un nouveau dev. Comment un nouveau dev obtient-il son tenant initial et devient-il OWNER de sa première org ? (parcours sign-up → admin, encore à préciser avec le DevOps)
3. **À quoi sert `POST /api/client-applications/me/authorize`** ? (peut-être le mécanisme de délégation app↔user le plus direct)
4. **Après attribution OWNER**, un `refresh` suffit-il à obtenir la nouvelle permission, ou faut-il un nouveau `select-context` / login complet ?
5. **Révocation** d'un refresh token (déconnexion, compromission) : quel endpoint ?
6. **`/api/auth/register`** (création de compte par une ClientApplication à permissions IAM) : utile pour notre `/v1/registration` ? Quelles permissions IAM exige-t-il ?

> Le gros du flux est maintenant clair. La décision structurante restante est **q.1** (flux web sécurisé vs discover/select plus simple). Trancher avant de coder.

---

## 8. Plan d'implémentation (voie discover/select-context)

1. **Page de login (frontend)** : formulaire principal/mot de passe, qui appelle le BC (pas le kernel directement). Sur le modèle de la `login-modal` comptabilité.
2. **Endpoint BC `POST /v1/auth/discover`** : reçoit `{principal, password}`, relaie vers `POST /api/auth/discover-contexts` (avec les clés Kernel côté serveur), renvoie `{selectionToken, contexts[]}`.
3. **Sélection** : si un seul contexte → auto ; sinon le front affiche le choix (libellé via `displayName` des orgs).
4. **Endpoint BC `POST /v1/auth/select`** : reçoit `{selectionToken, contextId, organizationId?}`, relaie vers `select-context`, récupère le token final + tenantId + refreshToken.
5. **Stocker le refresh token chiffré** (magasin délégué, `SecretCipher`) ; cacher l'accessToken (TTL court).
6. **Propagation** : sur chaque appel kernel ultérieur, poser `X-Tenant-Id` (du contexte) **et** `X-Organization-Id` ; renouveler via `/api/auth/refresh` à expiration.
7. **Adapter `KernelCredentialStore` / `KernelTokenService`** : fournir l'accessToken délégué du dev courant (refresh auto), au lieu du grant client-credentials.
8. **Détection OWNER** : lire les `authorities` du token ; si `organizations:write` absent → notifier le dev (parcours §6).
9. **Garde-fous anti-pièges** : jamais de tenant en dur ; `X-Organization-Id` toujours non vide quand requis.
10. **Tests** : flux discover→select complet (WireMock kernel), refresh, expiration, multi-contexte, multi-tenant.

À chaque étape : `mvn test` vert avant de continuer.
