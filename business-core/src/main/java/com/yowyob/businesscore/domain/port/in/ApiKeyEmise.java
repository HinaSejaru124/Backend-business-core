package com.yowyob.businesscore.domain.port.in;

/**
 * Confirmation d'inscription — aucune clé n'est émise ici : les clés API sont désormais scopées à une
 * entreprise (voir {@code POST /v1/api-keys}), qui n'existe pas encore à ce stade. Le développeur
 * vérifie son email, se connecte via JWT (identifiant stable exposé par {@code GET /v1/auth/me}), crée
 * une entreprise, puis émet une clé API pour cette entreprise.
 */
public record ApiKeyEmise(String plan, String message) {
}
