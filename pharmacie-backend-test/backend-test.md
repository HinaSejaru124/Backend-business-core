# Cahier des charges — Backend PharmaCore (test d'intégration Business Core)

**Rôle de ce document** : spécification extrêmement détaillée, pas à pas, du backend Spring Boot d'une pharmacie qui consomme **réellement** l'API Business Core (BCaaS) avec une clé API. Rien ici n'est inventé : chaque endpoint, chaque DTO, chaque enum cités proviennent du code source réel de `backend-business-core/`. Avant de commencer, lire `GUIDE-PROJET-PHARMACORE.md` (à la racine) pour le contexte global.

**Dossier** : `pharmacie-backend-test/`
**Stack imposée** : Spring Boot (même version/style que `backend-business-core` — Java 21, WebFlux réactif recommandé pour cohérence, mais un style MVC classique est acceptable si plus simple à livrer — à trancher en phase 0).
**Port proposé** : `9090` (aucun conflit avec `frontend-developpeur` (3000) ni `backend-business-core` (8081 en local)).
**Base de données** : PostgreSQL dédiée (nouvelle base, ex. `pharmacore`), séparée de `businesscore`.

---

## 0. Phase 0 — Spike obligatoire AVANT toute construction

> **Mise à jour 2026-07-08** — un pull récent sur `develop2` a changé des points importants (voir `GUIDE-PROJET-PHARMACORE.md` §4, notamment le piège d'activation de clé, et une nouvelle étape `ENGAGER_STOCK`). Le spike ci-dessous intègre ces changements.

Ne pas écrire une seule ligne de logique métier avant d'avoir validé manuellement (curl/Postman) le comportement suivant.

### 0.1 — Prérequis : une clé API activée
Une clé fraîchement créée par `POST /v1/registration` **ne fonctionne pas tant qu'aucun login n'a eu lieu** sur ce compte (cf. guide §4.2). Avant le spike :
1. Vérifier que le compte associé à la clé fournie par l'équipe (ou une clé nouvellement créée) a bien été activé — c'est-à-dire qu'un `POST /v1/auth/login` a réussi au moins une fois sur ce compte (par exemple déjà fait via `frontend-developpeur`).
2. Si ce n'est pas le cas : `POST /v1/registration` → vérifier l'e-mail (lien envoyé par le Kernel) → `POST /v1/auth/login` une fois. Ensuite seulement, la clé API (`X-BC-Client-Id`/`X-BC-Api-Key`) fonctionne pour les appels machine-à-machine.
3. Si besoin d'une clé dédiée à PharmaCore (recommandé, pour isoler son usage dans le dashboard) : une fois connecté (JWT), `POST /v1/api-keys` avec `{"name": "PharmaCore backend"}` → noter le secret **immédiatement** (affiché une seule fois).

### 0.2 — Séquence de test
1. `POST /v1/business-types` → créer un type `PHARMA_TEST`, `POST .../publish`.
2. `POST /v1/business-types/{typeId}/versions` → créer v1, `POST .../publish`.
3. `POST /v1/business-types/{typeId}/versions/1/offers` avec `capacites: ["STOCKABLE"]`, `formePrix: "FIXE"`, `prix: 500`.
4. `POST /v1/businesses` (typeId, versionNumber=1, nom) → obtenir un `businessId`.
5. **`POST /v1/businesses/{businessId}/approve`** → étape désormais nécessaire (approuve l'organisation kernel liée à l'entreprise). Sans elle, la suite peut échouer selon l'état de l'organisation — à vérifier empiriquement et documenter.
6. `POST /v1/business-types/{typeId}/versions/1/operations` → déclarer une opération `Vendre` avec les étapes `VERIFIER_STOCK`, `ENGAGER_STOCK`, `ENREGISTRER_VENTE`, `ENCAISSER` (minimal, sans règles pour ce premier test — voir §1.6 pour l'ordre exact retenu et pourquoi).
7. `POST /v1/businesses/{businessId}/operations/Vendre:execute` avec `{"parametres": {"offreId": "...", "quantite": 1}}`.
8. **Observer le résultat exact.** Deux issues possibles :
   - **200 OK** avec un `transactionId` → le stock initial était suffisant (produit créé avec un solde non-bloquant). Documenter le comportement observé.
   - **422 `STOCK_INSUFFISANT`** → confirme la limite documentée au §6 du guide et §5 ci-dessous. Dans ce cas, **le stock affiché dans PharmaCore sera géré comme une donnée locale au backend Pharmacie** (source de vérité pratique pour l'UI), et l'appel réel à `Vendre` sera tout de même exécuté à chaque vente (pour tester réellement l'opération, les règles, la trace) — sans bloquer l'expérience utilisateur sur son résultat de vérification de stock kernel. Documenter précisément le comportement observé dans un fichier `SPIKE-RESULTATS.md` à la racine de ce dossier, avec la réponse HTTP brute obtenue.

**Ce spike conditionne des décisions de la section 5. Ne pas sauter cette étape.**

---

## 1. Modélisation du métier « Pharmacie » sur les 7 briques Business Core

Ce mapping est la colonne vertébrale du projet. Chaque ligne ci-dessous correspond à un ou plusieurs appels réels à BCaaS, faits **une seule fois au démarrage** (bootstrap, section 4).

### 1.1 — Type Métier (brique 1)
```
POST /v1/business-types
{ "code": "PHARMACIE", "nom": "Pharmacie", "domainCode": "SANTE", "domainNom": "Santé" }
→ TypeMetierResponse { id, tenantId, businessDomainId, code, nom, statut: "BROUILLON" }

POST /v1/business-types/{typeId}/publish
→ statut: "PUBLIE"

POST /v1/business-types/{typeId}/versions
→ VersionTypeResponse { id, typeMetierId, numero: 1, immuable: false, publieeLe: null, libelle }

POST /v1/business-types/{typeId}/versions/1/publish
→ immuable: true, publieeLe renseigné
```

### 1.2 — Configuration (brique 7)
Paramètres par défaut au niveau du Type, définis un par un via :
```
POST /v1/business-types/{typeId}/versions/1/config
{ "cle": "devise", "valeur": "XAF", "verrouille": true }

POST .../config
{ "cle": "seuil_alerte_stock_defaut", "valeur": "10", "verrouille": false }

POST .../config
{ "cle": "tva_pourcentage", "valeur": "19.25", "verrouille": false }
```
> Ces clés/valeurs sont des choix du projet PharmaCore (le champ `cle`/`valeur` de `ParametreConfig` est du texte libre côté BCaaS — aucune clé n'est imposée par le backend). Documenter ce choix dans le code (constantes nommées), pas de valeur magique éparpillée.

### 1.3 — Offres = catalogue des médicaments (brique 2)
Chaque médicament du catalogue de référence est déclaré comme une Offre :
```
POST /v1/business-types/{typeId}/versions/1/offers
{ "nom": "Paracétamol 500mg (boîte de 20)", "formePrix": "FIXE", "prix": 500, "capacites": ["STOCKABLE"] }
→ OffreReponse { id, versionTypeId, nom, formePrix, prix, capacites: [{id, type: "STOCKABLE", active: true}] }
```
- `formePrix` ∈ `FIXE | GRATUIT | SUR_DEVIS` (enum réel `FormePrix`). Pour une pharmacie : quasi toujours `FIXE`.
- `capacites` ∈ sous-ensemble de `STOCKABLE | PLANIFIABLE | RESERVABLE | RECURRENT` (enum réel `TypeCapacite`). Un médicament = `STOCKABLE`. (`RESERVABLE` pourrait s'envisager plus tard pour une réservation d'ordonnance — hors périmètre v1.)
- Le `offreId` retourné doit être **mémorisé côté PharmaCore**, lié à la fiche médicament locale (section 2.1).

### 1.4 — Rôles métier (brique 3, partie rôles)
```
POST /v1/business-types/{typeId}/versions/1/roles
{ "code": "PHARMACIEN_RESPONSABLE", "categorie": "OPERATEUR" }

POST .../roles
{ "code": "CAISSIER", "categorie": "OPERATEUR" }

POST .../roles
{ "code": "CLIENT", "categorie": "BENEFICIAIRE" }
```
- `categorie` ∈ `OPERATEUR | BENEFICIAIRE` (enum réel `CategorieActeur`). Un client de la pharmacie est un **Bénéficiaire** (externe, pas d'accès système) ; le personnel est **Opérateur**.

### 1.5 — Règles (brique 4) — le cœur du test métier
Règle de référence du projet (littéralement l'exemple canonique de Business Core, cf. guide §3) :
```
POST /v1/business-types/{typeId}/versions/1/rules
{
  "declencheur": "AVANT_VENTE",
  "condition": "CATEGORIE_EGALE:valeur=medicament_prescription",
  "effet": "EXIGER",
  "rolesAutorisesADeroger": ["PHARMACIEN_RESPONSABLE"]
}
```
- `declencheur` ∈ `AVANT_OPERATION | APRES_OPERATION | AVANT_VENTE | APRES_VENTE | AVANT_ENCAISSEMENT | APRES_ENCAISSEMENT | AVANT_RESERVATION | AVANT_LIVRAISON` (enum réel `Declencheur`).
- `condition` : au Niveau 1 (seul niveau implémenté), le catalogue **fermé** de conditions supportées est : `TOUJOURS_VRAI`, `CATEGORIE_EGALE:valeur=X`, `MONTANT_SUPERIEUR:seuil=X`, `MONTANT_INFERIEUR:seuil=X` (vérifié dans `EvaluateurConditionN1.java`). **Ne pas inventer d'autres types de condition** — ils seraient simplement ignorés (retournent `false`).
- `effet` ∈ `BLOQUER | EXIGER | VALIDER | AJUSTER | ALERTER | DEROGER` (enum réel `Effet`). Pour « ordonnance requise » : `EXIGER` (bloque tant qu'un document n'est pas fourni).
- Pour que `CATEGORIE_EGALE` fonctionne, l'exécution de l'opération **doit** transmettre une valeur `categorie` dans son contexte (section 3.2) — c'est PharmaCore qui la fournit à partir de la catégorie du médicament vendu (ex. médicament classé « sur ordonnance » côté catalogue local → `categorie = "medicament_prescription"` envoyé à l'exécution).

Règle secondaire possible (seuil) :
```
POST .../rules
{ "declencheur": "AVANT_VENTE", "condition": "MONTANT_SUPERIEUR:seuil=50000", "effet": "ALERTER", "rolesAutorisesADeroger": [] }
```
→ alerte simple (non bloquante) pour une vente au-dessus de 50 000 XAF, tracée sans bloquer.

### 1.6 — Opérations (brique 5) — le workflow « Vendre »
```
POST /v1/business-types/{typeId}/versions/1/operations
{
  "nom": "Vendre",
  "roleDeclencheur": "CAISSIER",
  "declencheurRegles": "AVANT_VENTE",
  "differe": false,
  "etapes": [
    { "ordre": 0, "typeEtape": "VERIFIER_STOCK" },
    { "ordre": 1, "typeEtape": "EVALUER_REGLES" },
    { "ordre": 2, "typeEtape": "ENREGISTRER_VENTE" },
    { "ordre": 3, "typeEtape": "ENGAGER_STOCK" },
    { "ordre": 4, "typeEtape": "ENCAISSER" },
    { "ordre": 5, "typeEtape": "EMETTRE_EVENEMENT" }
  ]
}
```
- `typeEtape` ∈ catalogue **fermé** `VERIFIER_STOCK | ENGAGER_STOCK | EVALUER_REGLES | ENREGISTRER_VENTE | ENCAISSER | EMETTRE_EVENEMENT | ATTACHER_DOCUMENT` (enum réel `TypeEtape`, étendu par un pull du 2026-07-08). **Ne pas inventer d'étape** (ex. toujours pas de « AJOUTER_STOCK »/réapprovisionnement — ça n'existe pas côté BCaaS, cf. §5).
- **`ENGAGER_STOCK` (nouveau)** : décrémente réellement le stock kernel après la vente (mouvement de sortie validé), et sait se compenser (réinjection) si une étape suivante échoue. Placé **après `ENREGISTRER_VENTE`** (il a besoin du `commandeId` produit par cette étape — `ENGAGER_STOCK` échoue explicitement si `commandeId` est absent, donc l'ordre ci-dessus n'est pas négociable) et **avant `ENCAISSER`** (on décrémente le stock avant d'encaisser ; à confirmer/ajuster selon le résultat du spike §0 si un ordre différent s'avère nécessaire).
- Une deuxième opération, `VendreAvecOrdonnance`, peut ajouter `ATTACHER_DOCUMENT` en étape (après `EVALUER_REGLES`, avant `ENREGISTRER_VENTE`) pour les médicaments nécessitant le dépôt du document d'ordonnance avant de continuer.
- `differe: false` → exécution immédiate (réponse `200`). Garder `false` pour toutes les opérations de vente (l'expérience caisse doit être instantanée). `differe: true` s'envisagerait pour une opération lourde asynchrone (hors périmètre v1).

### 1.7 — Entreprise (brique 3, partie Entreprise)
Une seule entreprise pour la v1 (une pharmacie physique) :
```
POST /v1/businesses
{ "typeId": "...", "versionNumber": 1, "nom": "Pharmacie du Centre" }
→ EntrepriseResponse { id, nom, typeId, versionNumber, organizationId, cycleVie: "ACTIVE" }

POST /v1/businesses/{businessId}/approve
{ "reason": "Provisionnement initial PharmaCore" }   ← champ optionnel, défaut "Approbation initiale"
→ EntrepriseResponse (organisation kernel approuvée)
```
- **`approve` est une étape nouvelle (pull du 2026-07-08), à ne pas oublier dans le bootstrap** — elle approuve l'organisation kernel liée à l'entreprise. Comportement exact du système sans cette étape à vérifier lors du spike (§0).
- `cycleVie` ∈ `ACTIVE | SUSPENDUE | FERMEE` (enum réel `CycleVie`), modifiable via `PUT /v1/businesses/{businessId}/lifecycle`. L'entreprise peut aussi être renommée (`PUT /v1/businesses/{businessId}`) ou archivée (`DELETE /v1/businesses/{businessId}` → `FERMEE`).
- Le `businessId` retourné est **LA** référence pivot pour tous les appels suivants (acteurs, opérations, traces, transactions).

### 1.8 — Acteurs métier (brique 3, partie Acteurs)
```
POST /v1/businesses/{businessId}/actors
{ "roleMetierId": "<id du rôle PHARMACIEN_RESPONSABLE>", "identifiantPersonne": "<identifiant kernel ou local>" }
→ ActeurReponse { id, entrepriseId, roleMetierId, acteurKernelId, valideDepuis, valideJusqua }
```
- Un acteur par personne réelle rattachée à la pharmacie (le/la titulaire, chaque caissier). Les clients (Bénéficiaires) sont rattachés de la même façon avec le rôle `CLIENT` — **ou** gérés uniquement côté base locale PharmaCore si on ne veut pas créer un Acteur BCaaS par client (à trancher en section 2 : recommandation = ne pas créer d'Acteur BENEFICIAIRE par client pour ne pas polluer BCaaS avec des milliers de clients ; le `beneficiaireId` transmis à l'opération peut rester `null` pour une vente comptant simple, réservé aux ventes avec suivi nominatif/ordonnance).

### 1.9 — Transactions (brique 6) — consultation uniquement
```
GET /v1/businesses/{businessId}/transactions?page=0&size=20
→ Page<TransactionResponse> { transactionKernelId, montant, devise, statut, date }
```
Jamais écrites directement : elles **naissent** d'une opération exécutée (règle RG-05 du socle). PharmaCore les consulte pour son tableau de bord financier, ne les crée jamais.

---

## 2. Modèle de données PROPRE au backend Pharmacie (PostgreSQL)

Ce sont des tables qui **n'existent pas** côté Business Core — elles portent la richesse métier spécifique à une pharmacie (bien au-delà de ce que le modèle générique BCaaS peut représenter), et font le pont vers les identifiants BCaaS.

### 2.1 `medicament`
| Colonne | Type | Note |
|---|---|---|
| `id` | UUID (PK) | local |
| `offre_id` | UUID | **FK logique vers `OffreReponse.id` côté BCaaS** — obtenu à la création du catalogue (§1.3) |
| `nom` | text | ex. « Paracétamol 500mg » |
| `dci` | text | dénomination commune internationale (molécule) |
| `forme_galenique` | text | comprimé, sirop, injectable... |
| `code_cip` | text (unique, nullable) | code d'identification pharmaceutique, si disponible |
| `categorie` | text | valeur envoyée telle quelle dans le contexte d'exécution BCaaS pour la condition `CATEGORIE_EGALE` (ex. `medicament_prescription`, `medicament_libre`) |
| `ordonnance_requise` | boolean | miroir lisible de `categorie == medicament_prescription`, pour l'UI |
| `prix_unitaire` | numeric(12,2) | doit rester cohérent avec le `prix` de l'Offre BCaaS |
| `stock_actuel` | integer | **source de vérité locale** (cf. limite §5) |
| `seuil_alerte` | integer | seuil sous lequel une alerte se déclenche |
| `fournisseur_id` | UUID (FK) | |
| `statut` | text | ACTIF / RETIRE |
| `cree_le`, `maj_le` | timestamp | |

### 2.2 `client`
`id`, `nom`, `prenom`, `telephone`, `email` (nullable), `adresse` (nullable), `beneficiaire_id` (UUID nullable — rempli seulement si un Acteur BENEFICIAIRE a été créé côté BCaaS pour ce client, cf. §1.8), `cree_le`.

### 2.3 `ordonnance`
`id`, `client_id` (FK), `medecin_nom`, `medecin_numero_ordre` (nullable), `date_emission`, `document_nom`, `document_content_type`, `document_url_ou_reference` (stockage du fichier — voir §2.7), `document_id_bcaas` (UUID nullable, rempli si l'étape `ATTACHER_DOCUMENT` a retourné un id), `statut` (VALIDE / UTILISEE / EXPIREE), `cree_le`.

### 2.4 `ordonnance_ligne`
`id`, `ordonnance_id` (FK), `medicament_id` (FK), `quantite_prescrite`, `posologie` (texte libre).

### 2.5 `vente`
| Colonne | Type | Note |
|---|---|---|
| `id` | UUID (PK) | local |
| `business_id` | UUID | l'entreprise BCaaS (§1.7) |
| `client_id` | FK nullable | vente comptant possible sans client identifié |
| `ordonnance_id` | FK nullable | si vente liée à une ordonnance |
| `montant_total` | numeric(12,2) | recopié depuis la réponse BCaaS (`OperationResultResponse.details` / transaction) |
| `devise` | text | XAF par défaut |
| `mode_paiement` | text | ESPECES / MOBILE_MONEY / CARTE (choix PharmaCore, sans équivalent direct obligatoire côté BCaaS au niveau du payload d'exécution — à documenter comme métadonnée locale) |
| `statut_bcaas` | text | `COMPLETEE` / `EN_COURS` / `COMPENSEE` — recopié de la trace BCaaS |
| `transaction_kernel_id` | text nullable | recopié de `OperationResultResponse.transactionId` |
| `trace_id` | UUID nullable | recopié de `traceId` (permet de retrouver `GET /v1/businesses/{businessId}/traces/{traceId}`) |
| `idempotency_key` | UUID | générée par PharmaCore avant l'appel `:execute`, stockée pour rejouer/déduper |
| `cree_le` | timestamp | |

### 2.6 `vente_ligne`
`id`, `vente_id` (FK), `medicament_id` (FK), `quantite`, `prix_unitaire_facture`.

### 2.7 `fournisseur`
`id`, `nom`, `contact_nom`, `contact_telephone`, `email`, `delai_livraison_jours`, `cree_le`.

### 2.8 `commande_fournisseur`
`id`, `fournisseur_id` (FK), `statut` (BROUILLON / ENVOYEE / RECUE / ANNULEE), `date_commande`, `date_reception_prevue`, `date_reception_reelle` (nullable), `cree_le`.

### 2.9 `commande_fournisseur_ligne`
`id`, `commande_fournisseur_id` (FK), `medicament_id` (FK), `quantite_commandee`, `quantite_recue` (nullable), `prix_unitaire_achat`.

> **Réception d'une commande fournisseur** = incrémente **uniquement** `medicament.stock_actuel` (local). Aucun appel BCaaS n'existe pour ça (cf. §5) — documenté explicitement dans le code (commentaire à l'endroit précis, pas ailleurs) pour que ça ne surprenne personne plus tard.

### 2.10 `alerte_stock`
`id`, `medicament_id` (FK), `niveau_stock_constate`, `seuil`, `statut` (ACTIVE / TRAITEE), `cree_le`. Générée par un job local (déclenché à chaque vente/réception) qui compare `stock_actuel` à `seuil_alerte`.

---

## 3. Architecture technique du backend Pharmacie

### 3.1 Client BCaaS (couche d'accès)
Un client HTTP dédié (WebClient réactif ou RestClient, selon le choix de stack de la phase 0), avec :
- Base URL configurable (`pharmacore.bcaas.base-url`, **ne pas coder en dur `8080` ni `8081`** — la valeur diffère selon la machine, cf. guide).
- Intercepteur qui ajoute systématiquement `X-BC-Client-Id` et `X-BC-Api-Key` sur chaque appel sortant.
- Parsing des erreurs RFC 7807 (`application/problem+json`) — récupérer `title`, `detail`, `violatedRule`, `requiredAction`, `requiredDocument` pour les remonter proprement au frontend Pharmacie.
- Génération systématique d'un `Idempotency-Key` (UUID) avant tout appel `:execute`, pour permettre un retry sûr côté frontend sans double-vente.

### 3.2 Construction du contexte d'exécution d'une opération
Quand PharmaCore appelle `POST /v1/businesses/{businessId}/operations/Vendre:execute`, le corps `parametres` doit fournir les clés que le moteur BCaaS attend réellement (vérifié dans `ClesContexte.java`) :
```json
{
  "parametres": {
    "offreId": "<uuid de l'offre médicament>",
    "quantite": 2,
    "beneficiaireId": "<uuid ou null>",
    "categorie": "medicament_prescription",
    "documentNom": "ordonnance-2026-01-15.pdf",
    "documentContentType": "application/pdf",
    "documentContenu": "<base64>"
  }
}
```
- `categorie` : **c'est PharmaCore qui la calcule** à partir de `medicament.categorie` et l'injecte — BCaaS ne connaît pas le concept de « médicament », seulement une clé `categorie` texte libre passée à ses règles.
- Les champs `documentNom/documentContentType/documentContenu` ne sont fournis **que** si le médicament nécessite une ordonnance et qu'un document a été fourni côté UI. Sans eux, l'étape `EVALUER_REGLES` bloquera avec un effet `EXIGER` — c'est le comportement voulu et testé.

### 3.3 Secrets et configuration
`application.yml` (ou variables d'environnement, à privilégier) :
```yaml
pharmacore:
  bcaas:
    base-url: ${BCAAS_BASE_URL:http://localhost:8081}
    client-id: ${BCAAS_CLIENT_ID}
    api-key: ${BCAAS_API_KEY}
```
**`client-id`/`api-key` ne sont JAMAIS commités.** Utiliser un fichier `.env` local (gitignoré) ou les variables d'environnement du poste/CI. Rappel : la clé fournie par l'équipe (`clientId=bck_k6J1XJgEyRta`) est un exemple de démarrage — à ne pas laisser traîner en clair dans un fichier versionné, même en test.

### 3.4 Bootstrap au démarrage
**Prérequis non automatisable** : la clé API configurée (§3.3) doit provenir d'un compte déjà activé (cf. §0.1 — un login a déjà eu lieu au moins une fois sur ce compte). Le bootstrap automatique ne peut pas contourner ça ; il échouera explicitement (`EspaceNonLieException` relayée en 401/403) si ce n'est pas le cas — logguer ce cas clairement plutôt que de réessayer en boucle.

Un `ApplicationRunner` (ou équivalent) qui, **au démarrage de l'application, de façon idempotente** (vérifie d'abord si le type `PHARMACIE` existe déjà pour ce tenant avant de le recréer) :
1. Exécute la séquence complète §1.1 → §1.7 si rien n'existe encore, **en incluant `POST /v1/businesses/{businessId}/approve`** (§1.7 — ne pas l'oublier, c'est le piège le plus probable d'un bootstrap qui « marche à moitié »).
2. Persiste les identifiants obtenus (`typeId`, `versionNumber`, tous les `offreId` par médicament, `businessId`, `roleMetierId` par rôle) dans une table technique locale `bcaas_reference` (clé/valeur ou table dédiée) pour ne jamais avoir à les redemander.
3. Log clairement chaque étape (succès/échec) — c'est la première chose qu'on regarde si le démarrage échoue.

---

## 4. Endpoints REST exposés par le backend Pharmacie

Tous sous `/api` (préfixe propre à PharmaCore, sans rapport avec les préfixes `/v1` de BCaaS — pour bien signaler que c'est une API différente).

| Verbe | Chemin | Rôle |
|---|---|---|
| GET | `/api/medicaments` | Catalogue (recherche, filtre catégorie/statut) |
| POST | `/api/medicaments` | Créer un médicament → crée l'Offre BCaaS puis la fiche locale |
| GET | `/api/medicaments/{id}` | Détail |
| PUT | `/api/medicaments/{id}` | Mise à jour (locale ; ne modifie pas l'Offre BCaaS déjà publiée — cf. RG-03 immuabilité) |
| GET | `/api/clients` | Liste / recherche |
| POST | `/api/clients` | Créer |
| GET | `/api/ordonnances` | Liste |
| POST | `/api/ordonnances` | Créer une ordonnance (upload document) |
| POST | `/api/ventes` | **Orchestre l'appel réel `:execute` à BCaaS** (§3.2), puis persiste `vente` + `vente_ligne` |
| GET | `/api/ventes` | Historique (pagination) |
| GET | `/api/ventes/{id}` | Détail + statut de trace (rappelle `GET /v1/businesses/{businessId}/traces/{traceId}` si `EN_COURS`) |
| GET | `/api/fournisseurs` | Liste |
| POST | `/api/fournisseurs` | Créer |
| POST | `/api/commandes-fournisseurs` | Créer une commande |
| POST | `/api/commandes-fournisseurs/{id}/reception` | Réceptionner (incrémente le stock local — §2.9) |
| GET | `/api/alertes-stock` | Alertes actives |
| GET | `/api/dashboard` | Agrégats (CA du jour, ventes du jour, alertes actives, ruptures) |
| GET | `/api/transactions` | Relais de `GET /v1/businesses/{businessId}/transactions` (consultation) |

Documentation **Swagger obligatoire** (`springdoc-openapi`), exposée sur `/swagger-ui.html` — cohérent avec le reste du projet.

---

## 5. Limitation connue et décision de conception qui en découle

Rappel (détaillé au guide, §6) : **aucun endpoint Business Core ne permet d'incrémenter un stock**. Décision de conception qui en découle, à documenter dans le code :

- `medicament.stock_actuel` (PostgreSQL local) est la **source de vérité pour l'affichage et les décisions UI** (badge rupture, blocage bouton vendre côté frontend).
- L'appel réel `Vendre:execute` est **quand même effectué à chaque vente** (c'est tout l'intérêt du test) : si BCaaS renvoie `422 STOCK_INSUFFISANT` alors que le stock local dit le contraire, c'est **loggé comme une divergence** (pas silencieusement ignoré) et remonté à l'utilisateur avec un message clair (« Le socle Business Core indique un stock insuffisant côté plateforme — vente bloquée pour investigation. »), plutôt que masqué. Une vente réussie (`200`) reste le chemin nominal attendu et démontré.
- Ce point doit figurer explicitement dans le README du dossier `pharmacie-backend-test/` comme limitation connue de l'écosystème, pas comme un bug de PharmaCore.

---

## 6. Plan de développement, étape par étape

1. **Phase 0** (§0) — Spike de validation stock. Ne pas sauter.
2. **Squelette Spring Boot** — projet, dépendances (WebFlux ou MVC selon décision phase 0, `springdoc-openapi`, `r2dbc-postgresql` ou `spring-data-jpa`+driver Postgres selon le style choisi), config `.env`.
3. **Client BCaaS** (§3.1) — avec tests d'intégration réels contre l'environnement BCaaS local (pas de mocks pour ces tests-là, cohérent avec l'esprit du projet).
4. **Bootstrap** (§3.4) — dérouler la séquence §1 une fois, vérifier dans la console `frontend-developpeur` (page Types métier / Audit) que tout apparaît bien côté BCaaS.
5. **Modèle de données local** (§2) — migrations (Flyway ou Liquibase, cohérent avec le style du reste du projet).
6. **Endpoints CRUD simples** (médicaments, clients, fournisseurs) — sans logique BCaaS.
7. **Endpoint `/api/ventes`** (le plus important) — orchestration complète décrite en §3.2, avec gestion d'erreur RFC 7807 propre.
8. **Ordonnances** (upload document, liaison `ATTACHER_DOCUMENT`).
9. **Alertes stock, dashboard, commandes fournisseurs.**
10. **Tests end-to-end** contre le vrai BCaaS local : un scénario complet « vente d'un médicament libre » (doit réussir) et « vente d'un médicament sur ordonnance sans document » (doit échouer avec `422`/`EXIGER`, puis réussir une fois le document fourni).

## 7. Definition of Done

- [ ] Spike phase 0 exécuté et documenté (`SPIKE-RESULTATS.md`).
- [ ] Démarrage de l'application crée réellement le type PHARMACIE, sa version, ses offres, rôles, règles, opération, et l'entreprise dans BCaaS (vérifiable dans `frontend-developpeur`).
- [ ] Une vente d'un médicament sans ordonnance requise aboutit à une vraie transaction BCaaS (`200`, `transactionId` non nul).
- [ ] Une vente d'un médicament sur ordonnance sans document échoue avec `422` et `violatedRule`/`requiredAction: EXIGER` exploitable par le frontend.
- [ ] La même vente, avec document fourni, réussit.
- [ ] Les traces (`GET /v1/businesses/{businessId}/traces`) et transactions sont consultables et cohérentes avec l'historique local.
- [ ] Aucun secret (clé API, mot de passe DB) commité.
- [ ] Swagger exposé et à jour.
