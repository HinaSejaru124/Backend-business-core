package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseProfil;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import com.yowyob.businesscore.domain.port.out.OrganisationProvisionnee;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock PersisterVersionType persisterVersionType;
    @Mock PersisterEntreprise persisterEntreprise;
    @Mock DepotEntreprise depotEntreprise;
    @Mock JournaliserChangementSync journaliserSync;
    @Mock DepotEntrepriseContrat depotEntrepriseContrat;
    @Mock DepotEntrepriseProfil depotEntrepriseProfil;

    EntrepriseService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TYPE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID AGENCY_ID = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT_ID, null, Set.of(), null, "trace", Locale.FRENCH);

    @BeforeEach
    void setUp() {
        service = new EntrepriseService(depotEntreprise, persisterVersionType, persisterEntreprise,
                journaliserSync, depotEntrepriseContrat, depotEntrepriseProfil);
        when(journaliserSync.journaliser(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("provisionnement : actor → org → approve → services → agence")
    void provisionne_dans_l_ordre_kernel() {
        VersionType version = VersionType.creer(TYPE_ID, TENANT_ID, 1);
        when(persisterVersionType.trouverParTypeEtNumero(TYPE_ID, 1)).thenReturn(Mono.just(version));
        when(persisterEntreprise.creerOrganisation("Pharma Test"))
                .thenReturn(Mono.just(new OrganisationProvisionnee(ACTOR_ID, ORG_ID)));
        when(persisterEntreprise.approuverOrganisation(ORG_ID, "Approbation initiale"))
                .thenReturn(Mono.empty());
        when(persisterEntreprise.souscrireServices(ORG_ID)).thenReturn(Mono.empty());
        when(persisterEntreprise.creerAgence(ORG_ID, "Pharma Test — agence principale"))
                .thenReturn(Mono.just(AGENCY_ID));
        when(depotEntreprise.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(depotEntrepriseContrat.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(depotEntrepriseProfil.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.creer(TYPE_ID, 1, "Pharma Test", ctx))
                .assertNext(ent -> {
                    assertThat(ent.organizationId()).isEqualTo(ORG_ID);
                    assertThat(ent.businessActorId()).isEqualTo(ACTOR_ID);
                    assertThat(ent.agencyId()).isEqualTo(AGENCY_ID);
                })
                .verifyComplete();

        InOrder ordre = inOrder(persisterEntreprise);
        ordre.verify(persisterEntreprise).creerOrganisation("Pharma Test");
        ordre.verify(persisterEntreprise).approuverOrganisation(ORG_ID, "Approbation initiale");
        ordre.verify(persisterEntreprise).souscrireServices(ORG_ID);
        ordre.verify(persisterEntreprise).creerAgence(eq(ORG_ID), eq("Pharma Test — agence principale"));
        verifyNoMoreInteractions(persisterEntreprise);
    }

    @Test
    @DisplayName("organizationId fourni : rattachement à une organisation existante, aucune création")
    void rattache_organisation_existante_sans_provisionnement() {
        VersionType version = VersionType.creer(TYPE_ID, TENANT_ID, 1);
        when(persisterVersionType.trouverParTypeEtNumero(TYPE_ID, 1)).thenReturn(Mono.just(version));
        when(persisterEntreprise.resoudreBusinessActorCourant("Pharma Test")).thenReturn(Mono.just(ACTOR_ID));
        when(persisterEntreprise.trouverAgencePrincipale(ORG_ID)).thenReturn(Mono.just(AGENCY_ID));
        when(depotEntreprise.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(depotEntrepriseContrat.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(depotEntrepriseProfil.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.creer(TYPE_ID, 1, "Pharma Test", ORG_ID, ctx))
                .assertNext(ent -> {
                    assertThat(ent.organizationId()).isEqualTo(ORG_ID);
                    assertThat(ent.businessActorId()).isEqualTo(ACTOR_ID);
                    assertThat(ent.agencyId()).isEqualTo(AGENCY_ID);
                })
                .verifyComplete();

        verifyNoMoreInteractions(persisterEntreprise);
    }
}
