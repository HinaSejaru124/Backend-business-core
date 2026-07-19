package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.adapter.in.security.ApiKeyAuthenticationConverter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Documente l'authentification dans Swagger selon {@link AuthRouteClassifier} :
 * <ul>
 *   <li>Console dev : JWT via {@code bearerAuth} uniquement (controllers annotés).</li>
 *   <li>API intégration : {@code bearerAuth} obligatoire + headers {@code X-BC-*} optionnels
 *       (identifient la clé API du développeur).</li>
 *   <li>API à clé seule : pas de cadenas {@code bearerAuth} (Bearer refusé à l'application, cf.
 *       {@code ActeurAuthController.exigerCleEntreprise}) — seuls les headers {@code X-BC-*} sont
 *       documentés, marqués requis.</li>
 * </ul>
 */
@Component
public class BcClientIdHeaderOperationCustomizer implements OperationCustomizer {

    private static final String CLIENT_ID_HEADER = ApiKeyAuthenticationConverter.HEADER_CLIENT_ID;
    private static final String API_KEY_HEADER = ApiKeyAuthenticationConverter.HEADER_API_KEY;
    private static final String ON_BEHALF_HEADER = ApiKeyAuthenticationConverter.HEADER_ON_BEHALF_OF;
    private static final String BEARER_SCHEME = "bearerAuth";

    @Override
    public io.swagger.v3.oas.models.Operation customize(io.swagger.v3.oas.models.Operation operation,
                                                        HandlerMethod handlerMethod) {
        return switch (AuthRouteClassifier.classify(resolvePath(handlerMethod))) {
            case PUBLIC, CONSOLE_JWT -> operation;
            case API_INTEGRATION -> documenterIntegration(operation);
            case API_CLE_SEULE -> documenterCleSeule(operation);
        };
    }

    private io.swagger.v3.oas.models.Operation documenterIntegration(io.swagger.v3.oas.models.Operation operation) {
        assurerBearerSecurity(operation);
        addHeaderIfAbsent(operation, CLIENT_ID_HEADER,
                "Identifiant développeur stable (voir GET /v1/auth/me) — ne change jamais, quelle que "
                        + "soit l'application ciblée.",
                "00000000-0000-0000-0000-000000000000", false);
        addHeaderIfAbsent(operation, API_KEY_HEADER,
                "Secret de la clé de l'application ciblée. À utiliser avec X-BC-Client-Id.",
                null, false);
        addHeaderIfAbsent(operation, ON_BEHALF_HEADER,
                "Acteur métier asserté par le backend du développeur (UUID, optionnel).",
                null, false);
        return operation;
    }

    /** Pas de {@code bearerAuth} : le Bearer est explicitement refusé sur cette route (un seul mode d'appel). */
    private io.swagger.v3.oas.models.Operation documenterCleSeule(io.swagger.v3.oas.models.Operation operation) {
        retirerBearerSecurity(operation);
        addHeaderIfAbsent(operation, CLIENT_ID_HEADER,
                "Identifiant développeur stable (voir GET /v1/auth/me) — ne change jamais, quelle que "
                        + "soit l'application ciblée.",
                "00000000-0000-0000-0000-000000000000", true);
        addHeaderIfAbsent(operation, API_KEY_HEADER,
                "Secret de la clé de l'application ciblée. À utiliser avec X-BC-Client-Id.",
                null, true);
        return operation;
    }

    private void assurerBearerSecurity(io.swagger.v3.oas.models.Operation operation) {
        List<SecurityRequirement> security = operation.getSecurity();
        if (security == null) {
            security = new ArrayList<>();
        }
        boolean deja = security.stream().anyMatch(req -> req.containsKey(BEARER_SCHEME));
        if (!deja) {
            security.add(new SecurityRequirement().addList(BEARER_SCHEME));
            operation.setSecurity(security);
        }
    }

    /** Retire tout {@code bearerAuth} déjà présent (ex. hérité d'une annotation de classe). */
    private void retirerBearerSecurity(io.swagger.v3.oas.models.Operation operation) {
        if (operation.getSecurity() == null) {
            return;
        }
        operation.getSecurity().removeIf(req -> req.containsKey(BEARER_SCHEME));
    }

    private void addHeaderIfAbsent(io.swagger.v3.oas.models.Operation operation, String name,
                                   String description, String example, boolean required) {
        if (hasHeaderParameter(operation, name)) {
            return;
        }
        StringSchema schema = new StringSchema();
        if (example != null) {
            schema.example(example);
        }
        operation.addParametersItem(new Parameter()
                .in("header")
                .name(name)
                .required(required)
                .description(description)
                .schema(schema));
    }

    private boolean hasHeaderParameter(io.swagger.v3.oas.models.Operation operation, String headerName) {
        if (operation.getParameters() == null) {
            return false;
        }
        return operation.getParameters().stream()
                .anyMatch(p -> headerName.equalsIgnoreCase(p.getName()));
    }

    private String resolvePath(HandlerMethod handlerMethod) {
        StringBuilder path = new StringBuilder();
        appendPath(path, org.springframework.core.annotation.AnnotatedElementUtils
                .findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class));
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
}
