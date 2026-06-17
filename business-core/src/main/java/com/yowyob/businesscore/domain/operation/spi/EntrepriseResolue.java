package com.yowyob.businesscore.domain.operation.spi;

import java.util.UUID;

/**
 * Vue minimale d'une entreprise nécessaire à l'exécution d'une opération : sa version de Type épinglée
 * (pour retrouver la {@code DefinitionOperation}) et son organisation kernel (pour les appels
 * {@code *ForOrganization}). Contrat stable entre la feature Opérations et la brique Entreprise (Dev 3).
 */
public record EntrepriseResolue(
        UUID entrepriseId,
        UUID versionTypeId,
        UUID organizationId
) {
}
