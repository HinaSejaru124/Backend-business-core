# Audit de réalisation — feat/operations (Miguel · Dev 5)

> Journal d'audit de tout le travail effectué pour la tâche **feat-operations** (briques 5 & 6).
> Branche : `feat-operation-miguel` (partie de `develop2`, qui reste intacte).
> Commit : `44bb2e6`. Date : 2026-06-17.

---

## 1. Mission

Construire le **moteur d'exécution** du Business Core : les **Opérations** (brique 5 — workflows métier
déclarés en données, exécutés par un moteur saga) et les **Transactions** (brique 6 — façade financière
unifiée sur le kernel), reliées par l'entité **TraceOperation** (idempotence + compensation + audit).
Le tout en **architecture hexagonale réactive** (Spring Boot 4 / WebFlux / R2DBC), façade au-dessus du
**kernel RT-Comops**, dans le respect strict des conventions du socle.

Spec de référence : `docs/guides/feat-operations.md`. Conventions : `docs/guides/CONVENTIONS-SOCLE.md`.
Contrat d'API : `docs/api/business-core-openapi.yaml`.

---

## 2. Phase d'étude (avant tout codage)

Lecture intégrale et analyse de :
- Docs : `README-implementation.md`, `CONVENTIONS-SOCLE.md`, `feat-operations.md`, `feat-rules.md`,
  architecture (`hexagonale.md`, `sept-briques.md`), ADR, `business-core-openapi.yaml`.
- Socle / ports : `domain/port/out/*` (ExecuterWorkflow, EnregistrerVente, VerrouDIdempotence,
  PublierEvenement, VerifierDisponibilite, StockerDocument, RegistreDeRegles, PersisterVersionType…),
  `domain/port/internal/*` (ExecuteurDEtape, PlanificateurDOperation, PorteMonnaieGenerique,
  EvaluateurDeRegle, HorlogeSysteme), enums `domain/shared/*` (TypeEtape, StatutTrace, Declencheur, Effet).
- Features livrées (modèles à reproduire) : business-types (Dev 2), rules (Dev 4).
- Infra : `KernelClient`, `BusinessContext(Holder)`, `ProblemException`, `GlobalProblemHandler`,
  `TenantConnectionPoolFactory` (RLS runtime), dispatchers saga, `RedisVerrouDIdempotence`,
  `KafkaPublierEvenement`, changelogs Liquibase (`_socle.xml`, `business-types.xml`, `rules.xml`).

**Constat clé** : les briques Entreprise / Offre / Acteur (Dev 3) n'étaient pas encore livrées, alors
que l'exécution d'opération a besoin de résoudre `businessId → versionType / organization`.

---

## 3. Décisions d'architecture

| # | Décision | Justification |
|---|----------|---------------|
| 1 | **Saga orchestrée localement** (`MoteurOperation`) : chaînage réactif des étapes + compensation de la transaction kernel engagée via `ExecuterWorkflow.compenser`. | Conforme au prompt de la spec et testable ; le « quoi annuler » est le `transactionKernelId`. |
| 2 | **Brique Entreprise minimale** livrée (périmètre Dev 3) — sur demande explicite de l'utilisateur (« termine le travail de Dev 3 lié au mien »). Marquée « à fusionner ». | Indispensable pour faire fonctionner l'exécution de bout en bout. |
| 3 | Dépendance à Dev 3 via le **port** `ResoudreEntreprise` (jamais de couplage direct entre features). | Règle anti-conflit du projet. |
| 4 | `trace_operation.entreprise_id` en **uuid simple sans FK** vers la table d'une autre brique. | Découplage / merge facile (comme `regle_metier`). |
| 5 | Contexte lu via `BusinessContextHolder.currentContext()` (patron documenté de business-types). | Fiabilité (pas d'injection en paramètre). |
| 6 | Idempotence à 2 niveaux : verrou Redis (concurrents) + unicité `(tenant_id, cle_idempotence)` en base (rejeu séquentiel). | Robustesse. |
| 7 | Mode immédiat → trace écrite à la fin (COMPLETEE/COMPENSEE) ; différé → trace EN_COURS au début + événement Kafka. | Sémantique 200 / 202. |

---

## 4. Fichiers créés (72 sous `src/`)

### Domaine (`domain/`)
- `operation/DefinitionOperation.java`, `operation/EtapeOperation.java`, `operation/OperationAvecEtapes.java`, `operation/ResultatExecution.java`
- `operation/spi/PersisterOperation.java`, `operation/spi/ResoudreEntreprise.java`, `operation/spi/EntrepriseResolue.java`
- `transaction/TraceOperation.java`, `transaction/TransactionVue.java`
- `transaction/spi/PersisterTrace.java`, `transaction/spi/LireTransactions.java`
- `enterprise/Entreprise.java`, `enterprise/spi/DepotEntreprise.java`

### Application (`application/`)
- `saga/MoteurOperation.java`, `saga/ResultatMoteur.java`, `saga/ClesContexte.java`, `saga/Valeurs.java`
- `saga/etape/` : `VerifierStockExecuteur`, `EvaluerReglesExecuteur`, `EnregistrerVenteExecuteur`, `EncaisserExecuteur`, `EmettreEvenementExecuteur`, `AttacherDocumentExecuteur`
- `usecase/operation/` : `DeclarerOperationService`, `ConsulterOperationService`, `ExecuterOperationService`, `PlanificateurDOperationParBase`, `EtapeDeclaration`
- `usecase/transaction/` : `ConsulterTraceService`, `ConsulterTransactionService`
- `usecase/enterprise/EntrepriseService.java`

### Adapters entrants REST (`adapter/in/rest/`)
- `operation/` : `OperationController` (dont `POST …/operations/{name}:execute`), `CreerOperationRequest`, `EtapeRequest`, `OperationResponse`, `ExecuterOperationRequest`, `OperationResultResponse`, `OperationPendingResponse`
- `trace/` : `TraceController`, `OperationTraceResponse`
- `transaction/` : `TransactionController`, `TransactionResponse`
- `enterprise/` : `EntrepriseController`, `CreerEntrepriseRequest`, `EntrepriseResponse`, `ChangerCycleVieRequest`

### Adapters sortants (`adapter/out/`)
- `kernel/sales/` : `EnregistrerVenteKernelAdapter` (façade sales+cashier), `LireTransactionsKernelAdapter`
- `kernel/workflow/ExecuterWorkflowKernelAdapter`, `kernel/cashier/PorteMonnaieMonetaireAdapter`
- `kernel/inventory/VerifierDisponibiliteKernelAdapter`, `kernel/files/StockerDocumentKernelAdapter`
- `persistence/operation/` : `DefinitionOperationEntity`, `DefinitionOperationRepository`, `EtapeOperationEntity`, `EtapeOperationRepository`, `OperationPersistenceAdapter`
- `persistence/trace/` : `TraceOperationEntity`, `TraceOperationRepository`, `TracePersistenceAdapter`
- `persistence/enterprise/` : `EntrepriseEntity`, `EntrepriseRepository`, `EntreprisePersistenceAdapter`

### Migrations Liquibase (`src/main/resources/db/changelog/features/`)
- `operations.xml` : `definition_operation`, `etape_operation`, `trace_operation` + RLS (op-001..006)
- `enterprise.xml` : `entreprise` + RLS (ent-001..002)
- `_socle.xml` et `db.changelog-master.xml` : **non modifiés**.

### Tests (`src/test/`)
- `application/usecase/operation/ExecuterOperationServiceTest` (5), `DeclarerOperationServiceTest` (3)
- `application/saga/MoteurOperationTest` (3)
- `domain/transaction/TraceOperationTest` (4)
- `adapter/out/kernel/sales/EnregistrerVenteKernelAdapterTest` (WireMock, 1)
- `adapter/in/rest/operation/OperationRoutePatternTest` (1)
- `adapter/out/persistence/operation/OperationsRlsTest` (Testcontainers — exécuté en CI)

---

## 5. Endpoints REST exposés (alignés OpenAPI)

| Verbe | Chemin | Rôle |
|---|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/operations` | Déclarer une opération + étapes |
| GET | `/v1/businesses/{businessId}/operations` | Lister les opérations |
| POST | `/v1/businesses/{businessId}/operations/{name}:execute` | Exécuter (200 immédiat / 202 différé / 422 règle) |
| GET | `/v1/businesses/{businessId}/transactions` | Historique transactions (lu du kernel) |
| GET | `/v1/businesses/{businessId}/traces` | Lister les traces |
| GET | `/v1/businesses/{businessId}/traces/{traceId}` | Suivre une opération différée |
| POST/GET/PUT | `/v1/businesses` … `/{businessId}` … `/lifecycle` | Entreprise (minimal, périmètre Dev 3) |

---

## 6. Conformité aux conventions du socle

- [x] Entités R2DBC `implements Persistable<UUID>` + fabrique `nouveau(...)` + `@Transient nouveau`.
- [x] Repositories `ReactiveCrudRepository` **sans `WHERE tenant_id`** (RLS).
- [x] Toute table tenant : `tenant_id uuid NOT NULL` + **bloc RLS recopié à l'identique** du socle.
- [x] Un changelog par feature, IDs préfixés ; `_socle.xml`/master intouchés.
- [x] Appels kernel **uniquement** via `KernelClient` (jamais de `WebClient` brut).
- [x] Tenant lu du `BusinessContext`, jamais du payload.
- [x] DTO validés (`@Valid`) ; erreurs via `ProblemException` (RFC 7807).
- [x] Réactif de bout en bout (Mono/Flux), **aucun `.block()`**.
- [x] Dépendance à Dev 4 (règles) **par le port** `EvaluateurDeRegle`, jamais sa classe.

---

## 7. Vérification du build

| Étape | Résultat |
|---|---|
| `mvn clean compile` (sources principales) | ✅ OK |
| `mvn test-compile` (tests) | ✅ OK |
| Tests hors-ligne (Mockito + WireMock) | ✅ **17/17 verts** |
| Tests Testcontainers (RLS + contexte Spring) | ⏸️ **Reportés** — image Docker `postgres:16` non cachée + connexion limitée → exécutés en CI |

Détail des 17 tests verts : moteur saga & compensation (3), idempotence + immédiat/différé + blocage
422 (5), transitions de trace (4), façade vente multi-appels WireMock (1), déclaration + unicité + 404
(3), motif de route `:execute` (1).

Maven utilisé : binaire bundlé IntelliJ (3.9.9), cache `.m2` local. Java 21. Aucun téléchargement réseau.

---

## 8. Definition of Done (spec feat-operations.md)

- [x] Déclarer une opération « vente » avec ses étapes ordonnées.
- [x] L'exécuter en mode immédiat → 200 + transaction créée + trace COMPLETEE.
- [x] Simuler un échec d'étape → compensation saga → trace COMPENSEE.
- [x] Idempotence : rejouer la même clé ne crée pas de doublon.
- [x] Opération différée → 202 + trace EN_COURS suivable.
- [x] L'étape EVALUER_REGLES appelle le **port** EvaluateurDeRegle (pas la classe de Dev 4).
- [x] Tests unitaires + intégration sur EnregistrerVente (façade multi-appels).
- [~] Changelog appliqué, CI verte : appliqué/validé en local hors RLS ; **RLS + contexte à confirmer en CI** (Docker).

---

## 9. État Git

- Branche **`feat-operation-miguel`** @ `44bb2e6` — 1 commit d'avance sur `develop2`.
- **`develop2` intacte** (`fcb6400`, == `origin/develop2`) — jamais touchée.
- **Non poussé** (pas d'instruction de push + connexion limitée).
- Hors commit (docs d'équipe pré-existantes) : `docs/guides/feat-operations.md` (modifié), `docs/guides/CONVENTIONS-SOCLE.md` (non suivi).

---

## 10. Reste à faire / points d'attention

1. **Pousser** la branche : `git push -u origin feat-operation-miguel` puis ouvrir une PR vers `develop2`.
2. Lancer `mvn verify` complet **avec Docker** (tests RLS Testcontainers + chargement de contexte).
3. **Coordonner le merge de la brique Entreprise minimale avec Dev 3** (offers-actors) — fichiers
   marqués « à fusionner » : `domain/enterprise/*`, `adapter/*/enterprise/*`, `usecase/enterprise/*`,
   `db/changelog/features/enterprise.xml`.
4. Optionnel : consumer Kafka pour résoudre les opérations différées (`EN_COURS → COMPLETEE`) — hors
   chemin critique du DoD.
5. Décider si `CONVENTIONS-SOCLE.md` doit être commité (actuellement non suivi).
