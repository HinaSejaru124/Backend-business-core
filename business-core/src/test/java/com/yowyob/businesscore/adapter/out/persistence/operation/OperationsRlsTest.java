package com.yowyob.businesscore.adapter.out.persistence.operation;

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
import org.junit.jupiter.api.DisplayName;
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
 * Garde-fou RLS (Barrière 3) pour les tables de la feature operations. Vérifie que
 * {@code definition_operation} isole réellement les tenants (un tenant ne voit que ses lignes) et
 * refuse un INSERT cross-tenant (WITH CHECK), avec le mécanisme exact exécuté en production
 * ({@link TenantConnectionPoolFactory}).
 */
@Testcontainers
class OperationsRlsTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();

    // Chaîne FK du tenant A : type_metier → version_type → definition_operation
    static final UUID TYPE_A = UUID.randomUUID();
    static final UUID VERSION_A = UUID.randomUUID();
    static final UUID OP_A = UUID.randomUUID();

    static DatabaseClient appClient;

    @BeforeAll
    static void setUp() throws Exception {
        createRoles();
        runLiquibaseAsOwner();
        appClient = DatabaseClient.create(buildAppConnectionFactory());
    }

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

    /** Insère la chaîne type → version → opération pour le tenant donné, sous son contexte. */
    private Mono<Long> insererChaine(UUID tenant, UUID typeId, UUID versionId, UUID opId, String nom) {
        return appClient.sql("INSERT INTO type_metier (id, tenant_id, code, nom) VALUES (:id, :t, :c, :n)")
                .bind("id", typeId).bind("t", tenant).bind("c", "C-" + nom).bind("n", "Type " + nom)
                .fetch().rowsUpdated()
                .then(appClient.sql("INSERT INTO version_type (id, tenant_id, type_metier_id, numero) "
                                + "VALUES (:id, :t, :tm, 1)")
                        .bind("id", versionId).bind("t", tenant).bind("tm", typeId)
                        .fetch().rowsUpdated())
                .then(appClient.sql("INSERT INTO definition_operation "
                                + "(id, tenant_id, version_type_id, nom, declencheur_regles) "
                                + "VALUES (:id, :t, :vt, :nom, 'AVANT_VENTE')")
                        .bind("id", opId).bind("t", tenant).bind("vt", versionId).bind("nom", nom)
                        .fetch().rowsUpdated())
                .contextWrite(ctx -> ctx.putAll(tenantContext(tenant).readOnly()));
    }

    private Mono<Long> compterOperations(UUID tenant) {
        return appClient.sql("SELECT count(*) AS c FROM definition_operation")
                .map(row -> row.get("c", Long.class))
                .one()
                .contextWrite(ctx -> ctx.putAll(tenantContext(tenant).readOnly()));
    }

    @Test
    @DisplayName("un tenant ne voit que ses opérations et un INSERT cross-tenant est rejeté")
    void isolation_et_rejet_cross_tenant() {
        StepVerifier.create(insererChaine(TENANT_A, TYPE_A, VERSION_A, OP_A, "vente"))
                .expectNext(1L)
                .verifyComplete();

        // Tenant A voit son opération ; tenant B n'en voit aucune.
        StepVerifier.create(compterOperations(TENANT_A)).expectNext(1L).verifyComplete();
        StepVerifier.create(compterOperations(TENANT_B)).expectNext(0L).verifyComplete();

        // Tenant A tente d'insérer une opération marquée pour B (FK A valide) : rejet par WITH CHECK.
        Mono<Long> usurpation = appClient.sql("INSERT INTO definition_operation "
                        + "(id, tenant_id, version_type_id, nom, declencheur_regles) "
                        + "VALUES (:id, :t, :vt, :nom, 'AVANT_VENTE')")
                .bind("id", UUID.randomUUID()).bind("t", TENANT_B).bind("vt", VERSION_A).bind("nom", "usurpation")
                .fetch().rowsUpdated()
                .contextWrite(ctx -> ctx.putAll(tenantContext(TENANT_A).readOnly()));

        StepVerifier.create(usurpation).expectError().verify();
    }
}
