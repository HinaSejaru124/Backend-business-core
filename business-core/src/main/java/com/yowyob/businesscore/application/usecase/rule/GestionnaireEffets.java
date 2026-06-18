// application/usecase/rule/GestionnaireEffets.java
package com.yowyob.businesscore.application.usecase.rule;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.internal.EffetAAppliquer;
import com.yowyob.businesscore.domain.port.out.JournaliserAudit;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Applique les effets <b>non bloquants</b> d'une évaluation de règles, dans l'ordre :
 * <ul>
 *   <li><b>AJUSTER</b> (mutateur) : jamais silencieux, systématiquement tracé via l'audit ;</li>
 *   <li><b>ALERTER</b> (traçant) : publie un événement, sans interrompre ;</li>
 *   <li><b>DEROGER</b> (traçant) : cascade d'autorisation selon les rôles habilités et le motif —
 *       peut interrompre (RFC 7807) si la dérogation n'est pas autorisée.</li>
 * </ul>
 *
 * <p>Les effets bloquants (BLOQUER / EXIGER / VALIDER) sont gérés en amont par l'étape
 * {@code EVALUER_REGLES} (422 immédiat) ; ils sont ignorés ici.
 */
@Component
public class GestionnaireEffets {

    private final JournaliserAudit audit;
    private final PublierEvenement evenements;

    public GestionnaireEffets(JournaliserAudit audit, PublierEvenement evenements) {
        this.audit = audit;
        this.evenements = evenements;
    }

    /** Applique séquentiellement les effets non bloquants ; s'arrête à la première dérogation refusée. */
    public Mono<Void> appliquerNonBloquants(List<EffetAAppliquer> effets, Set<String> rolesActeur) {
        return Flux.fromIterable(effets)
                .filter(e -> !e.effet().estBloquant())
                .concatMap(e -> appliquerUn(e, rolesActeur))
                .then();
    }

    private Mono<Void> appliquerUn(EffetAAppliquer e, Set<String> rolesActeur) {
        return switch (e.effet()) {
            case AJUSTER -> appliquerAjuster(e);
            case ALERTER -> appliquerAlerter(e);
            case DEROGER -> appliquerDeroger(e, rolesActeur);
            default -> Mono.empty();
        };
    }

    private Mono<Void> appliquerAjuster(EffetAAppliquer e) {
        // AJUSTER n'est jamais silencieux : on trace ancienne + nouvelle valeur.
        String detail = "regle=" + e.regleId()
                + " ancienneValeur=" + e.details().getOrDefault("ancienneValeur", "N/A")
                + " nouvelleValeur=" + e.details().getOrDefault("nouvelleValeur", "N/A");
        return audit.journaliser("AJUSTER", detail);
    }

    private Mono<Void> appliquerAlerter(EffetAAppliquer e) {
        return evenements.publier("ALERTE_REGLE", Map.of(
                "regleId", String.valueOf(e.regleId()),
                "message", e.message() == null ? "" : e.message()));
    }

    /**
     * Cascade de la dérogation, du plus restrictif au plus permissif :
     * <ol>
     *   <li>aucun rôle autorisé sur la règle → équivaut à BLOQUER ;</li>
     *   <li>l'acteur courant n'a aucun des rôles autorisés → retombe sur VALIDER ;</li>
     *   <li>acteur autorisé mais motif absent → on exige le motif ;</li>
     *   <li>acteur autorisé + motif fourni → dérogation acceptée et archivée dans l'audit.</li>
     * </ol>
     */
    private Mono<Void> appliquerDeroger(EffetAAppliquer e, Set<String> rolesActeur) {
        List<String> rolesAutorises = rolesAutorises(e);

        if (rolesAutorises.isEmpty()) {
            return Mono.error(ProblemException.unprocessable("Dérogation impossible : aucun rôle autorisé")
                    .violatedRule(String.valueOf(e.regleId())).requiredAction("BLOQUER"));
        }

        boolean autorise = rolesActeur != null && rolesActeur.stream().anyMatch(rolesAutorises::contains);
        if (!autorise) {
            return Mono.error(ProblemException.unprocessable("Dérogation réservée aux rôles : " + rolesAutorises)
                    .violatedRule(String.valueOf(e.regleId())).requiredAction("VALIDER"));
        }

        Object motif = e.details().get("motif");
        if (motif == null || motif.toString().isBlank()) {
            return Mono.error(ProblemException.unprocessable("Motif de dérogation obligatoire")
                    .violatedRule(String.valueOf(e.regleId())).requiredAction("FOURNIR_MOTIF"));
        }

        String detail = "regle=" + e.regleId() + " roles=" + rolesActeur + " motif=" + motif;
        return audit.journaliser("DEROGER", detail);
    }

    @SuppressWarnings("unchecked")
    private static List<String> rolesAutorises(EffetAAppliquer e) {
        Object valeur = e.details().get("rolesAutorisesADeroger");
        return valeur instanceof List<?> liste ? (List<String>) liste : List.of();
    }
}
