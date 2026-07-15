# Cahier des charges — Frontend PharmaCore (interface pharmacien / caissier)

**Rôle de ce document** : spécification extrêmement détaillée, pas à pas, de l'interface Next.js de PharmaCore. Elle consomme **exclusivement** l'API du backend Pharmacie (`pharmacie-backend-test/backend-test.md` §4) — **jamais** directement Business Core, et **jamais** la clé API BCaaS (qui reste un secret serveur-à-serveur, cf. `GUIDE-PROJET-PHARMACORE.md` §4.4). Avant de commencer, lire le guide et le cahier des charges backend : chaque écran ci-dessous s'appuie sur un endpoint qui y est défini, rien n'est inventé côté données.

**Dossier** : `pharmacie-frontend-test/`
**Stack imposée** : **Next.js (App Router)**, TypeScript, Tailwind CSS. **Pas de React+Vite** (la base de code de départ fournie par la collègue était en React/Vite — elle sert de référence fonctionnelle pour les écrans déjà pensés — Dashboard, Types, Configuration — mais est **entièrement reconstruite en Next.js**, cohérent avec `frontend-developpeur`).
**Port proposé** : `3001` (le `3000` est déjà pris par `frontend-developpeur`).
**Backend consommé** : `pharmacie-backend-test` sur `:9090` (configurable via `NEXT_PUBLIC_PHARMACORE_API_URL`).

---

## 1. Principe de sécurité — à ne jamais violer

Le frontend ne connaît **aucun secret Business Core**. Il appelle uniquement `http://localhost:9090/api/*` (le backend Pharmacie). C'est le backend Pharmacie qui, lui, détient `X-BC-Client-Id`/`X-BC-Api-Key` et parle à BCaaS. Si un jour ce frontend a besoin d'un compte utilisateur (pharmacien/caissier qui se connecte), ce sera une authentification **propre à PharmaCore** (ex. JWT émis par le backend Pharmacie), sans rapport avec le JWT de `frontend-developpeur`. **Périmètre v1** : pas d'authentification utilisateur (application interne de test) — à confirmer/ajouter en v2 si besoin.

---

## 2. Design system — santé, professionnel, vert vif, pas de dégradé

Objectif explicite : **moderne, professionnel, lisible en usage intensif (caisse)**. Pas de multicolore, pas de gradient, angles très légèrement adoucis (jamais arrondis excessifs).

### 2.1 Palette (proposition à valider, cohérente avec la charte demandée)
| Rôle | Couleur | Usage |
|---|---|---|
| `brand` (DEFAULT) | `#16A34A` (vert vif, franc) | Actions principales, liens actifs, accents |
| `brand-hover` | `#128038` | États hover/actifs des boutons primaires |
| `brand-tint` | `#ECFDF5` | Fonds discrets (badges, sélection légère) |
| `ink` | `#0B2A1C` (vert très sombre, quasi noir) | Titres, texte fort, sidebar |
| `body` | `#111827` | Texte courant |
| `muted` | `#5B6B62` | Texte secondaire |
| `line` | `#DCE7DF` | Bordures hairline |
| `subtle` | `#F4F8F5` | Fonds de section alternés |
| `danger` | `#B42318` | Ruptures de stock, erreurs, alertes bloquantes |
| `warning` | `#B45309` | Alertes non bloquantes (stock bas) |
| `ok` | `#157347` | Confirmations, statut COMPLETEE |

**Interdits explicites** : aucun `linear-gradient`/`radial-gradient` sur les fonds ou boutons ; pas de palette arc-en-ciel (une seule teinte d'accent : le vert, + rouge/orange strictement réservés aux états d'alerte).

### 2.2 Typographie
Réutiliser la même base que `frontend-developpeur` pour la cohérence d'équipe (même stack, même famille de projets) : **Space Grotesk** (titres/display), **IBM Plex Sans** (texte courant), **JetBrains Mono** (identifiants, codes, montants techniques). Chargées via `next/font/google`.

### 2.3 Rayons et bordures
`border-radius` global très faible (`2px`–`4px` maximum, jamais `rounded-full`/`rounded-xl` par défaut) — cohérent avec la consigne « pas trop arrondi ». Bordures `1px` `line` par défaut, jamais d'ombre lourde (`shadow-sm`/`shadow-md` discrètes seulement, pas de `shadow-2xl` décoratif).

### 2.4 Composants de base à construire (miroir de `frontend-developpeur/components/`)
`Button` (variants primary/secondary/danger/ghost), `Field`, `Select`, `Badge` (statuts : ACTIF/RETIRE, VALIDE/UTILISEE/EXPIREE, statut de vente COMPLETEE/EN_COURS/COMPENSEE), `Card`, `Table`, `Modal` (confirmation de vente, upload d'ordonnance), `Toast` (retour d'action), `StockBadge` (vert = OK, orange = seuil bas, rouge = rupture).

---

## 3. Architecture Next.js (App Router)

```
pharmacie-frontend-test/
├── app/
│   ├── layout.tsx                 Shell global (sidebar + topbar, PAS de navbar marketing — c'est une appli métier interne)
│   ├── page.tsx                   Dashboard (redirection ou contenu direct)
│   ├── medicaments/
│   │   ├── page.tsx                Catalogue (liste, recherche, filtres)
│   │   └── [id]/page.tsx            Détail médicament + historique de stock
│   ├── vente/
│   │   └── page.tsx                Écran de caisse (POS) — cœur applicatif
│   ├── clients/
│   │   ├── page.tsx
│   │   └── [id]/page.tsx
│   ├── ordonnances/
│   │   ├── page.tsx
│   │   └── nouvelle/page.tsx
│   ├── ventes/
│   │   ├── page.tsx                Historique des ventes
│   │   └── [id]/page.tsx            Détail + statut de trace BCaaS en direct
│   ├── fournisseurs/
│   │   └── page.tsx
│   ├── commandes/
│   │   ├── page.tsx
│   │   └── [id]/page.tsx
│   └── alertes/
│       └── page.tsx
├── components/
├── lib/
│   ├── api.ts                      Client HTTP vers pharmacie-backend UNIQUEMENT
│   └── types.ts                    Types alignés sur les DTOs réels du backend Pharmacie
└── ...config (next.config, tailwind.config, tsconfig, .env.local)
```

Sidebar gauche fixe (comme `frontend-developpeur/app/console/layout.tsx`, même logique d'app plein écran) avec : Tableau de bord, Vente (mise en avant, action la plus fréquente), Médicaments, Ordonnances, Clients, Ventes (historique), Fournisseurs & Commandes, Alertes stock.

---

## 4. Pages en détail

### 4.1 Dashboard (`/`)
Consomme `GET /api/dashboard`. Affiche : chiffre d'affaires du jour, nombre de ventes du jour, alertes stock actives (compteur + lien), dernières ventes (5 dernières, lien vers détail). **Aucune donnée inventée** : si le backend ne renvoie pas encore un agrégat (ex. avant la première vente), afficher un état vide honnête (« Aucune vente aujourd'hui »), jamais un faux `0` habillé en fausse activité.

### 4.2 Catalogue médicaments (`/medicaments`)
`GET /api/medicaments` (recherche/filtre côté requête ou côté client selon volume). Tableau : nom, DCI, forme, catégorie (badge « Sur ordonnance » si `ordonnance_requise`), prix, stock actuel avec `StockBadge`, statut. Bouton « Nouveau médicament » → formulaire (`POST /api/medicaments`) avec les champs de la section 2.1 du cahier backend (nom, DCI, forme galénique, code CIP, catégorie, prix, stock initial, seuil d'alerte, fournisseur). **Champ catégorie** : select fermé (ex. `medicament_libre` / `medicament_prescription`) — jamais un champ texte libre côté UI, pour ne jamais désynchroniser la valeur envoyée à BCaaS lors d'une vente (cf. backend §3.2).

### 4.3 Détail médicament (`/medicaments/{id}`)
Fiche complète + historique des mouvements de stock connus localement (ventes, réceptions).

### 4.4 Écran de vente / caisse (`/vente`) — l'écran le plus important
1. Recherche/sélection d'un ou plusieurs médicaments (scanner code CIP simulé par saisie, ou recherche par nom), quantité par ligne.
2. Sélection optionnelle d'un client (`GET /api/clients` avec recherche).
3. **Si au moins une ligne concerne un médicament `ordonnance_requise`** : le panier affiche clairement un badge « Ordonnance requise » et **bloque le bouton Valider** tant qu'une ordonnance n'a pas été liée (upload via `/ordonnances/nouvelle` ou sélection d'une ordonnance existante du client) — reflet direct de la règle `EXIGER` réelle côté BCaaS (cf. backend §1.5). Le blocage frontend est un **confort UX**, pas une garantie de sécurité : la vraie vérification a lieu côté `POST /v1/businesses/{businessId}/operations/Vendre:execute`, dont le refus `422` doit être géré proprement même si l'UI a laissé passer.
4. Bouton « Encaisser » → `POST /api/ventes` avec les lignes, le client éventuel, le mode de paiement.
5. **Pendant l'appel** : état de chargement explicite (l'opération BCaaS est synchrone mais implique plusieurs appels Kernel en chaîne — laisser un retour visuel, pas un bouton figé sans feedback).
6. **Résultat** :
   - Succès (`200` relayé par le backend) → ticket de vente affiché (numéro, lignes, montant, mode de paiement, `transactionId` en petit, pour la traçabilité), option imprimer/nouvelle vente.
   - Échec règle (`422`, `violatedRule`/`requiredAction`) → message clair et actionnable (« Cette vente nécessite un document d'ordonnance : `<nom de la règle>` »), jamais un message générique type « Erreur ».
   - Échec stock (`422 STOCK_INSUFFISANT`, cf. limite documentée en backend §5) → message explicite distinct, invitant à vérifier le stock plutôt qu'à réessayer bêtement.

### 4.5 Ordonnances (`/ordonnances`, `/ordonnances/nouvelle`)
Liste + création : client, médecin, date, upload du document (PDF/image), lignes prescrites (médicament + quantité + posologie). `POST /api/ordonnances`.

### 4.6 Clients (`/clients`)
CRUD simple, historique des achats du client (jointure locale sur `vente`).

### 4.7 Historique des ventes (`/ventes`, `/ventes/{id}`)
`GET /api/ventes` (pagination), détail avec **statut de trace en direct** : si `statut_bcaas = EN_COURS`, bouton « Rafraîchir le statut » qui rappelle le backend (qui lui-même consulte `GET /v1/businesses/{businessId}/traces/{traceId}`). Affiche aussi la transaction associée si disponible (`GET /api/transactions`).

### 4.8 Fournisseurs & Commandes (`/fournisseurs`, `/commandes`, `/commandes/{id}`)
CRUD fournisseurs. Création de commande (médicaments + quantités). Réception (`POST /api/commandes-fournisseurs/{id}/reception`) → **augmente le stock local** (rappel honnête à l'écran : « Cette réception met à jour le stock PharmaCore ; le stock côté plateforme Business Core n'est pas modifiable via l'API à ce jour » — cf. limite backend §5, ne pas la cacher à l'utilisateur final non plus).

### 4.9 Alertes stock (`/alertes`)
`GET /api/alertes-stock`. Liste des médicaments sous seuil, avec accès rapide « Créer une commande fournisseur ».

---

## 5. Parcours utilisateur détaillés (à tester manuellement, pas juste construits)

**Parcours A — Vente simple (médicament libre)** : Dashboard → Vente → recherche « Paracétamol » → quantité 1 → Encaisser → ticket affiché avec `transactionId` réel. Vérifiable ensuite dans `frontend-developpeur` (Audit) que la trace existe côté BCaaS.

**Parcours B — Vente avec ordonnance requise, sans document** : Vente → médicament sur ordonnance → tentative d'encaissement sans document lié → doit échouer proprement avec le message de règle exact renvoyé par BCaaS (pas un message inventé côté frontend).

**Parcours C — Vente avec ordonnance, document fourni** : même parcours, ordonnance créée et liée au préalable → succès.

**Parcours D — Rupture de stock** : vendre plus que le stock local disponible → blocage **côté frontend d'abord** (UX), puis vérifier que l'appel réel serait de toute façon refusé/accepté selon le comportement observé en phase 0 du backend (cf. `SPIKE-RESULTATS.md`).

**Parcours E — Réapprovisionnement** : Commande fournisseur → réception → stock local mis à jour → alerte stock disparaît si elle existait.

---

## 6. Plan de développement, étape par étape

1. Squelette Next.js (App Router, Tailwind, fonts, design tokens §2).
2. `lib/api.ts` — client HTTP vers `pharmacie-backend-test`, types alignés (`lib/types.ts`) sur les DTOs réels exposés en backend §4 (jamais de champ inventé côté frontend qui n'existe pas côté backend).
3. Layout applicatif (sidebar + topbar), sans dépendance à `frontend-developpeur` (application distincte, pas de compte partagé).
4. Dashboard (état vide honnête d'abord, avant même que le backend ait des données).
5. Catalogue médicaments (lecture, puis création).
6. Écran de vente — **priorité absolue**, développé et testé contre le vrai backend Pharmacie dès que celui-ci expose `POST /api/ventes`.
7. Ordonnances, Clients.
8. Historique des ventes + détail trace.
9. Fournisseurs, Commandes, Alertes stock.
10. Passe de cohérence finale (même méthode que pour `frontend-developpeur` : vérifier qu'aucune donnée affichée n'est inventée, que chaque écran correspond à un endpoint réel du backend Pharmacie).

## 7. Definition of Done

- [ ] Aucun appel direct à Business Core (`:8081`) depuis le frontend — uniquement `pharmacie-backend-test` (`:9090`).
- [ ] Les 5 parcours de la section 5 sont exécutés manuellement avec succès (ou échec attendu et bien géré pour B/D).
- [ ] Design conforme à la charte (vert unique, pas de dégradé, radius faible) — vérifié visuellement, pas juste déclaré.
- [ ] Aucune donnée affichée (stock, montant, statut) qui ne provienne d'un appel réel au backend Pharmacie.
- [ ] Messages d'erreur de règle métier affichés tels que renvoyés par BCaaS (via le backend), jamais reformulés de façon à masquer l'information (`violatedRule`, `requiredAction`).
 