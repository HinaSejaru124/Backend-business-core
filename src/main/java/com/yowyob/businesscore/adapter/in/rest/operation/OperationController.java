package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.operation.ConsulterOperationService;
import com.yowyob.businesscore.application.usecase.operation.DeclarerOperationService;
import com.yowyob.businesscore.application.usecase.operation.EtapeDeclaration;
import com.yowyob.businesscore.application.usecase.operation.ExecuterOperationService;
import com.yowyob.businesscore.domain.operation.ResultatExecution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Opérations", description = "Déclaration et exécution des actes métier")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Déclarer une opération",
            description = "Déclare une opération et ses étapes de saga sous une version de Type.",
            tags = {"Contenu de version"})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Opération déclarée"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
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

    @Operation(summary = "Lister les opérations disponibles",
            description = "Opérations de la version épinglée à l'application.")
    @ApiResponse(responseCode = "200", description = "Liste des opérations")
    @GetMapping("/businesses/{businessId}/operations")
    public Flux<OperationResponse> lister(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
                .flatMapMany(ctx -> consulterOperation.listerParEntreprise(businessId, ctx))
                .map(OperationResponse::depuis);
    }

    @Operation(summary = "Consulter une opération par nom")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détail de l'opération"),
            @ApiResponse(responseCode = "404", description = "Opération introuvable")
    })
    @GetMapping("/businesses/{businessId}/operations/{name}")
    public Mono<OperationResponse> trouver(@PathVariable UUID businessId,
                                             @Parameter(example = "vente") @PathVariable String name) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
                .flatMap(ctx -> consulterOperation.trouverParNom(businessId, name, ctx))
                .map(OperationResponse::depuis);
    }

    @Operation(
            summary = "Exécuter une opération",
            description = """
                    Déclenche un acte métier. Synchrone par défaut (`200` COMPLETEE).
                    Différée : `202` EN_COURS avec URL de trace. Règle bloquante : `422`.
                    Header optionnel `Idempotency-Key` pour éviter les doublons.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opération terminée"),
            @ApiResponse(responseCode = "202", description = "Opération acceptée, en cours"),
            @ApiResponse(responseCode = "422", description = "Règle métier non respectée")
    })
    @PostMapping("/businesses/{businessId}/operations/{name}:execute")
    public Mono<ResponseEntity<Object>> executer(@PathVariable UUID businessId,
                                                 @Parameter(example = "vente") @PathVariable String name,
                                                 @Parameter(description = "Clé d'idempotence")
                                                 @RequestHeader(value = "Idempotency-Key", required = false)
                                                 String idempotencyKey,
                                                 @RequestBody(required = false) ExecuterOperationRequest requete) {
        Map<String, Object> parametres = requete == null ? Map.of() : requete.parametresOuVide();
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
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
