package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.security.JwtClaims;
import com.yowyob.businesscore.application.security.JwtDelegueResolver;
import org.springframework.dao.DataIntegrityViolationException;
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
                .switchIfEmpty(provisionnerDepuisJwt(ctx))
                .map(account -> account.getId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Compte développeur introuvable pour ce tenant. "
                                + "Inscrivez-vous via POST /v1/registration puis reconnectez-vous via POST /v1/auth/login.")));
    }

    private Mono<DeveloperAccountEntity> resoudreParJwt(BusinessContext ctx) {
        return jwtToken()
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

    /**
     * Filet de rattrapage pour les comptes kernel créés avant l'introduction de {@code developer_account}
     * (aucune ligne locale, quel que soit le critère de recherche) : le JWT prouve déjà que le kernel a
     * authentifié ce tenant/utilisateur, donc on crée l'enregistrement local à la volée plutôt que de
     * forcer une réinscription via /v1/registration qui échouerait côté kernel (compte déjà existant).
     *
     * <p>{@code kernel_user_id} porte une contrainte UNIQUE : deux requêtes concurrentes (ex. deux appels
     * quasi simultanés du même client) peuvent toutes les deux échouer la recherche initiale et tenter
     * l'insertion — la seconde viole alors la contrainte. On rattrape ce cas en relisant la ligne créée
     * par le concurrent plutôt que de laisser remonter une 500.
     */
    private Mono<DeveloperAccountEntity> provisionnerDepuisJwt(BusinessContext ctx) {
        return jwtToken()
                .flatMap(token -> {
                    String kernelUserId = JwtClaims.sub(token);
                    if (kernelUserId == null) {
                        return Mono.empty();
                    }
                    DeveloperAccountEntity account = DeveloperAccountEntity.nouveau(
                            UUID.randomUUID(), JwtClaims.email(token), ctx.tenantId(),
                            kernelUserId, null, null, "FREE");
                    return developerRepository.save(account)
                            .onErrorResume(DataIntegrityViolationException.class,
                                    ex -> developerRepository.findByKernelUserId(kernelUserId)
                                            .flatMap(existant -> alignerTenant(existant, ctx.tenantId())));
                });
    }

    private Mono<String> jwtToken() {
        return JwtDelegueResolver.courant()
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    private Mono<DeveloperAccountEntity> alignerTenant(DeveloperAccountEntity account, UUID tenantId) {
        if (account.getKernelTenantId() != null && tenantId.equals(account.getKernelTenantId())) {
            return Mono.just(account);
        }
        account.setKernelTenantId(tenantId);
        return developerRepository.save(account);
    }
}
