package com.pharmacore.pharmaciebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorise le frontend Pharmacie (localhost:3001 en dev) à appeler cette API depuis le navigateur.
 * Sans ceci, toute requête fetch() depuis pharmacie-frontend-test serait bloquée par le CORS du
 * navigateur (curl, lui, ne l'aurait jamais détecté — c'est une règle appliquée côté navigateur).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}
