package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.security.JwtClaims;
import com.yowyob.businesscore.application.security.JwtDelegueResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Résout le compte développeur courant pour les routes console (JWT).
 * Priorité : {@code kernel_tenant_id} du contexte, puis {@code kernel_user_id} (claim {@code sub} du JWT).
 */
@Service
public class ResoudreDeveloppeurCourant {

    private final DeveloperAccountRepository developerRepository;

    public ResoudreDeveloppeurCourant(DeveloperAccountRepository developerRepository) {
        this.developerRepository = developerRepository;
    }

    /**
     * Retourne {@link Mono#empty()} si le contexte est absent (drain WebFlux) — ne pas lever d'erreur.
     * L'authentification est garantie en amont par Spring Security sur les routes protégées.
     */
    public Mono<UUID> id() {
        return BusinessContextHolder.currentContext()
                .flatMap(this::resoudre);
    }

    private Mono<UUID> resoudre(BusinessContext ctx) {
        return developerRepository.findByKernelTenantId(ctx.tenantId())
                .switchIfEmpty(resoudreParJwt(ctx))
                .map(DeveloperAccountEntity::getId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Compte développeur introuvable pour ce tenant. "
                                + "Inscrivez-vous via POST /v1/registration puis reconnectez-vous via POST /v1/auth/login.")));
    }

    private Mono<DeveloperAccountEntity> resoudreParJwt(BusinessContext ctx) {
        return JwtDelegueResolver.courant()
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty))
                .flatMap(token -> {
                    String kernelUserId = JwtClaims.sub(token);
                    if (kernelUserId == null) {
                        return Mono.empty();
                    }
                    return developerRepository.findByKernelUserId(kernelUserId)
                            .switchIfEmpty(Mono.defer(() -> {
                                String email = JwtClaims.email(token);
                                return email == null
                                        ? Mono.empty()
                                        : developerRepository.findByEmail(email);
                            }))
                            .flatMap(account -> alignerTenant(account, ctx.tenantId()));
                });
    }

    private Mono<DeveloperAccountEntity> alignerTenant(DeveloperAccountEntity account, UUID tenantId) {
        if (account.getKernelTenantId() != null && tenantId.equals(account.getKernelTenantId())) {
            return Mono.just(account);
        }
        account.setKernelTenantId(tenantId);
        return developerRepository.save(account);
    }
}
