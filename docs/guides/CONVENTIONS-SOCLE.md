# Conventions du socle réel — à lire avant toute feature

> Ce fichier décrit les conventions **réelles** du socle déjà livré (`com.yowyob.businesscore`, Spring Boot 4.0.7 / Java 21). Tout dev feature les suit à l'identique. À donner en contexte à ton IA avec ton fichier `feat-*.md`.

## Le socle est déjà là — ce qui existe

- **Build** : Spring Boot **4.0.7**, Java **21**, Maven. WebFlux + R2DBC + Redis réactif + Kafka + Liquibase + Resilience4j + Testcontainers + WireMock.
- **Package base** : `com.yowyob.businesscore`.
- **Sécurité** : `BusinessContextWebFilter` (barrière 1), `TenantGuard` (barrière 2), RLS PostgreSQL (barrière 3) déjà en place. `SecretCipher` (AES-256-GCM) pour les secrets kernel.
- **Kernel** : `KernelClient` prêt. Il pose **automatiquement** `X-Client-Id` + `X-Api-Key` sur chaque appel, et
  `Authorization: Bearer` **quand une requête en porte un à déléguer** (JWT de l'utilisateur courant). Sans
  JWT à déléguer (ex. appel authentifié par clé `X-BC-*`, sans utilisateur), le client passe en app-only —
  `X-Client-Id`/`X-Api-Key` seuls, pas de Bearer (le contrat OpenAPI du kernel l'accepte pour les endpoints
  concernés). Pour une opération liée à une organisation, utilise `getForOrganization/postForOrganization`
  qui ajoutent `X-Organization-Id`.
- **Contexte** : `BusinessContext` (record) avec `tenantId`, `actorId`, `roles`, `businessId`, `traceId`, `locale`. Récupéré via `BusinessContextHolder`.
- **Erreurs** : `ProblemException` avec factory statiques — `notFound`, `conflict`, `unprocessable`, `forbidden`, `badRequest`, `badGateway` (échec d'un appel kernel). Le format RFC 7807 est géré par `GlobalProblemHandler`.
- **Ports** : tous définis dans `domain/port/out`, `domain/port/internal`, `domain/port/in`. Tu les **implémentes**, tu ne les modifies pas.

## Convention d'entité R2DBC (reproduis ce patron)

Le socle utilise `Persistable<UUID>` pour forcer l'INSERT avec un ID généré côté application, et une factory statique `nouveau(...)`. Exemple réel (`TypeMetierEntity`) :

```java
@Table("type_metier")
public class TypeMetierEntity implements Persistable<UUID> {
    @Id private UUID id;
    @Column("tenant_id") private UUID tenantId;       // OBLIGATOIRE pour toute table tenant
    @Column("business_domain_id") private UUID businessDomainId;
    private String code;
    private String nom;
    private String statut;
    @Column("created_at") private Instant createdAt;
    @Transient private boolean nouveau;

    public static TypeMetierEntity nouveau(UUID id, UUID tenantId, /* ... */) {
        TypeMetierEntity e = new TypeMetierEntity();
        e.id = id; e.tenantId = tenantId; /* ... */ e.nouveau = true;
        return e;
    }
    @Override public UUID getId() { return id; }
    @Override public boolean isNew() { return nouveau; }
    // getters / setters
}
```

## Convention de repository

```java
public interface TypeMetierRepository extends ReactiveCrudRepository<TypeMetierEntity, UUID> {
    Mono<TypeMetierEntity> findByCode(String code);   // le filtrage tenant est garanti par RLS, pas par un WHERE
}
```

> **Ne mets jamais `WHERE tenant_id = ?` à la main.** Le RLS s'en charge. Écris tes finders comme si une seule entreprise existait ; la base filtre.

## Convention de controller

```java
@RestController
@RequestMapping("/v1")
public class MonController {
    @PostMapping("/business-types")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MaReponse> creer(@Valid @RequestBody MaRequete req) { ... }
}
```

## RLS OBLIGATOIRE — pour CHAQUE table tenant (critique)

Toute table que tu crées et qui contient des données d'un tenant **doit** :
1. avoir une colonne `tenant_id uuid NOT NULL`,
2. reproduire le bloc RLS du socle dans ton changelog.

Patron exact (copié du socle, remplace `ma_table`) :

```sql
ALTER TABLE ma_table ENABLE ROW LEVEL SECURITY;
ALTER TABLE ma_table FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ma_table
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
```

> ⚠️ **Un test garde-fou (`RlsCoverageGuardTest`) échouera en CI** si tu crées une table avec `tenant_id` sans ce bloc. Ce n'est pas optionnel : la CI bloque la fusion. C'est voulu — c'est ce qui garantit l'isolation.

## Convention de migration Liquibase

- Crée **ton** fichier : `src/main/resources/db/changelog/features/<ta-feature>.xml`.
- Le master changelog l'inclut déjà. **Ne modifie jamais** `_socle.xml` ni le master.
- Préfixe tes IDs de changeset : `<feature>-001`, `<feature>-002`…
- Inclus le bloc RLS ci-dessus dans le même changeset que la création de table (ou juste après).

## Autorisation (rôles métier)

Avant une action sensible, vérifie le rôle via le contexte :
```java
businessContext.hasRole("pharmacien-responsable")
```
Pour l'effet `DEROGER` (Règles), seuls les rôles listés peuvent outrepasser.

## Definition of Done (rappel, + sécurité)
- [ ] Mes tables tenant ont `tenant_id` + le bloc RLS (le garde-fou passe).
- [ ] Mes requêtes ne filtrent pas le tenant à la main (RLS le fait).
- [ ] Mes use cases lisent le tenant du `BusinessContext`, jamais du payload.
- [ ] Mes DTO sont validés (`@Valid`).
- [ ] Mes adapters kernel utilisent `KernelClient` (pas de WebClient brut).
- [ ] Tests : use cases (unitaires) + adapter kernel (WireMock) + RLS de mes tables.
- [ ] Code réactif (Mono/Flux), pas de `.block()`.
