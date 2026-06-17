package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import com.yowyob.businesscore.domain.port.out.PersisterTypeMetier;
import com.yowyob.businesscore.domain.shared.StatutType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter R2DBC — implémente PersisterTypeMetier.
 * Traduit TypeMetier (domaine) ↔ TypeMetierEntity (R2DBC).
 * Le domaine ne connaît pas cet adapter.
 */
@Component
public class TypeMetierPersistenceAdapter implements PersisterTypeMetier {

    private final TypeMetierRepository repository;

    public TypeMetierPersistenceAdapter(TypeMetierRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<TypeMetier> sauvegarder(TypeMetier type) {
        TypeMetierEntity entity = versEntity(type);
        return repository.save(entity).map(this::versDomaine);
    }

    @Override
    public Mono<TypeMetier> trouverParId(UUID id) {
        return repository.findById(id).map(this::versDomaine);
    }

    @Override
    public Mono<TypeMetier> trouverParCode(String code) {
        return repository.findByCode(code).map(this::versDomaine);
    }

    @Override
    public Flux<TypeMetier> listerParTenant(UUID tenantId) {
        // RLS garantit déjà l'isolation — findAll ne retourne que les lignes du tenant courant
        return repository.findAll().map(this::versDomaine);
    }

    @Override
    public Mono<Boolean> existeParCodeEtTenant(String code, UUID tenantId) {
        return repository.findByCode(code).map(e -> true).defaultIfEmpty(false);
    }

    // ─── Mapping domaine ↔ entity ─────────────────────────────────────────

    private TypeMetierEntity versEntity(TypeMetier type) {
        TypeMetierEntity e = TypeMetierEntity.nouveau(
                type.id(),
                type.tenantId(),
                type.businessDomainId(),
                type.code(),
                type.nom(),
                type.statut().name()
        );
        return e;
    }

    private TypeMetier versDomaine(TypeMetierEntity e) {
        return new TypeMetier(
                e.getId(),
                e.getTenantId(),
                e.getBusinessDomainId(),
                e.getCode(),
                e.getNom(),
                StatutType.valueOf(e.getStatut())
        );
    }
}
