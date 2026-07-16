package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
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
    private final ResoudreDeveloppeurCourant developpeurCourant;
    private final DeveloperAccountRepository developerRepository;

    public AuthController(AuthentificationService authentification,
                          ResoudreDeveloppeurCourant developpeurCourant,
                          DeveloperAccountRepository developerRepository) {
        this.authentification = authentification;
        this.developpeurCourant = developpeurCourant;
        this.developerRepository = developerRepository;
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
            description = """
                    Identité dérivée du JWT ou de la clé BC courante (dont le rôle owner), enrichie de
                    l'identifiant développeur stable (`developerId`) — à conserver côté client, ne change
                    jamais, sert à référencer ce développeur (ex. dans les intégrations tierces).
                    """,
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    @ApiResponse(responseCode = "200", description = "Profil utilisateur")
    @GetMapping("/me")
    public Mono<MeResponse> me() {
        return BusinessContextHolder.currentContext()
                .flatMap(this::avecIdentiteDeveloppeur);
    }

    private Mono<MeResponse> avecIdentiteDeveloppeur(BusinessContext ctx) {
        return developpeurCourant.id()
                .flatMap(developerRepository::findById)
                .map(compte -> MeResponse.depuis(ctx, compte.getId(), compte.getEmail(), compte.getPlan()))
                .defaultIfEmpty(MeResponse.depuis(ctx, null, null, null));
    }
}
