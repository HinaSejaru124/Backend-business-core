# Business Core

Noyau métier générique, multi-tenant et extensible, conçu comme une **façade hexagonale** au-dessus
du kernel RT-Comops. Il répond à « c'est quoi un métier ? » via un modèle déclaratif (*metadata-driven*)
en sept briques, exposé par une API REST réactive.

Le projet vit dans [`business-core/`](business-core/). Le dossier `buaas/` (ancien prototype) a été
retiré de la branche `develop2` ; son historique reste sur la branche `develop`.

## Stack

Java 21, Spring Boot 4.0, Spring WebFlux + R2DBC (réactif, non bloquant), PostgreSQL 16, Redis 7,
Kafka 3, Liquibase, Maven.

## Démarrer en local

Prérequis : les ports **5432** (PostgreSQL), **6379** (Redis) et **9092** (Kafka) doivent être
**libres** sur l'hôte. Un service natif (ex. `postgresql.service`) ou un ancien conteneur qui occupe
l'un d'eux empêchera le conteneur correspondant de se mapper, et l'application se connecterait
silencieusement au mauvais service.

```bash
cd business-core
docker compose up -d         # PostgreSQL 16 + Redis 7 + Kafka 3
docker compose ps            # vérifier que chaque conteneur expose bien son port (colonne PORTS)
mvn spring-boot:run          # démarre l'application (profil dev)
curl http://localhost:8080/health
```

Copier [`business-core/.env.example`](business-core/.env.example) en `.env` et adapter au besoin.
La documentation OpenAPI est servie sur `/swagger-ui.html`.

### Dépannage des conflits de ports

`password authentication failed` au démarrage signifie généralement qu'un **PostgreSQL natif** occupe
déjà le port 5432 : l'application s'y connecte au lieu du conteneur. Deux options :

```bash
# Option 1 — libérer les ports standards (le plus simple)
sudo systemctl stop postgresql        # arrêter le PostgreSQL natif
docker compose down && docker compose up -d   # recréer pour que bc-postgres prenne 5432

# Option 2 — sans toucher aux services natifs : remapper les ports du conteneur
export BC_DB_PORT=5433 BC_REDIS_PORT=6380
docker compose up -d
BC_R2DBC_URL=r2dbc:postgresql://localhost:5433/businesscore \
BC_JDBC_URL=jdbc:postgresql://localhost:5433/businesscore \
BC_REDIS_PORT=6380 mvn spring-boot:run
```

Si tu as encore les conteneurs de l'ancien prototype `buaas-*`, ils peuvent aussi squatter 6379/9092 :
`docker stop buaas-redis buaas-kafka`.

## Architecture

Architecture hexagonale stricte : le domaine ne dépend de rien ; toute technologie (base, kernel,
Kafka) est un *adapter* branché sur un *port*. Le kernel est un adapter de sortie comme un autre.

```
business-core/src/main/java/com/yowyob/businesscore/
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

## Tests

```bash
cd business-core
mvn verify
```

Inclut le test d'isolation RLS (Testcontainers PostgreSQL) et le test témoin d'authentification kernel
(WireMock).
