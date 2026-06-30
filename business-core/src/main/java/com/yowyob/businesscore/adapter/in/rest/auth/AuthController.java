package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.auth.AuthentificationService;
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
 * API REST — authentification déléguée au kernel (cf. {@code Guide_Special_Auth.md}).
 * <ul>
 *   <li>{@code POST /v1/auth/login} (public) : connexion → renvoie le JWT kernel + permissions ;</li>
 *   <li>{@code GET /v1/auth/me} (protégé) : identité courante dérivée du JWT (dont {@code owner}).</li>
 * </ul>
 * Le mot de passe n'est jamais stocké : il est relayé au kernel le temps de l'appel de login.
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthentificationService authentification;

    public AuthController(AuthentificationService authentification) {
        this.authentification = authentification;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest requete) {
        return authentification.connecter(requete.principal(), requete.password())
                .map(LoginResponse::depuis);
    }

    @GetMapping("/me")
    public Mono<MeResponse> me() {
        return BusinessContextHolder.currentContext().map(MeResponse::depuis);
    }
}
