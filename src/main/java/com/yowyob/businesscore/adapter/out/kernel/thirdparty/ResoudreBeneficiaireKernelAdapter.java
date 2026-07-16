package com.yowyob.businesscore.adapter.out.kernel.thirdparty;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.actor.NomPersonne;
import com.yowyob.businesscore.domain.port.out.ResoudreBeneficiaire;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Bénéficiaire (externe) → tiers kernel. Un tiers n'est <b>pas autonome</b> : on crée d'abord l'actor
 * support ({@code POST /api/actors}), puis on le déclare comme tiers CLIENT
 * ({@code POST /api/third-parties}, {@code partyType=ACTOR}) sous l'organisation. Core DIFFÉRENT d'un
 * opérateur (RG-04).
 */
@Component
public class ResoudreBeneficiaireKernelAdapter implements ResoudreBeneficiaire {

    private final KernelClient kernel;

    public ResoudreBeneficiaireKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<UUID> resoudreBeneficiaire(UUID organizationId, String identifiant, String nom) {
        NomPersonne nomPersonne = NomPersonne.de(nom);
        CreerActeurRequest acteur = new CreerActeurRequest(
                organizationId, nomPersonne.prenom(), nomPersonne.nomFamille(), nom);
        return kernel.post("/api/actors", acteur, KernelId.class)
                .flatMap(party -> {
                    CreerTiersRequest tiers = new CreerTiersRequest(
                            organizationId, "ACTOR", party.id(), codeTiers(identifiant), nom,
                            List.of("CUSTOMER"));
                    return kernel.post("/api/third-parties", tiers, KernelId.class);
                })
                .map(kernelId -> kernelId.id());
    }

    private static String codeTiers(String identifiant) {
        String base = identifiant == null ? "" : identifiant.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (base.length() > 16) {
            base = base.substring(0, 16);
        }
        if (base.isBlank()) {
            base = "TIERS";
        }
        return "CLI-" + base;
    }
}

/** Corps de création d'un actor (mappe {@code CreateActorRequest}). */
record CreerActeurRequest(UUID organizationId, String firstName, String lastName, String name) {
}

/** Corps de déclaration d'un tiers (mappe {@code CreateThirdPartyRequest}). */
record CreerTiersRequest(UUID organizationId, String partyType, UUID partyId, String code,
                         String name, List<String> roles) {
}
