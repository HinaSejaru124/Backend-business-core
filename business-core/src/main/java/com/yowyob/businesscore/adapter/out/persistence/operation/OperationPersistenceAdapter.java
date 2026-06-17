package com.yowyob.businesscore.adapter.out.persistence.operation;

import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.EtapeOperation;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Adapter R2DBC — implémente {@link PersisterOperation}. Traduit le domaine ↔ entités R2DBC.
 * Aucun {@code WHERE tenant_id} : la RLS isole les lignes du tenant courant.
 */
@Component
public class OperationPersistenceAdapter implements PersisterOperation {

    private final DefinitionOperationRepository definitionRepo;
    private final EtapeOperationRepository etapeRepo;

    public OperationPersistenceAdapter(DefinitionOperationRepository definitionRepo,
                                       EtapeOperationRepository etapeRepo) {
        this.definitionRepo = definitionRepo;
        this.etapeRepo = etapeRepo;
    }

    @Override
    public Mono<DefinitionOperation> sauvegarderDefinition(DefinitionOperation operation) {
        DefinitionOperationEntity entity = DefinitionOperationEntity.nouveau(
                operation.id(),
                operation.tenantId(),
                operation.versionTypeId(),
                operation.nom(),
                operation.roleDeclencheur(),
                operation.declencheurRegles().name(),
                operation.differe());
        return definitionRepo.save(entity).map(this::versDomaine);
    }

    @Override
    public Flux<EtapeOperation> sauvegarderEtapes(UUID tenantId, List<EtapeOperation> etapes) {
        List<EtapeOperationEntity> entities = etapes.stream()
                .map(e -> EtapeOperationEntity.nouveau(
                        e.id(), tenantId, e.operationId(), e.ordre(), e.typeEtape().name()))
                .toList();
        return etapeRepo.saveAll(entities).map(this::versDomaine);
    }

    @Override
    public Mono<DefinitionOperation> trouverParId(UUID operationId) {
        return definitionRepo.findById(operationId).map(this::versDomaine);
    }

    @Override
    public Mono<DefinitionOperation> trouverParVersionEtNom(UUID versionTypeId, String nom) {
        // RLS isole déjà le tenant ; on filtre le nom (insensible à la casse) en mémoire (N faible par version).
        return definitionRepo.findByVersionTypeId(versionTypeId)
                .map(this::versDomaine)
                .filter(op -> op.aPourNom(nom))
                .next();
    }

    @Override
    public Flux<DefinitionOperation> listerParVersion(UUID versionTypeId) {
        return definitionRepo.findByVersionTypeId(versionTypeId).map(this::versDomaine);
    }

    @Override
    public Flux<EtapeOperation> listerEtapes(UUID operationId) {
        return etapeRepo.findByOperationIdOrderByOrdreAsc(operationId).map(this::versDomaine);
    }

    // ─── Mapping entity → domaine ─────────────────────────────────────────

    private DefinitionOperation versDomaine(DefinitionOperationEntity e) {
        return new DefinitionOperation(
                e.getId(),
                e.getTenantId(),
                e.getVersionTypeId(),
                e.getNom(),
                e.getRoleDeclencheur(),
                Declencheur.valueOf(e.getDeclencheurRegles()),
                e.isDiffere());
    }

    private EtapeOperation versDomaine(EtapeOperationEntity e) {
        return new EtapeOperation(
                e.getId(),
                e.getOperationId(),
                e.getOrdre(),
                TypeEtape.valueOf(e.getTypeEtape()));
    }
}
