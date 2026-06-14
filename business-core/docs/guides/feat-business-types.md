# feat/business-types — Type Métier + Configuration

> **Dev 2.** Briques 1 (Type Métier) et 7 (Configuration).
> Lis d'abord `README.md`. Ce fichier détaille ta mission. Donne les deux à ton IA de code.

## Ta mission en une phrase

Tu construis **le point d'entrée du modèle métier** : la déclaration des types, leur versionnement immuable, et les paramètres de configuration. Sans ta brique, rien ne se déclare.

---

## Branche
```
git checkout main && git pull
git checkout -b feat/business-types
```

## Tes packages (tu ne sors pas de là)
```
domain/businesstype/          domain/configuration/
application/usecase/businesstype/   application/usecase/configuration/
adapter/in/rest/businesstype/       adapter/in/rest/configuration/
adapter/out/persistence/businesstype/   adapter/out/persistence/configuration/
adapter/out/kernel/businessdomain/  (appel ResoudreBusinessDomain)
```

---

## Partie A — Type Métier (brique 1)

### Concept
Un **Type Métier** est le gabarit réutilisable d'une catégorie d'activité (« Pharmacie »), déclaré en données. Il est **versionné par épinglage** : une version publiée est **immuable** ; chaque entreprise reste figée sur sa version. On ne casse jamais une entreprise active.

### Entités à créer
| Entité | Champs clés |
|---|---|
| `TypeMetier` | `id` (UUID), `clientApplicationId` (réf kernel — propriétaire), `businessDomainId` (réf kernel — taxonomie, optionnel), `code`, `nom`, `statut` (`StatutType`) |
| `VersionType` | `id`, `typeMetierId` (FK), `numero` (int), `immuable` (bool), `publieeLe` (datetime) |

> `StatutType` ∈ { BROUILLON, PUBLIE, ARCHIVE } — déjà dans `domain/shared/`.

### Règles métier à coder
- **RG-03** : une version publiée est immuable. Toute tentative de modification → `ProblemException` 409.
- Publier une version crée une **nouvelle** `VersionType` avec `numero` incrémenté ; on ne modifie jamais l'ancienne.
- Le `businessDomainId` est une **référence** vers la taxonomie kernel (port `ResoudreBusinessDomain`), pas une duplication. Le Type Métier *porte le comportement* ; le BusinessDomain kernel n'est qu'un classement.

### Port de sortie à implémenter
- `ResoudreBusinessDomain` → appelle `GET/POST /api/business-domains` du kernel.

### Endpoints REST à exposer
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types` |
| GET | `/v1/business-types` |
| GET | `/v1/business-types/{typeId}` |
| POST | `/v1/business-types/{typeId}/versions` |
| GET | `/v1/business-types/{typeId}/versions` |
| GET | `/v1/business-types/{typeId}/versions/{n}` |

Schémas de requête/réponse : voir la spec OpenAPI (`CreateBusinessType`, `BusinessType`, `TypeVersion`, `TypeVersionDetail`).

---

## Partie B — Configuration (brique 7)

### Concept
La **Configuration** = les paramètres de réglage (devise, taxe, seuils…). Elle dit *avec quelle valeur*, là où une Règle dit *quoi faire*. Deux niveaux : **défaut au Type** (verrouillé ou ajustable) + **surcharge à l'Entreprise** (seulement ce qui est ajustable).

### Entité à créer
| Entité | Champs clés |
|---|---|
| `ParametreConfig` | `id`, `versionTypeId` (FK, défaut) **ou** `entrepriseId` (FK, surcharge), `cle`, `valeur`, `verrouille` (bool) |

> Un seul des deux FK est rempli (défaut **ou** surcharge), comme pour les règles.

### Règle métier à coder
- Surcharger un paramètre **verrouillé** au niveau Type → `ProblemException` 409.
- La résolution « surcharge entreprise sinon défaut type » est une logique simple : garde-la dans le domaine pour l'instant (le port `ResolveurDeParametre` n'est pas créé tant que ça reste trivial — cf. YAGNI).

### Port de sortie à implémenter
- `DepotDeConfiguration` → `GET/PUT /api/settings/organizations/{orgId}/operational-policy` du kernel (pour les paramètres qui se reflètent côté kernel).

### Endpoints REST à exposer
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/config` (défaut) |
| PUT | `/v1/businesses/{businessId}/config/{key}` (surcharge — coordonne-toi avec Dev 3 qui possède l'entité `Entreprise`) |

> ⚠️ La surcharge entreprise touche l'entité `Entreprise` (chez Dev 3). Tu **lis** son ID, tu ne modifies pas sa classe. Passe par le repository d'`Entreprise` exposé en lecture, ou expose un petit port si besoin (PR socle).

---

## Migration Liquibase
Crée `src/main/resources/db/changelog/features/business-types.xml` avec tes tables (`type_metier`, `version_type`, `parametre_config`). IDs de changeset préfixés `bt-001`, `bt-002`… Le master changelog du socle l'inclut automatiquement.

## Definition of Done
- [ ] Je peux créer un type, publier 2 versions, vérifier que la v1 reste inchangée.
- [ ] Une tentative de modif d'une version publiée renvoie 409 RFC 7807.
- [ ] Je peux définir un paramètre défaut verrouillé et vérifier qu'une surcharge entreprise échoue.
- [ ] Tests unitaires + 1 test d'intégration sur `ResoudreBusinessDomain`.
- [ ] Changelog appliqué, CI verte, code réactif.

## Suggestions de prompts pour ton IA
- « Implémente l'entité de domaine `TypeMetier` et son `VersionType` selon ces champs, en Java 21, sans dépendance Spring (domaine pur). »
- « Écris le use case `PublierVersion` qui crée une nouvelle VersionType immuable avec numéro incrémenté, et lève ProblemException 409 si on tente de modifier une version existante. »
- « Crée le repository R2DBC réactif et le mapping entité↔domaine pour TypeMetier. »
- « Écris le @RestController WebFlux pour les endpoints /v1/business-types, renvoyant des Mono/Flux, conforme à la spec OpenAPI. »
