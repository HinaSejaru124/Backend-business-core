# Feuille de route — PharmaCore, produit fini de démonstration

> Établie le 12/07/2026, sur la branche **develop2**, après vérification empirique du backend en cours
> d'exécution. Objectif : faire de PharmaCore une vitrine complète et crédible de l'écosystème
> Kernel Core → Business Core → application métier. Ce document est le plan ; il précède le
> développement, comme demandé.

---

## 0. Découverte structurante (à lire en premier)

En testant la clé API que tu m'as fournie, j'ai établi un fait qui **reconfigure toute la conception**
de PharmaCore. Ce n'est pas un détail — c'est la colonne vertébrale du plan.

**Le Business Core sépare désormais strictement deux temps, avec deux authentifications :**

```
┌───────────────────────────────────────────────────────────────────────┐
│  DESIGN-TIME  —  le développeur MODÉLISE son métier  (auth = JWT)      │
│  Console développeur (frontend-developpeur), une fois.                 │
│                                                                        │
│   • Type Métier « Pharmacie » + version publiée                        │
│   • Offres = catalogue de médicaments                                  │
│   • Rôles métier (pharmacien, caissier)                                │
│   • Règles (« médicament sur ordonnance → exiger le document »)        │
│   • Opérations (« Vendre » et ses étapes)                              │
│                                                                        │
│  → Routes JWT-only : /v1/business-types, .../offers, .../rules,        │
│    .../operations, POST /v1/businesses …                               │
└───────────────────────────────────────────────────────────────────────┘
                              │  (le modèle est déclaré, figé, versionné)
                              ▼
┌───────────────────────────────────────────────────────────────────────┐
│  RUNTIME  —  l'application EXÉCUTE le métier  (auth = clé API)         │
│  Backend terminal PharmaCore, à chaque vente.                          │
│                                                                        │
│   • GET /v1/businesses/me           → quelle entreprise je représente  │
│   • .../actors:login /:register     → connexion pharmacien/caissier    │
│   • .../operations/Vendre:execute   → LA vente réelle                  │
│   • .../traces, .../transactions    → historique, tableau de bord      │
│   • /v1/sync                        → synchro hors-ligne               │
│                                                                        │
│  → Seules ces routes acceptent la clé API (SecurityConfig,             │
│    ROUTES_INTEGRATION_TERMINAL). Tout le reste renvoie 401.            │
└───────────────────────────────────────────────────────────────────────┘
```

**Vérifié en direct** : avec ta clé, `GET /v1/businesses/me` → 200 « Pharmacie Test Compte2 E » ;
`GET /v1/business-types` → 401 (normal, route design-time).

### Ce que ça implique — et pourquoi le PharmaCore actuel est à refondre

Le PharmaCore actuel crée un médicament en **déclarant une Offre via la clé API**. Sous le nouveau
modèle, **c'est impossible** : déclarer une offre est du design-time (JWT). Donc :

- Le **catalogue de médicaments** doit être déclaré au design-time (par le développeur), pas au runtime.
- Le backend terminal PharmaCore (clé API) ne doit plus faire que du runtime : **vendre**, lire
  l'historique, connecter les acteurs.

C'est plus propre et plus fidèle à la philosophie (« le modèle métier est déclaré une fois, l'appli
l'exécute »), mais ça impose de **redistribuer les responsabilités** de PharmaCore. D'où la décision
ouverte du §6.

---

## 1. Où en est réellement le projet — audit des 7 briques

Vérifié en base de données (tenant de « Pharmacie Test Compte2 E »), pas supposé.

| # | Brique | État | Détail vérifié |
|---|--------|------|----------------|
| 1 | Type Métier | 🟡 Partiel | `PHARMA_TEST2` publié, v1 — mais nommage de test, pas « PHARMACIE » propre |
| 2 | Offre (catalogue) | 🟡 Pollué | 10 offres, dont ~7 créées par mes tests de débogage (« Isolation… », « Diagnostic… ») — à nettoyer |
| 3 | Acteurs métier | 🔴 Absent | 0 acteur déclaré (pharmacien/caissier) |
| 4 | Règles | 🔴 Absent | 0 règle (ex. ordonnance requise) |
| 5 | Opérations | 🔴 Absent | 0 opération « Vendre » déclarée ; **aucun module Vente côté PharmaCore** |
| 6 | Transactions | 🔴 Absent | découle des opérations, donc rien |
| 7 | Configuration | 🔴 Absent | devise / TVA / seuils non déclarés |

**Bilan** : PharmaCore ne démontre aujourd'hui que **2 briques sur 7**, et de façon polluée. Le cœur
métier (vendre, avec règles, acteurs, transactions) est entièrement à construire.

### État du code PharmaCore

- **Backend** (`pharmacie-backend-test`) : médicaments (crée une offre — **cassé** par le nouveau
  modèle), clients / fournisseurs / ordonnances / commandes (locaux, sans Business Core — voulu),
  dashboard local. **Pas de module Vente.**
- **Frontend** (`pharmacie-frontend-test`) : pages médicaments, clients, fournisseurs, commandes,
  ordonnances, alertes, **et déjà des pages `vente` / `ventes` sans backend** (incohérence à résoudre).

---

## 2. Où la clé API intervient (réponse à ta demande explicite)

| Fonctionnalité PharmaCore | Utilise la clé API ? | Route |
|---|---|---|
| Savoir quelle entreprise on est | ✅ Oui | `GET /v1/businesses/me` |
| Connexion d'un pharmacien/caissier | ✅ Oui | `POST /v1/businesses/{id}/actors:login` |
| **Vendre un médicament** | ✅ Oui | `POST /v1/businesses/{id}/operations/Vendre:execute` |
| Historique des ventes / transactions | ✅ Oui | `GET /v1/businesses/{id}/traces`, `.../transactions` |
| Synchronisation hors-ligne | ✅ Oui | `GET /v1/sync` |
| Déclarer le catalogue / règles / opérations | ❌ Non (JWT dev) | routes design-time |
| Gérer clients / fournisseurs / stock local | ❌ Non (base locale PharmaCore) | — |

**En clair** : la clé sert exclusivement au **runtime métier** (vendre + consulter). Tout ce qui est
purement local à la pharmacie (fiche client, stock physique) n'en a pas besoin ; tout ce qui est
modélisation se fait en amont via le compte développeur.

---

## 3. Feuille de route — Analyse

- [x] Cartographier les 7 briques (fait, §1)
- [x] Établir la séparation design-time / runtime (fait, §0)
- [x] Vérifier la validité de la clé et l'entreprise cible (fait : e6ae6f0c)
- [ ] **Décision** : comment déclarer le modèle design-time ? (§6)
- [ ] Nettoyer les offres de test polluantes du catalogue

## 4. Feuille de route — Développement

### Phase A — Modélisation design-time (le « moule » Pharmacie), JWT
Déclarer proprement, une fois, le modèle complet :
- A1. Type Métier `PHARMACIE` (+ domaine SANTE), version 1, publiée
- A2. Configuration (brique 7) : `devise=XAF`, `tva=19.25`, `seuil_alerte_defaut=10`
- A3. Offres (brique 2) : un vrai catalogue de médicaments de départ (avec capacité `STOCKABLE`)
- A4. Rôles (brique 3) : `PHARMACIEN_RESPONSABLE`, `CAISSIER`, `CLIENT`
- A5. Règles (brique 4) : `AVANT_VENTE` + `CATEGORIE_EGALE:medicament_prescription` → `EXIGER` (ordonnance)
- A6. Opération (brique 5) : `Vendre` avec étapes `VERIFIER_STOCK → EVALUER_REGLES → ENREGISTRER_VENTE → ENGAGER_STOCK → ENCAISSER → EMETTRE_EVENEMENT`

### Phase B — Backend runtime PharmaCore (clé API)
- B1. Remplacer la création d'offre par clé API par une **lecture** du catalogue (offres design-time)
- B2. `GET /v1/businesses/me` au démarrage → mémoriser le `businessId`
- B3. **Module Vente** (`POST /api/ventes`) : orchestre `Vendre:execute` (le cœur, brique 5+6)
- B4. Connexion acteur : `actors:login` (brique 3) → session pharmacien/caissier
- B5. Historique : lecture `traces` / `transactions` (brique 6) pour le dashboard
- B6. Gestion locale conservée : clients, fournisseurs, stock, ordonnances (avec justification)

### Phase C — Frontend PharmaCore
- C1. Écran de vente complet (panier → règles → paiement → ticket) branché sur `/api/ventes`
- C2. Connexion acteur (pharmacien/caissier) réelle
- C3. Catalogue affiché depuis les offres réelles
- C4. Dashboard depuis les vraies transactions
- C5. Gestion des états : chargement, erreurs RFC 7807 (dont blocage règle ordonnance), vides
- C6. Résoudre l'incohérence des pages `vente`/`ventes` existantes

### Phase D — Simulation de paiement professionnelle
- Voir §5.

## 5. Simulation de paiement — deux niveaux distincts

1. **Paiement d'une vente** (étape `ENCAISSER` de l'opération) : géré **côté Business Core → Kernel**.
   Si le core financier du Kernel répond, c'est réel ; sinon, Business Core doit exposer un
   comportement simulé propre (transaction créée, identifiant généré, statut). À vérifier
   empiriquement lors de la Phase A6/B3.
2. **Paiement d'un changement de plan développeur** : déjà construit proprement (`PaiementPort` +
   `SimulationPaiementAdapter`, confirme immédiatement, remplaçable par un `KernelPaiementAdapter`).

Principe commun exigé : **jamais `montant → succès`**. Toujours : validation du montant → création
d'une transaction → identifiant → état → réponse réaliste, derrière une interface remplaçable par une
vraie API en changeant l'adaptateur.

## 6. Feuille de route — Finalisation

- [ ] Tests de bout en bout du parcours de vente (nominal + règle bloquante)
- [ ] Nettoyage des données de test
- [ ] Documentation (mise à jour d'EXPLICATION.md + guide d'utilisation)
- [ ] Revue de cohérence Kernel / Business Core / PharmaCore
- [ ] Préparation démo

---

## 7. Décision prise (12/07/2026) : PharmaCore à deux espaces

Choix retenu : **Zone admin JWT + zone caisse clé API, dans PharmaCore**. C'est l'option la plus
« produit fini » : l'application montre elle-même les deux temps de l'écosystème.

```
┌──────────────────────────────────────────────────────────────────────┐
│  PHARMACORE  (une seule application, deux espaces)                     │
│                                                                        │
│  ESPACE ADMIN / TITULAIRE            │  ESPACE CAISSE / CAISSIER        │
│  auth = JWT (login développeur BC)   │  auth = clé API (X-BC-*)         │
│  ──────────────────────────────────  │  ──────────────────────────────  │
│  • déclarer / gérer le catalogue     │  • se connecter (acteur)         │
│    (offres = médicaments)            │  • VENDRE (Vendre:execute)       │
│  • déclarer rôles, règles            │  • historique ventes/transactions│
│  • déclarer l'opération « Vendre »   │  • alertes stock                 │
│  • configuration (devise, TVA…)      │                                  │
│                                                                        │
│  → appelle les routes DESIGN-TIME    │  → appelle les routes RUNTIME    │
│    de Business Core en Bearer JWT    │    en clé API                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Implications techniques (backend PharmaCore)
Le backend PharmaCore doit désormais porter **deux clients** vers Business Core :
1. **Client JWT (admin)** — nouveau. Login titulaire → `POST /v1/auth/login` sur BC → stocke le JWT
   en session → l'envoie en `Authorization: Bearer` sur les routes design-time. Sert les endpoints
   `/api/admin/**`.
2. **Client clé API (caisse)** — existe déjà (`BcaasClient`), à recadrer sur les seules routes runtime.

### Ordre de construction (remplace/précise §4)
- **A0. Fondation admin** : dans le backend PharmaCore, client JWT + endpoint de login titulaire +
  session. *(point de départ immédiat)*
- **A1–A6** : écrans + endpoints admin pour déclarer le modèle (type, config, offres, rôles, règles,
  opération Vendre) — chacun appelle la route design-time BC correspondante en JWT.
- **B**   : recadrer la caisse (clé API) sur le runtime + module Vente.
- **C**   : frontend — séparer visuellement Admin / Caisse, compléter tous les écrans.
- **D/E** : paiement, tests, docs.

---

## 8. Mise à jour (13/07/2026) — Phase A terminée, Phase B bloquée par le Kernel

### Phase A (modélisation design-time) : ✅ complète et vérifiée en base

- A1–A2 : rôles (CAISSIER, PHARMACIEN_RESPONSABLE, CLIENT), configuration (devise XAF, TVA 19.25,
  seuil 10), déclarés et idempotents.
- A5–A6 : règle « ordonnance requise » + opération « Vendre » (6 étapes), déclarées.
- **Nouveau** : le titulaire (compte développeur connecté) est rattaché comme **acteur CAISSIER réel**
  de l'entreprise (brique 3), avec sa vraie identité kernel — condition nécessaire pour que `Vendre`
  soit déclenchable (l'opération exige le rôle `CAISSIER`, qu'une clé API seule ne porte jamais).

### 5 bugs réels trouvés et corrigés en cours de route (business-core, vérifiés empiriquement)

1. **Login intermittent** — `businesscore.kernel.timeout-ms` à 5000ms alors que le kernel répond
   parfois en >9s ; aucun retry sur les appels d'auth. Corrigé : timeout 15000ms + retry avec backoff
   (`KernelAuthAdapter`).
2. **Login sur le mauvais tenant** — le compte a deux contextes kernel (un vide, un réel) ; l'ancien
   code prenait le premier contexte qui répondait sans erreur, pas le premier *utilisable*. Corrigé :
   tri des contextes par nombre d'organisations avant sélection (`KernelAuthAdapter.trierParUtilite`).
3. **`POST /api/roles` (kernel) rejeté (500)** — `AppliquerRoleTechniqueKernelAdapter` n'envoyait que
   `code`, alors que `CreateRoleRequest` exige aussi `name`. Corrigé.
4. **Pas d'idempotence sur l'assignation de rôle** — une 2ème tentative sur un acteur déjà assigné
   plantait (409 ou violation de contrainte SQL brute en 500, selon le chemin). Corrigé : recherche du
   rôle existant avant création, tolérance sur l'assignation déjà existante.
5. **`X-Organization-Id` + mauvais typage de réponse sur `/api/organizations/{id}/actors`** — même bug
   déjà connu ailleurs (401 si l'en-tête est présent en plus de l'id dans l'URL) + un `record Ack(boolean
   ok)` qui ne correspondait à aucune forme réelle de réponse kernel. Corrigé.

### Phase B — bloquée : le Kernel rejette `client_credentials`

**Résultat du test réel** (`Vendre:execute` via clé API, au nom de l'acteur CAISSIER) :

| Étape de la saga | Résultat |
|---|---|
| `VERIFIER_STOCK` | ✅ |
| `EVALUER_REGLES` | ✅ |
| `ENREGISTRER_VENTE` | ✅ |
| `ENGAGER_STOCK` | ❌ **bloqué** |

**Cause exacte** (à transmettre à l'équipe Kernel) :

```
POST /oauth2/token
→ HTTP 400 : {"error":"invalid_request","error_description":"Unsupported grant_type."}
```

`ENGAGER_STOCK` décrémente le stock kernel via un appel machine-à-machine : Business Core échange les
credentials de la plateforme (`KERNEL_CLIENT_ID` / `KERNEL_CLIENT_SECRET`, grant `client_credentials`)
contre un token, avant d'appeler `POST /api/inventory/movements`. Le kernel refuse ce grant type pour
ce client. **Déjà confirmé empiriquement, deux fois, sur deux comptes différents, avant cette session**
— ce n'est pas spécifique à un compte ni à cette entreprise, c'est systémique.

**Ce qui marche malgré tout** : la saga se compense proprement (aucune donnée corrompue), et
l'erreur remonte lisiblement au frontend (`502`, détail exact). Dès que le kernel acceptera ce grant
(ou qu'un autre mécanisme d'auth machine est fourni), la vente fonctionnera sans toucher au code
métier — seul `KernelTokenService`/`credentialStore` a besoin d'un ajustement de configuration.

### Décision (13/07/2026) : on avance ailleurs en attendant

On ne simule pas ENGAGER_STOCK pour l'instant — on continue le frontend PharmaCore (Admin/Caisse,
écran de vente, dashboard) pendant que ce blocage remonte à l'équipe Kernel.

### Mise à jour (15/07/2026) — le blocage a reculé, il n'est plus limité à ENGAGER_STOCK

**Retest en direct** (`POST /v1/businesses/{id}/operations/Vendre:execute`, clé API réelle de
PharmaCore, acteur CAISSIER réel) :

| Étape de la saga | Résultat (13/07) | Résultat (15/07) |
|---|---|---|
| `VERIFIER_STOCK` | ✅ | ❌ **bloqué ici désormais** |
| `EVALUER_REGLES` | ✅ | — (jamais atteinte) |
| `ENREGISTRER_VENTE` | ✅ | — (jamais atteinte) |
| `ENGAGER_STOCK` | ❌ bloqué | — (jamais atteinte) |

Réponse observée le 15/07 : `502` en **~1,0s** (échec propre et rapide, pas un timeout), même cause
exacte (`POST /oauth2/token → 400 Unsupported grant_type`). Un test isolé de `/oauth2/token` avec les
identifiants plateforme réels (indépendant de tout code applicatif) confirme le même rejet en ~1,5s.

**Explication** : les 4 étapes kernel de la saga (`VERIFIER_STOCK`, `ENREGISTRER_VENTE`,
`ENGAGER_STOCK`, `ENCAISSER`) passent toutes par `KernelClient.exchange()`, qui utilise un JWT délégué
utilisateur s'il existe (`JwtDelegueResolver`), sinon le flux machine `client_credentials`
(`exchangeMachine`). PharmaCore appelant Business Core par **clé API** (jamais de JWT délégué), toute
étape kernel emprunte systématiquement le flux machine — donc le même rejet. Que la saga ait pu passer
`VERIFIER_STOCK`/`ENREGISTRER_VENTE` le 13/07 s'explique probablement par un jeton machine encore en
cache à ce moment-là (`KernelTokenService`/`JwtCache`) ; une fois ce jeton expiré, toute nouvelle
demande de jeton se heurte au même rejet Kernel, dès le premier appel. Aucune régression côté Business
Core ou PharmaCore : le code n'a pas changé sur ce chemin depuis le 13/07.

**Conclusion inchangée** : toujours pas un bug applicatif, toujours un blocage Kernel confirmé
empiriquement — simplement plus étendu qu'observé initialement.

### Phase C1 — Espace Admin construit et vérifié de bout en bout

- **Backend** : `AdminMedicamentController` (`POST /api/admin/medicaments`, JWT) — déclare l'Offre via
  `BcaasAdminClient.declarerOffre` puis sauvegarde la fiche locale. L'ancien `POST /api/medicaments`
  (clé API, cassé par la séparation design-time/runtime) est retiré ; `GET` reste (lecture runtime,
  utile à la caisse). `BcaasClient`/`OffreDtos` (l'ancien client clé API pour les offres) supprimés —
  code mort, plus aucune référence après le déplacement.
- **Frontend** : nouvelle page `/admin` (connexion titulaire, bouton « Provisionner le modèle » avec
  compte-rendu, formulaire de création de médicament, catalogue actuel). `/medicaments` devient lecture
  seule avec un lien honnête vers `/admin`. Nouvel item de nav « Espace admin ».
- **Vérifié en réel** : login → statut → création (« Ibuprofène 400mg », offre réelle
  `7709b610-…`) → apparition dans `GET /api/medicaments` (12 médicaments, dont les 2 créés pendant ce
  test). Un bug annexe trouvé et corrigé au passage : `HttpRequestMethodNotSupportedException` masqué
  en 500 générique au lieu d'un 405 propre (`ApiExceptionHandler`).

### Phase C2 — Connexion acteur (caisse) construite et vérifiée

- **Backend** : nouveau package `caisse/` — `CaisseSession` (identité de l'acteur connecté, un seul à
  la fois, même principe que `AdminSession`), `BcaasCaisseAuthClient` (`POST .../actors:login` via la
  clé API de l'entreprise, jamais de JWT ici — refusé côté Business Core sur cette route),
  `CaisseAuthController` (`/api/caisse/auth/login|logout|status`).
- **Frontend** : bandeau de connexion sur `/vente` — formulaire si personne connecté, badge
  « Connecté : email (CAISSIER) » + déconnexion sinon.
- **Vérifié en réel** : login avec le compte déjà rattaché comme CAISSIER (Phase A2) →
  `{"connecte":true,"principal":"techlan500@gmail.com","roleCode":"CAISSIER",...}`. L'identité kernel
  de l'acteur est gardée côté serveur PharmaCore, prête à être envoyée en `X-BC-On-Behalf-Of` dès que
  le module Vente (`POST /api/ventes`) sera branché sur `Vendre:execute`.
- Cette brique ne débloque pas l'encaissement (`ENGAGER_STOCK` reste gelé côté Kernel), mais complète
  la brique 3 (Acteurs) côté runtime, en plus du rattachement design-time déjà fait.

### Phase C3 — Module Vente construit et vérifié (briques 5 + 6)

- **Backend** : nouveau package `vente/` — entités `Vente`/`VenteLigne` (migration `V7__vente.sql`,
  schéma exactement conforme à backend-test.md §2.5/§2.6), `BcaasVenteClient` (appelle
  `POST /v1/businesses/{businessId}/operations/Vendre:execute` avec `X-BC-On-Behalf-Of` = acteur
  connecté et `Idempotency-Key`), `VenteService` (orchestration), `VenteController`
  (`POST/GET /api/ventes`, `GET /api/ventes/{id}`).
- **Décision de conception documentée** : Business Core n'accepte qu'un `offreId`/`quantite` par appel
  `:execute` (§3.2, vérifié dans `ClesContexte.java`) — un panier à plusieurs lignes déclenche donc un
  appel séquentiel par ligne, chacun avec une clé d'idempotence dérivée (`idempotencyKey:medicamentId`)
  pour éviter qu'une même clé réutilisée sur deux offres ne fasse renvoyer par Business Core le résultat
  déjà tracé de la première ligne. Aucune ligne n'est persistée si un appel échoue — même logique que la
  création de médicament (§Phase C1) : jamais de donnée locale sans confirmation réelle de la plateforme.
- **`GET /api/dashboard`** branché sur les vraies données `vente` (somme/compte du jour) au lieu de
  zéros codés en dur — reste à 0 aujourd'hui parce qu'aucune vente n'aboutit encore (cf. blocage
  ci-dessous), pas parce que la valeur est figée.
- **Frontend** : bouton « Encaisser » de `/vente` branché sur le vrai `POST /api/ventes` (état
  `idle/loading/ok/error`), désactivé seulement si panier vide ou aucun caissier connecté. Affiche le
  résultat réel — `transactionId`/`traceId` en cas de succès, `detail`/`violatedRule`/`requiredAction`
  RFC 7807 en cas d'échec (plus de bouton « indisponible » figé).
- **Bug réel trouvé et corrigé pendant la vérification** : Business Core construit son
  `ContexteEtape` via `Map.copyOf(parametres)` (`ContexteEtape.java:13`), qui lève une
  `NullPointerException` dès qu'une valeur du map est `null` (contrat `Map.ofEntries`). PharmaCore
  envoyait `"beneficiaireId": null` pour une vente comptant sans client — corrigé côté PharmaCore en
  omettant la clé plutôt qu'en la mettant à `null` (`VenteService.construireParametres`). Comportement
  Business Core à signaler à l'équipe (un paramètre optionnel documenté nullable ne devrait pas faire
  planter le moteur), mais contournable entièrement de notre côté.
- **Vérifié en réel** (curl, compte CAISSIER connecté, offre réelle « Ibuprofène 400mg ») :
  `POST /api/ventes` traverse réellement `VERIFIER_STOCK → EVALUER_REGLES → ENREGISTRER_VENTE`, puis
  échoue à `ENGAGER_STOCK` avec le `502` exact déjà documenté en Phase B (`Unsupported grant_type`) —
  et **rien n'est persisté** (`GET /api/ventes` → `[]`). C'est le comportement attendu tant que le
  blocage Kernel n'est pas levé : la vente entière est honnête, jamais simulée.

### Phase C4 — Nettoyage du catalogue (brique 2)

- **Diagnostic** : `GET /api/admin/offres` (nouveau, vue brute du catalogue Business Core, distincte de
  `/api/medicaments` qui ne montre que les offres avec fiche locale) a révélé 21 offres réelles pour un
  catalogue censé n'en compter que 5 : bruit de tests répétés (`Test bascule compte`, `Diagnostic
  trace`, `Isolation totale`…) et doublons (`postunor 3` ×2, `postunor 5` ×3). En parallèle, 5 fiches
  locales `medicament` pointaient vers des `offreId` qui n'existent plus du tout côté Business Core
  (orphelines — vestiges d'une version de type métier antérieure, pas liées au module Vente).
- **Capacité ajoutée** : Business Core expose un vrai `DELETE .../offers/{offerId}` (jusque-là inutilisé
  par PharmaCore) — `BcaasAdminClient.supprimerOffre`, `AdminOffreController`
  (`GET/DELETE /api/admin/offres`) pour les offres sans fiche locale, et
  `DELETE /api/admin/medicaments/{id}` (`AdminMedicamentController`) qui supprime l'Offre côté Business
  Core **puis** la fiche locale, jamais l'inverse.
- **Cas réel rencontré** : 2 fiches locales (Paracetamol 500mg, Amoxicilline 500mg — orphelines) étaient
  encore référencées par de vraies lignes d'ordonnance (`ordonnance_ligne`, contrainte de clé
  étrangère) — les supprimer aurait cassé un historique de prescription réel. Ajouté un vrai retrait
  (`Medicament.retirer()`, `statut = RETIRE`, champ déjà prévu par backend-test.md §2.1 mais jamais
  câblé) : `AdminMedicamentController.supprimer` bascule automatiquement sur un retrait si la
  suppression échoue pour cause de référence réelle. `MedicamentService.lister()`/`alertesStock()`
  excluent désormais les fiches `RETIRE` du catalogue actif — elles restent consultables par
  `GET /api/medicaments/{id}` (l'ordonnance qui les référence reste lisible), juste plus proposées à
  la vente.
- **Résultat vérifié** : catalogue Business Core ramené à 5 offres réelles (Doliprane 1000mg, postunor 2,
  postunor 5, Paracetamol 500mg (Admin), Ibuprofène 400mg) = exactement les 5 fiches locales `ACTIF`.
  `GET /api/dashboard` → `totalMedicaments: 5` (recalculé sur le catalogue actif réel, plus une valeur
  approximative).

## Phase D — Restructuration par rôles (13/07/2026)

### Constat de départ

PharmaCore n'avait **aucune authentification pour le personnel de vente** : une seule sidebar montrait
toutes les pages à tout le monde (commentaire trouvé dans le code : *« Pas d'authentification en v1 »*),
et les deux sessions serveur existantes (`AdminSession`, `CaisseSession`) étaient des **singletons**
partagés par tout le process — un seul titulaire et un seul caissier connectés possibles *pour tout le
serveur à la fois*. Le « caissier » utilisé jusque-là était en réalité le compte du titulaire
lui-même, rattaché comme `CAISSIER` pour les besoins des tests (Phase A2) — aucun compte Pharmacien
Responsable distinct n'existait. Ce n'était pas une vraie séparation de rôles, c'était un test générique.

### Les 3 rôles réels (confirmés dans le code, pas supposés)

`ModeleProvisioningService.java` déclare exactement 3 rôles opérationnels :

| Rôle | Nature | Authentification |
|---|---|---|
| **Titulaire** | Propriétaire de l'entreprise Business Core (design-time) | JWT Business Core — inhérent, non contournable : les routes design-time (catalogue, règles, config) exigent un JWT développeur vérifié, la clé API y est explicitement rejetée |
| **Pharmacien Responsable** (`PHARMACIEN_RESPONSABLE`) | Acteur métier, peut déroger à la règle « ordonnance requise » | Compte local PharmaCore (voir ci-dessous) |
| **Caissier** (`CAISSIER`) | Acteur métier, déclenche « Vendre » | Compte local PharmaCore |

Un 4ᵉ rôle (`CLIENT`, bénéficiaire) existe côté modélisation mais n'est jamais un compte de connexion
(recommandation de la spec : ne pas créer d'Acteur bénéficiaire par client).

### Décision d'architecture : où vivent les comptes du personnel ?

Question posée en cours de session : Business Core impose-t-il que le personnel s'authentifie auprès du
Kernel ? Réponse précise après lecture du code :

- **Ce qui est réellement exigé par Business Core** : qu'un appel à `Vendre:execute` porte un
  `X-BC-On-Behalf-Of` = un identifiant Kernel *réel* (vérification `ROLE_DECLENCHEUR_REQUIS`, traçabilité).
  C'est tout — un simple UUID à fournir, pas une session Kernel vivante.
- **Ce qui n'est PAS exigé** : que le Pharmacien/Caissier tape un mot de passe Kernel à chaque connexion,
  ni que PharmaCore n'ait pas sa propre table de comptes.

Décision retenue : **le personnel est géré 100% localement par PharmaCore** (table `personnel`, mot de
passe bcrypt propre à PharmaCore). Le titulaire, lui, reste sur son compte Business Core réel : ses
actions design-time *sont* des actions sur la plateforme partagée, un second mot de passe séparé serait
redondant. Dans les deux cas, l'écran de connexion PharmaCore ne mentionne jamais Kernel ni Business
Core — l'application se présente comme un produit à part entière, cohérent avec le fait que PharmaCore
est l'application du développeur, pas une extension visible du Kernel.

**Résolution de l'identité kernel — deux tentatives, la seconde retenue :**
1. *Tentative 1* : `POST /v1/businesses/{id}/actors:register` (inscription libre-service, une identité
   kernel neuve par rôle). Abandonnée : Kernel exige une vérification d'email avant de confirmer le
   compte (`502` avec un message explicite), et les emails de vérification ne sont jamais arrivés en
   pratique (délai/fiabilité de livraison hors de notre contrôle) — inacceptable pour une appli de test
   dont le but est justement d'être facile à faire tourner.
2. *Tentative 2 (retenue)* : réutiliser l'identité kernel **déjà vérifiée** du titulaire
   (`PharmacoreSession.titulaireActorIdOuNull()`), rattachée au rôle demandé via le rattachement classique
   (`POST /v1/businesses/{id}/actors`, pas `:register`) — aucune inscription, aucun email, immédiat.
   **Limite assumée et documentée** (`PersonnelService`, javadoc de classe) : comme une seule identité
   kernel vérifiée est disponible, Pharmacien Responsable et Caissier sont tous deux rattachés à cette
   même identité (deux rattachements de rôle différents dessus). Business Core résout donc l'ensemble des
   rôles de cette identité quel que soit le compte PharmaCore utilisé — la dérogation n'est donc plus
   réellement bloquée pour un Caissier *au niveau de Business Core* dans cette configuration précise. Ce
   qui reste réel : l'espace PharmaCore (pages, bouton de dérogation) diffère correctement par rôle
   connecté — suffisant pour la démonstration de navigation demandée, documenté comme limite plutôt que
   caché. Une vraie séparation de droits en production demanderait une deuxième identité kernel vérifiée.

### Construit

**Backend** :
- `PharmacoreSession` (nouveau package `auth/`) — remplace `AdminSession`/`CaisseSession`, `@SessionScope`
  (une instance par session HTTP/cookie `JSESSIONID`, plus un singleton serveur) : un onglet Titulaire, un
  onglet Pharmacien et un onglet Caissier peuvent maintenant être connectés **simultanément** sans se
  marcher dessus — essentiel pour une démo à plusieurs rôles.
- `personnel/` (nouveau module) — `Personnel` (entité, migration `V8__personnel.sql`), `PersonnelService`
  (bcrypt local + rattachement de l'identité kernel du titulaire au rôle, une fois par rôle — voir
  ci-dessus), `PersonnelAdminController` (`/api/admin/personnel`, titulaire seulement).
- `AuthController` (`/api/auth/login|logout|status`) — point de connexion unique : essaie d'abord le
  personnel local (rapide, pas de réseau), sinon tente une connexion titulaire (JWT Business Core).
  Remplace `AdminAuthController`/`CaisseAuthController`/`BcaasCaisseAuthClient` (supprimés).
- Garde-fous de rôle (`PharmacoreSession.exigerRole(...)`, 401/403 propres) sur : catalogue/config/
  provisioning/personnel (titulaire), vente (personnel uniquement, jamais le titulaire), fournisseurs/
  commandes (titulaire).
- **Correctif de règle réel** (trouvé en creusant le mécanisme de dérogation) : la règle « ordonnance
  requise » était déclarée avec l'effet `EXIGER`, qui est un effet **bloquant** appliqué *avant* toute
  lecture de `rolesAutorisesADeroger` côté Business Core (`EvaluerReglesExecuteur.premierBloquant` — seul
  l'effet `DEROGER` consulte les rôles, dans `GestionnaireEffets.appliquerDeroger`, avec motif
  obligatoire tracé en audit). La « dérogation du Pharmacien » documentée depuis la Phase A ne
  fonctionnait donc jamais réellement. `ModeleProvisioningService` déclare désormais `DEROGER` (auto-
  corrective : détecte et remplace une règle `EXIGER` héritée d'un ancien provisioning). Comportement
  réel désormais : Caissier bloqué sur un médicament sur ordonnance (doit escalader), Pharmacien
  Responsable peut vendre en fournissant un motif.
- CORS : `allowCredentials(true)` (nécessaire pour le cookie de session cross-origin) ; durée de session
  HTTP portée à 8h (`server.servlet.session.timeout`).

**Frontend** :
- `/connexion` — page de connexion unique, brandée PharmaCore (jamais Kernel/Business Core à l'écran).
- `AppShell` — garde de route + navigation **entièrement différente par rôle** (`lib/roles.ts`) :
  Titulaire voit tableau de bord/catalogue/fournisseurs/personnel ; Pharmacien/Caissier voient un poste
  de vente partagé + clients (+ ordonnances pour le Pharmacien). Toute route non autorisée pour le rôle
  courant redirige vers l'espace naturel de ce rôle.
- `/vente` (poste de vente partagé) — un seul écran pour Pharmacien et Caissier ; le champ « motif de
  dérogation » n'apparaît que pour le Pharmacien Responsable quand le panier contient un médicament sur
  ordonnance ; le Caissier voit un message honnête l'invitant à escalader.
- `/admin` — plus de formulaire de connexion intégré (géré globalement) ; nouvelle carte « Personnel »
  (créer/lister/désactiver Pharmacien et Caissier).

### Panne Kernel du 13/07/2026 (10h20-17h30 environ)

Le Kernel a connu une vraie indisponibilité de plusieurs heures ce jour-là : `discover-contexts/
select-context` en échec (`RetryExhaustedException`, timeout après 2 tentatives) sur pratiquement
toute la plage 10h20-17h30, bloquant même la connexion titulaire (JWT). Confirmé comme panne externe
réelle, pas un bug PharmaCore — aucune action côté code n'aurait pu la contourner (vérifié : la route
kernel de renvoi d'email de vérification elle-même exige un contexte déjà authentifié, donc inutilisable
tant que le Kernel ne répond à rien). Rétabli en fin d'après-midi ; `businesscore-redis` (nécessaire aux
jetons Business Core) s'était aussi arrêté pendant la coupure et a dû être relancé manuellement.

### Vérifié en réel après rétablissement

- Connexion titulaire (JWT Business Core) : OK.
- Re-provisioning : la règle « ordonnance requise » auto-corrective a bien remplacé l'ancienne règle
  `EXIGER` par `DEROGER` sans intervention manuelle.
- Création des 2 comptes personnel (Pharmacien Responsable, Caissier) : instantanée, 100% locale, aucun
  appel Kernel visible depuis le frontend.
- Connexion locale des 2 comptes : OK, chacun résout son propre rôle
  (`PHARMACIEN_RESPONSABLE`/`CAISSIER`) depuis la base PharmaCore.
- `POST /api/ventes` testé pour les 3 cas (Caissier sur médicament libre, Caissier sur médicament sur
  ordonnance sans motif, Pharmacien sur médicament sur ordonnance avec motif) : les trois traversent
  la saga jusqu'à `ENGAGER_STOCK` et échouent au même point déjà documenté (502, grant Kernel) — aucune
  régression. Confirmation empirique de la limite documentée plus haut : le Caissier sans motif n'a pas
  été bloqué à `EVALUER_REGLES` comme il le serait avec une identité kernel séparée (identité partagée
  avec le Pharmacien) — cohérent avec la limite assumée, pas un bug nouveau.
- **Reconfirmé le 14/07/2026** (nouveau test en direct, compte Caissier réel) : le blocage `ENGAGER_STOCK`
  est identique — même erreur exacte (`Unsupported grant_type` sur `/oauth2/token`). Aucune évolution
  côté Kernel depuis la Phase B.

### Bug corrigé — boucle de redirection infinie à la connexion

`useSession()` créait un état de session indépendant à chaque appel : `AppShell` (monté une fois,
persiste entre les navigations) et `ConnexionPage` (remontée à chaque visite) avaient chacun leur propre
copie, jamais synchronisée entre elles. Après une connexion réussie, seule la copie de `ConnexionPage`
se mettait à jour ; celle d'`AppShell` restait figée sur « non connecté », donc elle redirigeait vers
`/connexion` — qui, avec une copie fraîche disant « connecté », repartait vers `/`. Boucle infinie
(clignotement noir/blanc rapporté par l'utilisateur). Corrigé en remplaçant le hook par un contexte React
partagé (`SessionProvider`, `lib/useSession.tsx`) : une seule source de vérité pour toute l'application.

### Complété — ordonnance liée à la vente

Le panier de `/vente` ne permettait pas de lier la vente à une fiche `Ordonnance` précise (le champ
`ordonnanceId` existait déjà côté `Vente`/`CreerVenteRequest`, juste aucun sélecteur à l'écran). Ajouté :
un sélecteur affiché quand le panier contient un médicament sur ordonnance et qu'un client est
sélectionné, limité aux ordonnances `VALIDE` de ce client — avec un lien direct vers la création d'une
ordonnance si aucune n'existe. Vérifié avec de vraies données (clientes ayant déjà des ordonnances
`VALIDE` en base).

### Clarifications apportées (14/07/2026)

- **Architecture des requêtes, confirmée dans le code (pas supposée)** : 3 niveaux réels — (1) backend
  PharmaCore seul (Postgres local, jamais d'appel externe : clients, catalogue en lecture, tableau de
  bord) ; (2) Business Core seul, JWT titulaire, design-time (déclarer type/offre/rôle/règle/opération —
  **aucun appel Kernel**, sauf `rattacher` un acteur) ; (3) Business Core → Kernel, clé API, runtime
  (vérifier stock, enregistrer vente, encaisser, journaliser l'audit, rattacher/inscrire un acteur).
  Contrairement à l'intuition, une requête Business Core ne déclenche **pas systématiquement** un appel
  Kernel — toute la modélisation (briques 2 à 5 côté design-time) reste 100% Postgres Business Core.
- **Monétisation** : le compteur d'usage (`UsageTrackingWebFilter`, ce qu'affiche le tableau de bord
  développeur) ne compte que les requêtes authentifiées par **clé API** (donc les appels runtime), qu'elles
  touchent le Kernel ou non. Les actions du titulaire (JWT, design-time) ne sont actuellement comptées
  nulle part.

### Reste
- `GET /api/transactions` (relais `GET /v1/businesses/{businessId}/transactions`, §4) — non construit :
  n'aurait aucune donnée réelle à afficher tant qu'aucune transaction n'aboutit (`ENGAGER_STOCK`/`ENCAISSER`).
- Signaler à l'équipe Business Core : `ContexteEtape` (`Map.copyOf`) plante sur une valeur `null` dans
  `parametres` au lieu de la traiter comme absente (cf. Phase C3) — contourné côté PharmaCore, mais reste
  un vrai bug côté plateforme pour tout autre intégrateur.
- **Statut de l'API de paiement Kernel** (`ENCAISSER`, `PorteMonnaieMonetaireAdapter` → `POST
  /api/bills/pay`) : code réel existant côté Business Core, jamais simulé, mais jamais vérifié en
  pratique — la saga échoue systématiquement à l'étape précédente (`ENGAGER_STOCK`) avant de l'atteindre.
  Les deux étapes partagent exactement le même mécanisme d'authentification kernel
  (`KernelClient.exchangeMachine` → grant `client_credentials`) : le jour où ce grant est corrigé côté
  Kernel, les deux se débloquent simultanément, sans changement de code PharmaCore/Business Core.
- Si une vraie séparation de droits Pharmacien/Caissier est nécessaire un jour (au-delà de la démo de
  navigation), il faudra une deuxième identité kernel vérifiée — actuellement bloqué par l'indisponibilité
  de la vérification d'email en libre-service.

---

## Investigation — l'API de paiement Kernel « passe »-t-elle déjà ?

Demande explicite : vérifier si l'API de paiement Kernel fonctionne désormais, avant d'envisager de
l'intégrer réellement.

**Ce qui existe déjà dans Business Core, en code réel (pas une simulation)** :
`PorteMonnaieMonetaireAdapter` (`adapter/out/kernel/cashier/`) implémente le port
`PorteMonnaieGenerique` utilisé par l'étape `ENCAISSER` de la saga — il appelle réellement
`POST /api/bills/pay` sur le Kernel (règle le bill cashier produit par la vente), avec résolution
automatique d'une session de caisse (`SessionCaisseKernelSupport`, `GET/POST .../sessions`). Ce n'est
donc pas une intégration inventée : le code de paiement existe, est testé unitairement (WireMock), et
est câblé dans le moteur de saga.

**Ce qui est simulé, ailleurs, sans rapport avec la vente** : `PaiementPort`/`SimulationPaiementAdapter`
concerne le paiement d'un **changement de plan développeur** (facturation SaaS de Business Core), un
sujet complètement différent — toujours une simulation explicite, confirmée par le code
(`"SIMULATION-" + UUID.randomUUID()`).

**Statut réel, vérifié empiriquement** : `PorteMonnaieMonetaireAdapter` partage exactement le même
mécanisme d'authentification que `ENGAGER_STOCK` — `KernelClient.exchangeMachine()`, qui échange les
identifiants machine de Business Core contre un jeton via `POST /oauth2/token` (grant
`client_credentials`). Ce grant est **le même** que celui confirmé cassé côté Kernel depuis le début du
projet (`Unsupported grant_type`) — retesté dans cette session (`POST /api/ventes`, 13/07 vers 09h56) :
toujours en échec. Comme l'opération `Vendre` échoue systématiquement à `ENGAGER_STOCK` (étape 4/6,
*avant* `ENCAISSER`), le paiement n'a en réalité **jamais été atteint** dans nos tests — impossible de
confirmer directement qu'il fonctionne.

**Conclusion** : ce qu'on t'a rapporté (« l'API de paiement passe ») n'est pas confirmé par nos tests.
Soit c'est prématuré, soit ça concerne un autre point (le paiement de plan, simulé). Il n'y a aucune
raison technique de penser que `client_credentials` fonctionnerait pour `ENCAISSER` mais pas pour
`ENGAGER_STOCK` — c'est le même appel, au même endpoint, avec les mêmes identifiants.

**Plan d'attaque concret** :
1. Demander confirmation directe à l'équipe Kernel/Business Core sur l'état de `client_credentials`
   (déjà signalé §8) — c'est la seule façon d'obtenir une réponse fiable, un nouveau test de notre côté
   reproduirait juste le même échec.
2. Dès que le grant fonctionne, **aucun changement de code n'est nécessaire côté PharmaCore ni Business
   Core** : `PorteMonnaieMonetaireAdapter` est déjà l'implémentation réelle, déjà câblée. Il suffit de
   relancer une vente de bout en bout pour vérifier que `ENGAGER_STOCK` **et** `ENCAISSER` passent tous
   les deux (ils partagent le même mécanisme, donc se débloquent ensemble).
3. En attendant, ne rien simuler : le comportement actuel (échec honnête à `ENGAGER_STOCK`, saga
   compensée proprement, rien de persisté) reste le bon comportement à démontrer — c'est la preuve que
   l'intégration est réelle, pas contournée.

---

*Document vivant — à faire évoluer à chaque phase validée.*
