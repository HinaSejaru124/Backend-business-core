package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.AppliquerRoleTechnique;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.port.out.RattacherAOrganisation;
import com.yowyob.businesscore.domain.port.out.ResoudreBeneficiaire;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import com.yowyob.businesscore.domain.port.out.SignUpResult;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GestionActeurServiceTest {

    private final DepotActeur depot = mock(DepotActeur.class);
    private final LireEntreprise lireEntreprise = mock(LireEntreprise.class);
    private final ResoudreBeneficiaire resoudreBeneficiaire = mock(ResoudreBeneficiaire.class);
    private final AppliquerRoleTechnique appliquerRoleTechnique = mock(AppliquerRoleTechnique.class);
    private final RattacherAOrganisation rattacherAOrganisation = mock(RattacherAOrganisation.class);
    private final AuthentifierUtilisateur authentifier = mock(AuthentifierUtilisateur.class);
    private final PersisterVersionType persisterVersionType = mock(PersisterVersionType.class);

    private final GestionActeurService service = new GestionActeurService(
            depot, lireEntreprise, resoudreBeneficiaire, appliquerRoleTechnique, rattacherAOrganisation,
            authentifier, persisterVersionType);

    {
        when(persisterVersionType.trouverParId(any()))
                .thenAnswer(inv -> Mono.just(VersionType.creer(UUID.randomUUID(), UUID.randomUUID(), 1)));
    }

    private final UUID businessId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    private void stubCommun() {
        Entreprise entreprise = new Entreprise(
                businessId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, orgId, null, null, "Pharma Yaoundé", CycleVie.ACTIVE);
        when(lireEntreprise.parId(businessId)).thenReturn(Mono.just(entreprise));
        when(rattacherAOrganisation.rattacher(any(), any())).thenReturn(Mono.empty());
        when(depot.enregistrerActeur(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(depot.acteursParEntreprise(businessId)).thenReturn(Flux.empty());
    }

    private RoleMetier roleOperateur() {
        return RoleMetier.nouveau(roleId, UUID.randomUUID(), "PHARMACIEN", CategorieActeur.OPERATEUR);
    }

    @Test
    @DisplayName("rattacher : OPERATEUR avec acteurKernelId déjà connu → rattaché directement, aucune résolution")
    void rattacher_operateur_avec_acteur_kernel_id_connu() {
        stubCommun();
        UUID acteurKernelId = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));
        when(appliquerRoleTechnique.appliquer(eq(acteurKernelId), eq("PHARMACIEN"))).thenReturn(Mono.empty());

        StepVerifier.create(service.rattacher(
                        new GestionActeurService.RattacherActeurCommande(businessId, roleId, acteurKernelId, null)))
                .assertNext(a -> assertThat(a.acteurKernelId()).isEqualTo(acteurKernelId))
                .verifyComplete();

        verify(appliquerRoleTechnique).appliquer(acteurKernelId, "PHARMACIEN");
        // RG-04 : étanchéité — le chemin bénéficiaire ne doit jamais être emprunté.
        verify(resoudreBeneficiaire, never()).resoudreBeneficiaire(any(), any(), any());
    }

    @Test
    @DisplayName("rattacher : OPERATEUR sans acteurKernelId → 400, Business Core ne résout plus d'identité")
    void rattacher_operateur_sans_acteur_kernel_id_refuse() {
        stubCommun();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));

        StepVerifier.create(service.rattacher(
                        new GestionActeurService.RattacherActeurCommande(businessId, roleId, null, null)))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(400))
                .verify();

        verifyNoInteractions(appliquerRoleTechnique);
        verify(depot, never()).enregistrerActeur(any());
    }

    @Test
    void beneficiaire_passe_par_third_parties_sans_role_technique() {
        stubCommun();
        UUID tiersId = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(
                RoleMetier.nouveau(roleId, UUID.randomUUID(), "CLIENT", CategorieActeur.BENEFICIAIRE)));
        when(resoudreBeneficiaire.resoudreBeneficiaire(orgId, "marie", "marie")).thenReturn(Mono.just(tiersId));

        StepVerifier.create(service.rattacher(
                        new GestionActeurService.RattacherActeurCommande(businessId, roleId, null, "marie")))
                .assertNext(a -> assertThat(a.acteurKernelId()).isEqualTo(tiersId))
                .verifyComplete();

        verify(resoudreBeneficiaire).resoudreBeneficiaire(orgId, "marie", "marie");
        // RG-04 : un bénéficiaire ne touche jamais le rôle technique.
        verify(appliquerRoleTechnique, never()).appliquer(any(), any());
    }

    @Test
    @DisplayName("rattacher : BENEFICIAIRE sans identifiantPersonne → 400")
    void rattacher_beneficiaire_sans_identifiant_refuse() {
        stubCommun();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(
                RoleMetier.nouveau(roleId, UUID.randomUUID(), "CLIENT", CategorieActeur.BENEFICIAIRE)));

        StepVerifier.create(service.rattacher(
                        new GestionActeurService.RattacherActeurCommande(businessId, roleId, null, "  ")))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(400))
                .verify();

        verifyNoInteractions(resoudreBeneficiaire);
    }

    private GestionActeurService.InscrireActeurCommande commandeInscription() {
        return new GestionActeurService.InscrireActeurCommande(
                businessId, roleId, "jean@pharma.cm", "pw", "Jean", "Kamdem");
    }

    @Test
    @DisplayName(
            "inscrire : identifiants valides d'un compte Yow déjà existant → réutilisé tel quel, aucun sign-up")
    void inscrire_reutilise_un_compte_yow_existant_sans_signup() {
        stubCommun();
        UUID actorIdExistant = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));
        // La personne a déjà un compte kernel (développeur existant, ou déjà acteur d'une autre
        // entreprise) : le login avec les identifiants fournis réussit du premier coup.
        when(authentifier.login("jean@pharma.cm", "pw")).thenReturn(Mono.just(
                new ResultatLogin("jwt", 900, List.of(), List.of(), null, actorIdExistant.toString())));
        when(appliquerRoleTechnique.appliquer(eq(actorIdExistant), eq("PHARMACIEN"))).thenReturn(Mono.empty());

        StepVerifier.create(service.inscrire(commandeInscription()))
                .assertNext(a -> assertThat(a.acteurKernelId()).isEqualTo(actorIdExistant))
                .verifyComplete();

        // Un seul compte Yow : pas de second compte créé pour rattacher cette entreprise.
        verify(authentifier, never()).signUp(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "inscrire : personne réellement nouvelle → sign-up puis connexion pour récupérer l'actorId lié")
    void inscrire_cree_le_compte_pour_une_personne_nouvelle() {
        stubCommun();
        UUID actorIdApresSignUp = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));
        // Rien n'existe encore : le premier login échoue.
        when(authentifier.login("jean@pharma.cm", "pw"))
                .thenReturn(Mono.error(ProblemException.forbidden("Identifiants invalides")))
                .thenReturn(Mono.just(new ResultatLogin(
                        "jwt", 900, List.of(), List.of(), null, actorIdApresSignUp.toString())));
        when(authentifier.signUp("jean@pharma.cm", "pw", "Jean", "Kamdem"))
                .thenReturn(Mono.just(new SignUpResult("kernel-id", "tenant-id", "PENDING", "ok")));
        when(appliquerRoleTechnique.appliquer(eq(actorIdApresSignUp), eq("PHARMACIEN"))).thenReturn(Mono.empty());

        StepVerifier.create(service.inscrire(commandeInscription()))
                .assertNext(a -> assertThat(a.acteurKernelId()).isEqualTo(actorIdApresSignUp))
                .verifyComplete();

        verify(authentifier).signUp("jean@pharma.cm", "pw", "Jean", "Kamdem");
    }

    @Test
    @DisplayName(
            "inscrire : vérification d'email requise après sign-up → erreur claire, aucune identité de repli")
    void inscrire_erreur_claire_si_verification_email_requise() {
        stubCommun();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));
        // Ni avant ni après le sign-up le login n'aboutit (vérification d'email en attente).
        when(authentifier.login("jean@pharma.cm", "pw"))
                .thenReturn(Mono.error(ProblemException.forbidden("Compte non vérifié")));
        when(authentifier.signUp("jean@pharma.cm", "pw", "Jean", "Kamdem"))
                .thenReturn(Mono.just(new SignUpResult("kernel-id", "tenant-id", "PENDING", "ok")));

        StepVerifier.create(service.inscrire(commandeInscription()))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(502))
                .verify();

        verify(authentifier).signUp("jean@pharma.cm", "pw", "Jean", "Kamdem");
        verify(depot, never()).enregistrerActeur(any());
    }

    @Test
    @DisplayName("inscrire : déjà rattaché activement à cette entreprise → 409")
    void inscrire_refuse_un_doublon_de_rattachement_actif() {
        stubCommun();
        UUID actorIdExistant = UUID.randomUUID();
        ActeurMetier dejaLa = new ActeurMetier(
                UUID.randomUUID(), businessId, roleId, actorIdExistant, Instant.now(), null);
        when(depot.acteursParEntreprise(businessId)).thenReturn(Flux.just(dejaLa));
        when(depot.roleParId(roleId)).thenReturn(Mono.just(roleOperateur()));
        when(authentifier.login("jean@pharma.cm", "pw")).thenReturn(Mono.just(
                new ResultatLogin("jwt", 900, List.of(), List.of(), null, actorIdExistant.toString())));
        when(appliquerRoleTechnique.appliquer(eq(actorIdExistant), eq("PHARMACIEN"))).thenReturn(Mono.empty());

        StepVerifier.create(service.inscrire(commandeInscription()))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(409))
                .verify();

        verify(depot, never()).enregistrerActeur(any());
    }

    @Test
    void inscrire_refuse_un_role_beneficiaire() {
        RoleMetier beneficiaire = RoleMetier.nouveau(roleId, UUID.randomUUID(), "CLIENT", CategorieActeur.BENEFICIAIRE);
        when(lireEntreprise.parId(businessId)).thenReturn(Mono.just(new Entreprise(
                businessId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, orgId, null, null, "Pharma Yaoundé", CycleVie.ACTIVE)));
        when(depot.roleParId(roleId)).thenReturn(Mono.just(beneficiaire));

        StepVerifier.create(service.inscrire(commandeInscription()))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(422))
                .verify();

        verifyNoInteractions(authentifier);
    }

    @Test
    @DisplayName("declarerRole : version publiée → 409 RG-03, rôle jamais enregistré")
    void declarerRole_sur_version_publiee_est_rejete_RG03() {
        UUID versionTypeId = UUID.randomUUID();
        VersionType publiee = VersionType.creer(UUID.randomUUID(), UUID.randomUUID(), 1)
                .publier(Instant.now());
        when(persisterVersionType.trouverParId(versionTypeId)).thenReturn(Mono.just(publiee));

        StepVerifier.create(service.declarerRole(
                        new GestionActeurService.DeclarerRoleCommande(versionTypeId, "CAISSIER", CategorieActeur.OPERATEUR)))
                .expectErrorSatisfies(ex -> {
                    assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(409);
                    assertThat(((ProblemException) ex).getExtensions().get("violatedRule")).isEqualTo("RG-03");
                })
                .verify();

        verify(depot, never()).enregistrerRole(any());
    }
}
