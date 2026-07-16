package com.yowyob.businesscore.adapter.in.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class ApiKeyAuthenticationConverterTest {

    private final ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter();

    @Test
    @DisplayName("Bearer présent → ne tente pas la clé BC (JWT prioritaire)")
    void bearer_prioritaire() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v1/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJSUzI1NiJ9.test")
                        .header(ApiKeyAuthenticationConverter.HEADER_CLIENT_ID, "bck_x")
                        .header(ApiKeyAuthenticationConverter.HEADER_API_KEY, "secret"));

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }

    @Test
    @DisplayName("clé BC seule → tente l'authentification par clé")
    void cle_bc_seule() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/v1/businesses")
                        .header(ApiKeyAuthenticationConverter.HEADER_CLIENT_ID, "bck_x")
                        .header(ApiKeyAuthenticationConverter.HEADER_API_KEY, "secret"));

        StepVerifier.create(converter.convert(exchange))
                .expectNextCount(1)
                .verifyComplete();
    }
}
