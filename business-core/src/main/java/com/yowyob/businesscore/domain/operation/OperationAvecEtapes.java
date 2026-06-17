package com.yowyob.businesscore.domain.operation;

import java.util.List;

/**
 * Agrégat de lecture : une opération déclarée avec sa séquence d'étapes ordonnées. Pratique pour les
 * réponses REST (déclarer / lister) sans exposer les entités de persistance.
 */
public record OperationAvecEtapes(DefinitionOperation definition, List<EtapeOperation> etapes) {

    public OperationAvecEtapes {
        etapes = etapes == null ? List.of() : List.copyOf(etapes);
    }
}
