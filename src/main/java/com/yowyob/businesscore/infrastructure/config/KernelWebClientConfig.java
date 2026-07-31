package com.yowyob.businesscore.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * WebClient réactif configuré vers le kernel.
 *
 * <p>En Spring Boot 4, le module WebFlux n'expose plus systématiquement un bean
 * {@code WebClient.Builder} auto-configuré. On s'appuie donc sur celui auto-configuré s'il est
 * présent (afin de bénéficier des customizations : codecs, observabilité), avec repli sur
 * {@code WebClient.builder()} sinon — ce qui garantit le démarrage sans dépendance optionnelle.
 *
 * <p><b>maxIdleTime</b> : sans cette borne, une connexion du pool peut rester ouverte des heures
 * entre deux appels (le kernel n'est sollicité que par intermittence — login, etc.). Un proxy/LB
 * intermédiaire referme alors la connexion côté serveur sans que Netty le détecte immédiatement ;
 * l'appel suivant réutilise cette connexion « zombie » et bloque jusqu'au timeout complet avant
 * d'échouer (symptôme observé le 30/07/2026 : le kernel répond en ~1,3s en direct, mais via ce
 * client applicatif l'appel prenait ~15s avant 503 — intermittent selon l'âge de la connexion
 * piochée dans le pool). En évinçant toute connexion inactive depuis plus de 20s, le pool ne garde
 * jamais de connexion assez vieille pour être fermée à notre insu.
 */
@Configuration
@EnableConfigurationProperties({KernelProperties.class, AuthProperties.class})
public class KernelWebClientConfig {

    @Bean("kernelWebClient")
    public WebClient kernelWebClient(KernelProperties properties,
                                     ObjectProvider<WebClient.Builder> builderProvider) {
        ConnectionProvider provider = ConnectionProvider.builder("kernel-http")
                .maxIdleTime(Duration.ofSeconds(20))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(provider);

        WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);
        return builder.baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
