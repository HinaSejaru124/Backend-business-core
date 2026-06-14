package com.yowyob.businesscore.domain.shared;

/** Statut d'une trace d'opération (compensation, idempotence, audit). */
public enum StatutTrace {
    EN_COURS,
    COMPLETEE,
    COMPENSEE
}
