package com.yowyob.businesscore.adapter.in.rest.requestlog;

import java.util.List;

/** Page du journal détaillé des requêtes. */
public record RequeteLogPageResponse(List<RequeteLogResponse> items, long total, int page, int taille) {
}
