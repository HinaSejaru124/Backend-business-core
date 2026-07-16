package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Jeton d'authentification par JWT kernel délégué.
 * Non authentifié : porte le token brut. Authentifié : porte le {@link BusinessContext} (principal)
 * <b>et</b> le token brut (credentials), pour que {@code KernelClient} puisse le re-transmettre.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String rawToken;
    private final BusinessContext businessContext;

    private JwtAuthenticationToken(String rawToken) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.rawToken = rawToken;
        this.businessContext = null;
        setAuthenticated(false);
    }

    private JwtAuthenticationToken(String rawToken, BusinessContext businessContext) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.rawToken = rawToken;
        this.businessContext = businessContext;
        super.setAuthenticated(true);
    }

    public static JwtAuthenticationToken unauthenticated(String rawToken) {
        return new JwtAuthenticationToken(rawToken);
    }

    public static JwtAuthenticationToken authenticated(String rawToken, BusinessContext businessContext) {
        return new JwtAuthenticationToken(rawToken, businessContext);
    }

    @Override
    public Object getCredentials() {
        return rawToken;
    }

    @Override
    public Object getPrincipal() {
        return businessContext;
    }
}
