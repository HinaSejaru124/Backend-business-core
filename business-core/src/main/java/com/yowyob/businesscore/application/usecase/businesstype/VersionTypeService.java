package com.yowyob.businesscore.application.usecase.businesstype;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.port.internal.HorlogeSysteme;
import com.yowyob.businesscore.domain.port.out.PersisterTypeMetier;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use Case — Gestion des Versions de Type Métier.
 *
 * Règle RG-03 : une version publiée est immuable.
 * Pour "modifier" un type publié → créer une nouvelle version.
 */
@Service
public class VersionTypeService {

    private final PersisterVersionType depotVersion;
    private final PersisterTypeMetier  depotType;
    private final HorlogeSysteme       horloge;

    public VersionTypeService(PersisterVersionType depotVersion,
                              PersisterTypeMetier depotType,
                              HorlogeSysteme horloge) {
        this.depotVersion = depotVersion;
        this.depotType    = depotType;
        this.horloge      = horloge;
    }

    // ─── Créer une nouvelle version ───────────────────────────────────────

    /**
     * Crée une nouvelle version pour un TypeMetier existant.
     *
     * Règles :
     * - Le TypeMetier doit être PUBLIE (verifierPeutVersionner)
     * - Le numéro est auto-incrémenté (dernierNumero + 1)
     */
    public Mono<VersionType> creerVersion(UUID typeId, BusinessContext ctx) {
        return depotType.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .flatMap(type -> {
                    // Vérifier appartenance et statut
                    type.verifierAppartenance(ctx.tenantId());
                    type.verifierPeutVersionner();

                    // Calculer le prochain numéro
                    return depotVersion.dernierNumero(typeId)
                            .map(dernierNum -> dernierNum + 1)
                            .flatMap(prochaineNum -> {
                                VersionType version = VersionType.creer(
                                        typeId, ctx.tenantId(), prochaineNum);
                                return depotVersion.sauvegarder(version);
                            });
                });
    }

    // ─── Publier une version ──────────────────────────────────────────────

    /**
     * Publie une version : la rend immuable et l'horodate.
     * Après cette opération, RG-03 s'applique — aucune modification possible.
     */
    public Mono<VersionType> publierVersion(UUID typeId, UUID versionId,
                                            BusinessContext ctx) {
        return depotType.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .flatMap(type -> {
                    type.verifierAppartenance(ctx.tenantId());
                    return depotVersion.trouverParId(versionId);
                })
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Version introuvable : " + versionId)))
                .flatMap(version -> {
                    version.verifierAppartenance(ctx.tenantId());
                    // Appliquer la transition — lance erreur si déjà immuable
                    VersionType versionPubliee = version.publier(horloge.maintenant());
                    return depotVersion.sauvegarder(versionPubliee);
                });
    }

    // ─── Lire ─────────────────────────────────────────────────────────────

    /** Liste toutes les versions d'un TypeMetier. */
    public Flux<VersionType> lister(UUID typeId, BusinessContext ctx) {
        return depotType.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .doOnNext(type -> type.verifierAppartenance(ctx.tenantId()))
                .thenMany(depotVersion.listerParTypeMetier(typeId));
    }

    /** Récupère une version par son ID. */
    public Mono<VersionType> trouverParId(UUID versionId, BusinessContext ctx) {
        return depotVersion.trouverParId(versionId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Version introuvable : " + versionId)))
                .doOnNext(v -> v.verifierAppartenance(ctx.tenantId()));
    }
}
