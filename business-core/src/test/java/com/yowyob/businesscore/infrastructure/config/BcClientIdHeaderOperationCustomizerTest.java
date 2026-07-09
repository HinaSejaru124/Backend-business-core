package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.adapter.in.rest.access.ApiKeyController;
import com.yowyob.businesscore.adapter.in.rest.access.CreerCleRequest;
import com.yowyob.businesscore.adapter.in.rest.access.DashboardController;
import com.yowyob.businesscore.adapter.in.rest.auth.AuthController;
import com.yowyob.businesscore.adapter.in.rest.auth.LoginRequest;
import com.yowyob.businesscore.adapter.in.rest.businesstype.BusinessTypeController;
import com.yowyob.businesscore.adapter.in.security.ApiKeyAuthenticationConverter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
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
    @DisplayName("POST /v1/api-keys : pas de headers BC (console dev JWT)")
    void apiKeys_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new ApiKeyController(null, null),
                ApiKeyController.class.getDeclaredMethod("creer", CreerCleRequest.class));

        assertThat(headerNames(operation)).doesNotContain(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
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
    @DisplayName("GET /v1/business-types : headers BC présents (API M2M)")
    void businessTypes_avecHeadersBc() throws Exception {
        Operation operation = customize(
                new BusinessTypeController(null, null, null),
                BusinessTypeController.class.getDeclaredMethod("lister"));

        assertThat(headerNames(operation)).containsExactly(
                ApiKeyAuthenticationConverter.HEADER_CLIENT_ID,
                ApiKeyAuthenticationConverter.HEADER_API_KEY);
    }

    @Test
    @DisplayName("POST /v1/auth/login : pas de headers BC (route publique)")
    void login_sansHeadersBc() throws Exception {
        Operation operation = customize(
                new AuthController(null),
                AuthController.class.getDeclaredMethod("login", LoginRequest.class));

        assertThat(headerNames(operation)).isEmpty();
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
                .map(Parameter::getName)
                .toList();
    }
}
