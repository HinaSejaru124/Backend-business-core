package com.yowyob.businesscore.adapter.in.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la chaîne d'authentification JWT entrante : un token RS256 signé par le kernel (mocké via une
 * clé de test exposée en JWKS WireMock) est validé localement et projeté en {@link BusinessContext}
 * (tenant, acteur, permissions). Un token expiré est rejeté.
 */
class JwtReactiveAuthenticationManagerTest {

    private WireMockServer wireMock;
    private RSAKey signingKey;
    private JwtReactiveAuthenticationManager manager;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        wireMock.stubFor(get(urlEqualTo("/.well-known/jwks.json"))
                .willReturn(okJson(new JWKSet(signingKey.toPublicJWK()).toString())));

        KernelProperties kernelProps = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "", "");
        AuthProperties authProps = new AuthProperties(
                "", "", "http://localhost:" + wireMock.port() + "/.well-known/jwks.json");
        manager = new JwtReactiveAuthenticationManager(new JwksJwtVerifier(kernelProps, authProps));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    private String jeton(Instant expiration) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .claim("tid", TENANT.toString())
                .claim("actor", ACTOR.toString())
                .claim("permissions", List.of("organizations:write", "products:read"))
                .jwtID("jti-1")
                .expirationTime(Date.from(expiration))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    @Test
    @DisplayName("token valide → authentifié + BusinessContext (tenant, acteur, permissions)")
    void token_valide_construit_le_contexte() throws Exception {
        String token = jeton(Instant.now().plusSeconds(900));

        StepVerifier.create(manager.authenticate(JwtAuthenticationToken.unauthenticated(token)))
                .assertNext(auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    assertThat(auth.getCredentials()).isEqualTo(token);
                    BusinessContext ctx = (BusinessContext) auth.getPrincipal();
                    assertThat(ctx.tenantId()).isEqualTo(TENANT);
                    assertThat(ctx.actorId()).isEqualTo(ACTOR);
                    assertThat(ctx.hasRole("organizations:write")).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("token expiré → rejeté (BadCredentials)")
    void token_expire_rejete() throws Exception {
        String token = jeton(Instant.now().minusSeconds(60));

        StepVerifier.create(manager.authenticate(JwtAuthenticationToken.unauthenticated(token)))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
