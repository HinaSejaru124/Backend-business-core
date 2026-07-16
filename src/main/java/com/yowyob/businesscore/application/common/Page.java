package com.yowyob.businesscore.application.common;

import java.util.List;

/** Conteneur paginé standard de l'API : {@code content} + {@code pageInfo}. */
public record Page<T>(List<T> content, PageInfo pageInfo) {

    public static <T> Page<T> of(List<T> content, int page, int size, long total) {
        return new Page<>(content, new PageInfo(page, size, total));
    }
}
