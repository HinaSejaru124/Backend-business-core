# Documentation — Business Core

Documentation du **Business Core** : noyau métier générique, multi-tenant et réactif, conçu comme une
façade hexagonale au-dessus du kernel RT-Comops.

## Sommaire

### Architecture (le « comment »)
- [Architecture hexagonale](architecture/hexagonale.md) — couches, ports & adapters, règle de dépendance.
- [Les sept briques](architecture/sept-briques.md) — le modèle métier piloté par les métadonnées.
- [Sécurité — défense en profondeur](architecture/securite-defense-profondeur.md) — isolation multi-tenant, RLS, secrets.
- [Authentification — trois flux](architecture/authentification-trois-flux.md) — développeur (JWT délégué),
  backend terminal (clé API business), acteur métier (identité KCore) : mécanismes, surfaces, schémas.

### Décisions d'architecture — ADR (le « pourquoi »)
- [ADR-001 — Spring Boot 4 réactif](adr/ADR-001-spring-boot-4-reactif.md)
- [ADR-002 — Isolation tenant par RLS](adr/ADR-002-isolation-tenant-rls.md)
- [ADR-004 — Modèle metadata-driven en 7 briques](adr/ADR-004-metadata-driven-7-briques.md)

### Guides développeur
- [Guide d'implémentation](guides/README-implementation.md) — conventions, workflow Git, règles anti-conflit.
- [Conventions du socle](guides/CONVENTIONS-SOCLE.md) — patrons réels (entités, repositories, RLS, KernelClient).
- [feat/business-types](guides/feat-business-types.md) — Type Métier + Configuration (brief de kickoff).
- [feat/offers-actors](guides/feat-offers-actors.md) — Offre + Acteurs + Entreprise (brief de kickoff).
- [feat/rules](guides/feat-rules.md) — Règles (brief de kickoff).
- [feat/operations](guides/feat-operations.md) — Opérations + Transactions (brief de kickoff).
- [Parcours de test E2E](guides/test-e2e-parcours-complet.md) — script manuel bout en bout.

> Les briefs `feat-*.md` datent du lancement du projet (répartition initiale par développeur) : ils
> restent utiles pour le découpage conceptuel des briques, mais pour le contrat exact d'un endpoint ou
> d'un port, la référence est le code et la spécification OpenAPI **auto-générée et toujours à jour**,
> servie par l'application elle-même (`GET /v3/api-docs`, `/swagger-ui.html`) — pas un fichier statique.

### Référence kernel (le contrat externe)
- [Guide spécial auth](Guide_Special_Auth.md) — comment le kernel expose son authentification OAuth2/OIDC.
- [Authentification déléguée](AUTHENTIFICATION-DELEGUEE%20(2).md) — proposition de conception initiale ;
  **le flux Authorization Code + redirection qu'elle décrit n'a pas été retenu**, voir le guide spécial
  auth et `architecture/authentification-trois-flux.md` pour le flux réellement implémenté
  (discover-contexts/select-context).
- [Référence kernel](REFERENCE-KERNEL%20(1).md) — endpoints, DTO, pièges empiriques.
- [Identifiants kernel non modélisés](IDENTIFIANTS-KERNEL%20(1).md) — stratégie de résolution
  (`ResolveurContexteKernel`).

### Exploitation
- [Déploiement sur l'infra yowyob](deploiement.md) — GitLab CI, réseau `yowyob`, Traefik, variables.

## Démarrage rapide

Voir le [README racine](../../README.md) pour lancer l'infrastructure et l'application en local.

## Statut

Le **socle** (fondations figées : sécurité, ports, BusinessContext, client kernel, RLS, RFC 7807) est
livré. Les features se branchent dessus via leurs sous-packages, sans modifier le socle.