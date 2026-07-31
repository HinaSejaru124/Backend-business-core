package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.enterprise.EnterpriseSelectionService;
import com.yowyob.businesscore.domain.port.out.OrganisationAccessible;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Association développeur ↔ entreprise (= organisation kernel), thème strict de la plateforme : un
 * développeur travaille pour une seule entreprise, à l'intérieur de laquelle il crée ses applications.
 *
 * <p>Le frontend appelle {@code GET /status} juste après le login pour savoir où en est ce développeur :
 * entreprise déjà fixée ({@code DEFINIE}), à choisir parmi ses organisations kernel existantes
 * ({@code CHOIX_REQUIS} — développeurs déjà là avant ce thème), ou à nommer pour en créer une toute
 * neuve ({@code NOM_REQUIS} — nouvelle inscription ou développeur sans aucune organisation).
 */
@Tag(name = "Entreprise", description = "Association développeur ↔ entreprise (organisation kernel unique)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/enterprise")
public class EnterpriseSelectionController {

    private final EnterpriseSelectionService service;

    public EnterpriseSelectionController(EnterpriseSelectionService service) {
        this.service = service;
    }

    public record OptionResponse(String organizationId, String code, String nom) {
        static OptionResponse depuis(OrganisationAccessible o) {
            return new OptionResponse(o.organizationId(), o.organizationCode(), o.displayName());
        }
    }

    public record StatusResponse(String etat, String nom, UUID organizationId, List<OptionResponse> options) {
        static StatusResponse depuis(EnterpriseSelectionService.Statut s) {
            return new StatusResponse(s.etat(), s.nom(), s.organizationId(),
                    s.options().stream().map(OptionResponse::depuis).toList());
        }
    }

    @Operation(summary = "État de l'entreprise du développeur",
            description = "DEFINIE (rien à faire), CHOIX_REQUIS (proposer `options`), ou NOM_REQUIS "
                    + "(demander un nom puis POST /provision). Auto-provisionne si le nom est déjà connu "
                    + "(inscription récente) mais l'organisation pas encore créée.")
    @GetMapping("/status")
    public Mono<StatusResponse> status() {
        return BusinessContextHolder.currentContext()
                .flatMap(service::status)
                .map(StatusResponse::depuis);
    }

    public record SelectRequest(@NotNull UUID organizationId, @NotBlank String nom) {
    }

    @Operation(summary = "Choisir son entreprise parmi ses organisations kernel existantes",
            description = "Rattache une organisation déjà possédée (aucune création). Modifiable à tout "
                    + "moment (« changer d'entreprise »).")
    @PostMapping("/select")
    public Mono<StatusResponse> select(@Valid @RequestBody SelectRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> service.selectionner(ctx, requete.organizationId(), requete.nom()))
                .map(StatusResponse::depuis);
    }

    public record ProvisionRequest(@NotBlank String nom) {
    }

    @Operation(summary = "Créer une nouvelle entreprise (organisation kernel)",
            description = "Provisionnement complet (actor → organisation → approbation → services → agence). "
                    + "Modifiable à tout moment (« changer d'entreprise »).")
    @PostMapping("/provision")
    public Mono<StatusResponse> provision(@Valid @RequestBody ProvisionRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> service.provisionner(ctx, requete.nom()))
                .map(StatusResponse::depuis);
    }
}
