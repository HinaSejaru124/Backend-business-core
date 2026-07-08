package com.yowyob.businesscore.application.usecase.configuration;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.configuration.ParametreConfig;
import com.yowyob.businesscore.domain.port.out.PersisterParametreConfig;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use Case — Gestion de la Configuration.
 *
 * Deux niveaux :
 * - Niveau TYPE    : défaut défini par le développeur sur une VersionType
 * - Niveau ENTREPRISE : surcharge locale, impossible si paramètre verrouillé
 */
@Service
public class ConfigurationService {

    private final PersisterParametreConfig depot;

    public ConfigurationService(PersisterParametreConfig depot) {
        this.depot = depot;
    }

    // ─── Niveau TYPE ──────────────────────────────────────────────────────

    /**
     * Définit un paramètre par défaut sur une VersionType.
     * Si le paramètre existe déjà → le remplace.
     *
     * @param verrouille  true = les entreprises ne pourront PAS surcharger
     */
    public Mono<ParametreConfig> definirPourType(UUID versionTypeId, String cle,String valeur, boolean verrouille,BusinessContext ctx) {
        // Vérifier si le paramètre existe déjà → supprimer l'ancien
        return depot.trouverParCleEtVersion(cle, versionTypeId)
                .flatMap(existant -> depot.supprimer(existant.id()))
                .then(Mono.defer(() -> {
                    ParametreConfig param = ParametreConfig.pourType(
                            ctx.tenantId(), versionTypeId, cle, valeur, verrouille);
                    return depot.sauvegarder(param);
                }));
    }

    // ─── Niveau ENTREPRISE ────────────────────────────────────────────────

    /**
     * Surcharge un paramètre au niveau d'une Entreprise.
     *
     * Règle : impossible si le paramètre est verrouillé au niveau TYPE.
     * Le use case doit recevoir le versionTypeId pour vérifier le verrou.
     */
    public Mono<ParametreConfig> surchargerpourEntreprise(UUID entrepriseId,
                                                           UUID versionTypeId,
                                                           String cle, String valeur,
                                                           BusinessContext ctx) {
        // 1. Chercher le paramètre au niveau TYPE pour vérifier le verrou
        return depot.trouverParCleEtVersion(cle, versionTypeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Paramètre '" + cle + "' introuvable au niveau du Type Métier. " +
                    "Définissez-le d'abord au niveau TYPE avant de le surcharger.")))
                .flatMap(parametreType -> {
                    // 2. Vérifier que le paramètre n'est pas verrouillé
                    parametreType.verifierSurchargeable();

                    // 3. Supprimer l'ancienne surcharge si elle existe
                    return depot.trouverParCleEtEntreprise(cle, entrepriseId)
                            .flatMap(existant -> depot.supprimer(existant.id()))
                            .then(Mono.defer(() -> {
                                ParametreConfig surcharge = ParametreConfig.pourEntreprise(
                                        ctx.tenantId(), entrepriseId, cle, valeur);
                                return depot.sauvegarder(surcharge);
                            }));
                });
    }

    // ─── Résolution effective ─────────────────────────────────────────────

    /**
     * Résout la valeur effective d'un paramètre pour une entreprise.
     *
     * Logique de priorité :
     * 1. Surcharge entreprise (si elle existe)
     * 2. Défaut du type (fallback)
     * 3. Erreur 404 si ni l'un ni l'autre
     */
    public Mono<String> resoudreValeur(UUID entrepriseId, UUID versionTypeId, String cle) {
        return depot.trouverParCleEtEntreprise(cle, entrepriseId)
                .map(parametre -> parametre.valeur())
                // Fallback sur le niveau TYPE si pas de surcharge entreprise
                .switchIfEmpty(
                    depot.trouverParCleEtVersion(cle, versionTypeId)
                         .map(parametre -> parametre.valeur())
                )
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                    "Paramètre '" + cle + "' introuvable pour cette entreprise.")));
    }

    // ─── Lire ─────────────────────────────────────────────────────────────

    /** Liste tous les paramètres d'une VersionType. */
    public Flux<ParametreConfig> listerParVersion(UUID versionTypeId) {
        return depot.listerParVersion(versionTypeId);
    }

    /** Liste tous les paramètres d'une Entreprise. */
    public Flux<ParametreConfig> listerParEntreprise(UUID entrepriseId) {
        return depot.listerParEntreprise(entrepriseId);
    }
}
