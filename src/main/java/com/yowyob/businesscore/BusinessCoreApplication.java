package com.yowyob.businesscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Le runtime est entièrement réactif (R2DBC). L'unique usage JDBC est Liquibase, configuré
 * explicitement dans {@code LiquibaseConfig} via {@code spring-jdbc} ; le module d'auto-configuration
 * JDBC de Spring Boot n'est pas sur le classpath, donc aucune DataSource runtime n'est tentée.
 *
 * <p>{@code @EnableScheduling} : flush périodique des compteurs d'usage Redis vers la base (dashboard).
 */
@SpringBootApplication
@EnableScheduling
public class BusinessCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessCoreApplication.class, args);
    }
}
