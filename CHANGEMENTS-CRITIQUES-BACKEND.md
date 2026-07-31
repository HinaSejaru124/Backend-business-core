# Changements critiques — Backend Business Core

> Journal des modifications **critiques côté backend** réalisées pendant le chantier « Console
> d'administration » (connexion front ↔ back, données 100 % réelles, suppression des simulations).
> Branche : `develop1`. Aucune donnée simulée / inventée ne subsiste : tout provient de la base ou du kernel.

---

## 1. AUTHENTIFICATION (le plus critique)

### 1.a — Identité de connexion (rappel, 2026-07-24)
Le kernel a cessé d'accepter l'**e-mail personnel** comme identifiant. La connexion se fait désormais avec
l'**identité Yowyob** `pending-<sub-sans-tirets>@yowyob.com` (l'e-mail personnel devient une simple adresse
de récupération). Business Core transmet simplement `principal` + `password` au kernel — **aucun changement
de code** requis pour ce point.

- Compte de test : personnel `techlan500@gmail.com` → identité `pending-981c47d0be9aa5b934af98d5`.

### 1.b — Correctif du 503 au login (2026-07-28) — `select-context`
Fichier : `adapter/out/kernel/auth/KernelAuthAdapter.java`

**Symptôme** : la connexion échouait avec `HTTP 503 — "Le service d'authentification est momentanément
indisponible"`, après ~16 s, de façon persistante.

**Cause racine** : le kernel a **changé son comportement** sur `POST /api/auth/select-context`. Il **refuse
désormais un `organizationId` explicite** et répond :
```
HTTP 400 — { "success": false, "errorCode": "AUTH_INVALID_REQUEST",
             "message": "The selected organization is not accessible in this context." }
```
Preuve (appel direct au kernel) :
- `select-context` **AVEC** `organizationId` → **400**.
- `select-context` **SANS** `organizationId` → **200 + accessToken**.

**État AVANT** :
- Le corps envoyé à `select-context` contenait `selectionToken` + `contextId` + **`organizationId`**
  (récupéré de `discover-contexts` → `contexts[i].organizations[0].organizationId`).
- Le `onStatus(...)` ne traitait comme « contexte refusé » (→ bascule contexte suivant) que les **401/403**.
- Conséquence : le **400** n'était pas capté ; il repassait par `retrieve()` → `WebClientResponseException`
  → **retenté** par `resilience()` (2 tentatives + backoff) → `RetryExhaustedException` → converti en **503**.

**État APRÈS (correctif)** :
1. On **n'envoie plus `organizationId`** : sélection par **`contextId` seul** — le kernel choisit
   lui-même l'organisation du contexte (comportement attendu par le kernel aujourd'hui).
2. `onStatus` traite désormais **400/401/403** comme un refus de contexte → mappé en `ProblemException`
   (donc **non retenté**, cf. filtre de `resilience`) → **bascule automatique sur le contexte suivant**
   via `essayerContexte(...)`. Plus de 503 parasite.

**Inchangé** : `discover-contexts` (rapide, ~1,7 s), le timeout (15 s) et le nombre de retries (2).

**Vérification** : `POST /v1/auth/login` → **HTTP 200** + jeton (≈4,6 s), de bout en bout.

---

## 2. PAIEMENT — suppression totale de la simulation
Fichiers : `adapter/out/payment/SimulationPaiementAdapter.java` (**supprimé**),
`domain/port/out/PaiementPort.java` (javadoc), `application/admin/AdminService.java` (garde-fou).

- **AVANT** : la classe `SimulationPaiementAdapter` existait (confirmait tout paiement en `CONFIRME` avec
  une référence `SIMULATION-<uuid>`). Non câblée (`@Component` uniquement sur `KernelPaiementAdapter`), mais
  présente — et de vieilles lignes `SIMULATION-` polluaient la comptabilité.
- **APRÈS** : classe **supprimée**. Seule implémentation de `PaiementPort` = **`KernelPaiementAdapter`**
  (vrai paiement mobile money via MyCoolPay / kernel `POST /api/payments/orders`). Aucun toggle de simulation
  ne subsiste.

Statuts issus **réellement** de la passerelle : `PENDING` → `EN_ATTENTE`, `SUCCESS/PAID/…` → `CONFIRME`,
`FAILED/CANCELLED/…` → `REFUSE`.

---

## 3. COMPTABILITÉ — encaissé réel honnête
Fichier : `application/admin/AdminService.java`

- **AVANT** : `billing()` renvoyait `encaisseReel = 0` codé en dur ; une première version sommait toutes les
  lignes `CONFIRME` (y compris les simulations) → affichait un faux « encaissé » (ex. 765 000).
- **APRÈS** : `encaisseReel` = somme des **paiements CONFIRMÉS réels du mois** uniquement — filtre
  `estPaiementReel(reference)` qui **exclut** les références `SIMULATION-`. Aujourd'hui = **0** (aucun
  paiement mobile money réellement encaissé). S'incrémentera automatiquement au premier vrai paiement kernel.
- Nouveaux endpoints réels : `GET /v1/admin/transactions?statut=` (achats de forfait, filtrable
  EN_ATTENTE/CONFIRME/REFUSE) et `GET /v1/admin/stats/revenue-timeseries?periode=` (revenus encaissés dans
  le temps). Les deux excluent aussi les simulations.

---

## 4. TARIFS — dynamiques, suppression, limite d'applications
Fichiers : `application/billing/PlanCatalogue.java`, `PlanPricingStore.java`,
`application/billing/BillingProperties.java`, `adapter/out/persistence/billing/PlanPricingEntity.java`,
`application/admin/AdminService.java`, `adapter/in/rest/admin/AdminController.java`,
`application/usecase/enterprise/EntrepriseService.java`, `resources/db/changelog/features/billing.xml`.

- **Prix dynamiques (déjà en place, confirmé)** : `PlanCatalogue` lit depuis `PlanPricingStore` (table
  `plan_pricing`, éditable par l'admin). Le prix fixé par l'admin est **immédiatement** vu par le
  développeur (`GET /v1/plans`) **et** débité au paiement (`PlanService` → `catalogue.prixMensuel()`).
  Le « 15 000 » n'était pas figé dans le code (défaut config = 0) : c'est une valeur de la base.
- **Suppression de forfait** : nouveau `DELETE /v1/admin/pricing/{code}` avec garde-fous — FREE (plan par
  défaut) protégé, et un plan encore utilisé par des développeurs ne peut pas être supprimé.
- **Limite d'applications (métrique hybride)** :
  - **Migration** `billing-003-plan-pricing-apps-max` : colonne `applications_max` (défaut `-1` = illimité).
  - `PlanDef` reçoit `applicationsMax` (+ constructeur de compatibilité conservant les appels existants).
  - **Enforcement réel** dans `EntrepriseService.creer(...)` : la création d'application est **bloquée**
    au-delà de la limite du forfait du développeur (message explicite), **fail-open** si le plan/compte
    n'est pas résoluble (ne casse jamais la création).

---

## 5. ADMIN / TABLEAU DE BORD — endpoints réels + correctifs
Fichiers : `application/admin/AdminService.java`, `adapter/in/rest/admin/AdminController.java`,
`adapter/out/persistence/requestlog/RequeteLogRepository.java`.

- **Correctif 500** sur `GET /v1/admin/developers/{id}` : un `.map(EntrepriseContratEntity::getCallbackUrl)`
  renvoyait `null` (réacteur interdit `null` dans `map`) quand une application avait un `callbackUrl` nul →
  `NullPointerException` → 500 pour les développeurs ayant des applications. Corrigé en **`.mapNotNull(...)`**
  aux deux endroits (`detail()` et `applications()`).
- **Nouveaux endpoints statistiques réels** (agrégés depuis `requete_log`, respectant le RLS par tenant) :
  `GET /v1/admin/stats/requests-timeseries` (série JOUR/SEMAINE/MOIS/ANNEE),
  `GET /v1/admin/stats/top-applications`, `GET /v1/admin/stats/activity`.
- **Actions admin réelles** : `POST /v1/admin/developers/{id}/plan` (forcer un forfait),
  `DELETE /v1/admin/developers/{id}` (supprimer un développeur — refusé s'il reste des clés actives).

---

## 6. DONNÉES — purge des artefacts de test
- Suppression en base de **37 lignes** `plan_change_request` dont la référence commençait par `SIMULATION-`
  (paiements de test qui gonflaient la comptabilité). Restait **3** transactions réelles (`EN_ATTENTE`).
- Restauration de la ligne `plan_pricing` **ENTERPRISE = 75 000 XAF** (elle avait disparu lors d'un reset
  de base antérieur ; FREE = 0, PRO = 15 000).

---

_Principe directeur de tout le chantier : **aucune donnée simulée, inventée ou figée en dur** — chaque
chiffre affiché par la console admin provient de la base de données ou du kernel, et chaque bouton déclenche
une action réelle._
