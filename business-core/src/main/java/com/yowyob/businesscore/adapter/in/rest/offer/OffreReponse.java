package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Réponse d'une offre déclarée (aligné OpenAPI {@code Offer}). */
public record OffreReponse(
        UUID id,
        UUID versionTypeId,
        String nom,
        FormePrix formePrix,
        BigDecimal prix,
        List<CapaciteReponse> capacites) {

    public static OffreReponse de(DefinitionOffre o) {
        return new OffreReponse(o.id(), o.versionTypeId(), o.nom(), o.formePrix(), o.prix(),
                o.capacites().stream().map(CapaciteReponse::de).toList());
    }

    public record CapaciteReponse(UUID id, TypeCapacite type, boolean active) {
        public static CapaciteReponse de(Capacite c) {
            return new CapaciteReponse(c.id(), c.type(), c.active());
        }
    }
}
