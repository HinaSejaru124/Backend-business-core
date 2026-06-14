package com.yowyob.businesscore.domain.port.out;

import java.util.UUID;

/** Référence à un BusinessDomain de la taxonomie kernel (classement, pas comportement). */
public record BusinessDomainRef(UUID id, String code, String nom) {
}
