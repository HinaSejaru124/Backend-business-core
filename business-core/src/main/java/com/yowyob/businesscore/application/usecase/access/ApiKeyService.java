package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Gestion des clés API d'une entreprise (famille Accès).
 *
 * <p>Une entreprise n'a jamais plus d'une clé {@code ACTIVE} à la fois : il faut révoquer la clé
 * existante avant d'en émettre une nouvelle. Le secret (porté par {@code X-BC-Api-Key}) est stocké
 * uniquement haché (BCrypt) et renvoyé en clair une seule fois, à la création. Il n'y a pas de
 * {@code prefix}/identifiant public par clé : le développeur s'identifie par son {@code developerId}
 * stable ({@code X-BC-Client-Id}, cf. {@code GET /v1/auth/me}).
 */
@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;
    private final EntrepriseRepository entrepriseRepository;
    private final DeveloperAccountRepository developerRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository repository,
                         EntrepriseRepository entrepriseRepository,
                         DeveloperAccountRepository developerRepository,
                         PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.entrepriseRepository = entrepriseRepository;
        this.developerRepository = developerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Secret complet d'une clé — n'existe qu'en mémoire, le temps de le renvoyer une fois au dev. */
    public record CleApiCreee(UUID id, String secret, String name, UUID entrepriseId) {
    }

    /**
     * Émet la clé d'une entreprise. Refuse si l'entreprise n'appartient pas au tenant du développeur,
     * ou si elle a déjà une clé active (409 — révoquer avant de recréer).
     */
    public Mono<CleApiCreee> creer(UUID developerId, UUID entrepriseId, String name) {
        if (entrepriseId == null) {
            return Mono.error(ProblemException.badRequest(
                    "entrepriseId est obligatoire : toute clé API est scopée à une entreprise."));
        }
        return verifierEntrepriseDuTenant(developerId, entrepriseId)
                .then(repository.countByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .flatMap(actives -> {
                    if (actives > 0) {
                        return Mono.error(ProblemException.conflict(
                                "Cette entreprise a déjà une clé active. "
                                        + "Révoquez-la avant d'en créer une nouvelle.")
                                .violatedRule("CLE_ACTIVE_EXISTANTE"));
                    }
                    String secret = jeton(32);
                    ApiKeyEntity entity = ApiKeyEntity.nouveau(UUID.randomUUID(), developerId, entrepriseId,
                            passwordEncoder.encode(secret), name);
                    return repository.save(entity)
                            .map(saved -> new CleApiCreee(saved.getId(), secret, saved.getName(),
                                    saved.getEntrepriseId()));
                });
    }

    private Mono<Void> verifierEntrepriseDuTenant(UUID developerId, UUID entrepriseId) {
        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Développeur introuvable")))
                .flatMap(developer -> entrepriseRepository.findById(entrepriseId)
                        .switchIfEmpty(Mono.error(ProblemException.notFound("Entreprise introuvable : " + entrepriseId)))
                        .flatMap(entreprise -> {
                            if (!entreprise.getTenantId().equals(developer.getKernelTenantId())) {
                                return Mono.error(ProblemException.notFound("Entreprise introuvable : " + entrepriseId));
                            }
                            return Mono.empty();
                        }))
                .then();
    }

    /**
     * La clé active de l'entreprise (au plus une). L'appelant (controller) doit avoir déjà vérifié que
     * cette entreprise appartient au tenant courant (ex. via {@code EntrepriseService.trouver}).
     */
    public Mono<ApiKeyEntity> trouverActive(UUID entrepriseId) {
        return repository.findByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE).next();
    }

    public Mono<ApiKeyEntity> renommer(UUID entrepriseId, String name) {
        return chargerActive(entrepriseId)
                .flatMap(entity -> {
                    entity.setName((name == null || name.isBlank()) ? entity.getName() : name);
                    return repository.save(entity);
                });
    }

    public Mono<ApiKeyEntity> revoquer(UUID entrepriseId) {
        return chargerActive(entrepriseId)
                .flatMap(entity -> {
                    entity.setStatus(ApiKeyEntity.STATUT_REVOKED);
                    return repository.save(entity);
                });
    }

    /** Met à jour la date de dernière utilisation (best-effort, appelé à chaque auth réussie). */
    public Mono<Void> marquerUtilisee(UUID keyId) {
        return repository.findById(keyId)
                .flatMap(entity -> {
                    entity.setLastUsedAt(Instant.now());
                    return repository.save(entity);
                })
                .then();
    }

    private Mono<ApiKeyEntity> chargerActive(UUID entrepriseId) {
        return repository.findByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE).next()
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Aucune clé active pour cette entreprise : " + entrepriseId)));
    }

    private static String jeton(int octets) {
        byte[] bytes = new byte[octets];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
