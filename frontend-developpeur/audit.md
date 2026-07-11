# Audit — Frontend Développeur (Business Core)

**Périmètre** : `frontend-developpeur/` — Next.js 15 (App Router) / TypeScript / Tailwind CSS.
**Principe directeur** : ce frontend est une **façade** du backend Business Core (`backend-business-core`, hexagonal, Spring Boot 4). Aucune donnée, endpoint, prix ou statistique n'est censé être inventé — tout doit être vérifiable dans le code backend réel. Ce document liste ce qui a été construit, corrigé, et ce qui reste un écart connu.

---

## 1. Architecture générale

Le frontend est composé de **deux espaces distincts**, séparés physiquement (pas seulement visuellement) via `components/SiteChrome.tsx` :

| Espace | Chrome | Pages | Authentification |
|---|---|---|---|
| **Site vitrine** (public) | `Navbar` (marketing) + `Footer` | `/`, `/docs`, `/pricing`, `/login` | Aucune — navbar toujours identique : *Se connecter* / *Créer un compte* |
| **Espace développeur** (`/console/*`) | Shell plein écran dédié (`app/console/layout.tsx`) : sidebar gauche fixe + barre supérieure | `/console`, `/console/docs`, `/console/pricing`, `/console/audit`, `/console/api-key` | Gardée en un seul point (`ConsoleLayout`) : session vérifiée auprès du backend (`GET /v1/auth/me`), jamais de localStorage lu directement par l'UI |

L'état d'authentification global vit dans `lib/auth-context.tsx` (`AuthProvider` / `useAuth`) — **source de vérité unique** consommée par la navbar, la console et toutes les gardes. Statuts : `loading` / `anon` / `authed`.

---

## 2. Inventaire des fichiers

```
app/
  page.tsx                     Landing page (vitrine)
  docs/page.tsx                 Documentation publique (avant inscription)
  pricing/page.tsx               Tarifs (vitrine) — catalogue de plans, sans chiffres fermes
  login/page.tsx                 Connexion + Inscription (onglets), Suspense (?tab=, ?plan=)
  audit/page.tsx                 Redirection → /console/audit (ancienne route)
  console/
    layout.tsx                   Shell app (sidebar, garde d'auth, barre mobile)
    page.tsx                     Tableau de bord (données réelles du tenant)
    docs/page.tsx                 Documentation (vue développeur connecté)
    pricing/page.tsx               Activité réelle + plan (pas de quotas/prix inventés)
    audit/page.tsx                 Sélecteur d'entreprise réel + traces d'opération
    api-key/page.tsx               Affichage des identifiants générés à l'inscription

components/
  SiteChrome.tsx                Bascule vitrine / app selon le pathname
  Navbar.tsx, Footer.tsx         Chrome vitrine (public uniquement)
  Button.tsx, Field.tsx, PasswordField.tsx   Primitives de formulaire
  CodeWindow.tsx, HttpBadge.tsx, DocsNav.tsx  Présentation de la documentation
  Container.tsx, Section.tsx, PageHeader.tsx, Reveal.tsx, Logo.tsx, tok.tsx   Mise en page / UI
  icons.tsx                      Icônes SVG inline

lib/
  api.ts                         Client HTTP (fetch + Bearer JWT), types alignés backend
  auth-context.tsx               État d'authentification global (AuthProvider/useAuth)
  endpoints.ts                   Liste des endpoints réels (source : contrôleurs REST), utilisée par les pages Documentation
  cn.ts                          Utilitaire de classes CSS (clsx-like)
```

---

## 3. Mapping frontend ↔ backend (endpoints réellement appelés)

| Fonction (`lib/api.ts`) | Méthode / route backend | Contrôleur backend | Auth |
|---|---|---|---|
| `login()` | `POST /v1/auth/login` | `AuthController` | public |
| `me()` | `GET /v1/auth/me` | `AuthController` | Bearer |
| `requestApiKey()` | `POST /v1/registration` | `RegistrationController` → `RegistrationService` | public |
| `listBusinessTypes()` | `GET /v1/business-types` | `BusinessTypeController` | Bearer |
| `listBusinesses()` | `GET /v1/businesses` | `EntrepriseController` | Bearer |
| `listTraces()` | `GET /v1/businesses/{id}/traces` | `TraceController` | Bearer |

Tous les endpoints affichés dans les pages Documentation (`lib/endpoints.ts`) sont recopiés des annotations `@GetMapping`/`@PostMapping`/... des contrôleurs réels (`BusinessTypeController`, `EntrepriseController`, `OperationController`, `TraceController`, `TransactionController`, `RoleMetierController`, `OffreController`, `RegleMetierController`, `ActeurMetierController`). Rien n'a été ajouté qui ne figure pas dans le code source du backend.

**Note** : le backend expose aussi `POST /v1/auth/register` (`AuthController`, inscription kernel *sans* clé). Ce endpoint existe mais n'est **plus appelé par le frontend** depuis l'unification du flux d'inscription sur `POST /v1/registration` (compte + clé en un seul appel) — code mort correspondant (`register()`/`SignUpResult`) retiré de `lib/api.ts`.

---

## 4. Ce qui a été construit / corrigé durant cette phase

### Séparation vitrine / application (incohérence majeure signalée par l'utilisateur)
- Avant : une seule navbar/footer marketing enveloppait tout, y compris la console → dashboard coincé au centre, navbar de connexion visible même une fois connecté.
- Après : `SiteChrome.tsx` bascule complètement de chrome selon la route. `/console/*` a son propre shell plein écran (sidebar gauche fixe, largeur pleine, barre supérieure avec identité réelle).

### Navbar strictement publique
- Ne montre plus jamais d'état "connecté" (menu compte, avatar). Uniquement *Se connecter* / *Créer un compte*, cohérent quel que soit l'état de session.

### Garde d'authentification unifiée
- Un seul point de vérification (`app/console/layout.tsx`) : `loading` → spinner ; `anon` → invite propre à se connecter (jamais de « faux » écran connecté avec un token périmé) ; `authed` → shell complet avec identité réelle (`principal`, `profil.owner`).
- Purge automatique du token sur `401` (`apiFetch`), pour éliminer les sessions fantômes.

### Inscription unifiée avec clé d'API réelle
- Le formulaire d'inscription (`/login?tab=register`) appelle directement `POST /v1/registration` — un seul geste : compte kernel + clé Business Core (`clientId`, `apiKey`, `plan`), affichée **une seule fois**, conforme au comportement réel du backend (`RegistrationService`).
- Le plan choisi sur `/pricing` est transmis via `?plan=` jusqu'au formulaire d'inscription.

### Documentation corrigée pour correspondre exactement au contrat backend
- L'exemple curl de `POST /v1/registration` utilisait un champ `nom` inexistant et omettait `firstName`/`lastName`/`password` (pourtant `@NotBlank` dans `RegistrationRequest.java`) → corrigé sur `/docs` et `/console/docs`.
- Suppression d'une classe `animate-pulse` involontaire sur un paragraphe de documentation (texte qui clignotait sans raison).
- Suppression d'une constante de code (`CODE_KEY`) définie mais jamais rendue dans `console/docs/page.tsx`.

### Suppression de données inventées (`/console/pricing`)
- **Avant** : quotas fictifs par plan (FREE : 5 types / 3 entreprises / 100 traces ; PRO : 50/30/10000...) et prix fixes inventés (49 €/mois pour PRO), en contradiction directe avec la page vitrine `/pricing` (qui reste volontairement vague : « Sur mesure », « Sur devis »).
- **Constat backend** : `planCode` est un champ texte libre, **non validé**, stocké tel quel (`DeveloperAccountEntity.plan`). Le connecteur qui pourrait réellement provisionner des quotas côté Kernel (`ProvisionnerAccesDevAdapter.provisionner`) est un **stub non implémenté** (lève `UnsupportedOperationException`) et n'est **même pas appelé** par le flux d'inscription actuel.
- **Après** : la page n'affiche plus que des compteurs réels (types métier, entreprises, traces — via l'API), et un message honnête si le plan n'est pas connu (« aucun endpoint ne permet de récupérer le plan actuel »). Renvoi vers `/pricing` pour le catalogue de plans.

### Nettoyage de code mort
- `register()` / `SignUpResult` retirés de `lib/api.ts` (endpoint `/v1/auth/register` non utilisé par le frontend).

### Docker
- `next.config.mjs` : ajout de `output: "standalone"` (sortie serveur Node minimale, requise pour une image Docker propre).
- `Dockerfile` : build multi-stage (`deps` → `builder` → `runner`, `node:20-alpine`), utilisateur non-root, `HEALTHCHECK`. `NEXT_PUBLIC_API_BASE_URL` est un `ARG`/`ENV` de build (les variables `NEXT_PUBLIC_*` sont inlinées par Next.js au moment du `next build`, pas au runtime).
- `.dockerignore`, `docker-compose.yml` (service unique, le backend a son propre `Dockerfile` séparé, hors périmètre de ce frontend).
- `public/.gitkeep` ajouté (le dossier `public/` était absent ; requis par la sortie `standalone`).

---

## 5. Limitations connues (mises de côté, décision explicite du porteur de projet)

Ces points ne sont **pas des bugs frontend** — ce sont des fonctionnalités backend qui n'existent pas encore. Le frontend ne les invente pas ; il les documente honnêtement ou masque l'UI correspondante :

1. **Régénération de clé d'API pour un compte déjà connecté** : aucune route backend ne le permet. La seule route (`POST /v1/registration`) crée compte + clé en un seul geste.
2. **Consommation/quotas détaillés par plan** : aucun endpoint de métriques n'existe. `/console/pricing` affiche uniquement des compteurs réels (types, entreprises, traces), pas de quotas.
3. **Lecture du plan courant d'un compte déjà inscrit** : aucun endpoint `GET` ne renvoie le plan associé à un compte. Le plan n'est connu que juste après l'inscription (réponse de `POST /v1/registration`), et stocké côté navigateur (`localStorage`) — invisible depuis un autre appareil/navigateur.
4. **Provisionnement Kernel réel par plan** (quotas, accès différencié) : port `ProvisionnerAccesDev` stubé côté backend, non implémenté, non appelé.

---

## 6. Vérifications effectuées

- `npx tsc --noEmit` → 0 erreur.
- `npm run build` (production) → 13/13 routes compilées, aucune erreur de lint/type.
- Recherche de statistiques marketing inventées (« 1000+ développeurs », « 99.9% uptime »...) sur la landing page → aucune trouvée.
- Recherche de code mort après chaque suppression (`grep` sur l'ensemble du projet, hors `node_modules`/`.next`).
- Vérification ligne à ligne des en-têtes d'authentification documentés (`X-BC-Client-Id`, `X-BC-Api-Key`) contre `ApiKeyAuthenticationConverter.java` (backend) → noms exacts confirmés.

## 7. Point de vigilance restant (non corrigé, à trancher par l'équipe)

- `mailto:commercial@businesscore.io` (utilisé sur `/console/pricing`) est une adresse **placeholder** — à remplacer par une vraie adresse de contact avant mise en production.
- Les noms de plans FREE/PRO/ENTERPRISE sont une convention frontend ; le backend accepte n'importe quelle chaîne pour `planCode`. Si un vrai catalogue de plans doit exister, il devra être défini et validé côté backend (aujourd'hui : aucune contrainte).
