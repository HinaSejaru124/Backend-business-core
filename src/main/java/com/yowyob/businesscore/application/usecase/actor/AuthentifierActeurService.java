package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Authentification d'un <b>acteur métier</b> (pharmacien, caissier...) — pas un développeur.
 *
 * <p>KCore répond à « qui es-tu ? » : {@link AuthentifierUtilisateur#login} délègue la vérification
 * du principal/mot de passe au kernel, exactement comme pour un développeur ({@code AuthentificationService})
 * — aucune identité ni mot de passe n'est jamais stocké ici. Business Core répond à « que peux-tu faire
 * dans ce Business ? » : une fois le JWT obtenu, on résout l'{@link ActeurMetier} (et son
 * {@link RoleMetier}) déjà déclaré via {@code POST /v1/businesses/{id}/actors} — cette brique existe
 * et n'est pas dupliquée ici, seulement consultée par {@code acteurKernelId}.
 *
 * <p>Un acteur kernel valide mais non rattaché à l'entreprise ciblée (jamais rattaché, ou détaché) est
 * refusé : le JWT prouve l'identité, pas le droit d'agir pour ce Business précis.
 */
@Service
public class AuthentifierActeurService {

    private final AuthentifierUtilisateur authentifier;
    private final DepotActeur depotActeur;

    public AuthentifierActeurService(AuthentifierUtilisateur authentifier, DepotActeur depotActeur) {
        this.authentifier = authentifier;
        this.depotActeur = depotActeur;
    }

    public record ActeurConnecte(ResultatLogin session, ActeurMetier acteur, RoleMetier role) {
    }

    public Mono<ActeurConnecte> connecter(UUID businessId, String principal, String motDePasse) {
        return authentifier.login(principal, motDePasse)
                .flatMap(session -> resoudreActeurActif(businessId, parseUuid(session.actorId()))
                        .flatMap(acteur -> depotActeur.roleParId(acteur.roleMetierId())
                                .map(role -> new ActeurConnecte(session, acteur, role))));
    }

    /** Résout l'acteur métier courant (profil « moi ») à partir de l'identité kernel portée par le JWT. */
    public Mono<ActeurConnecte> moi(UUID businessId, UUID acteurKernelId) {
        if (acteurKernelId == null) {
            return Mono.error(ProblemException.forbidden(
                    "Ce jeton ne porte pas d'identité acteur (claim kernel `actor` absent)."));
        }
        return resoudreActeurActif(businessId, acteurKernelId)
                .flatMap(acteur -> depotActeur.roleParId(acteur.roleMetierId())
                        .map(role -> new ActeurConnecte(null, acteur, role)));
    }

    private Mono<ActeurMetier> resoudreActeurActif(UUID businessId, UUID acteurKernelId) {
        if (acteurKernelId == null) {
            return Mono.error(ProblemException.forbidden(
                    "Le kernel n'a pas renvoyé d'identifiant acteur exploitable."));
        }
        return depotActeur.acteursParEntreprise(businessId)
                .filter(acteur -> acteur.estActif() && acteurKernelId.equals(acteur.acteurKernelId()))
                .next()
                .switchIfEmpty(Mono.error(ProblemException.forbidden(
                        "Cet utilisateur n'est pas rattaché à cette application.")
                        .violatedRule("ACTEUR_NON_RATTACHE")));
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
