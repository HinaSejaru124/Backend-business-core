package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import com.yowyob.businesscore.domain.enterprise.EntrepriseProfil;
import com.yowyob.businesscore.domain.enterprise.EnvironnementApplication;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseProfil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adapter R2DBC — implémente {@link DepotEntrepriseProfil}. RLS isole le tenant. */
@Component
public class EntrepriseProfilPersistenceAdapter implements DepotEntrepriseProfil {

    private final EntrepriseProfilRepository repository;

    public EntrepriseProfilPersistenceAdapter(EntrepriseProfilRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<EntrepriseProfil> sauvegarder(EntrepriseProfil profil) {
        return repository.findById(profil.entrepriseId())
                .map(existant -> appliquer(existant, profil))
                .switchIfEmpty(Mono.fromSupplier(() -> versEntityNouveau(profil)))
                .flatMap(repository::save)
                .map(this::versDomaine);
    }

    @Override
    public Mono<EntrepriseProfil> trouverParEntreprise(UUID entrepriseId) {
        return repository.findById(entrepriseId).map(this::versDomaine);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private EntrepriseProfilEntity versEntityNouveau(EntrepriseProfil p) {
        return EntrepriseProfilEntity.nouveau(p.entrepriseId(), p.tenantId(), p.description(),
                p.logoUrl(), p.couleur(), p.supportEmail(), p.siteWebUrl(),
                p.environnement().name(), p.mettreAJourLe());
    }

    private EntrepriseProfilEntity appliquer(EntrepriseProfilEntity e, EntrepriseProfil p) {
        e.setDescription(p.description());
        e.setLogoUrl(p.logoUrl());
        e.setCouleur(p.couleur());
        e.setSupportEmail(p.supportEmail());
        e.setSiteWebUrl(p.siteWebUrl());
        e.setEnvironnement(p.environnement().name());
        e.setMisAJourLe(p.mettreAJourLe());
        return e;
    }

    private EntrepriseProfil versDomaine(EntrepriseProfilEntity e) {
        return new EntrepriseProfil(e.getEntrepriseId(), e.getTenantId(), e.getDescription(),
                e.getLogoUrl(), e.getCouleur(), e.getSupportEmail(), e.getSiteWebUrl(),
                EnvironnementApplication.valueOf(e.getEnvironnement()), e.getMisAJourLe());
    }
}
