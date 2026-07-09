package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.adapter.in.security.ApiKeyAuthenticationConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Documente {@code X-BC-Client-Id} comme en-tête requis sur les routes protégées par clé BC.
 * L'identifiant public n'est pas un schéma Authorize (contrairement au secret {@code X-BC-Api-Key}).
 */
@Component
public class BcClientIdHeaderOperationCustomizer implements OperationCustomizer {

    private static final String CLIENT_ID_HEADER = ApiKeyAuthenticationConverter.HEADER_CLIENT_ID;
    private static final String BEARER_SCHEME = "bearerAuth";

    /** Aligné sur {@code SecurityConfig.ROUTES_PUBLIQUES} (partie controllers). */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/health",
            "/v1/registration",
            "/v1/auth/login"
    );

    @Override
    public io.swagger.v3.oas.models.Operation customize(io.swagger.v3.oas.models.Operation operation,
                                                        HandlerMethod handlerMethod) {
        if (hasClientIdParameter(operation)
                || isPublicRoute(handlerMethod)
                || isBearerOnly(handlerMethod, operation)) {
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

    private boolean isPublicRoute(HandlerMethod handlerMethod) {
        return PUBLIC_PATHS.contains(resolvePath(handlerMethod));
    }

    private String resolvePath(HandlerMethod handlerMethod) {
        StringBuilder path = new StringBuilder();
        appendPath(path, AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class));
        if (handlerMethod.hasMethodAnnotation(GetMapping.class)) {
            appendPath(path, handlerMethod.getMethodAnnotation(GetMapping.class));
        } else if (handlerMethod.hasMethodAnnotation(PostMapping.class)) {
            appendPath(path, handlerMethod.getMethodAnnotation(PostMapping.class));
        } else if (handlerMethod.hasMethodAnnotation(PutMapping.class)) {
            appendPath(path, handlerMethod.getMethodAnnotation(PutMapping.class));
        } else if (handlerMethod.hasMethodAnnotation(DeleteMapping.class)) {
            appendPath(path, handlerMethod.getMethodAnnotation(DeleteMapping.class));
        } else if (handlerMethod.hasMethodAnnotation(PatchMapping.class)) {
            appendPath(path, handlerMethod.getMethodAnnotation(PatchMapping.class));
        } else {
            appendPath(path, handlerMethod.getMethodAnnotation(RequestMapping.class));
        }
        String resolved = path.toString().replaceAll("/+", "/");
        return resolved.endsWith("/") && resolved.length() > 1
                ? resolved.substring(0, resolved.length() - 1)
                : resolved;
    }

    private void appendPath(StringBuilder path, Annotation mapping) {
        if (mapping == null) {
            return;
        }
        String[] segments = switch (mapping) {
            case RequestMapping rm -> rm.value().length > 0 ? rm.value() : rm.path();
            case GetMapping gm -> gm.value().length > 0 ? gm.value() : gm.path();
            case PostMapping pm -> pm.value().length > 0 ? pm.value() : pm.path();
            case PutMapping pm -> pm.value().length > 0 ? pm.value() : pm.path();
            case DeleteMapping dm -> dm.value().length > 0 ? dm.value() : dm.path();
            case PatchMapping pm -> pm.value().length > 0 ? pm.value() : pm.path();
            default -> new String[0];
        };
        if (segments.length > 0) {
            if (!segments[0].startsWith("/")) {
                path.append('/');
            }
            path.append(segments[0]);
        }
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
