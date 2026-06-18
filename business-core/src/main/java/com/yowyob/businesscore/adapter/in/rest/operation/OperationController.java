package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.operation.ConsulterOperationService;
import com.yowyob.businesscore.application.usecase.operation.DeclarerOperationService;
import com.yowyob.businesscore.application.usecase.operation.EtapeDeclaration;
import com.yowyob.businesscore.application.usecase.operation.ExecuterOperationService;
import com.yowyob.businesscore.domain.operation.ResultatExecution;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * API REST — Brique 5 (Opérations). Routes (cf. OpenAPI) :
 * <ul>
 *   <li>{@code POST /v1/business-types/{typeId}/versions/{n}/operations} — déclarer ;</li>
 *   <li>{@code GET  /v1/businesses/{businessId}/operations} — lister ;</li>
 *   <li>{@code POST /v1/businesses/{businessId}/operations/{name}:execute} — exécuter (200 / 202).</li>
 * </ul>
 * Le {@code BusinessContext} (donc le tenant) est lu du contexte réactif posé par le filtre du socle.
 */
@RestController
@RequestMapping("/v1")
public class OperationController {

    private final DeclarerOperationService declarerOperation;
    private final ConsulterOperationService consulterOperation;
    private final ExecuterOperationService executerOperation;

    public OperationController(DeclarerOperationService declarerOperation,
                              ConsulterOperationService consulterOperation,
                              ExecuterOperationService executerOperation) {
        this.declarerOperation = declarerOperation;
        this.consulterOperation = consulterOperation;
        this.executerOperation = executerOperation;
    }

    /** Déclarer une opération et ses étapes sous une version de Type. */
    @PostMapping("/business-types/{typeId}/versions/{versionNumber}/operations")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OperationResponse> declarer(@PathVariable UUID typeId,
                                            @PathVariable int versionNumber,
                                            @Valid @RequestBody CreerOperationRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> declarerOperation.declarer(
                        typeId,
                        versionNumber,
                        requete.nom(),
                        requete.roleDeclencheur(),
                        requete.declencheurRegles(),
                        Boolean.TRUE.equals(requete.differe()),
                        requete.etapes().stream()
                                .map(e -> new EtapeDeclaration(e.ordre(), e.typeEtape()))
                                .toList(),
                        ctx))
                .map(OperationResponse::depuis);
    }

    /** Lister les opérations disponibles pour une entreprise. */
    @GetMapping("/businesses/{businessId}/operations")
    public Flux<OperationResponse> lister(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> consulterOperation.listerParEntreprise(businessId, ctx))
                .map(OperationResponse::depuis);
    }

    /** Exécuter une opération : immédiate (200) ou différée (202). */
    @PostMapping("/businesses/{businessId}/operations/{name}:execute")
    public Mono<ResponseEntity<Object>> executer(@PathVariable UUID businessId,
                                                 @PathVariable String name,
                                                 @RequestHeader(value = "Idempotency-Key", required = false)
                                                 String idempotencyKey,
                                                 @RequestBody(required = false) ExecuterOperationRequest requete) {
        Map<String, Object> parametres = requete == null ? Map.of() : requete.parametresOuVide();
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> executerOperation.executer(businessId, name, idempotencyKey, parametres, ctx))
                .map(resultat -> versReponse(businessId, resultat));
    }

    private ResponseEntity<Object> versReponse(UUID businessId, ResultatExecution resultat) {
        if (resultat.estDiffere()) {
            String suivi = "/v1/businesses/" + businessId + "/traces/" + resultat.traceId();
            return ResponseEntity.accepted()
                    .body(new OperationPendingResponse("EN_COURS", resultat.traceId(), suivi));
        }
        String transactionId = resultat.transactionKernelId() != null
                ? resultat.transactionKernelId().toString()
                : null;
        return ResponseEntity.ok(new OperationResultResponse(
                "COMPLETEE", transactionId, resultat.traceId(), resultat.details()));
    }
}
