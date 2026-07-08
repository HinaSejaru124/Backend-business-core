package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.adapter.in.security.ApiKeyAuthenticationConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Map;

/**
 * Documente {@code X-BC-Client-Id} comme en-tête requis sur les routes protégées par clé BC.
 * L'identifiant public n'est pas un schéma Authorize (contrairement au secret {@code X-BC-Api-Key}).
 */
@Component
public class BcClientIdHeaderOperationCustomizer implements OperationCustomizer {

    private static final String CLIENT_ID_HEADER = ApiKeyAuthenticationConverter.HEADER_CLIENT_ID;
    private static final String BEARER_SCHEME = "bearerAuth";

    @Override
    public io.swagger.v3.oas.models.Operation customize(io.swagger.v3.oas.models.Operation operation,
                                                        HandlerMethod handlerMethod) {
        if (hasClientIdParameter(operation) || isPublic(handlerMethod, operation) || isBearerOnly(handlerMethod, operation)) {
            return operation;
        }
        operation.addParametersItem(new Parameter()
                .in("header")
                .name(CLIENT_ID_HEADER)
                .required(true)
                .description("Identifiant public de la clé BC (préfixe, émis à l'inscription)")
                .schema(new StringSchema().example("bc_live_abc123")));
        return operation;
    }

    private boolean hasClientIdParameter(io.swagger.v3.oas.models.Operation operation) {
        if (operation.getParameters() == null) {
            return false;
        }
        return operation.getParameters().stream()
                .anyMatch(p -> CLIENT_ID_HEADER.equalsIgnoreCase(p.getName()));
    }

    private boolean isPublic(HandlerMethod handlerMethod, io.swagger.v3.oas.models.Operation operation) {
        Operation annotation = handlerMethod.getMethodAnnotation(Operation.class);
        if (annotation != null && annotation.security().length == 0) {
            return true;
        }
        List<io.swagger.v3.oas.models.security.SecurityRequirement> security = operation.getSecurity();
        return security != null && security.isEmpty();
    }

    private boolean isBearerOnly(HandlerMethod handlerMethod, io.swagger.v3.oas.models.Operation operation) {
        Operation annotation = handlerMethod.getMethodAnnotation(Operation.class);
        if (annotation != null && annotation.security().length == 1) {
            SecurityRequirement req = annotation.security()[0];
            if (BEARER_SCHEME.equals(req.name()) && req.scopes().length == 0) {
                return true;
            }
        }
        List<io.swagger.v3.oas.models.security.SecurityRequirement> security = operation.getSecurity();
        if (security != null && security.size() == 1) {
            Map<String, List<String>> req = security.get(0);
            return req.size() == 1 && req.containsKey(BEARER_SCHEME);
        }
        return false;
    }
}
