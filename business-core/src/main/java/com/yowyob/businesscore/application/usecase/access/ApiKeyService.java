package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des clés API d'un développeur (famille Accès).
 *
 * <p>Une clé = un {@code prefix} public (porté par {@code X-BC-Client-Id}) + un secret aléatoire
 * (porté par {@code X-BC-Api-Key}), stocké uniquement haché (BCrypt). Le secret complet n'est renvoyé
 * qu'une seule fois, à la création. La révocation est immédiate. Le nombre de clés actives est borné.
 */
@Service
public class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final int maxActive;

    public ApiKeyService(ApiKeyRepository repository,
                         PasswordEncoder passwordEncoder,
                         @Value("${businesscore.api-keys.max-active:5}") int maxActive) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.maxActive = maxActive;
    }

    /** Secret complet d'une clé — n'existe qu'en mémoire, le temps de le renvoyer une fois au dev. */
    public record CleApiCreee(UUID id, String prefix, String secret, String name) {
    }

    /**
     * Crée une clé pour un développeur après vérification de la limite de clés actives.
     * Renvoie le secret en clair (affiché une seule fois) ; seul son haché est persisté.
     */
    public Mono<CleApiCreee> creer(UUID developerId, String name) {
        return repository.countByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                .flatMap(actives -> {
                    if (actives >= maxActive) {
                        return Mono.error(ProblemException.conflict(
                                "Limite de " + maxActive + " clés actives atteinte. "
                                        + "Révoquez une clé avant d'en créer une nouvelle.")
                                .violatedRule("MAX_CLES_ACTIVES"));
                    }
                    String prefix = "bck_" + jeton(9);
                    String secret = jeton(32);
                    ApiKeyEntity entity = ApiKeyEntity.nouveau(
                            UUID.randomUUID(), developerId, prefix, passwordEncoder.encode(secret), name);
                    return repository.save(entity)
                            .map(saved -> new CleApiCreee(saved.getId(), prefix, secret, saved.getName()));
                });
    }

    public Flux<ApiKeyEntity> lister(UUID developerId) {
        return repository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE);
    }

    /** Variante collectée pour éviter la re-souscription WebFlux sur {@code Flux} de controller. */
    public Mono<List<ApiKeyEntity>> listerCollect(UUID developerId) {
        return repository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE).collectList();
    }

    public Mono<ApiKeyEntity> renommer(UUID developerId, UUID keyId, String name) {
        return chargerPossedee(developerId, keyId)
                .flatMap(entity -> {
                    entity.setName((name == null || name.isBlank()) ? entity.getName() : name);
                    return repository.save(entity);
                });
    }

    public Mono<ApiKeyEntity> revoquer(UUID developerId, UUID keyId) {
        return chargerPossedee(developerId, keyId)
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

    private Mono<ApiKeyEntity> chargerPossedee(UUID developerId, UUID keyId) {
        return repository.findById(keyId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Clé introuvable : " + keyId)))
                .filter(entity -> entity.getDeveloperId().equals(developerId))
                .switchIfEmpty(Mono.error(ProblemException.notFound("Clé introuvable : " + keyId)));
    }

    private static String jeton(int octets) {
        byte[] bytes = new byte[octets];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
