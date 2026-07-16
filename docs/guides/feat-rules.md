# feat/rules — Règles

> **Dev 4.** Brique 4 (Règles). La pièce la plus algorithmique.
> Lis d'abord `README.md`. Donne les deux fichiers à ton IA de code.

## Ta mission en une phrase

Tu construis **le moteur de règles** : la capacité d'interpréter des contraintes déclarées en données et d'appliquer leurs effets. C'est le cœur de la généricité.

---

## Branche
```
git checkout main && git pull
git checkout -b feat/rules
```

## Tes packages
```
domain/rule/
application/usecase/rule/
adapter/in/rest/rule/
adapter/out/persistence/rule/
adapter/out/kernel/audit/        (pour l'effet ALERTER → JournaliserAudit / PublierEvenement)
```

---

## Concept

Une **Règle** est une contrainte **déclarée en données** (jamais codée), de forme universelle :

```
DÉCLENCHEUR  →  CONDITION  →  EFFET
(quand)         (ce qu'on vérifie)   (ce qui se passe)
```

Exemple « ordonnance requise » : déclencheur = avant la vente ; condition = catégorie == médicament sur ordonnance ; effet = EXIGER un document, sinon BLOQUER.

### Niveau de puissance (IMPORTANT — contrainte de conception)
On implémente le **Niveau 1 : règles paramétrées** (catalogue fermé de conditions/effets, le développeur remplit les blancs). **MAIS le code doit être conçu pour évoluer vers le Niveau 2** (conditions composables) **sans réécriture**. Tu réalises ça en implémentant l'interface `EvaluateurDeRegle` (fournie par le socle) : le Niveau 1 est *une* implémentation ; le Niveau 2 en sera une autre, ajoutée plus tard. Ne code jamais en dur la logique d'évaluation dans le domaine ; passe toujours par l'interface.

> Le Niveau 3 (scriptable) est **écarté**. Ne l'implémente pas.

---

## Entité à créer
| `RegleMetier` | `id`, `versionTypeId` (FK, règle de Type) **ou** `entrepriseId` (FK, règle locale), `declencheur` (String/enum), `condition` (JSON ou expression simple), `effet` (`Effet`), `rolesAutorisesADeroger` (liste) |

> `Effet` ∈ { BLOQUER, EXIGER, VALIDER, AJUSTER, ALERTER, DEROGER } — déjà dans `domain/shared/`.
> Un seul FK rempli : règle de Type (obligatoire pour toutes les entreprises) **ou** règle d'Entreprise (locale).

---

## Les 6 effets, en 3 familles (à implémenter)

| Famille | Effet | Comportement |
|---|---|---|
| **Bloquants** | `BLOQUER` | l'opération est refusée |
| | `EXIGER` | arrêt jusqu'à fourniture d'un élément (document, pièce) |
| | `VALIDER` | arrêt jusqu'à l'approbation d'un autre acteur |
| **Mutateur** | `AJUSTER` | l'opération continue, la donnée est **corrigée** — jamais silencieusement, **toujours tracée** (valeur d'origine + corrigée) |
| **Traçants** | `ALERTER` | l'opération continue, une trace/notification est posée |
| | `DEROGER` | outrepassement immédiat avec **motif audité**, limité aux `rolesAutorisesADeroger` (vide = personne = équivaut à BLOQUER ; sinon les autres rôles retombent sur VALIDER) |

### Précautions à coder absolument
- **AJUSTER n'est jamais silencieux** : il corrige, informe (message), et trace l'ancienne + la nouvelle valeur.
- **DEROGER** vérifie le rôle de l'acteur courant (depuis le `BusinessContext`) contre `rolesAutorisesADeroger`. Exige et archive un motif.

---

## Ports

### Port interne à implémenter (le cœur)
- `EvaluateurDeRegle` (interface fournie par le socle). Ton implémentation Niveau 1 :
  - reçoit un déclencheur + un contexte de valeurs (fournies par la couche application, **tu ne vas pas chercher les données toi-même**),
  - charge les règles applicables via `RegistreDeRegles`,
  - évalue chaque condition,
  - retourne la liste des effets à appliquer.

> ⚠️ Le contexte (stock, seuils, heure) t'est **passé** par la couche application. Une règle ne va jamais lire le kernel elle-même. Cela garde ta brique pure.

### Ports de sortie
- `RegistreDeRegles` → charge les règles applicables (Type + Entreprise) pour un déclencheur. (Lecture en base, fournie/à implémenter côté persistence.)
- `PublierEvenement` / `JournaliserAudit` → pour les effets ALERTER, AJUSTER (trace), DEROGER (motif).

### Endpoints REST
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/rules` (règle de Type) |
| POST | `/v1/businesses/{businessId}/rules` (règle locale) |

---

## Lien avec les Opérations (Dev 5) — via port, pas en direct
Les déclencheurs (« avant la vente ») correspondent à des **points d'ancrage** dans les étapes d'opération. Dev 5 appellera **ton `EvaluateurDeRegle`** à ces points. Vous ne partagez aucune classe : le port vous découple. Mets-toi d'accord avec Dev 5 sur le **vocabulaire des déclencheurs** (ex. `AVANT_VENTE`, `AVANT_ENCAISSEMENT`) — c'est votre seul point de contact, fige-le tôt.

## Format d'erreur
Quand un effet bloque (BLOQUER/EXIGER/VALIDER), lève `ProblemException` 422 avec les champs métier : `violatedRule` (code de la règle), `requiredAction` (l'effet), `requiredDocument` (si EXIGER). Le socle formate en RFC 7807.

## Migration Liquibase
`db/changelog/features/rules.xml` — table `regle_metier`. IDs préfixés `rule-001`…

## Definition of Done
- [ ] Déclarer une règle de Type et une règle locale.
- [ ] Les 6 effets fonctionnent ; AJUSTER trace l'ancienne valeur ; DEROGER vérifie le rôle.
- [ ] L'évaluateur passe par l'interface `EvaluateurDeRegle` (extensible N2).
- [ ] Une règle bloquante renvoie 422 RFC 7807 enrichi.
- [ ] Tests unitaires couvrant les 6 effets + cas limites (déroger sans rôle, etc.).
- [ ] Changelog appliqué, CI verte, réactif.

## Suggestions de prompts
- « Implémente l'interface `EvaluateurDeRegle` en version Niveau 1 (catalogue paramétré), en Java 21 pur, conçue pour qu'une implémentation Niveau 2 puisse coexister. »
- « Code les 6 effets de règle en 3 familles ; pour AJUSTER, garantis qu'il informe et trace l'ancienne et la nouvelle valeur. »
- « Écris la logique DEROGER qui lit le rôle de l'acteur courant dans le BusinessContext et le compare à rolesAutorisesADeroger, sinon retombe sur VALIDER. »
- « Génère des tests unitaires StepVerifier pour chaque effet, avec un contexte de valeurs mické. »
