// domain/rule/ContexteEvaluation.java
package com.yowyob.businesscore.domain.rule;

import java.util.Map;
import java.util.UUID;

/**
 * Les valeurs passées PAR la couche application à l'évaluateur.
 * La règle ne va jamais chercher des données elle-même.
 */
public record ContexteEvaluation(
    UUID tenantId,
    UUID versionTypeId,
    UUID entrepriseId,
    String roleActeurCourant,   // lu du BusinessContext
    String motifDerogatoire,    // renseigné si l'acteur demande à déroger
    Map<String, Object> valeurs // toutes les données utiles à évaluer
) {}