package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.out.OrganisationAccessible;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Thème « Entreprise » : un développeur travaille pour <b>une seule</b> entreprise (= une organisation
 * kernel), à l'intérieur de laquelle il crée ses applications (logiciels). Ce use case résout, à chaque
 * connexion, l'état de cette association et pilote les deux façons de la fixer :
 * <ul>
 *   <li>{@link #provisionner} — créer une organisation kernel toute neuve (nouvelle inscription, ou
 *       développeur existant qui n'en a aucune) ;</li>
 *   <li>{@link #selectionner} — rattacher une organisation kernel déjà possédée par le développeur
 *       (développeurs déjà existants avant ce thème, qui en ont souvent plusieurs).</li>
 * </ul>
 * Les deux restent appelables à tout moment (choix modifiable, cf. « changer d'entreprise »).
 */
@Service
public class EnterpriseSelectionService {

    private final DeveloperAccountRepository developerRepository;
    private final PersisterEntreprise persisterEntreprise;
    private final OrganisationProvisioningService provisioning;

    public EnterpriseSelectionService(DeveloperAccountRepository developerRepository,
                                      PersisterEntreprise persisterEntreprise,
                                      OrganisationProvisioningService provisioning) {
        this.developerRepository = developerRepository;
        this.persisterEntreprise = persisterEntreprise;
        this.provisioning = provisioning;
    }

    /** État d'association développeur↔entreprise, tel que renvoyé au client. */
    public record Statut(String etat, String nom, UUID organizationId, List<OrganisationAccessible> options) {
        static Statut definie(String nom, UUID organizationId) {
            return new Statut("DEFINIE", nom, organizationId, List.of());
        }

        static Statut choixRequis(List<OrganisationAccessible> options) {
            return new Statut("CHOIX_REQUIS", null, null, options);
        }

        static Statut nomRequis() {
            return new Statut("NOM_REQUIS", null, null, List.of());
        }
    }

    /**
     * Résout l'état courant. Si le nom de l'entreprise a été fixé à l'inscription mais que
     * l'organisation kernel n'existe pas encore, la provisionne maintenant (premier appel authentifié
     * après vérification d'email — l'onboarding kernel exige un compte déjà vérifié, indisponible à
     * l'inscription elle-même).
     */
    public Mono<Statut> status(BusinessContext ctx) {
        return trouverDeveloppeur(ctx).flatMap(dev -> {
            if (dev.getEntrepriseOrganizationId() != null) {
                return Mono.just(Statut.definie(dev.getEntrepriseNom(), dev.getEntrepriseOrganizationId()));
            }
            String nom = dev.getEntrepriseNom();
            if (nom != null && !nom.isBlank()) {
                return provisionnerEtPersister(dev, nom);
            }
            return persisterEntreprise.listerOrganisations()
                    .map(options -> options.isEmpty() ? Statut.nomRequis() : Statut.choixRequis(options));
        });
    }

    /** Rattache une organisation kernel déjà possédée (développeur existant, choix parmi ses organisations). */
    public Mono<Statut> selectionner(BusinessContext ctx, UUID organizationId, String nom) {
        return trouverDeveloppeur(ctx).flatMap(dev -> {
            dev.setEntrepriseOrganizationId(organizationId);
            dev.setEntrepriseNom(nom);
            return developerRepository.save(dev);
        }).map(saved -> Statut.definie(saved.getEntrepriseNom(), saved.getEntrepriseOrganizationId()));
    }

    /** Crée une organisation kernel toute neuve pour le nom donné, et la fixe comme entreprise du développeur. */
    public Mono<Statut> provisionner(BusinessContext ctx, String nom) {
        return trouverDeveloppeur(ctx).flatMap(dev -> provisionnerEtPersister(dev, nom));
    }

    private Mono<Statut> provisionnerEtPersister(DeveloperAccountEntity dev, String nom) {
        return provisioning.provisionner(nom)
                .flatMap(refs -> {
                    dev.setEntrepriseOrganizationId(refs.organizationId());
                    dev.setEntrepriseNom(nom);
                    return developerRepository.save(dev);
                })
                .map(saved -> Statut.definie(saved.getEntrepriseNom(), saved.getEntrepriseOrganizationId()));
    }

    private Mono<DeveloperAccountEntity> trouverDeveloppeur(BusinessContext ctx) {
        return developerRepository.findByKernelTenantId(ctx.tenantId())
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable.")));
    }
}
