package com.yowyob.businesscore.application.usecase.auth;

import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case — connexion d'un utilisateur. Délègue la vérification d'identité au kernel via le port
 * {@link AuthentifierUtilisateur} ; le BC ne stocke jamais de mot de passe. Le token renvoyé est
 * ensuite rejoué par le client en {@code Bearer} sur les appels suivants.
 */
@Service
public class AuthentificationService {

    private final AuthentifierUtilisateur authentifier;

    public AuthentificationService(AuthentifierUtilisateur authentifier) {
        this.authentifier = authentifier;
    }

    public Mono<ResultatLogin> connecter(String principal, String motDePasse) {
        return authentifier.login(principal, motDePasse);
    }
}
