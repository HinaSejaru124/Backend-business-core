package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseProfil;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    @Mock DeveloperAccountRepository developerRepository;
    @Mock PlanCatalogue catalogue;

    EntrepriseService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TYPE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ORG_ID = UUID.randomUUID();
    private static final UUID AGENCY_ID = UUID.randomUUID();
    private static final UUID DEVELOPER_ID = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT_ID, null, Set.of(), null, "trace", Locale.FRENCH);

    @BeforeEach
    void setUp() {
        OrganisationProvisioningService provisioning = new OrganisationProvisioningService(persisterEntreprise);
        service = new EntrepriseService(depotEntreprise, persisterVersionType, persisterEntreprise,
                journaliserSync, depotEntrepriseContrat, depotEntrepriseProfil, developerRepository, catalogue,
                provisioning);
        lenient().when(journaliserSync.journaliser(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        // verifierLimiteApplications (fail-open) : illimité par défaut dans ces tests, hors de propos ici.
        lenient().when(catalogue.applicationsMax(any())).thenReturn(-1L);
        lenient().when(depotEntreprise.listerParTenant(TENANT_ID)).thenReturn(Flux.empty());
    }

    private static DeveloperAccountEntity developpeurAvecEntreprise(UUID organizationId) {
        DeveloperAccountEntity dev = DeveloperAccountEntity.nouveau(
                DEVELOPER_ID, "dev@example.com", TENANT_ID, "kid", null, null, "FREE");
        dev.setEntrepriseOrganizationId(organizationId);
        return dev;
    }

    @Test
    @DisplayName("l'application se rattache à l'organisation de l'entreprise du développeur, sans en créer")
    void rattache_organisation_de_l_entreprise_du_developpeur() {
        VersionType version = VersionType.creer(TYPE_ID, TENANT_ID, 1);
        when(developerRepository.findByKernelTenantId(TENANT_ID))
                .thenReturn(Mono.just(developpeurAvecEntreprise(ORG_ID)));
        when(persisterVersionType.trouverParTypeEtNumero(TYPE_ID, 1)).thenReturn(Mono.just(version));
        when(persisterEntreprise.resoudreBusinessActorCourant("Pharma Test")).thenReturn(Mono.just(ACTOR_ID));
        when(persisterEntreprise.trouverAgencePrincipale(ORG_ID)).thenReturn(Mono.just(AGENCY_ID));
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

        // Aucune organisation créée : ni creerOrganisation, ni approbation, ni souscription de service
        // (non stubbés — un appel réel aurait fait échouer le test avec un Mono null).
    }

    @Test
    @DisplayName("aucune entreprise choisie : refus explicite (ENTREPRISE_NON_DEFINIE), aucun appel kernel")
    void refuse_si_entreprise_non_definie() {
        when(developerRepository.findByKernelTenantId(TENANT_ID))
                .thenReturn(Mono.just(developpeurAvecEntreprise(null)));

        StepVerifier.create(service.creer(TYPE_ID, 1, "Pharma Test", ctx))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(422);
                })
                .verify();

        verifyNoMoreInteractions(persisterEntreprise);
    }
}
