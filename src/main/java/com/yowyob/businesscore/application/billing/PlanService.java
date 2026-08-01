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

    /** Statut d'une demande de changement de plan dont le paiement n'est pas encore finalisé. */
    private static final String STATUT_EN_ATTENTE = PaiementPort.ResultatPaiement.Statut.EN_ATTENTE.name();

    /** Montant minimal accepté par la passerelle mobile money : en dessous, elle refuse la transaction. */
    private static final long MONTANT_MINIMUM = 100L;

    public Mono<ResultatUpgrade> changer(UUID developerId, String planCibleBrut, String payerReference,
                                         String modeBrut) {
        if (planCibleBrut == null || planCibleBrut.isBlank()) {
            return Mono.error(ProblemException.badRequest("Le plan cible est obligatoire.")
                    .violatedRule("PLAN_CIBLE_MANQUANT"));
        }
        if (!catalogue.existe(planCibleBrut)) {
            return Mono.error(ProblemException.badRequest("Plan inconnu : " + planCibleBrut)
                    .violatedRule("PLAN_INCONNU"));
        }
        // Le numéro du payeur n'a de sens qu'en mobile money : par carte, le porteur saisit ses
        // coordonnées sur la page sécurisée de la passerelle, jamais chez nous.
        PaiementPort.ModePaiement mode = PaiementPort.ModePaiement.depuis(modeBrut);
        if (mode.exigePayerReference() && (payerReference == null || payerReference.isBlank())) {
            return Mono.error(ProblemException.badRequest("Le numéro mobile money du payeur est obligatoire.")
                    .violatedRule("PAYER_REFERENCE_MANQUANT"));
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
                    // Garde-fou sur le montant. La passerelle mobile money refuse les micro-montants
                    // (minimum 100 XAF) et répond par un 400 opaque : un forfait payant dont le prix
                    // n'a pas été fixé par l'administrateur produisait donc une erreur incompréhensible
                    // pour le développeur. On échoue ici, avec un message qui désigne la vraie cause.
                    long montant = catalogue.prixMensuel(cible);
                    if (montant < MONTANT_MINIMUM) {
                        return Mono.error(ProblemException.unprocessable(
                                "Le forfait " + cible + " n'a pas de tarif valide (" + montant + " "
                                        + catalogue.devise(cible) + "). L'administrateur de la plateforme doit "
                                        + "lui attribuer un prix d'au moins " + MONTANT_MINIMUM + " "
                                        + catalogue.devise(cible) + " avant qu'il puisse être souscrit.")
                                .violatedRule("TARIF_FORFAIT_INVALIDE"));
                    }
                    PaiementPort.DemandePaiement demande = new PaiementPort.DemandePaiement(developerId, actuel,
                            cible, catalogue.prixMensuel(cible), catalogue.devise(cible),
                            payerReference == null ? null : payerReference.trim(), mode);
                    return paiement.demanderPaiement(demande)
                            .flatMap(resultat -> appliquer(account, actuel, cible, resultat));
                });
    }

    /**
     * Finalise le dernier paiement {@code EN_ATTENTE} du développeur : interroge le kernel pour l'issue
     * réelle et n'active le plan que sur {@code CONFIRME}. Idempotent : rappeler quand le paiement est
     * toujours en cours renvoie {@code EN_ATTENTE} sans effet de bord.
     */
    public Mono<ResultatUpgrade> finaliser(UUID developerId) {
        return changeRepository.findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(developerId, STATUT_EN_ATTENTE)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Aucun paiement en attente à finaliser.")
                        .violatedRule("AUCUN_PAIEMENT_EN_ATTENTE")))
                .flatMap(demande -> paiement.verifierStatut(demande.getPaymentReference())
                        .flatMap(resultat -> finaliserSelonStatut(developerId, demande, resultat)));
    }

    private Mono<ResultatUpgrade> finaliserSelonStatut(UUID developerId, PlanChangeRequestEntity demande,
                                                       PaiementPort.ResultatPaiement resultat) {
        String cible = demande.getPlanTo();
        return switch (resultat.statut()) {
            case CONFIRME -> developerRepository.findById(developerId)
                    .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable.")))
                    .flatMap(account -> {
                        account.setPlan(cible);
                        demande.marquerIssue("CONFIRME");
                        return developerRepository.save(account)
                                .then(changeRepository.save(demande))
                                .thenReturn(new ResultatUpgrade(cible, "CONFIRME", null,
                                        demande.getPaymentReference()));
                    });
            case EN_ATTENTE -> Mono.just(new ResultatUpgrade(demande.getPlanFrom(), "EN_ATTENTE", null,
                    demande.getPaymentReference()));
            case REFUSE -> {
                demande.marquerIssue("REFUSE");
                yield changeRepository.save(demande).then(Mono.error(
                        ProblemException.unprocessable("Le paiement du changement de plan a été refusé.")
                                .violatedRule("PAIEMENT_REFUSE")));
            }
        };
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
