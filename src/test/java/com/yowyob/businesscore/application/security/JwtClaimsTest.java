package com.yowyob.businesscore.application.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtClaimsTest {

    @Test
    @DisplayName("extrait tid et sub d'un JWT")
    void extrait_claims() throws Exception {
        UUID tenant = UUID.randomUUID();
        RSAKey key = new RSAKeyGenerator(2048).generate();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-kernel-42")
                .claim("tid", tenant.toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key));
        String token = jwt.serialize();

        assertThat(JwtClaims.tid(token)).isEqualTo(tenant);
        assertThat(JwtClaims.sub(token)).isEqualTo("user-kernel-42");
    }
}
