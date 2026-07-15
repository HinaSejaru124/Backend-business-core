package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Déclare, de façon <b>idempotente</b>, le modèle métier « Pharmacie » sur la version Business Core
 * ciblée : rôles (brique 3), règle « ordonnance requise » (brique 4) et opération « Vendre » (brique 5).
 * Le catalogue d'offres (brique 2) et la configuration (brique 7) sont gérés à part.
 *
 * <p>Idempotence : Business Core n'impose pas l'unicité, donc on liste l'existant avant de déclarer.
 * Les rôles sont dédupliqués par code. La règle « ordonnance requise » est vérifiée/corrigée à chaque
 * appel (remplacée si un ancien provisioning l'a déclarée avec le mauvais effet) ; l'opération « Vendre »
 * n'est déclarée qu'une fois (sa présence suffit à savoir qu'elle existe déjà).
 */
@Service
public class ModeleProvisioningService {

    // ─── Rôles métier (brique 3) ─────────────────────────────────────────────
    private static final String ROLE_CAISSIER = "CAISSIER";                       // déclenche la vente
    private static final String ROLE_PHARMACIEN = "PHARMACIEN_RESPONSABLE";       // peut déroger à l'ordonnance
    private static final String ROLE_CLIENT = "CLIENT";                           // bénéficiaire
    private static final String CAT_OPERATEUR = "OPERATEUR";
    private static final String CAT_BENEFICIAIRE = "BENEFICIAIRE";

    // ─── Règle « ordonnance requise » (brique 4) ─────────────────────────────
    private static final String DECLENCHEUR_VENTE = "AVANT_VENTE";
    private static final String CONDITION_ORDONNANCE = "CATEGORIE_EGALE:valeur=medicament_prescription";
    /**
     * DEROGER, pas EXIGER : EXIGER est un effet bloquant, appliqué avant toute lecture de
     * {@code rolesAutorisesADeroger} (cf. {@code EvaluerReglesExecuteur.premierBloquant} côté Business
     * Core — les rôles autorisés ne sont consultés que par {@code GestionnaireEffets.appliquerDeroger},
     * jamais pour EXIGER). Avec EXIGER, aucun rôle ne peut jamais outrepasser la règle : la « dérogation
     * du pharmacien » documentée plus haut ne fonctionnait donc pas réellement. DEROGER corrige ça et
     * correspond mieux à la réalité métier : le Caissier ne peut pas vendre un médicament sur ordonnance
     * (bloqué, doit escalader) ; le Pharmacien Responsable le peut, à condition de fournir un motif
     * (tracé en audit côté Business Core) — pas de document à joindre pour cette vente.
     */
    private static final String EFFET_DEROGER = "DEROGER";

    // ─── Opération « Vendre » (brique 5) ─────────────────────────────────────
    private static final String OPERATION_VENDRE = "Vendre";

    // ─── Configuration (brique 7) ────────────────────────────────────────────
    // Clés/valeurs propres au projet PharmaCore (le champ cle/valeur est du texte libre côté BCaaS).
    private static final String CFG_DEVISE = "devise";                    // verrouillée : la devise ne se change pas par entreprise
    private static final String CFG_TVA = "tva_pourcentage";              // ajustable
    private static final String CFG_SEUIL = "seuil_alerte_stock_defaut";  // ajustable

    private final BcaasAdminClient admin;
    private final BcaasProperties properties;

    public ModeleProvisioningService(BcaasAdminClient admin, BcaasProperties properties) {
        this.admin = admin;
        this.properties = properties;
    }

    /** Compte-rendu de ce qui a été déclaré ou laissé en place. */
    public record Rapport(List<String> actions) {
    }

    public Rapport provisionner() {
        String typeId = properties.typeId();
        int version = properties.versionNumber();
        String businessId = properties.businessId();
        List<String> actions = new ArrayList<>();

        // 1. Rôles — déclarer uniquement les codes manquants.
        Set<String> rolesPresents = admin.listerRoles(typeId, version).stream()
                .map(r -> String.valueOf(r.get("code")))
                .collect(Collectors.toSet());
        declarerRoleSiAbsent(typeId, version, ROLE_CAISSIER, CAT_OPERATEUR, rolesPresents, actions);
        declarerRoleSiAbsent(typeId, version, ROLE_PHARMACIEN, CAT_OPERATEUR, rolesPresents, actions);
        declarerRoleSiAbsent(typeId, version, ROLE_CLIENT, CAT_BENEFICIAIRE, rolesPresents, actions);

        // 2. Acteurs (brique 3) — le personnel (Pharmacien, Caissier) se crée depuis l'espace admin
        // (« Personnel », PersonnelAdminController), avec de vraies identités distinctes de celle du
        // titulaire. Le titulaire ne se rattache plus lui-même comme CAISSIER : ça mélangeait les rôles
        // (exactement le défaut que la restructuration par rôles corrige).

        // 3. Configuration (brique 7) — déclarer les clés manquantes.
        Set<String> cfgPresentes = admin.listerConfig(typeId, version).stream()
                .map(c -> String.valueOf(c.get("cle")))
                .collect(Collectors.toSet());
        declarerConfigSiAbsent(typeId, version, CFG_DEVISE, "XAF", true, cfgPresentes, actions);
        declarerConfigSiAbsent(typeId, version, CFG_TVA, "19.25", false, cfgPresentes, actions);
        declarerConfigSiAbsent(typeId, version, CFG_SEUIL, "10", false, cfgPresentes, actions);

        // 4. Règle « ordonnance requise » — corrigée si un ancien provisioning l'a déclarée en EXIGER.
        assurerRegleOrdonnance(typeId, version, actions);

        // 5. Opération « Vendre » — déclarée une seule fois (Business Core n'impose pas l'unicité).
        boolean vendreExiste = admin.listerOperations(businessId).stream()
                .anyMatch(o -> OPERATION_VENDRE.equalsIgnoreCase(String.valueOf(o.get("nom"))));
        if (vendreExiste) {
            actions.add("Opération « Vendre » déjà présente — non redéclarée.");
        } else {
            admin.declarerOperation(typeId, version, OPERATION_VENDRE, ROLE_CAISSIER, DECLENCHEUR_VENTE,
                    etapesVente());
            actions.add("Opération « Vendre » déclarée (6 étapes de saga).");
        }

        return new Rapport(actions);
    }

    /**
     * Idempotent et auto-corrective : si la règle existe déjà avec l'ancien effet EXIGER (provisioning
     * antérieur à ce correctif), elle est supprimée puis redéclarée en DEROGER — EXIGER ne peut pas être
     * mis à jour « en douceur » côté Business Core sans repasser par une suppression (pas de PATCH
     * partiel sur l'effet), donc on remplace plutôt que d'inventer une migration en place.
     */
    @SuppressWarnings("unchecked")
    private void assurerRegleOrdonnance(String typeId, int version, List<String> actions) {
        Map<String, Object> existante = admin.listerRegles(typeId, version).stream()
                .filter(r -> DECLENCHEUR_VENTE.equals(String.valueOf(r.get("declencheur")))
                        && CONDITION_ORDONNANCE.equals(String.valueOf(r.get("condition"))))
                .findFirst()
                .orElse(null);

        if (existante != null && EFFET_DEROGER.equals(String.valueOf(existante.get("effet")))) {
            actions.add("Règle « ordonnance requise » déjà correcte (DEROGER).");
            return;
        }
        if (existante != null) {
            admin.supprimerRegle(typeId, version, java.util.UUID.fromString(String.valueOf(existante.get("id"))));
            actions.add("Règle « ordonnance requise » obsolète (effet " + existante.get("effet")
                    + ") supprimée pour être redéclarée en DEROGER.");
        }
        admin.declarerRegle(typeId, version, DECLENCHEUR_VENTE, CONDITION_ORDONNANCE, EFFET_DEROGER,
                List.of(ROLE_PHARMACIEN));
        actions.add("Règle « ordonnance requise » déclarée (AVANT_VENTE → DEROGER, réservée à "
                + ROLE_PHARMACIEN + " avec motif obligatoire).");
    }

    private void declarerRoleSiAbsent(String typeId, int version, String code, String categorie,
                                      Set<String> presents, List<String> actions) {
        if (presents.contains(code)) {
            actions.add("Rôle " + code + " déjà présent.");
        } else {
            admin.declarerRole(typeId, version, code, categorie);
            actions.add("Rôle " + code + " déclaré (" + categorie + ").");
        }
    }

    private void declarerConfigSiAbsent(String typeId, int version, String cle, String valeur,
                                        boolean verrouille, Set<String> presentes, List<String> actions) {
        if (presentes.contains(cle)) {
            actions.add("Config " + cle + " déjà présente.");
        } else {
            admin.definirConfig(typeId, version, cle, valeur, verrouille);
            actions.add("Config " + cle + " = " + valeur + (verrouille ? " (verrouillée)." : "."));
        }
    }

    /**
     * Saga de la vente, dans l'ordre imposé par Business Core (cf. backend-test.md §1.6) :
     * {@code ENGAGER_STOCK} vient après {@code ENREGISTRER_VENTE} (il a besoin du commandeId produit),
     * et {@code EVALUER_REGLES} avant l'enregistrement (pour bloquer une vente sur ordonnance sans document).
     */
    private static List<Map<String, Object>> etapesVente() {
        return List.of(
                etape(0, "VERIFIER_STOCK"),
                etape(1, "EVALUER_REGLES"),
                etape(2, "ENREGISTRER_VENTE"),
                etape(3, "ENGAGER_STOCK"),
                etape(4, "ENCAISSER"),
                etape(5, "EMETTRE_EVENEMENT"));
    }

    private static Map<String, Object> etape(int ordre, String typeEtape) {
        return Map.of("ordre", ordre, "typeEtape", typeEtape);
    }
}
