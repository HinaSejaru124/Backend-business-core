// domain/rule/ContexteEvaluation.java
package com.yowyob.businesscore.domain.rule;

import java.util.Map;
import java.util.UUID;

/**
 * Les valeurs passées PAR la couche application à l'évaluateur.
 * La règle ne va jamais chercher des données elle-même.
 *
 * <p><b>Conservé volontairement pour le Niveau 2 (N2) — non utilisé au Niveau 1.</b> Le contrat
 * réellement consommé aujourd'hui par les évaluateurs est le port homonyme
 * {@code com.yowyob.businesscore.domain.port.internal.ContexteEvaluation} (simple porteur de
 * {@code Map<String,Object>}). Ce record, plus structuré, préfigure l'évaluation N2 ; ne pas le
 * confondre avec le port.
 */
public record ContexteEvaluation(
    UUID tenantId,
    UUID versionTypeId,
    UUID entrepriseId,
    String roleActeurCourant,   // lu du BusinessContext
    String motifDerogatoire,    // renseigné si l'acteur demande à déroger
    Map<String, Object> valeurs // toutes les données utiles à évaluer
) {}