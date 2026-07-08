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
                    Crée un compte développeur et provisionne en coulisse une Actor kernel.
                    Renvoie la clé Business Core (`clientId` + `apiKey`) — le secret n'est affiché qu'une fois.
                    """,
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Développeur inscrit, clé émise"),
            @ApiResponse(responseCode = "422", description = "Données invalides")
    })
    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiKeyResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return registrationUseCase.inscrire(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.planCode()
        ).map(emise -> new ApiKeyResponse(emise.clientId(), emise.apiKey(), emise.plan()));
    }
}
