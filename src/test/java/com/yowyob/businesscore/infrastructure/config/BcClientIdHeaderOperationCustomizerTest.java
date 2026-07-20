package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.adapter.in.rest.access.DashboardController;
import com.yowyob.businesscore.adapter.in.rest.actor.ActeurAuthController;
import com.yowyob.businesscore.adapter.in.rest.actor.InscrireActeurRequete;
import com.yowyob.businesscore.adapter.in.rest.auth.AuthController;
import com.yowyob.businesscore.adapter.in.rest.auth.LoginRequest;
import com.yowyob.businesscore.adapter.in.rest.businesstype.BusinessTypeController;
import com.yowyob.businesscore.adapter.in.rest.enterprise.CreerEntrepriseRequest;
import com.yowyob.businesscore.adapter.in.rest.enterprise.EntrepriseController;
import com.yowyob.businesscore.adapter.in.rest.sync.SyncController;
import com.yowyob.businesscore.adapter.in.security.ApiKeyAuthenticationConverter;
import java.util.UUID;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BcClientIdHeaderOperationCustomizerTest {

    private BcClientIdHeaderOperationCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new BcClientIdHeaderOperationCustomizer();
    }

    @Test
    @DisplayName("GET /v1/dashboard : pas de headers BC (console dev JWT)")
    void dashboard_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new DashboardController(null, null),
                DashboardController.class.getDeclaredMethod("tableau"));

        assertThat(headerNames(operation)).doesNotContain(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
    }

    @Test
    @DisplayName("POST /v1/applications : pas de headers BC (gestion réservée au JWT)")
    void applications_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new EntrepriseController(null),
                EntrepriseController.class.getDeclaredMethod("creer", CreerEntrepriseRequest.class));

        assertThat(headerNames(operation)).doesNotContain(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
    }

    @Test
    @DisplayName("GET /v1/business-types : pas de headers BC (gestion réservée au JWT)")
    void businessTypes_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new BusinessTypeController(null, null, null),
                BusinessTypeController.class.getDeclaredMethod("lister"));

        assertThat(headerNames(operation)).doesNotContain(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
    }

    @Test
    @DisplayName("GET /v1/sync : Bearer + headers BC documentés (seule route consommée par un terminal)")
    void sync_integration() throws Exception {
        Operation operation = customize(
                new SyncController(null),
                SyncController.class.getDeclaredMethod("consulter", long.class, Integer.class));

        assertThat(headerNames(operation)).contains(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY,
                ApiKeyAuthenticationConverter.HEADER_ON_BEHALF_OF);
        assertThat(operation.getSecurity()).anyMatch(req -> req.containsKey("bearerAuth"));
    }

    @Test
    @DisplayName("POST /v1/auth/login : pas de headers BC (route publique)")
    void login_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new AuthController(null, null, null),
                AuthController.class.getDeclaredMethod("login", LoginRequest.class));

        assertThat(headerNames(operation)).isEmpty();
    }

    @Test
    @DisplayName("POST .../actors:login : headers BC requis, aucun cadenas bearerAuth (Bearer refusé)")
    void actorsLogin_clientIdRequisSansBearer() throws Exception {
        Operation operation = customize(
                new ActeurAuthController(null, null),
                ActeurAuthController.class.getDeclaredMethod("login", UUID.class,
                        com.yowyob.businesscore.adapter.in.rest.auth.LoginRequest.class));

        assertThat(headerNames(operation)).contains(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
        assertThat(hasBearerSecurity(operation)).isFalse();
        assertThat(operation.getParameters().stream()
                .filter(p -> ApiKeyAuthenticationConverter.HEADER_CLIENT_ID.equals(p.getName()))
                .findFirst().orElseThrow().getRequired()).isTrue();
    }

    @Test
    @DisplayName("POST .../actors:register : headers BC requis, aucun cadenas bearerAuth (Bearer refusé)")
    void actorsRegister_clientIdRequisSansBearer() throws Exception {
        Operation operation = customize(
                new ActeurAuthController(null, null),
                ActeurAuthController.class.getDeclaredMethod("register", UUID.class, InscrireActeurRequete.class));

        assertThat(headerNames(operation)).contains(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
        assertThat(hasBearerSecurity(operation)).isFalse();
    }

    private boolean hasBearerSecurity(Operation operation) {
        return operation.getSecurity() != null
                && operation.getSecurity().stream().anyMatch(req -> req.containsKey("bearerAuth"));
    }

    private Operation customize(Object controller, Method method) {
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);
        return customizer.customize(new Operation(), handlerMethod);
    }

    private List<String> headerNames(Operation operation) {
        if (operation.getParameters() == null) {
            return List.of();
        }
        return operation.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()))
                .map(p -> p.getName())
                .toList();
    }
}
