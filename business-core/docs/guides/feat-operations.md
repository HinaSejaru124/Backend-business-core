# feat/operations — Opérations + Transactions

> **Dev 5.** Briques 5 (Opérations) et 6 (Transactions). Le moteur qui met tout en mouvement.
> Lis d'abord `README.md`. Donne les deux fichiers à ton IA de code.

## Ta mission en une phrase

Tu construis **le moteur d'exécution** : les workflows métier (opérations) et la trace des échanges de valeur (transactions). Ta brique orchestre toutes les autres via les ports.

---

## Branche
```
git checkout main && git pull
git checkout -b feat/operations
```

## Tes packages
```
domain/operation/          domain/transaction/
application/usecase/operation/   application/usecase/transaction/
application/saga/                 (exécution des workflows — avec le socle)
adapter/in/rest/operation/   adapter/in/rest/transaction/
adapter/out/persistence/operation/   .../transaction/   .../trace/
adapter/out/kernel/sales/   .../cashier/   (façade financière)
```

---

## Partie A — Opérations (brique 5)

### Concept
Une **Opération** est le verbe du métier (vendre, réserver). C'est un **workflow déclaré en données** : une séquence d'**étapes-types** ordonnées, exécutée par le **moteur Saga du kernel**. Bénéfice gratuit : la **compensation** (si une étape échoue, les précédentes sont annulées).

### Entités
| `DefinitionOperation` | `id`, `versionTypeId` (FK), `nom`, `roleDeclencheur` |
| `EtapeOperation` | `id`, `operationId` (FK), `ordre` (int), `typeEtape` (du catalogue) |

### Le catalogue d'étapes-types (discipline anti-dérive)
Les étapes viennent d'un **catalogue fermé** que le développeur ordonne — pas de liberté totale (sinon on dérive vers un langage de prog). Étapes-types : `VERIFIER_STOCK`, `EVALUER_REGLES`, `ENREGISTRER_VENTE`, `ENCAISSER`, `EMETTRE_EVENEMENT`, `ATTACHER_DOCUMENT`…

### Port interne (extensibilité — même principe que les règles)
- `ExecuteurDEtape` : chaque étape-type est une **stratégie** derrière cette interface. Ajouter une étape = ajouter une implémentation, sans toucher au moteur. Implémente d'abord les étapes de la vente.
- `PlanificateurDOperation` : donne la séquence d'étapes d'une opération.

### Points d'ancrage de règles (lien avec Dev 4 — via port)
À l'étape `EVALUER_REGLES`, **appelle le port `EvaluateurDeRegle`** (implémenté par Dev 4). Tu lui **passes le contexte de valeurs** (stock lu, montant…) — c'est la couche application qui résout ces valeurs via les autres ports, puis les donne à l'évaluateur. Tu n'importes jamais une classe de Dev 4 : seulement l'interface du socle. Mettez-vous d'accord sur le **vocabulaire des déclencheurs** (`AVANT_VENTE`, `AVANT_ENCAISSEMENT`…).

### Modes immédiat / différé
- **Immédiat** (vente comptoir) → résultat tout de suite, HTTP `200`.
- **Différé** (commande fournisseur) → `202` + une `TraceOperation` en statut `EN_COURS`, résolue plus tard via `PublierEvenement` (Kafka).

### Ports de sortie
- `ExecuterWorkflow` → moteur Saga du kernel (orchestration + compensation).
- `VerrouDIdempotence` → garantit qu'une opération ne s'exécute pas 2× (utilise l'en-tête `Idempotency-Key` + la `cleIdempotence` de la trace).
- `PublierEvenement` → Kafka, pour le différé.

### Endpoints REST
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/operations` (déclarer) |
| GET | `/v1/businesses/{businessId}/operations` (lister) |
| POST | `/v1/businesses/{businessId}/operations/{nom}:execute` (exécuter — 200 ou 202) |

---

## Partie B — Transactions (brique 6)

### Concept
Une **Transaction** = la trace d'un **échange de valeur** (≠ paiement, qui n'en est qu'une facette). C'est une **façade unifiée** sur les cores financiers du kernel : le développeur dit « enregistre cette vente », tu orchestres derrière `sales` + `cashier` + `accounting`. Échange **générique** : le monétaire est le cas courant, pas exclusif.

### Port interne
- `PorteMonnaieGenerique` : le monétaire est une *implémentation* parmi d'autres (troc, don possibles plus tard). Implémente d'abord le monétaire.

### Port de sortie
- `EnregistrerVente` → `POST /api/sales/orders` puis `POST /api/sales/orders/{orderId}/confirm`, puis lecture `GET /api/cashier/bills/{id}`. **Un port, plusieurs appels** — c'est la façade.

### Endpoint REST
| Verbe | Chemin |
|---|---|
| GET | `/v1/businesses/{businessId}/transactions` (lu du kernel, jamais stocké) |

---

## Partie C — TraceOperation (entité de liaison — clé du système)

| `TraceOperation` | `id`, `entrepriseId`, `operationId`, `cleIdempotence` (unique), `transactionKernelId` (réf corrélation), `statut` (`StatutTrace`), `resultatRegles` (JSON), `creeLe`, `resoluLe` |

> `StatutTrace` ∈ { EN_COURS, COMPLETEE, COMPENSEE }.

Cette entité fait fonctionner **3 mécanismes** :
- **Compensation** : `transactionKernelId` dit *quoi annuler* si une étape échoue.
- **Idempotence** : `cleIdempotence` empêche le double traitement.
- **Audit** : relie la demande au résultat kernel.

### Endpoints de suivi
| Verbe | Chemin |
|---|---|
| GET | `/v1/businesses/{businessId}/traces` |
| GET | `/v1/businesses/{businessId}/traces/{traceId}` (suivre une opération différée) |

---

## Migration Liquibase
`db/changelog/features/operations.xml` — tables `definition_operation`, `etape_operation`, `trace_operation`. IDs préfixés `op-001`…

## Definition of Done
- [ ] Déclarer une opération « vente » avec ses étapes ordonnées.
- [ ] L'exécuter en mode immédiat → 200 + transaction créée + trace COMPLETEE.
- [ ] Simuler un échec d'étape → compensation Saga → trace COMPENSEE, métier cohérent.
- [ ] Idempotence : rejouer la même clé ne crée pas de doublon.
- [ ] Une opération différée renvoie 202 + trace EN_COURS suivable.
- [ ] L'étape EVALUER_REGLES appelle bien le port EvaluateurDeRegle (pas la classe de Dev 4).
- [ ] Tests unitaires + intégration sur EnregistrerVente (façade multi-appels).
- [ ] Changelog appliqué, CI verte, réactif.

## Suggestions de prompts
- « Implémente le moteur d'opération qui lit une séquence d'EtapeOperation et exécute chaque étape via le port ExecuteurDEtape, en réactif (Mono chaîné). »
- « Code l'adapter `EnregistrerVente` qui enchaîne POST /api/sales/orders, /confirm, puis GET /api/cashier/bills/{id} avec WebClient, et renvoie un transactionKernelId. »
- « Implémente la TraceOperation et la logique d'idempotence basée sur l'en-tête Idempotency-Key. »
- « Écris la gestion de compensation : si une étape échoue, marque la trace COMPENSEE et déclenche l'annulation via ExecuterWorkflow. »
