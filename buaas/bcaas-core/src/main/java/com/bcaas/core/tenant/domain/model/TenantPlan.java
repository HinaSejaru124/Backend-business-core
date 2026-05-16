package com.bcaas.core.tenant.domain.model;

import com.bcaas.core.context.domain.PolicyLevel;

/**
 * Plan d'abonnement d'un tenant.
 * Détermine les limites d'utilisation et le niveau de service.
 */
public enum TenantPlan {

    FREE(PolicyLevel.FREE, 10, 100),
    STANDARD(PolicyLevel.STANDARD, 100, 10_000),
    PREMIUM(PolicyLevel.PREMIUM, 1_000, 100_000),
    ENTERPRISE(PolicyLevel.ENTERPRISE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final PolicyLevel policyLevel;
    private final int maxActors;
    private final int maxResourcesPerMonth;

    TenantPlan(PolicyLevel policyLevel, int maxActors, int maxResourcesPerMonth) {
        this.policyLevel = policyLevel;
        this.maxActors = maxActors;
        this.maxResourcesPerMonth = maxResourcesPerMonth;
    }

    public PolicyLevel getPolicyLevel() { return policyLevel; }
    public int getMaxActors() { return maxActors; }
    public int getMaxResourcesPerMonth() { return maxResourcesPerMonth; }
}
