package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Endpoint d'inscription développeur (famille Accès). Route publique : émet la clé Business Core et
 * provisionne en coulisse l'accès kernel.
 */
@RestController
@RequestMapping("/v1")
public class RegistrationController {

    private final RegistrationUseCase registrationUseCase;

    public RegistrationController(RegistrationUseCase registrationUseCase) {
        this.registrationUseCase = registrationUseCase;
    }

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiKeyResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return registrationUseCase.inscrire(request.nom(), request.email(), request.planCode())
                .map(emise -> new ApiKeyResponse(emise.clientId(), emise.apiKey(), emise.plan()));
    }
}
