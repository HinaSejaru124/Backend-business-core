package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Inscription d'un développeur :
 * 1. Crée le compte sur le kernel (sign-up)
 * 2. Crée le compte BC local (bcClientId + bcApiKey)
 * Le dev vérifie son email, puis se connecte via POST /v1/auth/login.
 */
@Service
public class RegistrationService implements RegistrationUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeveloperAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthentifierUtilisateur authentifier;

    public RegistrationService(DeveloperAccountRepository repository,
                               PasswordEncoder passwordEncoder,
                               AuthentifierUtilisateur authentifier) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authentifier = authentifier;
    }

    @Override
    public Mono<ApiKeyEmise> inscrire(String firstName, String lastName,
                                      String email, String password,
                                      String planCode) {
        String plan = (planCode == null || planCode.isBlank()) ? "FREE" : planCode;
        String bcClientId = "bck_" + jeton(9);
        String bcApiKey   = jeton(32);

        return authentifier.signUp(email, password, firstName, lastName)
                .flatMap(signUpResult -> {
                    DeveloperAccountEntity entity = DeveloperAccountEntity.nouveau(
                            UUID.randomUUID(),
                            bcClientId,
                            passwordEncoder.encode(bcApiKey),
                            null,
                            null,
                            plan);
                    return repository.save(entity);
                })
                .thenReturn(new ApiKeyEmise(bcClientId, bcApiKey, plan));
    }

    private static String jeton(int octets) {
        byte[] bytes = new byte[octets];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
