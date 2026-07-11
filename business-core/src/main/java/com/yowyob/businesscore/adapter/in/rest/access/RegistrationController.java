package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Accès")
@RestController
@RequestMapping("/v1")
public class RegistrationController {

    private final RegistrationUseCase registrationUseCase;

    public RegistrationController(RegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    @Operation(
            summary = "Inscrire un développeur",
            description = """
                    Crée un compte développeur et provisionne en coulisse une Actor kernel. N'émet aucune
                    clé API (elles sont scopées à une entreprise, créée après connexion) : connectez-vous via
                    POST /v1/auth/login, consultez GET /v1/auth/me pour votre identifiant développeur stable,
                    créez une entreprise puis une clé API pour cette entreprise.
                    """,
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Développeur inscrit"),
            @ApiResponse(responseCode = "422", description = "Données invalides")
    })
    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<InscriptionResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return registrationUseCase.inscrire(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.planCode()
        ).map(InscriptionResponse::depuis);
    }
}
