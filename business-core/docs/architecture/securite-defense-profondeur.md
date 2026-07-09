# Sécurité — défense en profondeur

La sécurité n'est pas qu'un filtre à l'entrée : chaque couche se protège elle-même, indépendamment des
autres. Si une couche laisse passer une erreur, la suivante la rattrape. Le socle fournit l'ossature ;
chaque feature respecte la checklist de sécurité.

## Définition du tenant

Le **tenant = le développeur**, identifié par `tenant_id = clientApplicationId` (propriétaire des Types
Métier). C'est la frontière d'isolation **dure**. Les données rattachées à une entreprise portent aussi
`business_id` : l'accès inter-entreprises au sein d'un même développeur relève de l'**autorisation**
(relationnel, dynamique), pas de l'isolation RLS.

## Isolation multi-tenant — trois barrières

```mermaid
flowchart TB
  Filtre["Barriere 1 - entree (cle Business Core)"] -->|"tenantId dans BusinessContext"| UseCase
  UseCase["Barriere 2 - application (use cases)"] -->|"tenant du contexte, jamais du payload"| Base
  Base["Barriere 3 - base (RLS PostgreSQL)"] -->|"refuse physiquement les lignes d'un autre tenant"| PG[(PostgreSQL)]
```

1. **Entrée** — JWT Bearer et/ou clé BC (`X-BC-Client-Id` / `X-BC-Api-Key`) construisent le
   `BusinessContext` (avec `tenantId`) propagé dans le `Context` Reactor. Pas de tenant → 401.
2. **Application** — les use cases lisent le tenant **du `BusinessContext`, jamais du payload client** ;
   tout `id` reçu est revérifié comme appartenant au tenant courant (`TenantGuard`).
3. **Base** — **PostgreSQL Row-Level Security**, via un rôle applicatif **non-owner** (`bc_app`) :
   - `ENABLE` **et** `FORCE ROW LEVEL SECURITY` (le propriétaire lui-même est soumis) ;
   - policy `USING` **et** `WITH CHECK` (bloque aussi un INSERT cross-tenant) ;
   - prédicat `tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid` →
     **fail closed** (aucune ligne visible) si le contexte tenant n'est pas posé ;
   - le pool R2DBC pose `app.current_tenant` à l'allocation de connexion depuis le `BusinessContext` et
     le réinitialise au release (pas de bleed entre requêtes).

Pourquoi les deux derniers niveaux ensemble : la couche applicative protège même si RLS est mal
configuré ; RLS protège même si une requête brute contourne la couche applicative. Une fuite devrait
traverser deux protections indépendantes.

## Authentification

- **Développeur → Business Core** :
  - **JWT Bearer** (login kernel délégué) : identité utilisateur et délégation vers le kernel.
  - **Clé BC** (`X-BC-Client-Id` / `X-BC-Api-Key`) : identifie la clé API du développeur sur les routes
    d'intégration (hachée BCrypt, révocation, suivi d'usage). Complète le JWT ; ne le remplace pas pour
    les appels kernel.
- **Business Core → kernel** : credentials plateforme (`KERNEL_CLIENT_ID`/`SECRET`) + Bearer utilisateur
  re-transmis + `X-Tenant-Id`. Voir ADR-003.

## Autorisation et identité de l'acteur

Authentifié ≠ autorisé. L'identité de l'opérateur agissant est **assertée** par le backend du
développeur via `X-BC-On-Behalf-Of` (modèle de confiance ; le Business Core n'authentifie pas les
utilisateurs finaux). `AuthorizationService` vérifie le rôle métier requis avant une action sensible ;
l'effet `DEROGER` est limité aux rôles autorisés. Évolution documentée : RFC 8693 (OAuth Token
Exchange, claims `sub`/`act`) si une validation cryptographique de l'acteur devient nécessaire.

## Autres volets

- **Validation des entrées** : Bean Validation (`@Valid`) sur tous les DTO → 400 RFC 7807.
- **Secrets** : clés via variables d'environnement / vault, jamais en dur ni en base claire ; jamais
  de secret/JWT dans les logs. Cible prod : Spring Cloud Vault.
- **Transport / en-têtes / CORS** : HTTPS en prod ; en-têtes de sécurité et CORS stricts centralisés.
- **Rate limiting** : quotas natifs du kernel par ClientApplication (primaire) ; garde-fou token bucket
  Redis par tenant (secondaire, post-MVP).
- **Traçabilité** : échecs d'auth/autorisation et usages sensibles journalisés via `JournaliserAudit`.

## Erreurs (RFC 7807)

Toute erreur ressort en `application/problem+json`. `ProblemException` porte les extensions métier
`violatedRule`, `requiredAction`, `requiredDocument` (sémantique des effets de règle). Les 401/403 hors
controller sont aussi formatés en problem+json.

## Checklist sécurité par PR

- [ ] Mes tables portent `tenant_id` ; mes requêtes le filtrent (et la policy RLS est posée).
- [ ] Mes use cases lisent le tenant du `BusinessContext`, pas du payload.
- [ ] Je revérifie tout `id` reçu comme appartenant au tenant courant.
- [ ] Mes DTO sont validés (`@Valid`).
- [ ] Aucun secret ni JWT dans les logs.
- [ ] Les actions sensibles vérifient le rôle métier et tracent les refus.
