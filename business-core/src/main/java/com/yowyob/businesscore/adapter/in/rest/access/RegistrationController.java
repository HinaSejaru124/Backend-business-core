package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
        return registrationUseCase.inscrire(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.planCode()
        ).map(emise -> new ApiKeyResponse(emise.clientId(), emise.apiKey(), emise.plan()));
    }
}