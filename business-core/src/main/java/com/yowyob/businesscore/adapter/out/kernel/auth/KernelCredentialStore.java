package com.yowyob.businesscore.adapter.out.kernel.auth;

import org.springframework.stereotype.Service;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import com.yowyob.businesscore.infrastructure.security.SecretCipher;

import reactor.core.publisher.Mono;

/**
 * Fournit les credentials kernel pour les appels machine (sans user connecté).
 * Si le compte n'a pas de credentials kernel propres (nouveau flux login délégué),
 * replie sur les credentials plateforme du BC.
 */
@Service
public class KernelCredentialStore {

    public record KernelCreds(String clientId, String secret) {}

    private final DeveloperAccountRepository repository;
    private final SecretCipher cipher;
    private final KernelProperties kernelProperties;

    public KernelCredentialStore(DeveloperAccountRepository repository,
                                 SecretCipher cipher,
                                 KernelProperties kernelProperties) {
        this.repository = repository;
        this.cipher = cipher;
        this.kernelProperties = kernelProperties;
    }

    public Mono<KernelCreds> pourTenantCourant() {
        return BusinessContextHolder.currentTenantId()
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(
                                new IllegalStateException("Aucun tenant courant"))))
                .flatMap(repository::findById)
                .flatMap(account -> {
                    // Nouveau flux : pas de credentials kernel propres → plateforme BC
                    if (account.getKernelClientId() == null
                            || account.getKernelClientId().isBlank()) {
                        return Mono.just(new KernelCreds(
                                kernelProperties.clientId(),
                                kernelProperties.clientSecret()));
                    }
                    return Mono.just(new KernelCreds(
                            account.getKernelClientId(),
                            cipher.dechiffrer(account.getKernelSecretEncrypted())));
                });
    }
}