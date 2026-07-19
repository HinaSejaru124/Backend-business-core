package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.domain.enterprise.EntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseContrat;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case — contrat technique d'une Entreprise (application cliente) : URLs de callback/succès/erreur/
 * annulation, clé publique. Le contrat est créé vierge à la création de l'entreprise
 * ({@link EntrepriseService#creer}) et modifié ensuite par le développeur.
 */
@Service
public class EntrepriseContratService {

    private final EntrepriseService entrepriseService;
    private final DepotEntrepriseContrat depotContrat;

    public EntrepriseContratService(EntrepriseService entrepriseService, DepotEntrepriseContrat depotContrat) {
        this.entrepriseService = entrepriseService;
        this.depotContrat = depotContrat;
    }

    /** Crée le contrat vierge associé à une entreprise qui vient d'être créée. */
    public Mono<EntrepriseContrat> initialiser(UUID entrepriseId, UUID tenantId) {
        return depotContrat.sauvegarder(EntrepriseContrat.vierge(entrepriseId, tenantId, Instant.now()));
    }

    public Mono<EntrepriseContrat> trouver(UUID entrepriseId, BusinessContext ctx) {
        return entrepriseService.trouver(entrepriseId, ctx)
                .flatMap(entreprise -> depotContrat.trouverParEntreprise(entreprise.id())
                        .switchIfEmpty(depotContrat.sauvegarder(
                                EntrepriseContrat.vierge(entreprise.id(), ctx.tenantId(), Instant.now()))));
    }

    public Mono<EntrepriseContrat> modifier(UUID entrepriseId, String callbackUrl, String successUrl,
                                            String errorUrl, String cancelUrl, BusinessContext ctx) {
        return trouver(entrepriseId, ctx)
                .map(contrat -> contrat.avecParametres(callbackUrl, successUrl, errorUrl, cancelUrl, Instant.now()))
                .flatMap(depotContrat::sauvegarder);
    }
}
