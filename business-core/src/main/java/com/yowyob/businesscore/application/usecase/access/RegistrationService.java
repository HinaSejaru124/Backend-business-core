package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Inscription d'un développeur :
 * <ol>
 *   <li>crée le compte sur le kernel (sign-up) — le kernel fait autorité sur l'identité et le tenant ;</li>
 *   <li>crée le compte BC local en mémorisant le {@code kernel_tenant_id} (le tenant métier) et l'email.</li>
 * </ol>
 * Aucune clé API n'est émise ici : les clés sont désormais scopées à une entreprise, qui n'existe pas
 * encore au moment de l'inscription. Le dev vérifie son email, se connecte via {@code POST /v1/auth/login}
 * (JWT — son identifiant stable est exposé par {@code GET /v1/auth/me}), crée une entreprise via
 * {@code POST /v1/businesses}, puis émet une clé API pour cette entreprise via {@code POST /v1/api-keys}.
 */
@Service
public class RegistrationService implements RegistrationUseCase {

    private final DeveloperAccountRepository repository;
    private final AuthentifierUtilisateur authentifier;

    public RegistrationService(DeveloperAccountRepository repository,
                               AuthentifierUtilisateur authentifier) {
        this.repository = repository;
        this.authentifier = authentifier;
    }

    @Override
    public Mono<ApiKeyEmise> inscrire(String firstName, String lastName,
                                      String email, String password,
                                      String planCode) {
        String plan = (planCode == null || planCode.isBlank()) ? "FREE" : planCode;

        return authentifier.signUp(email, password, firstName, lastName)
                .flatMap(signUpResult -> {
                    DeveloperAccountEntity entity = DeveloperAccountEntity.nouveau(
                            UUID.randomUUID(),
                            email,
                            parseUuid(signUpResult.tenantId()),
                            signUpResult.id(),
                            null,
                            null,
                            plan);
                    return repository.save(entity);
                })
                .map(account -> new ApiKeyEmise(plan,
                        "Compte créé. Vérifiez votre email, connectez-vous via POST /v1/auth/login, "
                                + "créez une entreprise puis une clé API pour cette entreprise."));
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
