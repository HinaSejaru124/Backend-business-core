package com.yowyob.businesscore.application.usecase.businesstype;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.port.internal.HorlogeSysteme;
import com.yowyob.businesscore.domain.port.out.PersisterTypeMetier;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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
                    return creerProchaineVersion(typeId, ctx.tenantId());
                });
    }

    /**
     * Calcule le prochain numéro (dernier + 1) puis persiste. Si deux créations concurrentes
     * tombent sur le même numéro, la contrainte unique (type, numéro) rejette l'une d'elles
     * ({@link DataIntegrityViolationException}) : on recalcule et on réessaie (jusqu'à 3 fois).
     */
    private Mono<VersionType> creerProchaineVersion(UUID typeId, UUID tenantId) {
        return depotVersion.dernierNumero(typeId)
                .map(dernierNum -> dernierNum + 1)
                .flatMap(prochaineNum -> depotVersion.sauvegarder(
                        VersionType.creer(typeId, tenantId, prochaineNum)))
                .retryWhen(Retry.max(3)
                        .filter(e -> e instanceof DataIntegrityViolationException));
    }

    // ─── Publier une version ──────────────────────────────────────────────

    /**
     * Publie une version : la rend immuable et l'horodate.
     * Après cette opération, RG-03 s'applique — aucune modification possible.
     */
    public Mono<VersionType> publierVersion(UUID typeId, int numero,
                                            BusinessContext ctx) {
        return depotType.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .flatMap(type -> {
                    type.verifierAppartenance(ctx.tenantId());
                    return depotVersion.trouverParTypeEtNumero(typeId, numero);
                })
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Version " + numero + " introuvable pour le type " + typeId)))
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

    /** Récupère une version par (type, numéro) — adressage REST par numéro. */
    public Mono<VersionType> trouverParNumero(UUID typeId, int numero, BusinessContext ctx) {
        return depotVersion.trouverParTypeEtNumero(typeId, numero)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Version " + numero + " introuvable pour le type " + typeId)))
                .doOnNext(v -> v.verifierAppartenance(ctx.tenantId()));
    }
}
