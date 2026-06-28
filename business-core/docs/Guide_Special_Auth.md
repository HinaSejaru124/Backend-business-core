
# Utiliser le kernel comme fournisseur d'authentification (OAuth/OIDC)

> Guide pratique pour brancher une application sur l'auth du kernel.
> Lisible tel quel, et copiable à un assistant IA pour aider à l'implémentation.

## D'abord, le point qui change tout

Le kernel **n'expose pas** le flux « à la Google » (la redirection vers une page de
login + l'écran « Autorisez-vous cette app à accéder à votre compte ? »). Ce flux-là
repose sur un endpoint `/oauth2/authorize` (Authorization Code Flow) qu'on **n'a pas
implémenté** pour le moment.

Ce que le kernel expose, c'est :

- **Des API d'authentification** (`/api/auth/*`) : login, sign-up, gestion de compte.
- **Des endpoints OIDC backend** (`/oauth2/token`, `/oauth2/userinfo`,
  `/oauth2/introspect`, `/.well-known/*`) pour la vérification et l'échange de tokens
  **serveur-à-serveur**.

👉 Concrètement : **c'est TON app qui affiche l'écran de connexion**, et elle délègue
la vérification du couple identifiant/mot de passe au kernel. Le kernel est le
« cerveau » des comptes ; l'UI, c'est toi.

C'est le pattern classique d'**identity provider centralisé** : ton app ne stocke
jamais de mots de passe, elle ne fait que demander au kernel « est-ce que ce user est
légitime ? » et reçoit un **JWT signé** qu'elle utilise ensuite partout.

---

## Vue d'ensemble du flux

```
┌──────────┐   1. formulaire login     ┌─────────────┐
│  User    │ ─────────────────────────▶│   TON APP    │
└──────────┘                            │  (frontend)  │
                                        └──────┬───────┘
                                               │ 2. POST /api/auth/login
                                               │    (+ X-Client-Id, X-Api-Key, X-Tenant-Id)
                                               ▼
                                        ┌─────────────┐
                                        │   KERNEL     │ vérifie le mot de passe,
                                        │  (auth-core) │ génère un JWT RS256
                                        └──────┬───────┘
                                               │ 3. { accessToken, organizations[], authorities[] }
                                               ▼
                                        ┌─────────────┐
              4. appels API métier      │   TON APP    │ stocke le token,
              Authorization: Bearer ───▶│  (backend)   │ le vérifie via JWKS
                                        └─────────────┘
```

---

## Étape 0 — Obtenir l'identité de ton application (`client_id` / `client_secret`)

C'est l'équivalent de « créer un projet dans Google Cloud Console ». Chaque
application qui parle au kernel doit avoir une **ClientApplication** enregistrée. Ça se
fait côté admin :

```bash
POST /api/client-applications
Content-Type: application/json
Authorization: Bearer <token-admin>

{
  "clientId": "yowauth-poc",
  "name": "YowAuth POC",
  "clientSecret": "un-secret-fort",
  "planCode": "<plan>",
  "allowedServices": ["ORGANIZATION", "PRODUCT", "INVENTORY"]
}
```

Tu reçois un `clientId` + `clientSecret`. **C'est l'équipe auth qui te les
provisionne** — précise quels services ton app doit consommer (la liste
`allowedServices`), parce que si tu appelles un service qui n'est pas dans cette liste,
le kernel répond `403 CLIENT_APPLICATION_SERVICE_NOT_ALLOWED`.

> ⚠️ Le `clientSecret` n'est montré **qu'une fois** à la création. Garde-le en variable
> d'environnement, jamais en dur dans le code.

---

## Étape 1 — Connecter un utilisateur (le cœur du flux)

Ton app affiche son propre écran de login, puis ton **backend** (pas le navigateur
directement) appelle :

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: yowauth-poc" \
  -H "X-Api-Key: <client-secret>" \
  -H "X-Tenant-Id: <tenant-id>" \
  -d '{
    "principal": "user@email.com",
    "password": "le-mot-de-passe"
  }'
```

Réponse :

```json
{
  "data": {
    "id": "user-id",
    "tenantId": "tenant-id",
    "actorId": "actor-id",
    "accessToken": "<jwt-rs256>",
    "sessionToken": "<jwt-rs256>",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "authorities": ["organizations:write", "products:read"],
    "organizations": [
      {
        "organizationId": "uuid",
        "organizationCode": "ORG-001",
        "displayName": "...",
        "services": ["COMMERCIAL", "PRODUCT", "INVENTORY"]
      }
    ]
  }
}
```

Les 3 choses à retenir :

- **`accessToken`** → c'est LE token (le `sessionToken` est juste un alias de
  compatibilité, ignore-le).
- **`authorities`** → les permissions du user (utile pour afficher/cacher des éléments
  dans ton UI).
- **`organizations[]`** → à quelles organisations le user appartient et quels modules
  chacune utilise. Si un user a plusieurs organisations, c'est ici que TON app affiche un
  « choix d'organisation » (l'équivalent du « choix de compte » Google).

> Note multi-tenant : si tu ne connais pas encore le `tenantId` au moment du login, il y
> a `POST /api/auth/identify` puis `/discover-contexts` / `/select-context` pour le
> découvrir. Pour un POC mono-tenant, tu peux mettre le `tenantId` en config et sauter
> cette étape.

---

## Étape 2 — Appeler les API protégées du kernel

Une fois que tu as l'`accessToken`, tu le mets en header sur chaque appel :

```bash
curl http://localhost:8080/api/users/me \
  -H "X-Client-Id: yowauth-poc" \
  -H "X-Api-Key: <client-secret>" \
  -H "X-Tenant-Id: <tenant-id>" \
  -H "Authorization: Bearer <accessToken>"
```

Pour un endpoint scopé à une organisation (ou une agence), tu ajoutes :

- `X-Organization-Id: <organization-id>` (obligatoire pour les routes métier scopées)
- `X-Agency-Id: <agency-id>` si besoin

Les règles d'accès appliquées par le kernel, dans l'ordre :

1. ta `ClientApplication` doit être autorisée sur le service de la route,
2. l'organisation doit être abonnée à ce service,
3. les quotas (tenant + organisation) doivent laisser passer l'appel.

Sinon tu reçois un code clair : `403 ...SERVICE_NOT_ALLOWED`,
`400 ORGANIZATION_CONTEXT_REQUIRED`, `403 ORGANIZATION_SERVICE_NOT_SUBSCRIBED`,
`429 ...QUOTA_EXCEEDED`. Ces codes sont tes amis pour débugger.

---

## Étape 3 — Vérifier le token côté ton app (sans rappeler le kernel)

C'est la partie « OAuth/OIDC » propre. Ton app peut valider le JWT **toute seule**, sans
appeler le kernel à chaque requête, grâce à la clé publique :

```bash
curl http://localhost:8080/.well-known/jwks.json
```

Tu récupères la clé publique RS256, et tu vérifies la signature du token. Les claims
utiles à l'intérieur du JWT :

| Claim         | Signification                  |
| ------------- | ------------------------------ |
| `sub`         | l'`userId`                     |
| `tid`         | le `tenantId`                  |
| `oid`         | `organizationId` (si présent)  |
| `aid`         | `agencyId` (si présent)        |
| `actor`       | `actorId`                      |
| `permissions` | la liste des permissions       |
| `iss`         | l'émetteur (à vérifier)        |
| `exp`         | expiration                     |
| `jti`         | identifiant unique du token    |

Ton app vérifie : **signature RS256 + `iss` + `exp`**, puis lit `tid`/`oid`/`permissions`
pour décider ce que le user a le droit de faire. C'est exactement le principe d'un
« Verify ID token » OIDC.

Alternative si tu préfères demander au kernel : `GET /oauth2/userinfo` avec le Bearer
token te renvoie les infos du user, et `POST /oauth2/introspect` te dit si un token est
encore actif.

---

## Étape 4 (avancé, optionnel) — Token-exchange entre services

Tu n'en as **pas besoin pour démarrer**. C'est pour quand un token SSO partagé doit être
échangé contre un token scopé à un service précis (scénario multi-plateformes). Le seul
`grant_type` supporté est le token-exchange :

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Authorization: Basic $(printf '%s' 'yowauth-poc:<secret>' | base64 -w0)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
  --data-urlencode "subject_token_type=urn:ietf:params:oauth:token-type:jwt" \
  --data-urlencode "subject_token=<token-sso>" \
  --data-urlencode "context_id=<context-id>" \
  --data-urlencode "organization_id=<org-id>" \
  --data-urlencode "service_code=ORGANIZATION"
```

(Le `context_id` se découvre via `/oauth2/userinfo`.) Garde ça dans un coin pour plus
tard.

---

## Récap des règles à toujours respecter

- Toujours envoyer les 3 headers d'identité d'app : `X-Client-Id`, `X-Api-Key`,
  `X-Tenant-Id`.
- Toujours mettre `Authorization: Bearer <accessToken>` pour les appels au nom d'un user.
- Ne jamais stocker de mot de passe côté ton app : tu délègues au kernel.
- Vérifier le token via le JWKS, pas en faisant confiance aveuglément.
- Le token expire (`expiresInSeconds`, ~15 min) : prévois de redemander un login quand il
  expire (pas de `refresh_token` pour l'instant).

---

## To-do pour démarrer

1. Récupérer son `client_id` / `client_secret` (préciser les services nécessaires).
2. Coder l'écran de login → il appelle `POST /api/auth/login`.
3. Stocker l'`accessToken` et le rejouer en `Bearer` sur les appels.
4. Vérifier le token avec `/.well-known/jwks.json`.

---

## Référence des endpoints

| Endpoint                               | Rôle                                         |
| -------------------------------------- | -------------------------------------------- |
| `POST /api/auth/login`                 | connexion d'un utilisateur                   |
| `POST /api/auth/sign-up`               | création de compte                           |
| `POST /api/auth/identify`              | découverte du contexte (multi-tenant)        |
| `POST /api/client-applications`        | enregistrer une application cliente (admin)  |
| `GET /.well-known/openid-configuration`| métadonnées OIDC (discovery)                 |
| `GET /.well-known/jwks.json`           | clé publique RS256 pour vérifier les tokens  |
| `POST /oauth2/token`                   | token-exchange (backend-à-backend)           |
| `GET /POST /oauth2/userinfo`           | infos utilisateur à partir d'un token        |
| `POST /oauth2/introspect`              | valider/inspecter un token (RFC 7662)        |
