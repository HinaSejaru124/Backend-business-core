package com.yowyob.businesscore.application.common;

/** Métadonnées de pagination standard de l'API (alignées sur la spec OpenAPI). */
public record PageInfo(int page, int size, long total) {
}
