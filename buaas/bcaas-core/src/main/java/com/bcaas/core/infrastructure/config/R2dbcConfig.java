package com.bcaas.core.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Configuration R2DBC — Base de données réactive.
 * Couche 1 (Infrastructure) de la pile protocolaire BCaaS.
 */
@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories(basePackages = "com.bcaas.core.infrastructure.persistence")
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
