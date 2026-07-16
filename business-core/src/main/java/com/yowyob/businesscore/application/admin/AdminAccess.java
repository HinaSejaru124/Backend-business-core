package com.yowyob.businesscore.application.admin;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Garde d'accès à la console d'administration.
 *
 * <p>Résout le développeur courant (JWT console) puis vérifie que son e-mail figure dans
 * {@link AdminProperties}. Toute route admin passe par {@link #exigerAdmin()} : un non-admin reçoit un
 * {@code 403} explicite. C'est le SEUL point de contrôle d'accès admin — les routes existantes ne sont
 * pas touchées.
 */
@Service
public class AdminAccess {

    private final ResoudreDeveloppeurCourant developpeurCourant;
    private final DeveloperAccountRepository developerRepository;
    private final AdminProperties adminProperties;

    public AdminAccess(ResoudreDeveloppeurCourant developpeurCourant,
                       DeveloperAccountRepository developerRepository,
                       AdminProperties adminProperties) {
        this.developpeurCourant = developpeurCourant;
        this.developerRepository = developerRepository;
        this.adminProperties = adminProperties;
    }

    /**
     * Renvoie le compte administrateur courant, ou erreur {@code 403} si le développeur connecté n'est
     * pas déclaré administrateur (ou {@code 401} si aucune session n'est résolue).
     */
    public Mono<DeveloperAccountEntity> exigerAdmin() {
        return developpeurCourant.id()
                .flatMap(developerRepository::findById)
                .switchIfEmpty(Mono.error(ProblemException.forbidden(
                        "Accès administrateur requis.")))
                .flatMap(compte -> adminProperties.estAdmin(compte.getEmail())
                        ? Mono.just(compte)
                        : Mono.error(ProblemException.forbidden(
                                "Votre compte n'a pas les droits d'administration de la plateforme.")));
    }
}
