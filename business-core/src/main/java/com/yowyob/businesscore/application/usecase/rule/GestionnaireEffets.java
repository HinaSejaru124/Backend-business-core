// application/usecase/rule/GestionnaireEffets.java
package com.yowyob.businesscore.application.usecase.rule;

import java.util.List;

import org.springframework.stereotype.Component;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.internal.EffetAAppliquer;
import com.yowyob.businesscore.domain.port.out.JournaliserAudit;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import com.yowyob.businesscore.domain.port.out.RegleChargee;

import reactor.core.publisher.Mono;

@Component
public class GestionnaireEffets {

    private final JournaliserAudit audit;
    private final PublierEvenement evenements;

    public GestionnaireEffets(JournaliserAudit audit, PublierEvenement evenements) {
        this.audit = audit;
        this.evenements = evenements;
    }

    /**
     * Applique les effets dans l'ordre.
     * Le premier effet bloquant lève une ProblemException et court-circuite le reste.
     */
    public Mono<Void> appliquer(
            List<EffetAAppliquer> effets,
            BusinessContext ctx,
            List<RegleChargee> regles) {

        return Mono.defer(() -> {
            for (EffetAAppliquer e : effets) {
                if (e.effet().estBloquant()) {
                    return appliquerBloquant(e, regles);
                }
            }
            // Effets non bloquants : on les enchaîne tous
            return Mono.when(
                effets.stream()
                    .filter(e -> !e.effet().estBloquant())
                    .map(e -> appliquerNonBloquant(e, ctx, regles))
                    .toList()
            );
        });
    }

    // --- Famille bloquants ---

    private Mono<Void> appliquerBloquant(EffetAAppliquer e, List<RegleChargee> regles) {
        return switch (e.effet()) {
            case BLOQUER -> Mono.error(
                ProblemException.unprocessable("Opération refusée par une règle métier")
                    .violatedRule(e.regleId().toString())
                    .requiredAction("BLOQUER")
            );
            case EXIGER -> Mono.error(
                ProblemException.unprocessable("Un document est requis avant de continuer")
                    .violatedRule(e.regleId().toString())
                    .requiredAction("EXIGER")
                    .requiredDocument(
                        (String) e.details().getOrDefault("document", "Document non précisé"))
            );
            case VALIDER -> Mono.error(
                ProblemException.unprocessable("Une approbation est requise avant de continuer")
                    .violatedRule(e.regleId().toString())
                    .requiredAction("VALIDER")
            );
            default -> Mono.empty();
        };
    }

    // --- Familles mutateur + traçants ---

    private Mono<Void> appliquerNonBloquant(
            EffetAAppliquer e, BusinessContext ctx, List<RegleChargee> regles) {
        return switch (e.effet()) {
            case AJUSTER -> appliquerAjuster(e, ctx);
            case ALERTER -> appliquerAlerter(e);
            case DEROGER -> appliquerDeroger(e, ctx, trouver(regles, e));
            default      -> Mono.empty();
        };
    }

    private Mono<Void> appliquerAjuster(EffetAAppliquer e, BusinessContext ctx) {
        // AJUSTER n'est jamais silencieux : on trace ancienne + nouvelle valeur
        String detail = "tenant=" + ctx.tenantId()
                + " regle=" + e.regleId()
                + " ancienneValeur=" + e.details().getOrDefault("ancienneValeur", "N/A")
                + " nouvelleValeur=" + e.details().getOrDefault("nouvelleValeur", "N/A");
        return audit.journaliser("AJUSTER", detail);
    }

    private Mono<Void> appliquerAlerter(EffetAAppliquer e) {
        return evenements.publier("ALERTE_REGLE",
                java.util.Map.of("regleId", e.regleId().toString(), "message", e.message()));
    }

    private Mono<Void> appliquerDeroger(
            EffetAAppliquer e, BusinessContext ctx, RegleChargee regle) {

        List<String> rolesAutorises = regle.rolesAutorisesADeroger();

        // Liste vide → personne autorisé → équivaut à BLOQUER
        if (rolesAutorises.isEmpty()) {
            return Mono.error(
                ProblemException.unprocessable("Dérogation impossible : aucun rôle autorisé")
                    .violatedRule(e.regleId().toString())
                    .requiredAction("BLOQUER")
            );
        }

        // L'acteur n'a pas le rôle → retombe sur VALIDER
        boolean autorise = ctx.roles().stream().anyMatch(rolesAutorises::contains);
        if (!autorise) {
            return Mono.error(
                ProblemException.unprocessable(
                        "Dérogation réservée aux rôles : " + rolesAutorises)
                    .violatedRule(e.regleId().toString())
                    .requiredAction("VALIDER")
            );
        }

        // Acteur autorisé → motif obligatoire + archivage
        Object motif = e.details().get("motif");
        if (motif == null || motif.toString().isBlank()) {
            return Mono.error(
                ProblemException.unprocessable("Motif de dérogation obligatoire")
                    .violatedRule(e.regleId().toString())
                    .requiredAction("FOURNIR_MOTIF")
            );
        }

        String detail = "tenant=" + ctx.tenantId()
                + " regle=" + e.regleId()
                + " roles=" + ctx.roles()
                + " motif=" + motif;
        return audit.journaliser("DEROGER", detail);
    }

    private RegleChargee trouver(List<RegleChargee> regles, EffetAAppliquer e) {
        return regles.stream()
                .filter(r -> r.id().equals(e.regleId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Règle introuvable : " + e.regleId()));
    }
}