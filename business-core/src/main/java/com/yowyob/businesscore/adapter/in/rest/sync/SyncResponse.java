package com.yowyob.businesscore.adapter.in.rest.sync;

import com.yowyob.businesscore.application.usecase.sync.SyncService.SyncResultat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SyncResponse(
        @Schema(description = "Version courante (curseur à repasser au prochain appel via `since`)")
        long versionCourante,
        List<SyncEventResponse> items
) {
    public static SyncResponse depuis(SyncResultat resultat) {
        return new SyncResponse(resultat.versionCourante(),
                resultat.items().stream().map(SyncEventResponse::depuis).toList());
    }
}
