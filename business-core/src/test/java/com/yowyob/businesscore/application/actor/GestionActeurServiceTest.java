package com.yowyob.businesscore.application.actor;

import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur.RattacherActeurCommande;
import com.yowyob.businesscore.domain.port.out.actor.*;
import com.yowyob.businesscore.domain.port.out.enterprise.LireEntreprise;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GestionActeurServiceTest {

    private final DepotActeur depot = mock(DepotActeur.class);
    private final LireEntreprise lireEntreprise = mock(LireEntreprise.class);
    private final ResoudrePersonne resoudrePersonne = mock(ResoudrePersonne.class);
    private final ResoudreBeneficiaire resoudreBeneficiaire = mock(ResoudreBeneficiaire.class);
    private final AppliquerRoleTechnique appliquerRoleTechnique = mock(AppliquerRoleTechnique.class);
    private final RattacherAOrganisation rattacherAOrganisation = mock(RattacherAOrganisation.class);

    private final GestionActeurService service = new GestionActeurService(
            depot, lireEntreprise, resoudrePersonne, resoudreBeneficiaire,
            appliquerRoleTechnique, rattacherAOrganisation);

    private final UUID businessId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    private void stubCommun() {
        Entreprise entreprise = new Entreprise(
                businessId, UUID.randomUUID(), orgId, "Pharma Yaoundé", CycleVie.ACTIVE);
        when(lireEntreprise.parId(businessId)).thenReturn(Mono.just(entreprise));
        when(rattacherAOrganisation.rattacher(any(), any())).thenReturn(Mono.empty());
        when(depot.enregistrerActeur(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void operateur_passe_par_actor_core_et_recoit_un_role_technique() {
        stubCommun();
        UUID actorKernelId = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(
                RoleMetier.nouveau(roleId, UUID.randomUUID(), "PHARMACIEN", CategorieActeur.OPERATEUR)));
        when(resoudrePersonne.resoudreOperateur("jean")).thenReturn(Mono.just(actorKernelId));
        when(appliquerRoleTechnique.appliquer(eq("PHARMACIEN"), eq(actorKernelId)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.rattacher(
                        new RattacherActeurCommande(businessId, roleId, "jean")))
                .assertNext(a -> { assert a.acteurKernelId().equals(actorKernelId); })
                .verifyComplete();

        verify(resoudrePersonne).resoudreOperateur("jean");
        verify(appliquerRoleTechnique).appliquer("PHARMACIEN", actorKernelId);
        // RG-04 : étanchéité — le chemin bénéficiaire ne doit jamais être emprunté.
        verify(resoudreBeneficiaire, never()).resoudreBeneficiaire(any());
    }

    @Test
    void beneficiaire_passe_par_third_parties_sans_role_technique() {
        stubCommun();
        UUID tiersId = UUID.randomUUID();
        when(depot.roleParId(roleId)).thenReturn(Mono.just(
                RoleMetier.nouveau(roleId, UUID.randomUUID(), "CLIENT", CategorieActeur.BENEFICIAIRE)));
        when(resoudreBeneficiaire.resoudreBeneficiaire("marie")).thenReturn(Mono.just(tiersId));

        StepVerifier.create(service.rattacher(
                        new RattacherActeurCommande(businessId, roleId, "marie")))
                .assertNext(a -> { assert a.acteurKernelId().equals(tiersId); })
                .verifyComplete();

        verify(resoudreBeneficiaire).resoudreBeneficiaire("marie");
        // RG-04 : un bénéficiaire ne touche jamais l'actor-core ni le rôle technique.
        verify(resoudrePersonne, never()).resoudreOperateur(any());
        verify(appliquerRoleTechnique, never()).appliquer(any(), any());
    }
}
