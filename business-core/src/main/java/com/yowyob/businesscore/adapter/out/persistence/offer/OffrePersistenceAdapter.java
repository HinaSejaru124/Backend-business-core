package com.yowyob.businesscore.adapter.out.persistence.offer;

import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.out.offer.DepotOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import com.yowyob.businesscore.shared.context.BusinessContext;
import com.yowyob.businesscore.shared.context.BusinessContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class OffrePersistenceAdapter implements DepotOffre {

    private final DefinitionOffreRepository offreRepo;
    private final CapaciteRepository capaciteRepo;

    public OffrePersistenceAdapter(DefinitionOffreRepository offreRepo, CapaciteRepository capaciteRepo) {
        this.offreRepo = offreRepo;
        this.capaciteRepo = capaciteRepo;
    }

    @Override
    public Mono<DefinitionOffre> enregistrer(DefinitionOffre offre) {
        return BusinessContextHolder.current().flatMap(ctx -> {
            DefinitionOffreEntity entity = DefinitionOffreEntity.nouveau(
                    offre.id(), ctx.tenantId(), offre.versionTypeId(),
                    offre.nom(), offre.formePrix().name(), offre.prix());
            return offreRepo.save(entity)
                    .then(enregistrerCapacites(offre, ctx))
                    .thenReturn(offre);
        });
    }

    private Mono<Void> enregistrerCapacites(DefinitionOffre offre, BusinessContext ctx) {
        return Flux.fromIterable(offre.capacites())
                .map(c -> CapaciteEntity.nouveau(
                        c.id(), ctx.tenantId(), c.definitionOffreId(), c.type().name(), c.active()))
                .flatMap(capaciteRepo::save)
                .then();
    }

    @Override
    public Flux<DefinitionOffre> parVersionType(UUID versionTypeId) {
        return offreRepo.findByVersionTypeId(versionTypeId).flatMap(this::recharger);
    }

    @Override
    public Mono<DefinitionOffre> parId(UUID id) {
        return offreRepo.findById(id).flatMap(this::recharger);
    }

    private Mono<DefinitionOffre> recharger(DefinitionOffreEntity e) {
        return capaciteRepo.findByDefinitionOffreId(e.getId())
                .map(OffrePersistenceAdapter::versCapacite)
                .collectList()
                .map(caps -> versDomaine(e, caps));
    }

    static DefinitionOffre versDomaine(DefinitionOffreEntity e, List<Capacite> capacites) {
        return new DefinitionOffre(e.getId(), e.getVersionTypeId(), e.getNom(),
                FormePrix.valueOf(e.getFormePrix()), e.getPrix(), capacites);
    }

    static Capacite versCapacite(CapaciteEntity c) {
        return new Capacite(c.getId(), c.getDefinitionOffreId(),
                TypeCapacite.valueOf(c.getType()), c.isActive());
    }
}