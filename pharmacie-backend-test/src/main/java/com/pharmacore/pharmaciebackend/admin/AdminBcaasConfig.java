package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clients HTTP de l'espace <b>admin</b> (design-time) vers Business Core — distincts du client de la
 * caisse ({@code bcaasRestClient}, qui porte la clé API). Ici, pas de clé API : l'authentification se
 * fait par <b>JWT</b> (celui du titulaire, cf. {@link PharmacoreSession}).
 *
 * <ul>
 *   <li>{@code bcaasAnonRestClient} : sans authentification — sert uniquement au login
 *       ({@code POST /v1/auth/login} est public).</li>
 *   <li>{@code bcaasAdminRestClient} : ajoute automatiquement {@code Authorization: Bearer <jwt>} à
 *       chaque appel, en lisant le JWT courant de la session titulaire. Un appel émis sans session
 *       active part sans Bearer et sera rejeté (401) par Business Core — comportement voulu.</li>
 * </ul>
 */
@Configuration
public class AdminBcaasConfig {

    @Bean
    public RestClient bcaasAnonRestClient(BcaasProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient bcaasAdminRestClient(BcaasProperties properties, PharmacoreSession session) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    String jwt = session.jwtOuNull();
                    if (jwt != null) {
                        request.getHeaders().setBearerAuth(jwt);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
