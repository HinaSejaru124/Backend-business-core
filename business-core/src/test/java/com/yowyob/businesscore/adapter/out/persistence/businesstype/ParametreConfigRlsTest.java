package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.infrastructure.persistence.TenantConnectionPoolFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
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
 * Isolation RLS de {@code parametre_config} (Barrière 3) — miroir du spike socle
 * {@code TenantIsolationRlsTest}, appliqué à la table de la brique Configuration.
 *
 * <p>Les insertions sont faites au niveau ENTREPRISE ({@code entreprise_id} sans contrainte FK),
 * ce qui évite de monter une chaîne {@code type_metier → version_type}. Le mécanisme testé
 * ({@link TenantConnectionPoolFactory} + RLS) est exactement celui exécuté en production.
 *
 * <p>Nécessite Docker (Testcontainers) ; s'exécute en CI, pas forcément en local.
 */
@Testcontainers
class ParametreConfigRlsTest {

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

    private Mono<Long> insertParam(UUID contextTenant, UUID rowTenant, String cle) {
        return appClient.sql(
                        "INSERT INTO parametre_config (tenant_id, entreprise_id, cle, valeur) "
                                + "VALUES (:t, :e, :c, :v)")
                .bind("t", rowTenant)
                .bind("e", UUID.randomUUID())   // niveau ENTREPRISE → pas de FK à satisfaire
                .bind("c", cle)
                .bind("v", "XAF")
                .fetch().rowsUpdated()
                .contextWrite(ctx -> ctx.putAll(tenantContext(contextTenant).readOnly()));
    }

    private Mono<Long> countParams(UUID contextTenant) {
        return appClient.sql("SELECT count(*) AS c FROM parametre_config")
                .map(row -> row.get("c", Long.class))
                .one()
                .contextWrite(ctx -> ctx.putAll(tenantContext(contextTenant).readOnly()));
    }

    @Test
    void tenant_ne_voit_que_ses_parametres_et_refuse_le_cross_tenant() {
        StepVerifier.create(insertParam(TENANT_A, TENANT_A, "devise"))
                .expectNext(1L).verifyComplete();
        StepVerifier.create(insertParam(TENANT_B, TENANT_B, "devise"))
                .expectNext(1L).verifyComplete();

        StepVerifier.create(countParams(TENANT_A)).expectNext(1L).verifyComplete();
        StepVerifier.create(countParams(TENANT_B)).expectNext(1L).verifyComplete();

        // Tenant A tente d'écrire une ligne marquée tenant B : rejetée par WITH CHECK.
        StepVerifier.create(insertParam(TENANT_A, TENANT_B, "usurpation"))
                .expectError().verify();
    }

    @Test
    void sans_contexte_tenant_aucune_ligne_visible() {
        // current_setting('app.current_tenant') NULL → aucune ligne (fail closed).
        StepVerifier.create(appClient.sql("SELECT count(*) AS c FROM parametre_config")
                        .map(row -> row.get("c", Long.class))
                        .one())
                .expectNext(0L)
                .verifyComplete();
    }
}
