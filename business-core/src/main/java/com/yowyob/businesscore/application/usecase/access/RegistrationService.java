package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import com.yowyob.businesscore.domain.port.out.KernelClientCredentials;
import com.yowyob.businesscore.domain.port.out.ProvisionnerAccesDev;
import com.yowyob.businesscore.infrastructure.security.SecretCipher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Inscription d'un développeur (famille Accès, socle).
 *
 * <p>Provisionne une ClientApplication kernel dédiée (gouvernance native par développeur), stocke sa
 * clé kernel chiffrée (AES-GCM) et la clé Business Core hachée, puis renvoie la clé Business Core
 * (le secret n'est affiché qu'une fois). Le secret kernel n'est jamais exposé au développeur (ENF-04).
 */
@Service
public class RegistrationService implements RegistrationUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProvisionnerAccesDev provisionnerAccesDev;
    private final DeveloperAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecretCipher secretCipher;

    public RegistrationService(ProvisionnerAccesDev provisionnerAccesDev,
                               DeveloperAccountRepository repository,
                               PasswordEncoder passwordEncoder,
                               SecretCipher secretCipher) {
        this.provisionnerAccesDev = provisionnerAccesDev;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.secretCipher = secretCipher;
    }

    @Override
    public Mono<ApiKeyEmise> inscrire(String nom, String email, String planCode) {
        String plan = (planCode == null || planCode.isBlank()) ? "FREE" : planCode;
        String bcClientId = "bck_" + jeton(9);
        String bcApiKey = jeton(32);

        return provisionnerAccesDev.provisionner(plan)
                .flatMap(kernelCreds -> sauvegarder(bcClientId, bcApiKey, plan, kernelCreds))
                .thenReturn(new ApiKeyEmise(bcClientId, bcApiKey, plan));
    }

    private Mono<DeveloperAccountEntity> sauvegarder(String bcClientId, String bcApiKey, String plan,
                                                     KernelClientCredentials kernelCreds) {
        DeveloperAccountEntity entity = DeveloperAccountEntity.nouveau(
                UUID.randomUUID(),
                bcClientId,
                passwordEncoder.encode(bcApiKey),
                kernelCreds.clientId(),
                secretCipher.chiffrer(kernelCreds.secret()),
                plan);
        return repository.save(entity);
    }

    private static String jeton(int octets) {
        byte[] bytes = new byte[octets];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
