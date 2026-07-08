package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Jeton d'authentification par clé Business Core.
 * Non authentifié : porte clientId + apiKey (+ acteur asserté). Authentifié : porte le BusinessContext.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String clientId;
    private final String apiKey;
    private final String onBehalfOf;
    private final BusinessContext businessContext;
    private final java.util.UUID apiKeyId;

    private ApiKeyAuthenticationToken(String clientId, String apiKey, String onBehalfOf) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.onBehalfOf = onBehalfOf;
        this.businessContext = null;
        this.apiKeyId = null;
        setAuthenticated(false);
    }

    private ApiKeyAuthenticationToken(BusinessContext businessContext, java.util.UUID apiKeyId) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.clientId = null;
        this.apiKey = null;
        this.onBehalfOf = null;
        this.businessContext = businessContext;
        this.apiKeyId = apiKeyId;
        super.setAuthenticated(true);
    }

    public static ApiKeyAuthenticationToken unauthenticated(String clientId, String apiKey, String onBehalfOf) {
        return new ApiKeyAuthenticationToken(clientId, apiKey, onBehalfOf);
    }

    public static ApiKeyAuthenticationToken authenticated(BusinessContext businessContext, java.util.UUID apiKeyId) {
        return new ApiKeyAuthenticationToken(businessContext, apiKeyId);
    }

    public String getClientId() {
        return clientId;
    }

    public String getOnBehalfOf() {
        return onBehalfOf;
    }

    /** Identifiant de la clé API ayant authentifié la requête (null pour le flux JWT). */
    public java.util.UUID getApiKeyId() {
        return apiKeyId;
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return businessContext;
    }
}
