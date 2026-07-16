# Business Core

Noyau métier générique, multi-tenant et extensible, conçu comme une **façade hexagonale** au-dessus
du kernel RT-Comops. Il répond à « c'est quoi un métier ? » via un modèle déclaratif (*metadata-driven*)
en sept briques, exposé par une API REST réactive.

## Stack

Java 21, Spring Boot 4.0, Spring WebFlux + R2DBC (réactif, non bloquant), PostgreSQL 16, Redis 7,
Kafka 3, Liquibase, Maven.

## Démarrer en local (Docker, all-in-one)

L'application, PostgreSQL, Redis et Kafka tournent dans un **seul conteneur** (voir [`Dockerfile`](Dockerfile)).

```bash
cp .env.example .env       # adapter les valeurs (mots de passe, clé de chiffrement...)
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

La documentation OpenAPI est servie sur `/swagger-ui.html`.

## Architecture

Architecture hexagonale stricte : le domaine ne dépend de rien ; toute technologie (base, kernel,
Kafka) est un *adapter* branché sur un *port*. Le kernel est un adapter de sortie comme un autre.

```
src/main/java/com/yowyob/businesscore/
├── domain/         # les 7 briques + ports (in/out/internal) + enums partagés. Aucune dépendance technique.
├── application/    # use cases, BusinessContext, sécurité, saga, dispatchers
└── adapter/
    ├── in/         # REST, sécurité (clé Business Core), consumers Kafka
    └── out/        # client kernel, persistance R2DBC, cache Redis, publishers Kafka
```

## Sécurité — défense en profondeur

L'isolation multi-tenant repose sur trois barrières indépendantes :

1. **Entrée** : la clé Business Core (`X-BC-Client-Id` / `X-BC-Api-Key`) détermine le tenant, placé
   dans le `BusinessContext` ; aucune requête ne progresse sans tenant.
2. **Application** : les use cases lisent le tenant du contexte (jamais du payload) et re-vérifient
   tout identifiant reçu.
3. **Base** : PostgreSQL Row-Level Security (`ENABLE` + `FORCE`, `USING` + `WITH CHECK`) via un rôle
   applicatif non-owner ; la base refuse physiquement les lignes d'un autre tenant.

Les secrets kernel sont chiffrés au repos (AES-256-GCM) ; la clé Business Core est stockée hachée.
Aucun secret réel ne doit être commité — voir [`.env.example`](.env.example).

## Tests

```bash
mvn verify
```

Inclut le test d'isolation RLS (Testcontainers PostgreSQL) et le test témoin d'authentification kernel
(WireMock).

## Déploiement

Voir [`docs/deploiement.md`](docs/deploiement.md).
