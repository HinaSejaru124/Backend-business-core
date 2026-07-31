package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import com.yowyob.businesscore.domain.port.in.RegistrationUseCase;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Inscription d'un développeur :
 * <ol>
 *   <li>crée le compte sur le kernel (sign-up) — le kernel fait autorité sur l'identité et le tenant ;</li>
 *   <li>crée le compte BC local en mémorisant le {@code kernel_tenant_id} (le tenant métier) et l'email.</li>
 * </ol>
 * Aucune clé API n'est émise ici : les clés sont désormais scopées à une entreprise, qui n'existe pas
 * encore au moment de l'inscription. Le dev vérifie son email, se connecte via {@code POST /v1/auth/login}
 * (JWT — son identifiant stable est exposé par {@code GET /v1/auth/me}), crée une entreprise via
 * {@code POST /v1/businesses}, puis émet une clé API pour cette entreprise via {@code POST /v1/api-keys}.
 */
@Service
public class RegistrationService implements RegistrationUseCase {

    private final DeveloperAccountRepository repository;
    private final AuthentifierUtilisateur authentifier;

    public RegistrationService(DeveloperAccountRepository repository,
                               AuthentifierUtilisateur authentifier) {
        this.repository = repository;
        this.authentifier = authentifier;
    }

    @Override
    public Mono<ApiKeyEmise> inscrire(String firstName, String lastName,
                                      String email, String password,
                                      String entreprise) {
        // Tout nouveau compte démarre sur le plan gratuit, sans exception. Le plan ne peut être changé
        // que par PlanService.changer/finaliser, qui n'active un plan payant que sur un paiement
        // réellement CONFIRME par la passerelle. Auparavant l'inscription reprenait un `planCode` fourni
        // par le client : n'importe qui pouvait s'octroyer PRO ou ENTERPRISE gratuitement en le
        // choisissant dans le formulaire (ou en forgeant la requête). Le champ a été retiré du contrat
        // REST ; ne jamais le réintroduire ici — un plan payant ne s'obtient que par paiement.
        String plan = PlanCatalogue.PLAN_DEFAUT;
        String entrepriseNettoyee = entreprise == null ? null : entreprise.trim();

        return repository.findByEmail(email)
                .flatMap(existant -> Mono.<ApiKeyEmise>error(ProblemException.conflict(
                        "Un compte existe déjà avec l'adresse " + email + ". Connectez-vous plutôt.")
                        .violatedRule("EMAIL_DEJA_UTILISE")))
                .switchIfEmpty(Mono.defer(() -> authentifier.signUp(email, password, firstName, lastName)
                        .flatMap(signUpResult -> {
                            DeveloperAccountEntity entity = DeveloperAccountEntity.nouveau(
                                    UUID.randomUUID(),
                                    email,
                                    parseUuid(signUpResult.tenantId()),
                                    signUpResult.id(),
                                    null,
                                    null,
                                    plan);
                            // Nom de l'entreprise mémorisé dès l'inscription ; l'organisation kernel elle-même
                            // est provisionnée au premier appel authentifié (POST /v1/enterprise/status),
                            // l'onboarding kernel exigeant un compte déjà vérifié.
                            entity.setEntrepriseNom(entrepriseNettoyee);
                            return repository.save(entity)
                                    .onErrorMap(DuplicateKeyException.class, ex -> ProblemException.conflict(
                                            "Un compte existe déjà avec l'adresse " + email + ". Connectez-vous plutôt.")
                                            .violatedRule("EMAIL_DEJA_UTILISE"))
                                    .thenReturn(signUpResult);
                        })
                        // L'identifiant de connexion est une adresse @yowyob.com générée par la plateforme
                        // d'identité : l'e-mail personnel ne permet PAS de se connecter (il sert à la
                        // vérification et à la récupération). Le développeur ne peut pas le deviner, il
                        // faut donc le lui donner explicitement — sinon il reste bloqué au login.
                        .map(signUpResult -> new ApiKeyEmise(plan,
                                messageBienvenue(signUpResult.identifiantConnexion()),
                                signUpResult.identifiantConnexion()))));
    }

    private static String messageBienvenue(String identifiantConnexion) {
        String base = "Compte créé. Vérifiez votre e-mail pour activer le compte";
        if (identifiantConnexion == null || identifiantConnexion.isBlank()) {
            return base + ", puis connectez-vous et créez votre première application.";
        }
        return base + ", puis connectez-vous avec l'identifiant " + identifiantConnexion
                + " et le mot de passe choisi. Conservez cet identifiant : votre adresse personnelle ne "
                + "permet pas de vous connecter.";
    }

    private static UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
