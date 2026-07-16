package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Port de sortie — authentifie un utilisateur auprès du kernel (fournisseur d'identité central).
 * Le BC délègue la vérification du mot de passe au kernel et n'en stocke jamais aucun.
 * Implémenté par un adapter qui appelle {@code POST /api/auth/login} (mode app-only).
 */
public interface AuthentifierUtilisateur {

    Mono<ResultatLogin> login(String principal, String motDePasse);
    Mono<SignUpResult> signUp(String principal, String password,
                                                   String firstName, String lastName);
}
