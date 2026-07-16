package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.infrastructure.persistence.TenantConnectionPoolFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connexion R2DBC runtime tenant-aware (Barrière 3).
 *
 * <p>Construit la {@link ConnectionFactory} du runtime à partir de {@code spring.r2dbc.*} et la
 * décore via {@link TenantConnectionPoolFactory} pour appliquer l'isolation RLS par tenant.
 * En définissant ce bean, l'auto-configuration R2DBC de Spring Boot recule.
 */
@Configuration
public class R2dbcConfig {

    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username:}")
    private String username;

    @Value("${spring.r2dbc.password:}")
    private String password;

    @Value("${spring.r2dbc.pool.initial-size:5}")
    private int initialSize;

    @Value("${spring.r2dbc.pool.max-size:20}")
    private int maxSize;

    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions.Builder options = ConnectionFactoryOptions.parse(url).mutate();
        if (!username.isBlank()) {
            options.option(ConnectionFactoryOptions.USER, username);
        }
        if (!password.isBlank()) {
            options.option(ConnectionFactoryOptions.PASSWORD, password);
        }
        ConnectionFactory base = ConnectionFactories.get(options.build());
        return TenantConnectionPoolFactory.tenantAwarePool(base, initialSize, maxSize);
    }
}
