package com.yowyob.businesscore.domain.transaction;

import com.yowyob.businesscore.domain.shared.StatutTrace;

import java.time.Instant;
import java.util.UUID;

/**
 * Brique 6 (liaison) — TraceOperation : la clé de voûte du système.
 *
 * <p>Relie une demande d'opération à son résultat kernel et fait fonctionner <b>trois mécanismes</b> :
 * <ul>
 *   <li><b>Idempotence</b> : {@code cleIdempotence} (unique) empêche le double traitement.</li>
 *   <li><b>Compensation</b> : {@code transactionKernelId} dit <i>quoi annuler</i> si une étape échoue.</li>
 *   <li><b>Audit</b> : relie la demande au résultat (statut + résultat des règles).</li>
 * </ul>
 *
 * <p>Record immuable : les transitions renvoient une nouvelle instance.
 */
public record TraceOperation(
        UUID id,
        UUID tenantId,
        UUID entrepriseId,
        UUID operationId,
        String operationNom,
        String cleIdempotence,
        UUID transactionKernelId,   // null tant qu'aucune transaction kernel n'est produite
        StatutTrace statut,
        String resultatRegles,      // JSON des effets de règles appliqués (audit), nullable
        String codeErreur,          // renseigné si COMPENSEE : la règle/cause violée (ex. ProblemException.violatedRule)
        String messageErreur,       // renseigné si COMPENSEE : message lisible, pour diagnostic côté application cliente
        Instant creeLe,
        Instant resoluLe            // null tant que EN_COURS
) {

    public TraceOperation {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (entrepriseId == null)
            throw new IllegalArgumentException("entrepriseId est obligatoire");
        if (operationId == null)
            throw new IllegalArgumentException("operationId est obligatoire");
        if (cleIdempotence == null || cleIdempotence.isBlank())
            throw new IllegalArgumentException("cleIdempotence est obligatoire");
        if (statut == null)
            throw new IllegalArgumentException("statut est obligatoire");
    }

    /** Fabrique : ouvre une trace en statut EN_COURS au moment {@code maintenant}. */
    public static TraceOperation demarrer(UUID tenantId, UUID entrepriseId, UUID operationId,
                                          String operationNom, String cleIdempotence, Instant maintenant) {
        return new TraceOperation(
                UUID.randomUUID(), tenantId, entrepriseId, operationId, operationNom,
                cleIdempotence, null, StatutTrace.EN_COURS, null, null, null, maintenant, null);
    }

    /** Transition : opération terminée avec succès → COMPLETEE, horodatée. */
    public TraceOperation completer(UUID transactionKernelId, String resultatRegles, Instant maintenant) {
        return new TraceOperation(id, tenantId, entrepriseId, operationId, operationNom,
                cleIdempotence, transactionKernelId, StatutTrace.COMPLETEE, resultatRegles,
                null, null, creeLe, maintenant);
    }

    /**
     * Transition : une étape a échoué, les précédentes ont été annulées → COMPENSEE, horodatée.
     * {@code transactionKernelId} mémorise ce qui a été annulé (peut être null si rien n'était engagé).
     * {@code codeErreur}/{@code messageErreur} donnent à l'application cliente le pourquoi et permettent
     * de décider quelle action prendre, sans avoir à ré-interroger l'erreur HTTP d'origine.
     */
    public TraceOperation compenser(UUID transactionKernelId, String resultatRegles,
                                    String codeErreur, String messageErreur, Instant maintenant) {
        return new TraceOperation(id, tenantId, entrepriseId, operationId, operationNom,
                cleIdempotence, transactionKernelId, StatutTrace.COMPENSEE, resultatRegles,
                codeErreur, messageErreur, creeLe, maintenant);
    }

    public boolean estResolue() {
        return statut != StatutTrace.EN_COURS;
    }
}
