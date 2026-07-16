package com.yowyob.businesscore.application.usecase.transaction;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.transaction.TraceOperation;
import com.yowyob.businesscore.domain.transaction.spi.PersisterTrace;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case — suivi des traces d'opération (lister / suivre une opération différée).
 */
@Service
public class ConsulterTraceService {

    private final PersisterTrace persisterTrace;

    public ConsulterTraceService(PersisterTrace persisterTrace) {
        this.persisterTrace = persisterTrace;
    }

    public Flux<TraceOperation> listerParEntreprise(UUID entrepriseId, BusinessContext ctx) {
        return persisterTrace.listerParEntreprise(entrepriseId);
    }

    public Mono<TraceOperation> trouver(UUID entrepriseId, UUID traceId, BusinessContext ctx) {
        return persisterTrace.trouverParId(traceId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Trace introuvable : " + traceId)))
                .flatMap(trace -> trace.entrepriseId().equals(entrepriseId)
                        ? Mono.just(trace)
                        : Mono.error(ProblemException.notFound("Trace introuvable : " + traceId)));
    }
}
