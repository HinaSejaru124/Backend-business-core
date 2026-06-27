package com.yowyob.businesscore.adapter.in.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Vérifie un JWT kernel <b>localement</b> via les clés publiques RS256 du kernel
 * ({@code /.well-known/jwks.json}), sans rappeler le kernel à chaque requête (principe OIDC
 * « verify ID token »). Contrôle : signature RS256 + expiration ({@code exp}) + émetteur ({@code iss})
 * si configuré. Les clés sont récupérées et mises en cache par nimbus (lazy, au premier usage).
 */
@Component
public class JwksJwtVerifier {

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwksJwtVerifier(KernelProperties kernelProperties, AuthProperties authProperties) {
        String jwksUri = authProperties.jwksUriEffective(kernelProperties.baseUrl());
        URL jwksUrl;
        try {
            jwksUrl = new URL(jwksUri);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("URI JWKS invalide : " + jwksUri, e);
        }

        JWKSource<SecurityContext> source = JWKSourceBuilder.create(jwksUrl).build();

        DefaultJWTProcessor<SecurityContext> p = new DefaultJWTProcessor<>();
        p.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source));

        JWTClaimsSet correspondanceExacte = authProperties.verifieEmetteur()
                ? new JWTClaimsSet.Builder().issuer(authProperties.issuer()).build()
                : null;
        // Exige la présence de exp et tid ; vérifie exp/nbf automatiquement (avec tolérance d'horloge).
        p.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                correspondanceExacte, Set.of("exp", "tid")));

        this.processor = p;
    }

    /**
     * Vérifie le token et renvoie ses claims. Lève une exception (nimbus) si signature/exp/iss invalides.
     */
    public JWTClaimsSet verifier(String token) throws Exception {
        return processor.process(token, null);
    }
}
