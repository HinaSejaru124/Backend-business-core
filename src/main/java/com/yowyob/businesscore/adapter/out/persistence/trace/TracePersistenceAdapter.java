package com.yowyob.businesscore.adapter.out.persistence.trace;

import com.yowyob.businesscore.domain.shared.StatutTrace;
import com.yowyob.businesscore.domain.transaction.TraceOperation;
import com.yowyob.businesscore.domain.transaction.spi.PersisterTrace;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter R2DBC — implémente {@link PersisterTrace}. INSERT à l'ouverture de la trace, UPDATE aux
 * transitions (COMPLETEE / COMPENSEE) en rechargeant la ligne existante. RLS isole le tenant.
 */
@Component
public class TracePersistenceAdapter implements PersisterTrace {

    private final TraceOperationRepository repository;

    public TracePersistenceAdapter(TraceOperationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<TraceOperation> sauvegarder(TraceOperation trace) {
        return repository.findById(trace.id())
                .map(existant -> appliquer(existant, trace))
                .switchIfEmpty(Mono.fromSupplier(() -> versEntityNouveau(trace)))
                .flatMap(repository::save)
                .map(this::versDomaine);
    }

    @Override
    public Mono<TraceOperation> trouverParId(UUID traceId) {
        return repository.findById(traceId).map(this::versDomaine);
    }

    @Override
    public Mono<TraceOperation> trouverParCleIdempotence(String cleIdempotence) {
        return repository.findByCleIdempotence(cleIdempotence).map(this::versDomaine);
    }

    @Override
    public Flux<TraceOperation> listerParEntreprise(UUID entrepriseId) {
        return repository.findByEntrepriseId(entrepriseId).map(this::versDomaine);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private TraceOperationEntity versEntityNouveau(TraceOperation t) {
        return TraceOperationEntity.nouveau(
                t.id(), t.tenantId(), t.entrepriseId(), t.operationId(), t.operationNom(),
                t.cleIdempotence(), t.transactionKernelId(), t.statut().name(),
                t.resultatRegles(), t.codeErreur(), t.messageErreur(), t.creeLe(), t.resoluLe());
    }

    private TraceOperationEntity appliquer(TraceOperationEntity e, TraceOperation t) {
        e.setTransactionKernelId(t.transactionKernelId());
        e.setStatut(t.statut().name());
        e.setResultatRegles(t.resultatRegles());
        e.setCodeErreur(t.codeErreur());
        e.setMessageErreur(t.messageErreur());
        e.setResoluLe(t.resoluLe());
        return e;
    }

    private TraceOperation versDomaine(TraceOperationEntity e) {
        return new TraceOperation(
                e.getId(), e.getTenantId(), e.getEntrepriseId(), e.getOperationId(), e.getOperationNom(),
                e.getCleIdempotence(), e.getTransactionKernelId(), StatutTrace.valueOf(e.getStatut()),
                e.getResultatRegles(), e.getCodeErreur(), e.getMessageErreur(), e.getCreeLe(), e.getResoluLe());
    }
}
