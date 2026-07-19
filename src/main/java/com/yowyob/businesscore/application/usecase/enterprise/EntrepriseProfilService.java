package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.domain.enterprise.EntrepriseProfil;
import com.yowyob.businesscore.domain.enterprise.EnvironnementApplication;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseProfil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case — fiche produit d'une Entreprise (Application) : description, logo, couleur, support,
 * site web, environnement. Créée vierge à la création de l'entreprise ({@link EntrepriseService#creer})
 * et modifiée ensuite par le développeur.
 */
@Service
public class EntrepriseProfilService {

    private final EntrepriseService entrepriseService;
    private final DepotEntrepriseProfil depotProfil;

    public EntrepriseProfilService(EntrepriseService entrepriseService, DepotEntrepriseProfil depotProfil) {
        this.entrepriseService = entrepriseService;
        this.depotProfil = depotProfil;
    }

    /** Crée la fiche produit vierge associée à une entreprise qui vient d'être créée. */
    public Mono<EntrepriseProfil> initialiser(UUID entrepriseId, UUID tenantId) {
        return depotProfil.sauvegarder(EntrepriseProfil.vierge(entrepriseId, tenantId, Instant.now()));
    }

    public Mono<EntrepriseProfil> trouver(UUID entrepriseId, BusinessContext ctx) {
        return entrepriseService.trouver(entrepriseId, ctx)
                .flatMap(entreprise -> depotProfil.trouverParEntreprise(entreprise.id())
                        .switchIfEmpty(depotProfil.sauvegarder(
                                EntrepriseProfil.vierge(entreprise.id(), ctx.tenantId(), Instant.now()))));
    }

    public Mono<EntrepriseProfil> modifier(UUID entrepriseId, String description, String logoUrl,
                                           String couleur, String supportEmail, String siteWebUrl,
                                           EnvironnementApplication environnement, BusinessContext ctx) {
        return trouver(entrepriseId, ctx)
                .map(profil -> profil.avecInformations(description, logoUrl, couleur,
                        supportEmail, siteWebUrl, environnement, Instant.now()))
                .flatMap(depotProfil::sauvegarder);
    }
}
