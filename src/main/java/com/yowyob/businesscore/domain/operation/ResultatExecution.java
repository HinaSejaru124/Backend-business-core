package com.yowyob.businesscore.domain.operation;

import com.yowyob.businesscore.domain.shared.StatutTrace;

import java.util.Map;
import java.util.UUID;

/**
 * Résultat de l'exécution d'une opération, indépendant du transport.
 *
 * <p>La couche REST le traduit en {@code 200} (immédiat → {@link StatutTrace#COMPLETEE}) ou
 * {@code 202} (différé → {@link StatutTrace#EN_COURS}).
 */
public record ResultatExecution(
        StatutTrace statut,
        UUID traceId,
        UUID transactionKernelId,
        Map<String, Object> details
) {

    public ResultatExecution {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public boolean estImmediat() {
        return statut == StatutTrace.COMPLETEE;
    }

    public boolean estDiffere() {
        return statut == StatutTrace.EN_COURS;
    }
}
