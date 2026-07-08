package com.yowyob.businesscore.application.usecase.auth;

import org.springframework.stereotype.Service;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
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
 * <p>À la connexion, si le compte développeur (retrouvé par email) n'a pas encore de tenant kernel lié,
 * on le renseigne depuis le token ({@code tid}). C'est le filet de sécurité de l'alignement de tenant :
 * après un premier login, la clé API et le JWT résolvent le même espace.
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
                .flatMap(resultat -> lierTenantSiAbsent(principal, resultat).thenReturn(resultat));
    }

    public Mono<SignUpResult> creerCompte(String principal, String password,
            String firstName, String lastName) {
        return authentifier.signUp(principal, password, firstName, lastName);
    }

    /** Lie le tenant kernel au compte (par email) s'il n'est pas encore renseigné. */
    private Mono<Void> lierTenantSiAbsent(String email, ResultatLogin resultat) {
        UUID tenantId = parseUuid(resultat.tenantId());
        if (tenantId == null) {
            return Mono.empty();
        }
        return developerRepository.findByEmail(email)
                .filter(account -> account.getKernelTenantId() == null)
                .flatMap(account -> {
                    account.setKernelTenantId(tenantId);
                    return developerRepository.save(account);
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
