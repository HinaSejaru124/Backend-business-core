package com.yowyob.businesscore.application.billing;

import com.yowyob.businesscore.application.billing.BillingProperties.PlanDef;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Accès en lecture au catalogue des plans ({@link BillingProperties}) avec repli sûr.
 *
 * <p>Un code de plan inconnu est ramené au plan {@code FREE} (fail-safe : jamais d'illimité par erreur).
 * {@link #existe(String)} reste strict pour valider une cible d'upgrade.
 */
@Component
public class PlanCatalogue {

    public static final String PLAN_DEFAUT = "FREE";

    private final BillingProperties properties;

    public PlanCatalogue(BillingProperties properties) {
        this.properties = properties;
    }

    public Map<String, PlanDef> plans() {
        return properties.plans();
    }

    /** Majuscule + trim ; null/blanc => plan par défaut. */
    public String normaliser(String plan) {
        return (plan == null || plan.isBlank()) ? PLAN_DEFAUT : plan.trim().toUpperCase(Locale.ROOT);
    }

    /** Vrai seulement si le code correspond réellement à un plan du catalogue. */
    public boolean existe(String plan) {
        return plan != null && !plan.isBlank()
                && properties.plans().containsKey(plan.trim().toUpperCase(Locale.ROOT));
    }

    /** Définition du plan, avec repli sur FREE si inconnu. */
    public PlanDef definition(String plan) {
        PlanDef def = properties.plans().get(normaliser(plan));
        if (def != null) {
            return def;
        }
        return properties.plans().getOrDefault(PLAN_DEFAUT, new PlanDef(1_000, 0, "XAF"));
    }

    public long quotaMensuel(String plan) {
        return definition(plan).quotaMensuel();
    }

    public boolean illimite(String plan) {
        return definition(plan).illimite();
    }

    public long prixMensuel(String plan) {
        return definition(plan).prixMensuel();
    }

    public String devise(String plan) {
        return definition(plan).devise();
    }
}
