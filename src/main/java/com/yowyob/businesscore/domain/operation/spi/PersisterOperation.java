package com.yowyob.businesscore.domain.operation.spi;

import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.EtapeOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Port de sortie (feature Opérations) — persistance des définitions d'opération et de leurs étapes.
 * L'adapter R2DBC l'implémente ; le domaine ne connaît pas R2DBC. Le filtrage tenant est garanti par
 * la RLS (jamais de {@code WHERE tenant_id} à la main).
 */
public interface PersisterOperation {

    Mono<DefinitionOperation> sauvegarderDefinition(DefinitionOperation operation);

    /** {@code tenantId} requis : l'INSERT doit renseigner {@code tenant_id} (contrainte RLS WITH CHECK). */
    Flux<EtapeOperation> sauvegarderEtapes(UUID tenantId, List<EtapeOperation> etapes);

    Mono<DefinitionOperation> trouverParId(UUID operationId);

    /** Résout l'opération d'une version par son nom (insensible à la casse côté domaine). */
    Mono<DefinitionOperation> trouverParVersionEtNom(UUID versionTypeId, String nom);

    Flux<DefinitionOperation> listerParVersion(UUID versionTypeId);

    /** Séquence ordonnée (ordre croissant) des étapes d'une opération. */
    Flux<EtapeOperation> listerEtapes(UUID operationId);
}
