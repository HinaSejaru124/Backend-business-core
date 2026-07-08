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
 *   <li>crée le compte BC local en mémorisant le {@code kernel_tenant_id} (le tenant métier) et l'email ;</li>
 *   <li>émet une première clé API (dans la table {@code api_key}).</li>
 * </ol>
 * Le tenant BC est donc le tenant kernel dès l'inscription : la clé API et le JWT résolvent le même
 * espace. Le dev vérifie son email, puis se connecte via {@code POST /v1/auth/login}.
 */
@Service
public class RegistrationService implements RegistrationUseCase {

    private final DeveloperAccountRepository repository;
    private final AuthentifierUtilisateur authentifier;
    private final ApiKeyService apiKeyService;

    public RegistrationService(DeveloperAccountRepository repository,
                               AuthentifierUtilisateur authentifier,
                               ApiKeyService apiKeyService) {
        this.repository = repository;
        this.authentifier = authentifier;
        this.apiKeyService = apiKeyService;
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
                            null,
                            null,
                            plan);
                    return repository.save(entity);
                })
                .flatMap(account -> apiKeyService.creer(account.getId(), "Default")
                        .map(cle -> new ApiKeyEmise(cle.prefix(), cle.secret(), plan)));
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
