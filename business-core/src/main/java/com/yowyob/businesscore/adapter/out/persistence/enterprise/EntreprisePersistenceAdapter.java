package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.operation.spi.EntrepriseResolue;
import com.yowyob.businesscore.domain.operation.spi.ResoudreEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter R2DBC — implémente {@link DepotEntreprise} (persistance locale), {@link ResoudreEntreprise}
 * (point de contact pour la feature Opérations) et {@link LireEntreprise} (lecture cross-feature, brique
 * Acteurs). RLS isole le tenant.
 */
@Component
public class EntreprisePersistenceAdapter implements DepotEntreprise, ResoudreEntreprise, LireEntreprise {

    private final EntrepriseRepository repository;

    public EntreprisePersistenceAdapter(EntrepriseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Entreprise> sauvegarder(Entreprise entreprise) {
        return repository.findById(entreprise.id())
                .map(existant -> appliquer(existant, entreprise))
                .switchIfEmpty(Mono.fromSupplier(() -> versEntityNouveau(entreprise)))
                .flatMap(repository::save)
                .map(this::versDomaine);
    }

    @Override
    public Mono<Entreprise> trouverParId(UUID id) {
        return repository.findById(id).map(this::versDomaine);
    }

    @Override
    public Flux<Entreprise> listerParTenant(UUID tenantId) {
        return repository.findAll().map(this::versDomaine);
    }

    @Override
    public Mono<EntrepriseResolue> resoudre(UUID entrepriseId) {
        return repository.findById(entrepriseId)
                .map(e -> new EntrepriseResolue(e.getId(), e.getVersionTypeId(), e.getOrganizationId()));
    }

    @Override
    public Mono<Entreprise> parId(UUID id) {
        return repository.findById(id).map(this::versDomaine);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private EntrepriseEntity versEntityNouveau(Entreprise e) {
        return EntrepriseEntity.nouveau(
                e.id(), e.tenantId(), e.typeMetierId(), e.versionTypeId(),
                e.numeroVersion(), e.organizationId(), e.nom(), e.cycleVie().name());
    }

    private EntrepriseEntity appliquer(EntrepriseEntity entity, Entreprise e) {
        entity.setNom(e.nom());
        entity.setCycleVie(e.cycleVie().name());
        entity.setOrganizationId(e.organizationId());
        return entity;
    }

    private Entreprise versDomaine(EntrepriseEntity e) {
        return new Entreprise(
                e.getId(), e.getTenantId(), e.getTypeMetierId(), e.getVersionTypeId(),
                e.getNumeroVersion(), e.getOrganizationId(), e.getNom(),
                CycleVie.valueOf(e.getCycleVie()));
    }
}
