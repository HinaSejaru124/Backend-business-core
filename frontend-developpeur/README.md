# Business Core — Frontend développeur

Plateforme pour les développeurs qui consomment l'API **Business Core** (accueil, documentation,
connexion, clé d'API, audit). **Next.js (App Router) + TypeScript + Tailwind CSS.**

Design : thème clair, **bleu uniquement** (`#1B4DF5`) + neutres, **angles nets** (aucun `border-radius`),
polices Space Grotesk / IBM Plex Sans / JetBrains Mono.

## Démarrage

```bash
# 1. Installer les dépendances (nécessite une connexion internet, une seule fois)
npm install

# 2. Configurer l'URL du backend
cp .env.local.example .env.local
# éditer .env.local si besoin (défaut : http://localhost:8080)

# 3. Lancer en développement
npm run dev
# → http://localhost:3000
```

> **Backend requis pour l'auth :** la connexion (`/login`), la console, la clé d'API et l'audit appellent
> le backend Business Core (`NEXT_PUBLIC_API_BASE_URL`). Les pages Accueil, Documentation et Tarifs
> fonctionnent sans backend.

## Structure

```
app/
  layout.tsx            Layout global (Navbar + Footer, polices)
  page.tsx              Accueil (landing)
  docs/page.tsx         Documentation (référence des endpoints réels)
  login/page.tsx        Connexion (réelle) + Inscription (UI, à câbler)
  console/page.tsx      Tableau de bord (garde d'authentification)
  console/api-key/page.tsx   Clé d'API (POST /v1/registration)
  audit/page.tsx        Audit (GET .../traces)
  pricing/page.tsx      Tarifs
components/             Navbar, Footer, Button, CodeWindow, Field, PageHeader, AuthGate, icons…
lib/
  api.ts                Client API (base URL, JWT Bearer, appels alignés backend)
  endpoints.ts          Liste réelle des endpoints (pour la doc)
```

## Endpoints consommés (alignés backend, rien d'inventé)

- `POST /v1/auth/login` — connexion → JWT stocké et rejoué en `Authorization: Bearer`.
- `GET /v1/auth/me` — profil courant (garde de la console).
- `POST /v1/registration` — génération de la clé d'API.
- `GET /v1/businesses/{businessId}/traces` — audit.

**Non câblé (volontairement) :** l'inscription utilisateur et la régénération de clé (endpoints à décider
avec l'équipe) — l'UI est prête, les `onSubmit` sont commentés.

## Build de production

```bash
npm run build && npm run start
```

Déployable tel quel sur Vercel (projet Next.js autonome).
