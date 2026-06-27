package com.yowyob.businesscore.infrastructure.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient réactif configuré vers le kernel.
 *
 * <p>En Spring Boot 4, le module WebFlux n'expose plus systématiquement un bean
 * {@code WebClient.Builder} auto-configuré. On s'appuie donc sur celui auto-configuré s'il est
 * présent (afin de bénéficier des customizations : codecs, observabilité), avec repli sur
 * {@code WebClient.builder()} sinon — ce qui garantit le démarrage sans dépendance optionnelle.
 */
@Configuration
@EnableConfigurationProperties({KernelProperties.class, AuthProperties.class})
public class KernelWebClientConfig {

    @Bean("kernelWebClient")
    public WebClient kernelWebClient(KernelProperties properties,
                                     ObjectProvider<WebClient.Builder> builderProvider) {
        WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
