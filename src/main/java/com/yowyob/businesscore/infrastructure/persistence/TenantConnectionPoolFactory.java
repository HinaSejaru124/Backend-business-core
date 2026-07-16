package com.yowyob.businesscore.infrastructure.persistence;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Fabrique du pool R2DBC tenant-aware (Barrière 3 de la défense en profondeur).
 *
 * <p>Chaque connexion empruntée au pool reçoit la variable de session PostgreSQL
 * {@code app.current_tenant} positionnée depuis le tenant courant du Reactor Context ; elle est
 * réinitialisée au retour de la connexion pour éviter tout bleed entre requêtes. Les policies RLS
 * comparent {@code tenant_id} à {@code current_setting('app.current_tenant', true)::uuid}.
 *
 * <p>Centralisé ici pour être utilisé à l'identique par la configuration runtime et par les tests
 * d'intégration : le mécanisme testé est exactement le mécanisme exécuté en production.
 */
public final class TenantConnectionPoolFactory {

    /** Pose / désactive la variable de session du tenant courant sur une connexion. */
    static final String SET_TENANT_SQL = "SELECT set_config('app.current_tenant', $1, false)";

    private TenantConnectionPoolFactory() {
    }

    public static ConnectionPool tenantAwarePool(ConnectionFactory base, int initialSize, int maxSize) {
        ConnectionPoolConfiguration config = ConnectionPoolConfiguration.builder(base)
                .initialSize(initialSize)
                .maxSize(maxSize)
                .maxIdleTime(Duration.ofMinutes(30))
                .postAllocate(connection ->
                        BusinessContextHolder.currentTenantId()
                                .flatMap(tenant -> Mono.from(applyTenant(connection, tenant))))
                .preRelease(connection -> applyTenant(connection, Optional.empty()))
                .build();
        return new ConnectionPool(config);
    }

    static Publisher<Void> applyTenant(Connection connection, Optional<UUID> tenant) {
        Statement statement = connection.createStatement(SET_TENANT_SQL);
        if (tenant.isPresent()) {
            statement.bind(0, tenant.get().toString());
        } else {
            statement.bindNull(0, String.class);
        }
        return Mono.from(statement.execute())
                .flatMapMany(result -> result.map((row, meta) -> row.get(0)))
                .then();
    }
}
