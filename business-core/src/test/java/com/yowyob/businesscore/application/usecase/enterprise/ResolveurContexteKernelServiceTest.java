package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.configuration.ConfigurationService;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolveurContexteKernelServiceTest {

    private final LireEntreprise lireEntreprise = mock(LireEntreprise.class);
    private final DepotEntreprise depotEntreprise = mock(DepotEntreprise.class);
    private final PersisterEntreprise persisterEntreprise = mock(PersisterEntreprise.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);

    private final ResolveurContexteKernelService resolveur = new ResolveurContexteKernelService(
            lireEntreprise, depotEntreprise, persisterEntreprise, configurationService);

    private final UUID businessId = UUID.randomUUID();
    private final UUID versionTypeId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID businessActorId = UUID.randomUUID();
    private final UUID cashierId = UUID.randomUUID();

    private Entreprise entreprise(UUID agencyId) {
        return new Entreprise(businessId, UUID.randomUUID(), UUID.randomUUID(), versionTypeId,
                1, orgId, businessActorId, agencyId, "Pharma Yaoundé", CycleVie.ACTIVE);
    }

    private BusinessContext contexte() {
        return new BusinessContext(UUID.randomUUID(), cashierId, Set.of(), businessId, "trace", Locale.FRENCH);
    }

    @Test
    void agence_memorisee_utilisee_sans_appel_kernel() {
        UUID agencyId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        when(lireEntreprise.parId(businessId)).thenReturn(Mono.just(entreprise(agencyId)));
        when(configurationService.resoudreValeur(businessId, versionTypeId, "devise"))
                .thenReturn(Mono.just("EUR"));
        when(configurationService.resoudreValeur(businessId, versionTypeId, "caisse_principale"))
                .thenReturn(Mono.just(registerId.toString()));

        StepVerifier.create(resolveur.resoudre(businessId)
                        .contextWrite(c -> BusinessContextHolder.withContext(c, contexte())))
                .assertNext(ck -> {
                    assertThat(ck.organizationId()).isEqualTo(orgId);
                    assertThat(ck.agencyId()).isEqualTo(agencyId);
                    assertThat(ck.businessActorId()).isEqualTo(businessActorId);
                    assertThat(ck.registerId()).isEqualTo(registerId);
                    assertThat(ck.cashierId()).isEqualTo(cashierId);
                    assertThat(ck.currency()).isEqualTo("EUR");
                })
                .verifyComplete();

        verify(persisterEntreprise, never()).trouverAgencePrincipale(any());
    }

    @Test
    void agence_absente_resolue_et_memorisee_devise_par_defaut() {
        UUID agence = UUID.randomUUID();
        when(lireEntreprise.parId(businessId)).thenReturn(Mono.just(entreprise(null)));
        when(persisterEntreprise.trouverAgencePrincipale(orgId)).thenReturn(Mono.just(agence));
        when(depotEntreprise.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(configurationService.resoudreValeur(eq(businessId), eq(versionTypeId), eq("devise")))
                .thenReturn(Mono.error(ProblemException.notFound("absent")));
        when(configurationService.resoudreValeur(eq(businessId), eq(versionTypeId), eq("caisse_principale")))
                .thenReturn(Mono.empty());

        StepVerifier.create(resolveur.resoudre(businessId)
                        .contextWrite(c -> BusinessContextHolder.withContext(c, contexte())))
                .assertNext(ck -> {
                    assertThat(ck.agencyId()).isEqualTo(agence);
                    assertThat(ck.currency()).isEqualTo("XAF");
                    assertThat(ck.registerId()).isNull();
                    assertThat(ck.cashierId()).isEqualTo(cashierId);
                })
                .verifyComplete();

        verify(depotEntreprise).sauvegarder(any());
    }
}
