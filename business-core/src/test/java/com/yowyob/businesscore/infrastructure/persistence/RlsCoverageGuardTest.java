package com.yowyob.businesscore.infrastructure.persistence;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou structurel de la défense en profondeur (Barrière 3).
 *
 * <p>Contrairement à {@link TenantIsolationRlsTest} qui prouve que le mécanisme RLS fonctionne sur les
 * tables du socle, ce test vérifie une <b>invariante de tout le schéma</b> : <i>toute</i> table possédant
 * une colonne {@code tenant_id} doit avoir la Row-Level Security ACTIVÉE, FORCÉE, et porter au moins une
 * policy. Il applique le master changelog complet (socle + futures features) puis interroge les
 * catalogues système PostgreSQL.
 *
 * <p>But : transformer la discipline « ne pas oublier le RLS » en filet automatique. Le jour où un
 * développeur de feature ajoute une table tenant sans bloc RLS, la CI échoue ici avec le nom exact de
 * la table fautive — la fuite est interceptée avant la fusion, pas en production.
 *
 * <p>Conventions reconnues :
 * <ul>
 *   <li>Une table est « tenant-owned » si elle a une colonne {@code tenant_id}.</li>
 *   <li>{@code developer_account} est exemptée : elle DÉFINIT le tenant, ne le porte pas.</li>
 *   <li>Les tables techniques de Liquibase ({@code databasechangelog*}) sont ignorées.</li>
 * </ul>
 */
@Testcontainers
class RlsCoverageGuardTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    /** Tables sans tenant_id par conception (ne doivent pas être signalées). */
    private static final List<String> EXEMPTES = List.of("developer_account");

    @BeforeAll
    static void setUp() throws Exception {
        // bc_owner applique les migrations (mêmes rôles qu'en production).
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
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "bc_owner", "bc_owner")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(c));
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    @Test
    void touteTableTenantALeRlsActiveForceEtUnePolicy() throws Exception {
        // Tables qui ont une colonne tenant_id.
        String tablesTenantSql =
                "SELECT c.relname, c.relrowsecurity, c.relforcerowsecurity "
                + "FROM pg_class c "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = 'public' AND c.relkind = 'r' "
                + "AND EXISTS (SELECT 1 FROM pg_attribute a "
                + "            WHERE a.attrelid = c.oid AND a.attname = 'tenant_id' AND NOT a.attisdropped)";

        // Tables portant au moins une policy RLS.
        String tablesAvecPolicySql = "SELECT DISTINCT tablename FROM pg_policies WHERE schemaname = 'public'";

        List<String> manquements = new ArrayList<>();

        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "bc_owner", "bc_owner");
             Statement s = c.createStatement()) {

            List<String> tablesAvecPolicy = new ArrayList<>();
            try (ResultSet rs = s.executeQuery(tablesAvecPolicySql)) {
                while (rs.next()) {
                    tablesAvecPolicy.add(rs.getString("tablename"));
                }
            }

            int tablesTenantVerifiees = 0;
            try (Statement s2 = c.createStatement();
                 ResultSet rs = s2.executeQuery(tablesTenantSql)) {
                while (rs.next()) {
                    String table = rs.getString("relname");
                    if (EXEMPTES.contains(table) || table.startsWith("databasechangelog")) {
                        continue;
                    }
                    tablesTenantVerifiees++;
                    boolean rlsActive = rs.getBoolean("relrowsecurity");
                    boolean rlsForce = rs.getBoolean("relforcerowsecurity");
                    boolean aUnePolicy = tablesAvecPolicy.contains(table);

                    if (!rlsActive) {
                        manquements.add(table + " : RLS non activé (ENABLE ROW LEVEL SECURITY manquant)");
                    }
                    if (!rlsForce) {
                        manquements.add(table + " : RLS non forcé (FORCE ROW LEVEL SECURITY manquant — "
                                + "le propriétaire contournerait l'isolation)");
                    }
                    if (!aUnePolicy) {
                        manquements.add(table + " : aucune policy RLS (CREATE POLICY manquant)");
                    }
                }
            }

            // Sanity : on doit avoir vérifié au moins les tables tenant du socle (type_metier, version_type).
            assertTrue(tablesTenantVerifiees >= 2,
                    "Le test devrait trouver au moins 2 tables tenant du socle ; trouvé " + tablesTenantVerifiees
                            + ". Le master changelog s'applique-t-il correctement ?");
        }

        assertTrue(manquements.isEmpty(),
                "Des tables tenant n'appliquent pas la défense en profondeur (Barrière 3). "
                        + "Chaque table avec tenant_id doit reproduire le bloc RLS du socle.\n  - "
                        + String.join("\n  - ", manquements));
    }
}
