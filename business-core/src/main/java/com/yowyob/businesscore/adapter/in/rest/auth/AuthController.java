package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.auth.AuthentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Authentification déléguée au kernel. Le mot de passe n'est jamais stocké localement.
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthentificationService authentification;

    public AuthController(AuthentificationService authentification) {
        this.authentification = authentification;
    }

    @Operation(
            summary = "Connexion",
            description = "Authentifie l'utilisateur auprès du kernel et renvoie un JWT Bearer + permissions.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT émis"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest requete) {
        return authentification.connecter(requete.principal(), requete.password())
                .map(LoginResponse::depuis);
    }

    @Operation(
            summary = "Profil courant",
            description = "Identité dérivée du JWT ou de la clé BC courante (dont le rôle owner).",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponse(responseCode = "200", description = "Profil utilisateur")
    @GetMapping("/me")
    public Mono<MeResponse> me() {
        return BusinessContextHolder.currentContext().map(MeResponse::depuis);
    }
}
