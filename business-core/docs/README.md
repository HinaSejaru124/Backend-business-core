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

## Démarrage rapide

Voir le [README racine](../../README.md) pour lancer l'infrastructure et l'application en local.

## Statut

Le **socle** (fondations figées : sécurité, ports, BusinessContext, client kernel, RLS, RFC 7807) est
livré. Les quatre features se branchent dessus via leurs sous-packages, sans modifier le socle.
