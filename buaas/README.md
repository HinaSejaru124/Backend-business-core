# BCaaS — Business Core as a Service

> Un noyau métier générique, modulaire et configurable,
> inspiré de l'ingénierie des protocoles réseau (OSI/TCP-IP).

## Vision

BCaaS fournit un socle réutilisable pour n'importe quelle application métier.
Chaque domaine (santé, transport, logistique, orientation...) reconstruit
les mêmes briques : acteurs, ressources, règles, workflow, audit, notifications.
BCaaS mutualise ce cœur en un service configurable et multi-tenant.

## Structure du projet
bcaas-core/     Noyau générique — publié sur Nexus comme librairie
bcaas-sdk/      SDK léger pour les intégrateurs tiers
buaas-app/      Application d'orientation (1er consommateur de bcaas-core)
frontend/       Interface web Next.js (SSR, PWA, i18n)
mobile/         Application React Native
infra/          Docker, CI/CD, Nginx, Nexus
docs/           Documentation, ADR, schémas d'architecture

## Pile protocolaire métier (5 couches)
Couche 5 — Business Capabilities   Acteurs, ressources, workflow, audit
Couche 4 — Context & Policy        Identité, permissions, SLA, saga
Couche 3 — Tenant & Routing        Multi-tenant, discovery, versioning
Couche 2 — Transport & Messaging   REST/Kafka, retry, idempotence
Couche 1 — Infrastructure          PostgreSQL, Redis, Kafka, MinIO

## Stack technique

| Composant | Technologie |
|-----------|-------------|
| Backend | Spring Boot 3.5 / Java 21 / WebFlux + R2DBC |
| Frontend | Next.js (SSR, PWA, SEO, i18n) |
| Mobile | React Native |
| Base de données | PostgreSQL 16 + PostGIS |
| Cache | Redis 7 |
| Messaging | Apache Kafka |
| Registre Maven | Sonatype Nexus 3 |
| Stockage | MinIO |
| Monitoring | Prometheus + Grafana + ELK |
| CI/CD | GitLab CE |

## Démarrage rapide

```bash
# Lancer toute l'infrastructure
cd infra/docker
docker compose --env-file .env up -d

# Compiler tous les modules
mvn clean install

# Lancer buaas-app
cd buaas-app
mvn spring-boot:run
```

## Documentation

- [ADR-001 Structure Maven Multi-Module](docs/adr/ADR-001-multi-module-maven.md)
- [ADR-002 Architecture Hexagonale](docs/adr/ADR-002-architecture-hexagonale.md)
- [ADR-003 Pile Protocolaire Métier](docs/adr/ADR-003-pile-protocolaire-metier.md)
- [ADR-004 Nexus Registre Maven](docs/adr/ADR-004-nexus-registre-maven.md)
