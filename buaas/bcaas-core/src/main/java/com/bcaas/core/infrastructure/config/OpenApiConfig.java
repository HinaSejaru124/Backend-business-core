package com.bcaas.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * Configuration OpenAPI / Swagger pour BCaaS Core.
 * Documente les contrats d'API — "protocoliser avant d'implémenter" (ADR-003).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bcaasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BCaaS — Business Core as a Service")
                        .description("""
                            Noyau métier générique inspiré de l'ingénierie des protocoles réseau.
                            
                            Pile protocolaire en 5 couches :
                            - Couche 5 : Business Capabilities (Acteurs, Ressources, Workflow)
                            - Couche 4 : Context & Policy (Identité, Permissions, SLA)
                            - Couche 3 : Tenant & Routing (Multi-tenant, Discovery)
                            - Couche 2 : Transport & Messaging (REST, Kafka)
                            - Couche 1 : Infrastructure (PostgreSQL, Redis, MinIO)
                            """)
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Équipe BCaaS")
                                .email("email_de_l'admin"))
                        .license(new License().name("Propriétaire")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Développement local"),
                        new Server().url("https://api.bcaas.io").description("Production")
                ));
    }
}
