package com.yowyob.businesscore.adapter.in.rest.access;

/** Corps de {@code POST /v1/api-keys}. {@code name} est libre (« Prod », « Dev »…), optionnel. */
public record CreerCleRequest(String name) {
}
