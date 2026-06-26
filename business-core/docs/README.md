# Documentation — Business Core

Documentation du **Business Core** : noyau métier générique, multi-tenant et réactif, conçu comme une
façade hexagonale au-dessus du kernel RT-Comops.

## Sommaire

### Architecture (le « comment »)
- [Architecture hexagonale](architecture/hexagonale.md) — couches, ports & adapters, règle de dépendance.
- [Les sept briques](architecture/sept-briques.md) — le modèle métier piloté par les métadonnées.
- [Sécurité — défense en profondeur](architecture/securite-defense-profondeur.md) — isolation multi-tenant, RLS, secrets.

### Décisions d'architecture — ADR (le « pourquoi »)
- [ADR-001 — Spring Boot 4 réactif](adr/ADR-001-spring-boot-4-reactif.md)
- [ADR-002 — Isolation tenant par RLS](adr/ADR-002-isolation-tenant-rls.md)
- [ADR-003 — Façade kernel à double clé](adr/ADR-003-facade-kernel-double-cle.md)
- [ADR-004 — Modèle metadata-driven en 7 briques](adr/ADR-004-metadata-driven-7-briques.md)

### Guides développeur
- [Guide d'implémentation](guides/README-implementation.md) — conventions, workflow Git, règles anti-conflit.
- [feat/business-types](guides/feat-business-types.md) — Dev 2 : Type Métier + Configuration.
- [feat/offers-actors](guides/feat-offers-actors.md) — Dev 3 : Offre + Acteurs + Entreprise.
- [feat/rules](guides/feat-rules.md) — Dev 4 : Règles.
- [feat/operations](guides/feat-operations.md) — Dev 5 : Opérations + Transactions.

### API
- [Spécification OpenAPI](api/business-core-openapi.yaml) — l'API exposée (importable dans Swagger / Postman).

### Exploitation
- [Déploiement sur l'infra yowyob](deploiement.md) — GitLab CI, réseau `yowyob`, Traefik, variables.

## Démarrage rapide

Voir le [README racine](../../README.md) pour lancer l'infrastructure et l'application en local.

## Statut

Le **socle** (fondations figées : sécurité, ports, BusinessContext, client kernel, RLS, RFC 7807) est
livré. Les quatre features se branchent dessus via leurs sous-packages, sans modifier le socle.


BASE_URL=https://kernel-core.yowyob.com
CLIENT_ID=prod-platform-backend
TENANT_ID=11111111-1111-1111-1111-111111111111
USER_ID=0303cc67-924f-4a16-aa69-bd1e463995b5
BUSINESS_ACTOR_ID=461f1f3b-efb7-42a5-8166-ce01764bd90e
ORGANIZATION_ID=0920af6a-aee9-45de-b57f-f0102106a7e5
ACCESS_TOKEN=eyJraWQiOiJrZXJuZWwtY29yZS1rZXktMSIsImFsZyI6IlJTMjU2In0.eyJhY3RvciI6ImNmMTBlNGZiLTJiY2QtNDUwMS04MDdjLTEyNjg1NjFiMWMxNiIsInN1YiI6IjAzMDNjYzY3LTkyNGYtNGExNi1hYTY5LWJkMWU0NjM5OTViNSIsImF1ZCI6Iml3bS1hcGkiLCJpc3MiOiJrZXJuZWwtY29yZSIsIm1mYSI6ZmFsc2UsImFkbSI6ZmFsc2UsImV4cCI6MTc4MjQ4ODk0MCwiaWF0IjoxNzgyNDg4MDQwLCJqdGkiOiJkZmJiOTAxYi03YmJjLTQ1OTctYjY1Mi00ZWYxZDNkZmJjMTMiLCJ0aWQiOiIxMTExMTExMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTEifQ.iJt83PlD-tilR3eX90qn_MeOcun4QYngqSHdnshMU8k7HQ35bYwKjPWMDouRsMkMRR_aSS3uDNcJrEejPUrhjv_5nz10BuDmWpKaW3ffK2xxJPZkJVBwj2GgjxC4JTHncBLG2u8BrgZeEw1mI7DcqJyrdt_xpzbh0PWLXTTdLMifFqbGKCnQzEZVFAKZ6BChaonHQvyRNEeFfEMvWbA1VCtEp445IfED_5OuFhGcXj2qex1GmwIh5tUvyUf5k_CVrSw_PpupaVRaOU91BM85ZkgHKEKCl9xql5i0-6tc6p8VbD_0vOK2lsxzoTYh4K0K_9AQ0kYSwXQP6DjoQV-SaQ