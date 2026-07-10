package com.pharmacore.pharmaciebackend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BcaasProperties.class)
public class PharmacoreConfig {

    private final BcaasProperties properties;

    public PharmacoreConfig(BcaasProperties properties) {
        this.properties = properties;
    }

    /**
     * Échoue au démarrage (message explicite) plutôt que de démarrer avec une config BCaaS
     * incomplète et échouer silencieusement, un par un, sur chaque création qui en dépend
     * (piège vécu : BCAAS_TYPE_ID oublié au lancement → 500 générique sur /api/medicaments,
     * aucune indication de la vraie cause).
     */
    @PostConstruct
    void validerConfiguration() {
        StringBuilder manquants = new StringBuilder();
        if (properties.clientId() == null || properties.clientId().isBlank()) manquants.append("BCAAS_CLIENT_ID ");
        if (properties.apiKey() == null || properties.apiKey().isBlank()) manquants.append("BCAAS_API_KEY ");
        if (properties.typeId() == null || properties.typeId().isBlank()) manquants.append("BCAAS_TYPE_ID ");
        if (!manquants.isEmpty()) {
            throw new IllegalStateException(
                    "Configuration BCaaS incomplète — variables manquantes : " + manquants
                            + "— voir .env.local.example. Sans BCAAS_TYPE_ID, toute création de médicament "
                            + "échoue avec un 500 générique sans indication claire de la cause.");
        }
    }

    /**
     * Client HTTP vers Business Core. Ajoute systématiquement les en-têtes d'authentification
     * par clé API (X-BC-Client-Id / X-BC-Api-Key) — jamais un JWT ici, ce backend est une machine,
     * pas un utilisateur (cf. GUIDE-PROJET-PHARMACORE.md §4.3).
     */
    @Bean
    public RestClient bcaasRestClient(BcaasProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-BC-Client-Id", properties.clientId())
                .defaultHeader("X-BC-Api-Key", properties.apiKey())
                .build();
    }
}
