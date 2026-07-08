package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Résout les codes de rôle métier <b>actifs</b> d'un acteur kernel au sein d'une entreprise.
 *
 * <p>C'est le pont entre la brique Acteurs (qui déclare {@code ActeurMetier} + {@code RoleMetier}) et
 * l'exécution d'opérations : les rôles ainsi résolus alimentent le contexte d'exécution (rôle
 * déclencheur d'une opération, effet DEROGER d'une règle). Renvoie un ensemble vide si aucun acteur
 * actif ne correspond.
 */
@Service
public class ResoudreRolesMetier {

    private final DepotActeur depotActeur;

    public ResoudreRolesMetier(DepotActeur depotActeur) {
        this.depotActeur = depotActeur;
    }

    public Mono<Set<String>> rolesActifs(UUID entrepriseId, UUID acteurKernelId) {
        if (entrepriseId == null || acteurKernelId == null) {
            return Mono.just(Set.of());
        }
        return depotActeur.acteursParEntreprise(entrepriseId)
                .filter(acteur -> acteur.estActif() && acteurKernelId.equals(acteur.acteurKernelId()))
                .flatMap(acteur -> depotActeur.roleParId(acteur.roleMetierId()))
                .map(role -> role.code())
                .collect(Collectors.toUnmodifiableSet());
    }
}
