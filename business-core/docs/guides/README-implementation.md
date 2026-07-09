# Guide d'implémentation — équipe

> À lire avant de coder. Chaque développeur a aussi son fichier dédié : `feat-business-types.md`,
> `feat-offers-actors.md`, `feat-rules.md`, `feat-operations.md`.

## 1. Le projet en une page

Le **Business Core** est un noyau métier générique qui s'appuie sur le **kernel RT-Comops** (déjà
déployé, distant). Il répond à « c'est quoi un métier ? » via un modèle déclaratif (_metadata-driven_)
en sept briques, exposé par une API REST réactive, en architecture hexagonale.

Principe fondateur : **le développeur déclare son métier en données** ; il ne code rien dans le Business
Core. Nous codons le moteur générique qui interprète ces données. Le domaine ne dépend de rien : toute
technologie (base, kernel, Kafka) est un _adapter_ branché sur un _port_.

## 2. Stack (figée)

| Élément    | Choix                                                            |
| ---------- | ---------------------------------------------------------------- |
| Langage    | Java 21 (LTS)                                                    |
| Framework  | Spring Boot 4.0.x (Framework 7, Security 7, Jackson 3)           |
| Web        | Spring WebFlux (réactif)                                         |
| Données    | Spring Data R2DBC + r2dbc-postgresql                             |
| Base       | PostgreSQL 16                                                    |
| Cache      | Redis 7 (réactif)                                                |
| Événements | Kafka 3                                                          |
| Migrations | Liquibase (JDBC au démarrage)                                    |
| Build      | Maven                                                            |
| Tests      | JUnit 5, Reactor Test (`StepVerifier`), Testcontainers, WireMock |

**Réactif partout** : `Mono`/`Flux`, jamais `.block()`, `WebClient` (pas `RestTemplate`), pas de JDBC
au runtime.

## 3. Structure du code

Voir [architecture hexagonale](../architecture/hexagonale.md). Chaque développeur travaille dans les
sous-packages de ses briques, à travers les trois couches (`domain/`, `application/usecase/`,
`adapter/.../<brique>`). Tant que tu restes dans tes packages, tu ne touches le code de personne.

## 4. Ce que fournit le socle (figé, ne pas modifier)

Squelette qui démarre · sécurité (clé Business Core → `BusinessContext`) · client kernel de base
(JWT cache Redis) · **toutes les interfaces de ports** · `BusinessContext` · `ProblemException` +
handler RFC 7807 · enums partagés (`domain/shared/`) · socle BD (R2DBC + Liquibase + master changelog)
· cadres des moteurs génériques (`EvaluateurDeRegle`, `ExecuteurDEtape` + dispatchers) · isolation
tenant (RLS + pool tenant-aware) · CI.

## 5. Catalogue des ports

Voir [architecture hexagonale](../architecture/hexagonale.md#ports). Tu **implémentes** ces interfaces,
tu ne les modifies pas. Besoin d'un nouveau port → PR sur le socle, validée par le lead.

## 6. Modèle de données

Voir [les sept briques](../architecture/sept-briques.md#entités-et-stockage). Règle d'or : on stocke le
modèle métier + des références kernel, **jamais** les données opérationnelles du kernel (cache Redis TTL
pour les données kernel fréquentes).

## 7. Workflow Git

```
develop2 (= main d'intégration)   ← le socle. Toujours vert.
 ├── feat/business-types   ← Dev 2
 ├── feat/offers-actors    ← Dev 3
 ├── feat/rules            ← Dev 4
 └── feat/operations       ← Dev 5
```

1. Personne ne pousse sur l'intégration sauf le lead ; les features arrivent par Pull Request.
2. Pars toujours de l'intégration à jour ; rebase régulièrement pour récupérer les évolutions du socle.
3. Une PR = une fonctionnalité testée, CI verte. Commits petits et fréquents.

## 8. Les cinq règles anti-conflit

1. **Reste dans tes packages.**
2. **Un changelog Liquibase par feature** (`db/changelog/features/<feature>.xml`, IDs préfixés). Ne
   modifie jamais un changelog existant ; reproduis le bloc RLS du socle pour tes tables tenant.
3. **Un `@RestController` par feature** dans ton sous-package.
4. **Ne modifie jamais** : `domain/port/`, `domain/shared/`, `application/context/`,
   `adapter/in/security/`, `adapter/out/cache/`, la config racine.
5. **Pas de dépendance directe entre features** : passe par un port (ex. Opérations appelle
   `EvaluateurDeRegle`, jamais la classe de Dev 4).

## 9. Conventions

- Domaine pur : aucun import Spring/R2DBC/kernel dans `domain/`.
- Use cases dans `application/usecase/` : orchestrent, appellent les ports.
- Lève `ProblemException` (jamais de stacktrace au client).
- DTO ≠ entité R2DBC ≠ objet de domaine.
- Classes de domaine en français (`TypeMetier`, `RegleMetier`).

## 10. Sécurité

Voir [sécurité — défense en profondeur](../architecture/securite-defense-profondeur.md) et la checklist
sécurité par PR qui s'y trouve.

## 11. Tests manuels E2E

Voir [parcours de test E2E](test-e2e-parcours-complet.md) et le script
[`scripts/e2e-parcours.sh`](../../scripts/e2e-parcours.sh). Utiliser le **JWT** (`Authorization: Bearer`)
sur toutes les routes protégées après login. Sur les routes d'intégration, ajouter optionnellement
`X-BC-Client-Id` / `X-BC-Api-Key` pour identifier la clé API du développeur.

## 12. Definition of Done (par PR)

- [ ] Compile et démarre ; endpoints testés (Swagger/Postman).
- [ ] Tests unitaires des use cases + au moins un test d'intégration kernel.
- [ ] Changelog appliqué proprement ; tables avec `tenant_id` + RLS.
- [ ] Aucun fichier hors de mes packages modifié (sauf mon changelog feature).
- [ ] CI verte ; code réactif (pas de `.block()`).

## 13. Ordre de démarrage

1. Lead : socle livré (fait).
2. Les 4 features démarrent en parallèle. Dev 2 (Types) a une légère priorité : les autres déclarent
   leur contenu sous une version de type.
