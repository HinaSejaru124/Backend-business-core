package com.yowyob.businesscore.adapter.in.rest.sync;

import com.yowyob.businesscore.application.usecase.sync.SyncService.SyncEventItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record SyncEventResponse(
        long version,
        @Schema(example = "RULE") String entityType,
        UUID entityId,
        @Schema(example = "UPDATE", allowableValues = {"CREATE", "UPDATE", "DELETE"}) String operation,
        @Schema(description = "Snapshot JSON de l'entité au moment de l'événement") String payload
) {
    public static SyncEventResponse depuis(SyncEventItem item) {
        return new SyncEventResponse(item.version(), item.entityType(), item.entityId(),
                item.operation(), item.payload());
    }
}
