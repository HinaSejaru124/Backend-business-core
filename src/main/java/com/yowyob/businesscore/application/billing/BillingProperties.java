package com.yowyob.businesscore.application.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogue des plans tarifaires : quota mensuel de requêtes (et prix d'affichage) par code de plan.
 *
 * <p>Configuré via {@code businesscore.billing.plans.*}. {@code quotaMensuel < 0} signifie « illimité ».
 * Le <b>prix</b> et le <b>paiement</b> réels relèvent de Kernel Core (indisponible pour l'instant) :
 * {@code prixMensuel}/{@code devise} ne servent qu'à l'affichage et peuvent rester à 0. Les valeurs par
 * défaut (FREE 1000 / PRO 100000 / ENTERPRISE illimité) s'appliquent si rien n'est configuré.
 */
@ConfigurationProperties(prefix = "businesscore.billing")
public record BillingProperties(Map<String, PlanDef> plans) {

    /**
     * Définition d'un plan. {@code quotaMensuel < 0} => requêtes illimitées ;
     * {@code applicationsMax < 0} => nombre d'applications illimité.
     */
    public record PlanDef(long quotaMensuel, long prixMensuel, String devise, long applicationsMax, String libelle) {
        public PlanDef {
            if (devise == null || devise.isBlank()) {
                devise = "XAF";
            }
        }

        /** Constructeur de compatibilité : applications illimitées, sans libellé. */
        public PlanDef(long quotaMensuel, long prixMensuel, String devise) {
            this(quotaMensuel, prixMensuel, devise, -1, null);
        }

        /** Constructeur de compatibilité : sans libellé. */
        public PlanDef(long quotaMensuel, long prixMensuel, String devise, long applicationsMax) {
            this(quotaMensuel, prixMensuel, devise, applicationsMax, null);
        }

        public boolean illimite() {
            return quotaMensuel < 0;
        }

        public boolean illimiteApplications() {
            return applicationsMax < 0;
        }
    }

    public BillingProperties {
        if (plans == null || plans.isEmpty()) {
            plans = defauts();
        }
    }

    private static Map<String, PlanDef> defauts() {
        Map<String, PlanDef> m = new LinkedHashMap<>();
        m.put("FREE", new PlanDef(1_000, 0, "XAF"));
        m.put("PRO", new PlanDef(100_000, 0, "XAF"));
        m.put("ENTERPRISE", new PlanDef(-1, 0, "XAF"));
        return m;
    }
}
