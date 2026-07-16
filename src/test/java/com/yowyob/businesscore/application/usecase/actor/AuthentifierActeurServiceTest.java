package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import com.yowyob.businesscore.application.error.ProblemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthentifierActeurServiceTest {

    @Mock AuthentifierUtilisateur authentifier;
    @Mock DepotActeur depotActeur;

    private final UUID businessId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    private AuthentifierActeurService service() {
        return new AuthentifierActeurService(authentifier, depotActeur);
    }

    private ResultatLogin login(UUID acteurKernelId) {
        return new ResultatLogin("jwt-acteur", 900, List.of("SELL"), List.of(), null,
                acteurKernelId == null ? null : acteurKernelId.toString());
    }

    @Test
    @DisplayName("connecter : acteur rattaché et actif → session + acteur + rôle résolus")
    void connecter_acteur_rattache_resout_le_contexte() {
        UUID acteurKernelId = UUID.randomUUID();
        UUID acteurId = UUID.randomUUID();
        ActeurMetier acteur = new ActeurMetier(acteurId, businessId, roleId, acteurKernelId, Instant.now(), null);
        RoleMetier role = RoleMetier.nouveau(roleId, UUID.randomUUID(), "PHARMACIEN", CategorieActeur.OPERATEUR);

        when(authentifier.login("jean@pharma.cm", "pw")).thenReturn(Mono.just(login(acteurKernelId)));
        when(depotActeur.acteursParEntreprise(businessId)).thenReturn(Flux.just(acteur));
        when(depotActeur.roleParId(roleId)).thenReturn(Mono.just(role));

        StepVerifier.create(service().connecter(businessId, "jean@pharma.cm", "pw"))
                .assertNext(c -> {
                    assertThat(c.session().accessToken()).isEqualTo("jwt-acteur");
                    assertThat(c.acteur()).isEqualTo(acteur);
                    assertThat(c.role().code()).isEqualTo("PHARMACIEN");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("connecter : identifiants kernel valides mais acteur non rattaché à cette entreprise → 403")
    void connecter_acteur_non_rattache_refuse() {
        when(authentifier.login("intrus@x.co", "pw")).thenReturn(Mono.just(login(UUID.randomUUID())));
        when(depotActeur.acteursParEntreprise(businessId)).thenReturn(Flux.empty());

        StepVerifier.create(service().connecter(businessId, "intrus@x.co", "pw"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(403);
                })
                .verify();
    }

    @Test
    @DisplayName("connecter : acteur détaché (valideJusqua renseigné) est ignoré comme s'il n'existait pas")
    void connecter_acteur_detache_refuse() {
        UUID acteurKernelId = UUID.randomUUID();
        ActeurMetier detache = new ActeurMetier(
                UUID.randomUUID(), businessId, roleId, acteurKernelId, Instant.now().minusSeconds(3600), Instant.now());

        when(authentifier.login("ancien@x.co", "pw")).thenReturn(Mono.just(login(acteurKernelId)));
        when(depotActeur.acteursParEntreprise(businessId)).thenReturn(Flux.just(detache));

        StepVerifier.create(service().connecter(businessId, "ancien@x.co", "pw"))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(403))
                .verify();
    }

    @Test
    @DisplayName("connecter : kernel ne renvoie aucun actorId exploitable → 403")
    void connecter_sans_actor_id_kernel_refuse() {
        when(authentifier.login("x@x.co", "pw")).thenReturn(Mono.just(login(null)));

        StepVerifier.create(service().connecter(businessId, "x@x.co", "pw"))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(403))
                .verify();
    }

    @Test
    @DisplayName("moi : sans claim acteur kernel (JWT développeur) → 403 explicite, aucun appel au dépôt")
    void moi_sans_actor_id_refuse() {
        StepVerifier.create(service().moi(businessId, null))
                .expectErrorSatisfies(ex -> assertThat(((ProblemException) ex).getStatus().value()).isEqualTo(403))
                .verify();
    }

    @Test
    @DisplayName("moi : acteur actif rattaché → contexte résolu sans appel kernel")
    void moi_resout_le_contexte_sans_kernel() {
        UUID acteurKernelId = UUID.randomUUID();
        ActeurMetier acteur = new ActeurMetier(
                UUID.randomUUID(), businessId, roleId, acteurKernelId, Instant.now(), null);
        RoleMetier role = RoleMetier.nouveau(roleId, UUID.randomUUID(), "CAISSIER", CategorieActeur.OPERATEUR);

        when(depotActeur.acteursParEntreprise(businessId)).thenReturn(Flux.just(acteur));
        when(depotActeur.roleParId(roleId)).thenReturn(Mono.just(role));

        StepVerifier.create(service().moi(businessId, acteurKernelId))
                .assertNext(c -> {
                    assertThat(c.session()).isNull();
                    assertThat(c.role().code()).isEqualTo("CAISSIER");
                })
                .verifyComplete();

        org.mockito.Mockito.verifyNoInteractions(authentifier);
    }
}
