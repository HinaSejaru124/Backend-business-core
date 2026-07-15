package com.pharmacore.pharmaciebackend.auth;

import com.pharmacore.pharmaciebackend.admin.BcaasAuthClient;
import com.pharmacore.pharmaciebackend.personnel.Personnel;
import com.pharmacore.pharmaciebackend.personnel.PersonnelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

/**
 * Point de connexion <b>unique</b> de PharmaCore, pour les 3 rôles (Titulaire, Pharmacien Responsable,
 * Caissier) — un seul écran de connexion côté frontend, jamais de mention de Kernel/Business Core à
 * l'écran. La résolution du rôle se fait ici, côté serveur :
 * <ol>
 *   <li>Le personnel (compte local PharmaCore) est essayé en premier — vérification locale, aucun
 *       appel réseau ;</li>
 *   <li>Sinon, on tente une connexion titulaire (JWT Business Core) — c'est le seul cas où PharmaCore
 *       transmet réellement un mot de passe à Business Core, parce que c'est le compte qui EST le
 *       compte Business Core (cf. FEUILLE-DE-ROUTE.md, section restructuration par rôles).</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BcaasAuthClient titulaireAuthClient;
    private final PersonnelService personnelService;
    private final PharmacoreSession session;

    public AuthController(BcaasAuthClient titulaireAuthClient, PersonnelService personnelService,
                          PharmacoreSession session) {
        this.titulaireAuthClient = titulaireAuthClient;
        this.personnelService = personnelService;
        this.session = session;
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String motDePasse) {}

    public record StatutSession(boolean connecte, String role, String nomAffichage, String identifiant,
                                Instant expireLe) {}

    @PostMapping("/login")
    public StatutSession login(@Valid @RequestBody LoginRequest req) {
        Optional<Personnel> personnel = personnelService.authentifier(req.email(), req.motDePasse());
        if (personnel.isPresent()) {
            Personnel p = personnel.get();
            session.ouvrirPersonnel(PharmacoreSession.Role.valueOf(p.getRole()), p.getId(),
                    p.getPrenom() + " " + p.getNom(), p.getEmail(), p.getActeurKernelId().toString());
            return statutCourant();
        }

        BcaasAuthClient.ResultatLogin resultat = titulaireAuthClient.login(req.email(), req.motDePasse());
        session.ouvrirTitulaire(resultat.accessToken(), resultat.expiresInSeconds(), req.email(), resultat.actorId());
        return statutCourant();
    }

    @PostMapping("/logout")
    public StatutSession logout() {
        session.fermer();
        return statutCourant();
    }

    @GetMapping("/status")
    public StatutSession status() {
        return statutCourant();
    }

    private StatutSession statutCourant() {
        boolean connecte = session.estActive();
        return new StatutSession(connecte,
                connecte ? session.role().name() : null,
                connecte ? session.nomAffichage() : null,
                connecte ? session.identifiant() : null,
                connecte ? session.expireLe() : null);
    }
}
