// domain/rule/RegleMetier.java
package com.yowyob.businesscore.domain.rule;

import java.util.List;
import java.util.UUID;

import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

public class RegleMetier {

    private final UUID id;
    private final UUID tenantId;
    private final UUID versionTypeId;   // règle de Type — ou null
    private final UUID entrepriseId;    // règle locale — ou null
    private final Declencheur declencheur;
    private final String condition;     // format N1 : "TYPE:param=val[,param=val]"
    private final Effet effet;
    private final List<String> rolesAutorisesADeroger;

    public RegleMetier(
            UUID id, UUID tenantId,
            UUID versionTypeId, UUID entrepriseId,
            Declencheur declencheur, String condition,
            Effet effet, List<String> rolesAutorisesADeroger) {

        if (versionTypeId != null && entrepriseId != null) {
            throw new IllegalArgumentException(
                    "Une règle ne peut appartenir à la fois à un type et à une entreprise");
        }
        if (versionTypeId == null && entrepriseId == null) {
            throw new IllegalArgumentException(
                    "Une règle doit appartenir à un type ou à une entreprise");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.versionTypeId = versionTypeId;
        this.entrepriseId = entrepriseId;
        this.declencheur = declencheur;
        this.condition = condition;
        this.effet = effet;
        this.rolesAutorisesADeroger = rolesAutorisesADeroger != null
                ? List.copyOf(rolesAutorisesADeroger) : List.of();
    }

    public boolean estDeType()  { return versionTypeId != null; }
    public boolean estLocale()  { return entrepriseId != null; }

    public UUID getId()                           { return id; }
    public UUID getTenantId()                     { return tenantId; }
    public UUID getVersionTypeId()                { return versionTypeId; }
    public UUID getEntrepriseId()                 { return entrepriseId; }
    public Declencheur getDeclencheur()           { return declencheur; }
    public String getCondition()                  { return condition; }
    public Effet getEffet()                       { return effet; }
    public List<String> getRolesAutorisesADeroger() { return rolesAutorisesADeroger; }
}