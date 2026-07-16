package com.yowyob.businesscore.infrastructure.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Migrations Liquibase via une connexion JDBC dédiée (au démarrage uniquement), distincte du
 * runtime R2DBC. Liquibase s'exécute en tant que rôle propriétaire ({@code bc_owner}) pour créer
 * le schéma ; le runtime utilise le rôle applicatif non-owner ({@code bc_app}) soumis à RLS.
 *
 * <p>On fournit explicitement le {@link SpringLiquibase} (l'auto-configuration Spring Boot recule
 * en présence de ce bean), ce qui évite toute ambiguïté de résolution de DataSource.
 */
@Configuration
public class LiquibaseConfig {

    @Value("${spring.liquibase.url}")
    private String url;

    @Value("${spring.liquibase.user}")
    private String user;

    @Value("${spring.liquibase.password}")
    private String password;

    @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.xml}")
    private String changeLog;

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(liquibaseDataSource());
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }

    private DataSource liquibaseDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }
}
