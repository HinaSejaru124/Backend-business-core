package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Offre déclarée sur une version")
public record OffreReponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        UUID versionTypeId,
        @Schema(example = "Forfait mensuel") String nom,
        @Schema(example = "FIXE") FormePrix formePrix,
        @Schema(example = "15000.00") BigDecimal prix,
        List<CapaciteReponse> capacites
) {

    public static OffreReponse de(DefinitionOffre o) {
        return new OffreReponse(o.id(), o.versionTypeId(), o.nom(), o.formePrix(), o.prix(),
                o.capacites().stream().map(CapaciteReponse::de).toList());
    }

    @Schema(description = "Capacité activée sur l'offre")
    public record CapaciteReponse(
            UUID id,
            @Schema(example = "STOCK") TypeCapacite type,
            boolean active
    ) {
        public static CapaciteReponse de(Capacite c) {
            return new CapaciteReponse(c.id(), c.type(), c.active());
        }
    }
}
