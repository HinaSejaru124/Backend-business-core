package com.yowyob.businesscore.adapter.out.kernel.auth;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.infrastructure.security.SecretCipher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Fournit les identifiants kernel (clientId + secret déchiffré) du développeur courant, à partir du
 * tenant présent dans le BusinessContext. Le secret n'est jamais exposé au développeur ni journalisé.
 */
@Service
public class KernelCredentialStore {

    public record KernelCreds(String clientId, String secret) {
    }

    private final DeveloperAccountRepository repository;
    private final SecretCipher cipher;

    public KernelCredentialStore(DeveloperAccountRepository repository, SecretCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public Mono<KernelCreds> pourTenantCourant() {
        return BusinessContextHolder.currentTenantId()
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalStateException("Aucun tenant courant"))))
                .flatMap(repository::findById)
                .map(account -> new KernelCreds(
                        account.getKernelClientId(),
                        cipher.dechiffrer(account.getKernelSecretEncrypted())));
    }
}
