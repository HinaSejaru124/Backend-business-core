package com.yowyob.businesscore.application.usecase.businesstype;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import com.yowyob.businesscore.domain.port.out.PersisterTypeMetier;
import com.yowyob.businesscore.domain.port.out.ResoudreBusinessDomain;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use Case — Gestion des Types Métier.
 *
 * Orchestre : création, publication, archivage.
 * Ne connaît ni R2DBC, ni REST, ni kernel directement —
 * tout passe par les ports de sortie.
 */
@Service
public class TypeMetierService {

    private final PersisterTypeMetier depot;
    private final ResoudreBusinessDomain resoudreBusinessDomain;

    public TypeMetierService(PersisterTypeMetier depot,
                             ResoudreBusinessDomain resoudreBusinessDomain) {
        this.depot = depot;
        this.resoudreBusinessDomain = resoudreBusinessDomain;
    }

    // ─── Créer ────────────────────────────────────────────────────────────

    /**
     * Crée un nouveau TypeMetier en statut BROUILLON.
     *
     * Vérifie l'unicité du code dans le tenant avant de sauvegarder.
     * Si domainCode est fourni, résout ou crée le BusinessDomain dans le kernel.
     */
    public Mono<TypeMetier> creer(String code, String nom,
                                  String domainCode, String domainNom,
                                  BusinessContext ctx) {

        // 1. Vérifier unicité du code dans ce tenant
        return depot.existeParCodeEtTenant(code, ctx.tenantId())
                .flatMap(existe -> {
                    if (existe) {
                        return Mono.error(ProblemException.conflict(
                            "Un Type Métier avec le code '" + code +
                            "' existe déjà dans votre tenant."
                        ).violatedRule("CODE_UNIQUE_PAR_TENANT"));
                    }

                    // 2. Résoudre le BusinessDomain kernel si fourni
                    if (domainCode != null && !domainCode.isBlank()) {
                        return resoudreBusinessDomain
                                .resoudreOuCreer(domainCode, domainNom)
                                .flatMap(domainId -> sauvegarder(code, nom, domainId, ctx));
                    }

                    // 3. Pas de domaine → créer sans businessDomainId
                    return sauvegarder(code, nom, null, ctx);
                });
    }

    private Mono<TypeMetier> sauvegarder(String code, String nom,
                                         UUID businessDomainId, BusinessContext ctx) {
        TypeMetier type = TypeMetier.creer(ctx.tenantId(), code, nom, businessDomainId);
        return depot.sauvegarder(type);
    }

    // ─── Publier ──────────────────────────────────────────────────────────

    /**
     * Publie un TypeMetier : BROUILLON → PUBLIE.
     * Après publication, le type est utilisable pour créer des Entreprises.
     */
    public Mono<TypeMetier> publier(UUID typeId, BusinessContext ctx) {
        return depot.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .flatMap(type -> {
                    // Vérifier appartenance au tenant
                    type.verifierAppartenance(ctx.tenantId());
                    // Appliquer la transition (lance une exception si statut incorrect)
                    TypeMetier typePublie = type.publier();
                    return depot.sauvegarder(typePublie);
                });
    }

    // ─── Archiver ─────────────────────────────────────────────────────────

    /**
     * Archive un TypeMetier : PUBLIE → ARCHIVE.
     * Les Entreprises existantes restent fonctionnelles sur leur version épinglée.
     */
    public Mono<TypeMetier> archiver(UUID typeId, BusinessContext ctx) {
        return depot.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .flatMap(type -> {
                    type.verifierAppartenance(ctx.tenantId());
                    TypeMetier typeArchive = type.archiver();
                    return depot.sauvegarder(typeArchive);
                });
    }

    // ─── Lire ─────────────────────────────────────────────────────────────

    /** Récupère un TypeMetier par son ID. */
    public Mono<TypeMetier> trouverParId(UUID typeId, BusinessContext ctx) {
        return depot.trouverParId(typeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Type Métier introuvable : " + typeId)))
                .doOnNext(type -> type.verifierAppartenance(ctx.tenantId()));
    }

    /** Liste tous les Types Métier du tenant courant. */
    public Flux<TypeMetier> lister(BusinessContext ctx) {
        return depot.listerParTenant(ctx.tenantId());
    }
}
