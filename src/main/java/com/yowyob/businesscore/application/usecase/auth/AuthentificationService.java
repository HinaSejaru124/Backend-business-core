package com.yowyob.businesscore.application.usecase.auth;

import org.springframework.stereotype.Service;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.security.JwtClaims;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import com.yowyob.businesscore.domain.port.out.SignUpResult;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case — connexion d'un utilisateur. Délègue la vérification d'identité au kernel via le port
 * {@link AuthentifierUtilisateur} ; le BC ne stocke jamais de mot de passe. Le token renvoyé est
 * ensuite rejoué par le client en {@code Bearer} sur les appels suivants.
 *
 * <p>À chaque connexion, le {@code kernel_tenant_id} du compte développeur (retrouvé par email) est
 * synchronisé avec le {@code selectedTenantId} du kernel. Ainsi le JWT ({@code tid}), les clés API et
 * la console dev résolvent toujours le même espace.
 */
@Service
public class AuthentificationService {

    private final AuthentifierUtilisateur authentifier;
    private final DeveloperAccountRepository developerRepository;

    public AuthentificationService(AuthentifierUtilisateur authentifier,
                                   DeveloperAccountRepository developerRepository) {
        this.authentifier = authentifier;
        this.developerRepository = developerRepository;
    }

    public Mono<ResultatLogin> connecter(String principal, String motDePasse) {
        return authentifier.login(principal, motDePasse)
                .flatMap(resultat -> synchroniserTenant(principal, resultat).thenReturn(resultat));
    }

    public Mono<SignUpResult> creerCompte(String principal, String password,
            String firstName, String lastName) {
        return authentifier.signUp(principal, password, firstName, lastName);
    }

    /** Aligne {@code kernel_tenant_id} et {@code kernel_user_id} sur le contexte sélectionné au login. */
    private Mono<Void> synchroniserTenant(String email, ResultatLogin resultat) {
        UUID tenantId = parseUuid(resultat.tenantId());
        if (tenantId == null) {
            tenantId = JwtClaims.tid(resultat.accessToken());
        }
        String kernelUserId = JwtClaims.sub(resultat.accessToken());
        if (tenantId == null && kernelUserId == null) {
            return Mono.empty();
        }
        UUID tenantFinal = tenantId;
        return developerRepository.findByEmail(email)
                .switchIfEmpty(Mono.defer(() -> kernelUserId == null
                        ? Mono.empty()
                        : developerRepository.findByKernelUserId(kernelUserId)))
                // Aucun compte local : on le crée DÈS la connexion avec l'e-mail du principal. Le JWT kernel
                // ne porte pas toujours l'e-mail (ex. compte créé par username, cas de l'admin) ; sans ceci
                // le compte serait provisionné plus tard avec un e-mail vide → l'accès admin (basé sur
                // l'e-mail) échouerait à la 1re connexion. Garantit le bon comportement même en déploiement neuf.
                .switchIfEmpty(Mono.defer(() -> {
                    if (kernelUserId == null && tenantFinal == null) {
                        return Mono.empty();
                    }
                    DeveloperAccountEntity nouveau = DeveloperAccountEntity.nouveau(
                            UUID.randomUUID(), email, tenantFinal, kernelUserId, null, null, "FREE");
                    return developerRepository.save(nouveau)
                            .onErrorResume(DataIntegrityViolationException.class, ex -> kernelUserId == null
                                    ? Mono.empty()
                                    : developerRepository.findByKernelUserId(kernelUserId));
                }))
                .flatMap(account -> {
                    // Blocage effectif : un développeur suspendu par l'administrateur ne peut plus se
                    // connecter du tout (ni console, ni API), et reçoit un feedback explicite.
                    if ("SUSPENDED".equalsIgnoreCase(account.getStatus())) {
                        return Mono.error(new ProblemException(HttpStatus.FORBIDDEN, "Compte bloqué",
                                "Votre compte a été bloqué par l'administrateur de Business Core. "
                                        + "Contactez le support pour toute réclamation."));
                    }
                    boolean changed = false;
                    if (tenantFinal != null && !tenantFinal.equals(account.getKernelTenantId())) {
                        account.setKernelTenantId(tenantFinal);
                        changed = true;
                    }
                    if (kernelUserId != null && !kernelUserId.equals(account.getKernelUserId())) {
                        account.setKernelUserId(kernelUserId);
                        changed = true;
                    }
                    if ((account.getEmail() == null || account.getEmail().isBlank())
                            && email != null && !email.isBlank()) {
                        account.setEmail(email);
                        changed = true;
                    }
                    return changed ? developerRepository.save(account) : Mono.just(account);
                })
                .then();
    }

    private static UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
