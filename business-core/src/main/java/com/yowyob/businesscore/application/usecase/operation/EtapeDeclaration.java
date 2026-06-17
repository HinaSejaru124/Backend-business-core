package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.domain.shared.TypeEtape;

/**
 * Entrée de déclaration d'une étape (avant persistance : pas encore d'identifiant d'opération).
 * Le couple (ordre, typeEtape) provient du payload de déclaration d'opération.
 */
public record EtapeDeclaration(int ordre, TypeEtape typeEtape) {
}
