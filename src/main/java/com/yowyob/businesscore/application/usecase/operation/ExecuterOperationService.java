package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.MoteurOperation;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.application.usecase.actor.ResoudreRolesMetier;
import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.ResultatExecution;
import com.yowyob.businesscore.domain.operation.spi.EntrepriseResolue;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.operation.spi.ResoudreEntreprise;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.HorlogeSysteme;
import com.yowyob.businesscore.domain.port.internal.PlanificateurDOperation;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import com.yowyob.businesscore.domain.port.out.VerrouDIdempotence;
import com.yowyob.businesscore.domain.shared.StatutTrace;
import com.yowyob.businesscore.domain.transaction.TraceOperation;
import com.yowyob.businesscore.domain.transaction.spi.PersisterTrace;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Use case — <b>exécuter une opération</b> sur une entreprise. C'est le chef d'orchestre de la feature.
 *
 * <p>Garanties :
 * <ul>
 *   <li><b>Idempotence</b> : un verrou Redis ({@link VerrouDIdempotence}) bloque les doublons concurrents,
 *       et la trace (clé d'idempotence unique par tenant) rejoue le résultat des doublons séquentiels.</li>
 *   <li><b>Immédiat / différé</b> : opération immédiate → exécution synchrone → {@code 200} + trace
 *       {@code COMPLETEE} ; opération différée → {@code 202} + trace {@code EN_COURS} + événement Kafka.</li>
 *   <li><b>Compensation</b> : si une étape échoue après un effet engagé, le {@link MoteurOperation}
 *       compense, et la trace devient {@code COMPENSEE}. Une règle bloquante remonte en {@code 422}
 *       (RFC 7807), sans trace, lock relâché (un nouvel essai corrigé reste possible).</li>
 * </ul>
 * Le tenant provient toujours du {@link BusinessContext}.
 */
@Service
public class ExecuterOperationService {

    private static final Duration TTL_VERROU = Duration.ofMinutes(10);
    private static final String EVENEMENT_DIFFERE = "operation.differee";

    private final ResoudreEntreprise resoudreEntreprise;
    private final PersisterOperation persisterOperation;
    private final PlanificateurDOperation planificateur;
    private final MoteurOperation moteur;
    private final PersisterTrace persisterTrace;
    private final VerrouDIdempotence verrou;
    private final PublierEvenement publierEvenement;
    private final HorlogeSysteme horloge;
    private final ObjectMapper objectMapper;
    private final ResoudreRolesMetier resoudreRolesMetier;

    public ExecuterOperationService(ResoudreEntreprise resoudreEntreprise,
                                    PersisterOperation persisterOperation,
                                    PlanificateurDOperation planificateur,
                                    MoteurOperation moteur,
                                    PersisterTrace persisterTrace,
                                    VerrouDIdempotence verrou,
                                    PublierEvenement publierEvenement,
                                    HorlogeSysteme horloge,
                                    ObjectMapper objectMapper,
                                    ResoudreRolesMetier resoudreRolesMetier) {
        this.resoudreEntreprise = resoudreEntreprise;
        this.persisterOperation = persisterOperation;
        this.planificateur = planificateur;
        this.moteur = moteur;
        this.persisterTrace = persisterTrace;
        this.verrou = verrou;
        this.publierEvenement = publierEvenement;
        this.horloge = horloge;
        this.objectMapper = objectMapper;
        this.resoudreRolesMetier = resoudreRolesMetier;
    }

    public Mono<ResultatExecution> executer(UUID entrepriseId, String nom, String cleIdempotenceHeader,
                                            Map<String, Object> parametres, BusinessContext ctx) {
        String cle = (cleIdempotenceHeader != null && !cleIdempotenceHeader.isBlank())
                ? cleIdempotenceHeader.trim()
                : "auto:" + UUID.randomUUID();

        // Rejeu : si une trace existe déjà pour cette clé (RLS isole le tenant), on renvoie son résultat.
        return persisterTrace.trouverParCleIdempotence(cle)
                .flatMap(this::rejouer)
                .switchIfEmpty(Mono.defer(() -> executerSousVerrou(entrepriseId, nom, cle, parametres, ctx)));
    }

    // ─── Rejeu d'une trace existante (idempotence séquentielle) ───────────

    private Mono<ResultatExecution> rejouer(TraceOperation trace) {
        return switch (trace.statut()) {
            case COMPLETEE -> Mono.just(new ResultatExecution(
                    StatutTrace.COMPLETEE, trace.id(), trace.transactionKernelId(), Map.of()));
            case EN_COURS -> Mono.just(new ResultatExecution(
                    StatutTrace.EN_COURS, trace.id(), null, Map.of()));
            case COMPENSEE -> Mono.error(ProblemException.conflict(
                    "Opération déjà traitée et compensée pour cette clé d'idempotence.")
                    .violatedRule("IDEMPOTENCE_COMPENSEE"));
        };
    }

    // ─── Exécution sous verrou d'idempotence (doublons concurrents) ───────

    private Mono<ResultatExecution> executerSousVerrou(UUID entrepriseId, String nom, String cle,
                                                       Map<String, Object> parametres, BusinessContext ctx) {
        String cleVerrou = ctx.tenantId() + ":" + cle;
        return verrou.acquerir(cleVerrou, TTL_VERROU).flatMap(acquis -> {
            if (!Boolean.TRUE.equals(acquis)) {
                return Mono.error(ProblemException.conflict(
                        "Opération déjà en cours pour cette clé d'idempotence.")
                        .violatedRule("IDEMPOTENCE_EN_COURS"));
            }
            return resoudreEtExecuter(entrepriseId, nom, cle, parametres, ctx)
                    .flatMap(resultat -> liberer(cleVerrou).thenReturn(resultat))
                    .onErrorResume(erreur -> liberer(cleVerrou).then(Mono.error(erreur)));
        });
    }

    private Mono<Void> liberer(String cleVerrou) {
        return verrou.liberer(cleVerrou).onErrorResume(e -> Mono.empty());
    }

    // ─── Résolution entreprise + opération, puis exécution ────────────────

    private Mono<ResultatExecution> resoudreEtExecuter(UUID entrepriseId, String nom, String cle,
                                                       Map<String, Object> parametres, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMap(entreprise -> persisterOperation
                        .trouverParVersionEtNom(entreprise.versionTypeId(), nom)
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Opération '" + nom + "' introuvable pour cette entreprise.")))
                        .flatMap(definition -> resoudreRolesMetier
                                .rolesActifs(entreprise.entrepriseId(), ctx.actorId())
                                .flatMap(rolesActeur -> {
                                    Set<String> roles = fusionnerRoles(ctx, rolesActeur);
                                    if (definition.roleDeclencheur() != null
                                            && !roles.contains(definition.roleDeclencheur())) {
                                        return Mono.error(ProblemException.forbidden(
                                                        "Rôle requis pour déclencher l'opération : "
                                                                + definition.roleDeclencheur())
                                                .violatedRule("ROLE_DECLENCHEUR_REQUIS"));
                                    }
                                    ContexteEtape contexte = contexteInitial(
                                            definition, entreprise, parametres, ctx, roles);
                                    return definition.differe()
                                            ? executerDiffere(definition, entreprise, cle, ctx.tenantId(), parametres)
                                            : executerImmediat(definition, entreprise, cle, ctx.tenantId(), contexte);
                                })));
    }

    /** Rôles effectifs = rôles du contexte d'auth (futurs) ∪ rôles métier résolus depuis les acteurs. */
    private static Set<String> fusionnerRoles(BusinessContext ctx, Set<String> rolesActeur) {
        Set<String> roles = new HashSet<>();
        if (ctx.roles() != null) {
            roles.addAll(ctx.roles());
        }
        if (rolesActeur != null) {
            roles.addAll(rolesActeur);
        }
        return roles;
    }

    private ContexteEtape contexteInitial(DefinitionOperation definition, EntrepriseResolue entreprise,
                                          Map<String, Object> parametres, BusinessContext ctx, Set<String> roles) {
        Map<String, Object> donnees = new HashMap<>();
        if (parametres != null) {
            donnees.putAll(parametres);
        }
        donnees.put(ClesContexte.TENANT_ID, ctx.tenantId());
        donnees.put(ClesContexte.ENTREPRISE_ID, entreprise.entrepriseId());
        donnees.put(ClesContexte.VERSION_TYPE_ID, entreprise.versionTypeId());
        if (entreprise.organizationId() != null) {
            donnees.put(ClesContexte.ORGANIZATION_ID, entreprise.organizationId());
        }
        donnees.put(ClesContexte.OPERATION_NOM, definition.nom());
        donnees.put(ClesContexte.DECLENCHEUR, definition.declencheurRegles());
        if (roles != null && !roles.isEmpty()) {
            donnees.put(ClesContexte.ROLES, roles);
        }
        return new ContexteEtape(donnees);
    }

    // ─── Mode immédiat (200) ──────────────────────────────────────────────

    private Mono<ResultatExecution> executerImmediat(DefinitionOperation definition, EntrepriseResolue entreprise,
                                                     String cle, UUID tenantId, ContexteEtape contexte) {
        return planificateur.planifier(definition.id()).collectList()
                .flatMap(etapes -> moteur.executer(etapes, contexte))
                .flatMap(resultat -> {
                    Instant maintenant = horloge.maintenant();
                    String resultatRegles = serialiserRegles(resultat.contexte());
                    UUID transactionKernelId = Valeurs.versUuid(
                            resultat.contexte().get(ClesContexte.TRANSACTION_KERNEL_ID));

                    if (resultat.succes()) {
                        TraceOperation trace = TraceOperation.demarrer(
                                        tenantId, entreprise.entrepriseId(), definition.id(),
                                        definition.nom(), cle, maintenant)
                                .completer(transactionKernelId, resultatRegles, maintenant);
                        return persisterTrace.sauvegarder(trace).map(sauvee -> new ResultatExecution(
                                StatutTrace.COMPLETEE, sauvee.id(), transactionKernelId,
                                details(resultat.contexte())));
                    }

                    if (transactionKernelId != null) {
                        // Un effet a été engagé puis compensé par le moteur : on trace COMPENSEE.
                        TraceOperation trace = TraceOperation.demarrer(
                                        tenantId, entreprise.entrepriseId(), definition.id(),
                                        definition.nom(), cle, maintenant)
                                .compenser(transactionKernelId, resultatRegles, maintenant);
                        return persisterTrace.sauvegarder(trace).then(Mono.error(resultat.erreur()));
                    }

                    // Rejet avant tout effet (ex. règle bloquante 422) : aucune trace, on remonte l'erreur.
                    return Mono.error(resultat.erreur());
                });
    }

    // ─── Mode différé (202) ────────────────────────────────────────────────

    private Mono<ResultatExecution> executerDiffere(DefinitionOperation definition, EntrepriseResolue entreprise,
                                                    String cle, UUID tenantId, Map<String, Object> parametres) {
        Instant maintenant = horloge.maintenant();
        TraceOperation trace = TraceOperation.demarrer(
                tenantId, entreprise.entrepriseId(), definition.id(), definition.nom(), cle, maintenant);

        return persisterTrace.sauvegarder(trace).flatMap(sauvee -> {
            Map<String, Object> evenement = new HashMap<>();
            evenement.put("traceId", sauvee.id().toString());
            evenement.put("operationId", definition.id().toString());
            evenement.put("operationNom", definition.nom());
            evenement.put("entrepriseId", entreprise.entrepriseId().toString());
            if (parametres != null) {
                evenement.put("parametres", parametres);
            }
            return publierEvenement.publier(EVENEMENT_DIFFERE, evenement)
                    .thenReturn(new ResultatExecution(StatutTrace.EN_COURS, sauvee.id(), null, Map.of()));
        });
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private Map<String, Object> details(ContexteEtape contexte) {
        Map<String, Object> details = new HashMap<>();
        ajouter(details, contexte, ClesContexte.MONTANT);
        ajouter(details, contexte, ClesContexte.DEVISE);
        ajouter(details, contexte, ClesContexte.MOUVEMENT_ID);
        ajouter(details, contexte, ClesContexte.DOCUMENT_ID);
        ajouter(details, contexte, ClesContexte.STOCK);
        return details;
    }

    private void ajouter(Map<String, Object> details, ContexteEtape contexte, String cle) {
        Object valeur = contexte.get(cle);
        if (valeur != null) {
            details.put(cle, valeur);
        }
    }

    /** Sérialise les effets de règles appliqués (audit) ; null si aucun ou en cas d'échec de sérialisation. */
    //@SuppressWarnings("unchecked")
    private String serialiserRegles(ContexteEtape contexte) {
        Object effets = contexte.get(ClesContexte.RESULTAT_REGLES);
        if (!(effets instanceof List<?> liste) || liste.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(liste);
        } catch (RuntimeException erreurSerialisation) {
            return null;
        }
    }
}
