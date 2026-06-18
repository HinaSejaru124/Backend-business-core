package com.yowyob.businesscore.adapter.out.persistence.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ActeurPersistenceAdapter implements DepotActeur {

    private final RoleMetierRepository roleRepo;
    private final ActeurMetierRepository acteurRepo;

    public ActeurPersistenceAdapter(RoleMetierRepository roleRepo, ActeurMetierRepository acteurRepo) {
        this.roleRepo = roleRepo;
        this.acteurRepo = acteurRepo;
    }

    @Override
    public Mono<RoleMetier> enregistrerRole(RoleMetier role) {
        return BusinessContextHolder.currentContext().flatMap(ctx -> {
            RoleMetierEntity entity = RoleMetierEntity.nouveau(
                    role.id(), ctx.tenantId(), role.versionTypeId(),
                    role.code(), role.categorie().name());
            return roleRepo.save(entity).map(ActeurPersistenceAdapter::versRole);
        });
    }

    @Override
    public Mono<RoleMetier> roleParId(UUID id) {
        return roleRepo.findById(id).map(ActeurPersistenceAdapter::versRole);
    }

    @Override
    public Mono<ActeurMetier> enregistrerActeur(ActeurMetier acteur) {
        return BusinessContextHolder.currentContext().flatMap(ctx ->
                acteurRepo.existsById(acteur.id()).flatMap(existe -> {
                    ActeurMetierEntity entity = ActeurMetierEntity.nouveau(
                            acteur.id(), ctx.tenantId(), acteur.entrepriseId(), acteur.roleMetierId(),
                            acteur.acteurKernelId(), acteur.valideDepuis(), acteur.valideJusqua());
                    if (existe) {
                        entity.enModification();
                    }
                    return acteurRepo.save(entity).map(ActeurPersistenceAdapter::versActeur);
                }));
    }

    @Override
    public Mono<ActeurMetier> acteurParId(UUID id) {
        return acteurRepo.findById(id).map(ActeurPersistenceAdapter::versActeur);
    }

    @Override
    public Flux<ActeurMetier> acteursParEntreprise(UUID entrepriseId) {
        return acteurRepo.findByEntrepriseId(entrepriseId).map(ActeurPersistenceAdapter::versActeur);
    }

    static RoleMetier versRole(RoleMetierEntity e) {
        return new RoleMetier(e.getId(), e.getVersionTypeId(), e.getCode(),
                CategorieActeur.valueOf(e.getCategorie()));
    }

    static ActeurMetier versActeur(ActeurMetierEntity e) {
        return new ActeurMetier(e.getId(), e.getEntrepriseId(), e.getRoleMetierId(),
                e.getActeurKernelId(), e.getValideDepuis(), e.getValideJusqua());
    }
}