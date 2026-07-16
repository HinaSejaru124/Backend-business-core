package com.pharmacore.pharmaciebackend.telemetry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Capture chaque requête reçue par le backend PharmaCore (ses requêtes <b>propres</b>) et la rapporte à
 * Business Core (catégorie APP, non facturable) via {@link TelemetryReporter}.
 *
 * <p>Ne trace que l'API applicative ({@code /api/**}) : pas les ressources statiques ni l'actuator. Le
 * rapport est asynchrone et best-effort — il n'altère jamais la requête réelle.
 */
@Component
@Order(Integer.MIN_VALUE + 10)
public class RequeteTelemetryFilter extends OncePerRequestFilter {

    private final TelemetryReporter reporter;

    public RequeteTelemetryFilter(TelemetryReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long debut = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duree = System.currentTimeMillis() - debut;
            String endpoint = request.getRequestURI();
            if (request.getQueryString() != null) {
                endpoint = endpoint + "?" + request.getQueryString();
            }
            reporter.rapporter(request.getMethod(), endpoint, response.getStatus(), duree);
        }
    }
}
