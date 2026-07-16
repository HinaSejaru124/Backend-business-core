package com.yowyob.businesscore.application.billing;

import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestEntity;
import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.out.PaiementPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Orchestration d'un changement de plan : validation → paiement (via {@link PaiementPort}) → activation.
 *
 * <p>Chaque tentative est tracée dans {@code plan_change_request}. En cas de paiement confirmé (adapter de
 * simulation aujourd'hui, Kernel demain), le plan du compte est mis à jour et prend effet immédiatement :
 * les requêtes suivantes ré-authentifient et voient le nouveau quota (déblocage automatique). En cas de
 * paiement EN_ATTENTE, le plan reste inchangé jusqu'à confirmation ; en cas de refus, une erreur métier.
 */
@Service
public class PlanService {

    private final DeveloperAccountRepository developerRepository;
    private final PlanChangeRequestRepository changeRepository;
    private final PlanCatalogue catalogue;
    private final PaiementPort paiement;

    public PlanService(DeveloperAccountRepository developerRepository,
                       PlanChangeRequestRepository changeRepository,
                       PlanCatalogue catalogue,
                       PaiementPort paiement) {
        this.developerRepository = developerRepository;
        this.changeRepository = changeRepository;
        this.catalogue = catalogue;
        this.paiement = paiement;
    }

    /** Issue d'un changement de plan renvoyée au client. */
    public record ResultatUpgrade(String plan, String statut, String urlPaiement, String reference) {
    }

    public Mono<ResultatUpgrade> changer(UUID developerId, String planCibleBrut) {
        if (planCibleBrut == null || planCibleBrut.isBlank()) {
            return Mono.error(ProblemException.badRequest("Le plan cible est obligatoire.")
                    .violatedRule("PLAN_CIBLE_MANQUANT"));
        }
        if (!catalogue.existe(planCibleBrut)) {
            return Mono.error(ProblemException.badRequest("Plan inconnu : " + planCibleBrut)
                    .violatedRule("PLAN_INCONNU"));
        }
        String cible = catalogue.normaliser(planCibleBrut);

        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable.")))
                .flatMap(account -> {
                    String actuel = catalogue.normaliser(account.getPlan());
                    if (cible.equals(actuel)) {
                        return Mono.error(ProblemException.conflict("Vous êtes déjà sur le plan " + cible + ".")
                                .violatedRule("PLAN_INCHANGE"));
                    }
                    PaiementPort.DemandePaiement demande = new PaiementPort.DemandePaiement(
                            developerId, actuel, cible, catalogue.prixMensuel(cible), catalogue.devise(cible));
                    return paiement.demanderPaiement(demande)
                            .flatMap(resultat -> appliquer(account, actuel, cible, resultat));
                });
    }

    private Mono<ResultatUpgrade> appliquer(DeveloperAccountEntity account, String actuel, String cible,
                                            PaiementPort.ResultatPaiement resultat) {
        PlanChangeRequestEntity audit = PlanChangeRequestEntity.nouveau(
                account.getId(), actuel, cible, resultat.statut().name(), resultat.reference());
        Mono<Void> tracer = changeRepository.save(audit).then();

        return switch (resultat.statut()) {
            case CONFIRME -> {
                account.setPlan(cible);
                yield tracer.then(developerRepository.save(account))
                        .thenReturn(new ResultatUpgrade(cible, "CONFIRME", null, resultat.reference()));
            }
            case EN_ATTENTE -> tracer.thenReturn(
                    new ResultatUpgrade(actuel, "EN_ATTENTE", resultat.urlPaiement(), resultat.reference()));
            case REFUSE -> tracer.then(Mono.error(
                    ProblemException.unprocessable("Le paiement du changement de plan a été refusé.")
                            .violatedRule("PAIEMENT_REFUSE")));
        };
    }
}
