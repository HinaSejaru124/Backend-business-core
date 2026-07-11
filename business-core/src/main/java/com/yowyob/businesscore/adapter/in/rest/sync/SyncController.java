package com.yowyob.businesscore.adapter.in.rest.sync;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.sync.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Synchronisation", description = "Journal de changements pour les backends terminaux (mode offline)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @Operation(summary = "Récupérer les changements de l'entreprise depuis une version",
            description = """
                    Réservé à une clé API scopée à une entreprise (`X-BC-Client-Id`/`X-BC-Api-Key` émis avec
                    `entrepriseId`) : l'entreprise ciblée est déduite de la clé, jamais d'un paramètre client.
                    `since=0` (ou absent) rejoue tout le journal depuis le début (équivalent d'un snapshot
                    initial). Le terminal doit repasser `versionCourante` en `since` à l'appel suivant.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Changements depuis `since`"),
            @ApiResponse(responseCode = "403", description = "Clé non scopée à une entreprise")
    })
    @GetMapping
    public Mono<SyncResponse> consulter(
            @Parameter(description = "Curseur de version connu du terminal (0 = depuis le début)")
            @RequestParam(defaultValue = "0") long since,
            @Parameter(description = "Nombre maximum d'événements retournés (défaut 100, max 500)")
            @RequestParam(required = false) Integer limit) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> {
                    if (ctx.businessId() == null) {
                        return Mono.error(ProblemException.forbidden(
                                "Ce endpoint nécessite une clé API scopée à une entreprise "
                                        + "(POST /v1/api-keys avec entrepriseId)."));
                    }
                    return syncService.consulter(ctx.businessId(), since, limit);
                })
                .map(SyncResponse::depuis);
    }
}
