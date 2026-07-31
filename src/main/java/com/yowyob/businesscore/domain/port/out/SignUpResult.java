package com.yowyob.businesscore.domain.port.out;

/**
 * @param identifiantConnexion adresse {@code @yowyob.com} générée par la plateforme d'identité. C'est
 *                             elle — et non l'e-mail personnel, réservé à la vérification et à la
 *                             récupération — qui permet de se connecter. Sans la renvoyer au
 *                             développeur, celui-ci n'a aucun moyen de la deviner.
 */
public record SignUpResult(
        String id,
        String tenantId,
        String status,
        String message,
        String identifiantConnexion
) {}
