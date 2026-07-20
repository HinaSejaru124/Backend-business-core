package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.adapter.in.rest.auth.LoginRequest;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.actor.AuthentifierActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.InscrireActeurCommande;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Authentification des <b>acteurs métier</b> (pharmacien, caissier...) — distincte de celle du
 * développeur ({@code /v1/auth/*} et {@code /v1/registration}, qui restent inchangés). Kernel Core fait
 * autorité sur l'identité : Business Core ne stocke jamais de mot de passe, et ne résout/crée une
 * identité kernel qu'ici, en libre-service ({@code :register}) — jamais dans
 * {@link ActeurMetierController#rattacher}, qui n'accepte plus qu'une identité déjà connue.
 *
 * <p>Chemin d'appel typique : Frontend terminal → Backend terminal (ex. PharmAPI) → ici → Kernel → ici →
 * Backend terminal. Le backend terminal ne parle jamais directement au kernel.
 */
@Tag(name = "Acteurs — Authentification",
        description = "Inscription/connexion d'un acteur métier (déléguées au kernel) et résolution de son contexte")
@RestController
public class ActeurAuthController {

    private final AuthentifierActeurService authentifierActeur;
    private final GestionActeurService gestionActeur;

    public ActeurAuthController(AuthentifierActeurService authentifierActeur, GestionActeurService gestionActeur) {
        this.authentifierActeur = authentifierActeur;
        this.gestionActeur = gestionActeur;
    }

    @Operation(
            summary = "Inscription d'un acteur métier",
            description = """
                    Un seul compte Yow, réutilisable dans N applications : tente d'abord une connexion avec
                    les identifiants fournis (le compte existe peut-être déjà — développeur, ou déjà acteur
                    d'une autre application) ; ne crée un compte kernel (sign-up) que si la connexion échoue,
                    c'est-à-dire pour une personne réellement nouvelle. Puis rattache (ou signale le
                    rattachement déjà existant) au rôle métier demandé dans cette application. Distincte de
                    `/v1/registration` (inscription développeur : provisionne un tenant, pas un rattachement
                    métier) et de `POST /v1/applications/{id}/actors` (rattachement d'une identité déjà
                    connue, piloté par le développeur, sans création kernel). Réservée aux rôles OPERATEUR ;
                    un bénéficiaire n'a pas d'identifiants de connexion et reste rattaché par le développeur.
                    Réservée au backend terminal de l'application, identifié par sa clé Business Core
                    (X-BC-Client-Id + X-BC-Api-Key).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Acteur inscrit et rattaché"),
            @ApiResponse(responseCode = "404", description = "Application ou rôle introuvable"),
            @ApiResponse(responseCode = "409", description = "Déjà rattaché activement à cette application"),
            @ApiResponse(responseCode = "422", description = "Rôle non OPERATEUR"),
            @ApiResponse(responseCode = "403", description = "Clé API absente/JWT utilisé"),
            @ApiResponse(responseCode = "502",
                    description = "Compte créé mais identité non confirmée (vérification d'email en attente ?)")
    })
    @PostMapping("/v1/applications/{businessId}/actors:register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ActeurReponse> register(@PathVariable UUID businessId,
                                        @Valid @RequestBody InscrireActeurRequete requete) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> exigerCleEntreprise(ctx, businessId))
                .then(gestionActeur.inscrire(new InscrireActeurCommande(
                        businessId, requete.roleMetierId(), requete.email(), requete.password(),
                        requete.firstName(), requete.lastName())))
                .map(ActeurReponse::de);
    }

    @Operation(
            summary = "Connexion d'un acteur métier",
            description = """
                    Authentifie l'acteur auprès du kernel (principal/mot de passe, jamais stockés) puis
                    résout son rattachement à cette application (rôle métier actif). Réservée au backend
                    terminal de l'application, identifié par sa propre clé Business Core (X-BC-Client-Id +
                    X-BC-Api-Key) — un JWT développeur n'est volontairement pas accepté ici, pour qu'un seul
                    mode d'appel identifie sans ambiguïté qui déclenche une connexion acteur.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT émis + contexte métier résolu"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
            @ApiResponse(responseCode = "403", description = "Clé API absente/JWT utilisé, ou acteur non rattaché")
    })
    @PostMapping("/v1/applications/{businessId}/actors:login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ActeurLoginResponse> login(@PathVariable UUID businessId,
                                           @Valid @RequestBody LoginRequest requete) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> exigerCleEntreprise(ctx, businessId))
                .then(authentifierActeur.connecter(businessId, requete.principal(), requete.password()))
                .map(ActeurLoginResponse::depuis);
    }

    /**
     * N'accepte que la clé API de l'entreprise (seul mode qui identifie sans ambiguïté quel backend
     * terminal déclenche la connexion) — {@code ctx.businessId()} n'est jamais renseigné par la voie
     * JWT développeur, seulement par {@code ApiKeyReactiveAuthenticationManager}.
     */
    private void exigerCleEntreprise(BusinessContext ctx, UUID businessId) {
        if (ctx.businessId() == null) {
            throw ProblemException.forbidden(
                    "Cette route n'accepte que la clé API de l'application (X-BC-Client-Id + X-BC-Api-Key) "
                            + "— pas de JWT développeur.");
        }
        ctx.verifierAcces(businessId);
    }

    @Operation(
            summary = "Contexte de l'acteur courant",
            description = """
                    Résout, à partir du JWT courant (claim kernel `actor`), l'acteur métier et le rôle actif
                    de son porteur dans cette application — l'équivalent de `/v1/auth/me` mais côté acteur.
                    """,
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contexte métier résolu"),
            @ApiResponse(responseCode = "403", description = "Jeton sans identité acteur, ou acteur non rattaché")
    })
    @GetMapping("/v1/applications/{businessId}/actors/me")
    public Mono<ActeurContexteReponse> moi(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> authentifierActeur.moi(businessId, ctx.actorId())
                        .map(connecte -> ActeurContexteReponse.depuis(connecte, ctx.roles())));
    }
}
