# ADR-002 — Isolation tenant en défense en profondeur (RLS PostgreSQL)

- Statut : accepté
- Date : 2026-06-14

## Contexte

Plusieurs développeurs et entreprises coexistent sur la même instance (ENF-09). Un tenant ne doit
**jamais** voir les données d'un autre. Le filtrage « à l'entrée seulement » est fragile : il suffit
d'un use case ou d'une requête qui oublie le filtre — risque élevé avec quatre développeurs en
parallèle.

## Décision

- **Tenant = le développeur** (`tenant_id = clientApplicationId`). Frontière d'isolation dure. Le
  cloisonnement par entreprise (`business_id`) relève de l'autorisation, pas de RLS.
- **Trois barrières** :
  1. entrée (clé Business Core → `tenantId` dans le `BusinessContext`, jamais issu du payload) ;
  2. application (use cases + `TenantGuard`) ;
  3. base : **PostgreSQL Row-Level Security**, les deux mécanismes combinés (couche d'accès commune +
     RLS).
- RLS : rôle applicatif **non-owner** `bc_app` (sans `BYPASSRLS`), `ENABLE` + `FORCE`, policy `USING` +
  `WITH CHECK` sur `NULLIF(current_setting('app.current_tenant', true), '')::uuid`. Le pool R2DBC pose
  la variable de session par connexion depuis le `BusinessContext`.

## Alternatives écartées

- **Filtrage à l'entrée seul** : une seule omission = fuite. Rejeté.
- **Une base par tenant** : ne passe pas à l'échelle pour un SaaS multi-développeurs. Rejeté.
- **RLS via un rôle PostgreSQL par tenant** : ingérable ; on utilise un rôle applicatif partagé + une
  variable de session.

## Conséquences

- La base **refuse physiquement** une ligne d'un autre tenant, même si une requête brute oublie le
  filtre. Prouvé par un test d'intégration (`TenantIsolationRlsTest`) : isolation A≠B, INSERT
  cross-tenant rejeté, aucune ligne visible sans contexte (fail closed).
- Coût : un décorateur de `ConnectionFactory` + les policies (snippet de référence dans `_socle.xml`),
  invisible pour les features. Chaque feature reproduit le bloc RLS pour ses tables tenant.
- Pièges PostgreSQL à connaître : `FORCE` indispensable (sinon l'owner contourne), `WITH CHECK`
  indispensable (sinon INSERT cross-tenant possible), tester avec un rôle non-superuser.
