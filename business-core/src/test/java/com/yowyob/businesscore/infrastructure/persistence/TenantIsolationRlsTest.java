package com.yowyob.businesscore.infrastructure.persistence;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactories;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

/**
 * Spike critique de la défense en profondeur (Barrière 3).
 *
 * Valide que le mécanisme RLS + pool R2DBC tenant-aware isole réellement les tenants :
 * un tenant ne voit jamais les lignes d'un autre, et un INSERT cross-tenant est rejeté par WITH CHECK,
 * y compris si la requête applicative ne filtre pas explicitement par tenant_id (la base refuse).
 *
 * Le mécanisme testé ({@link TenantConnectionPoolFactory}) est exactement celui exécuté en production.
 */
@Testcontainers
class TenantIsolationRlsTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();

    static DatabaseClient appClient;

    @BeforeAll
    static void setUp() throws Exception {
        createRoles();
        runLiquibaseAsOwner();
        appClient = DatabaseClient.create(buildAppConnectionFactory());
    }

    /** Crée les rôles bc_owner (migrations) et bc_app (runtime non-owner soumis à RLS). */
    private static void createRoles() throws Exception {
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {
            s.execute("CREATE ROLE bc_owner LOGIN PASSWORD 'bc_owner'");
            s.execute("CREATE ROLE bc_app LOGIN PASSWORD 'bc_app'");
            s.execute("GRANT CREATE, USAGE ON SCHEMA public TO bc_owner");
            s.execute("GRANT USAGE ON SCHEMA public TO bc_app");
            s.execute("ALTER DEFAULT PRIVILEGES FOR ROLE bc_owner IN SCHEMA public "
                    + "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bc_app");
        }
    }

    /** Applique le master changelog (tables + RLS) en tant que propriétaire. */
    private static void runLiquibaseAsOwner() throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "bc_owner", "bc_owner")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(c));
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** Pool R2DBC tenant-aware connecté en bc_app (non-owner). */
    private static ConnectionFactory buildAppConnectionFactory() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, POSTGRES.getHost())
                .option(PORT, POSTGRES.getFirstMappedPort())
                .option(DATABASE, POSTGRES.getDatabaseName())
                .option(USER, "bc_app")
                .option(PASSWORD, "bc_app")
                .build();
        ConnectionFactory base = ConnectionFactories.get(options);
        return TenantConnectionPoolFactory.tenantAwarePool(base, 1, 4);
    }

    private static Context tenantContext(UUID tenantId) {
        BusinessContext ctx = new BusinessContext(
                tenantId, null, Set.of(), null, "trace-" + tenantId, Locale.FRENCH);
        return BusinessContextHolder.withContext(Context.empty(), ctx);
    }

    private Mono<Long> insertType(UUID contextTenant, UUID rowTenant, String code) {
        return appClient.sql("INSERT INTO type_metier (tenant_id, code, nom) VALUES (:t, :c, :n)")
                .bind("t", rowTenant)
                .bind("c", code)
                .bind("n", "Type " + code)
                .fetch().rowsUpdated()
                .contextWrite(ctx -> ctx.putAll(tenantContext(contextTenant).readOnly()));
    }

    private Mono<Long> countTypes(UUID contextTenant) {
        return appClient.sql("SELECT count(*) AS c FROM type_metier")
                .map(row -> row.get("c", Long.class))
                .one()
                .contextWrite(ctx -> ctx.putAll(tenantContext(contextTenant).readOnly()));
    }

    @Test
    void tenant_ne_voit_que_ses_propres_lignes_et_refuse_le_cross_tenant() {
        // Tenant A insère une ligne pour lui-même.
        StepVerifier.create(insertType(TENANT_A, TENANT_A, "PHARMA_A"))
                .expectNext(1L)
                .verifyComplete();

        // Tenant B insère une ligne pour lui-même.
        StepVerifier.create(insertType(TENANT_B, TENANT_B, "PHARMA_B"))
                .expectNext(1L)
                .verifyComplete();

        // Tenant A ne voit QUE sa ligne (la base filtre via RLS, sans WHERE explicite).
        StepVerifier.create(countTypes(TENANT_A))
                .expectNext(1L)
                .verifyComplete();

        // Tenant B ne voit QUE sa ligne.
        StepVerifier.create(countTypes(TENANT_B))
                .expectNext(1L)
                .verifyComplete();

        // Tenant A tente d'insérer une ligne marquée pour le tenant B : rejeté par WITH CHECK.
        StepVerifier.create(insertType(TENANT_A, TENANT_B, "USURPATION"))
                .expectError()
                .verify();
    }

    @Test
    void sans_contexte_tenant_aucune_ligne_visible() {
        // Sans tenant dans le contexte, current_setting est NULL -> aucune ligne (fail closed).
        StepVerifier.create(appClient.sql("SELECT count(*) AS c FROM type_metier")
                        .map(row -> row.get("c", Long.class))
                        .one())
                .expectNext(0L)
                .verifyComplete();
    }
}
