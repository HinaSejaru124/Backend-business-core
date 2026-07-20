package com.yowyob.businesscore.adapter.in.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;

/**
 * Sécurité réactive du Business Core (Barrière 1 + en-têtes + CORS).
 *
 * <p>Deux voies d'authentification, pour deux acteurs différents :
 * <ul>
 *   <li>JWT (Bearer) — voie principale, couvre <b>toutes</b> les routes protégées : gestion de la
 *       plateforme (types métier, applications, clés API, dashboard...). Consommée par le développeur
 *       humain via le front Business Core.</li>
 *   <li>Clé Business Core (en-têtes {@code X-BC-*}) — repli scopé aux routes d'usage <b>runtime</b>
 *       d'une application, consommées par le backend terminal qui la représente (ex. PharmAPI) :
 *       synchronisation ({@code /v1/sync}), exécution/consultation d'opérations, traces, transactions.
 *       Jamais la création/gestion d'applications, de clés ou de types métier — ça, c'est le développeur
 *       qui le fait, via JWT.</li>
 * </ul>
 * En succès, {@link BusinessContextWebFilter} propage le BusinessContext dans le Reactor Context.
 * Routes publiques : santé, inscription, documentation OpenAPI. Tout le reste exige une authentification.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    // Remplace uniquement le tableau ROUTES_PUBLIQUES
private static final String[] ROUTES_PUBLIQUES = {
        "/health",
        "/actuator/health",
        "/v1/registration",
        "/v1/auth/login",
        "/v1/auth/discover",
        "/v1/auth/select",
        "/v1/plans",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**"
};

    /**
     * Surface consommée par un backend terminal (usage runtime d'une application) : synchronisation,
     * opérations, traces, transactions. Tenue en phase avec {@link com.yowyob.businesscore.infrastructure.config.AuthRouteClassifier}
     * (qui documente cette même surface dans Swagger).
     */
    private static final String[] ROUTES_INTEGRATION_TERMINAL = {
            "/v1/sync", "/v1/sync/**",
            "/v1/applications/me",
            "/v1/applications/*/actors:login", "/v1/applications/*/actors:register",
            "/v1/applications/*/operations", "/v1/applications/*/operations/**",
            "/v1/applications/*/traces", "/v1/applications/*/traces/**",
            "/v1/applications/*/transactions", "/v1/applications/*/transactions/**",
            "/v1/applications/*/orders/**",
            // Ingestion de télémétrie (requêtes propres de l'app) — clé API. Exclue du comptage facturable
            // dans UsageTrackingWebFilter : reporter sa télémétrie ne consomme pas le quota.
            "/v1/telemetry/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ApiKeyAuthenticationConverter converter,
            ApiKeyReactiveAuthenticationManager authenticationManager,
            ReactiveJwtDecoder jwtDecoder,
            BusinessContextJwtConverter jwtConverter,
            CorsConfigurationSource corsConfigurationSource,
            ProblemAuthenticationEntryPoint authenticationEntryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler,
            com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur usageCompteur,
            com.yowyob.businesscore.application.billing.QuotaService quotaService,
            com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter requeteLogWriter,
            com.yowyob.businesscore.adapter.in.rest.error.ProblemResponseWriter problemResponseWriter) {

        // Authentification par clé Business Core (X-BC-*) — réservée aux routes d'usage runtime d'une
        // application (cf. ROUTES_INTEGRATION_TERMINAL). Tout le reste (gestion des types métier, des
        // applications, des clés, du dashboard...) est exclusivement JWT : ces routes appellent le kernel
        // au nom d'un utilisateur et sont consommées par le front Business Core, jamais par un terminal.
        AuthenticationWebFilter apiKeyFilter = new AuthenticationWebFilter(authenticationManager);
        apiKeyFilter.setServerAuthenticationConverter(converter);
        apiKeyFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(ROUTES_INTEGRATION_TERMINAL));

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(ROUTES_PUBLIQUES).permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Voie principale : Resource Server JWT standard Spring (Authorization: Bearer).
                // Decoder réactif (signature RS256 + exp + iss) + JWKS et rotation des clés gérés par Spring ;
                // le converter projette le JWT en BusinessContext.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(jwtConverter)))
                // Repli : clé Business Core quand il n'y a pas de Bearer (converter vide -> filtre suivant).
                .addFilterAfter(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                // Propagation du contexte vers le Reactor Context (lisible par use cases + KernelClient).
                .addFilterAfter(new BusinessContextWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                // Porte de quota (402 quand le quota mensuel du plan est atteint) — AVANT le comptage,
                // pour qu'une requête bloquée ne soit pas comptabilisée. Sans effet sur le flux JWT.
                .addFilterAfter(new QuotaEnforcementWebFilter(quotaService, problemResponseWriter),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                // Comptabilise l'usage par clé API (dashboard développeur + compteur mensuel de quota).
                .addFilterAfter(new UsageTrackingWebFilter(usageCompteur, quotaService, requeteLogWriter),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                // Journalise le flux JWT (design-time) dans le même onglet Audit / Requêtes, marqué
                // non-facturable — cf. DesignTimeAuditWebFilter.
                .addFilterAfter(new DesignTimeAuditWebFilter(requeteLogWriter),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Decoder JWT réactif standard (Spring Security Resource Server) : récupère les clés publiques du
     * kernel via JWKS (cache + rotation automatiques) et vérifie signature RS256 + {@code exp}
     * (+ {@code iss} si configuré). S'appuie sur nimbus en interne.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(KernelProperties kernelProperties, AuthProperties authProperties) {
        String jwksUri = authProperties.jwksUriEffective(kernelProperties.baseUrl());
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> validateur = authProperties.verifieEmetteur()
                ? JwtValidators.createDefaultWithIssuer(authProperties.issuer())
                : JwtValidators.createDefault();
        decoder.setJwtValidator(validateur);
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${businesscore.security.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(origin -> origin.trim()).toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
