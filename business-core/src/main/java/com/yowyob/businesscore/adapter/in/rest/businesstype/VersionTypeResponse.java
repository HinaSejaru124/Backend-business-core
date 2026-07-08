package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.businesstype.VersionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Version d'un type métier")
public record VersionTypeResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        UUID typeMetierId,
        @Schema(example = "1") int numero,
        @Schema(description = "Verrouillée après publication") boolean immuable,
        Instant publieeLe,
        @Schema(example = "v1 initiale") String libelle
) {
    public static VersionTypeResponse depuis(VersionType v) {
        return new VersionTypeResponse(
                v.id(), v.typeMetierId(), v.numero(),
                v.immuable(), v.publieeLe(), v.libelle()
        );
    }
}
