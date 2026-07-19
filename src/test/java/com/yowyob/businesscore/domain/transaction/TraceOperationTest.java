package com.yowyob.businesscore.domain.transaction;

import com.yowyob.businesscore.domain.shared.StatutTrace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests des transitions immuables de la TraceOperation (idempotence / compensation / audit).
 */
class TraceOperationTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ENTREPRISE = UUID.randomUUID();
    private static final UUID OPERATION = UUID.randomUUID();
    private static final UUID TX = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-06-17T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-17T08:00:05Z");

    @Test
    @DisplayName("demarrer → EN_COURS, non résolue")
    void demarrer_en_cours() {
        TraceOperation trace = TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente", "cle-1", T0);

        assertThat(trace.statut()).isEqualTo(StatutTrace.EN_COURS);
        assertThat(trace.estResolue()).isFalse();
        assertThat(trace.transactionKernelId()).isNull();
        assertThat(trace.resoluLe()).isNull();
        assertThat(trace.id()).isNotNull();
    }

    @Test
    @DisplayName("completer → COMPLETEE, transaction + horodatage de résolution")
    void completer_completee() {
        TraceOperation trace = TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente", "cle-1", T0)
                .completer(TX, "[]", T1);

        assertThat(trace.statut()).isEqualTo(StatutTrace.COMPLETEE);
        assertThat(trace.estResolue()).isTrue();
        assertThat(trace.transactionKernelId()).isEqualTo(TX);
        assertThat(trace.resoluLe()).isEqualTo(T1);
        assertThat(trace.creeLe()).isEqualTo(T0);
    }

    @Test
    @DisplayName("compenser → COMPENSEE, mémorise la transaction annulée")
    void compenser_compensee() {
        TraceOperation trace = TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente", "cle-1", T0)
                .compenser(TX, null, null, null, T1);

        assertThat(trace.statut()).isEqualTo(StatutTrace.COMPENSEE);
        assertThat(trace.transactionKernelId()).isEqualTo(TX);
        assertThat(trace.resoluLe()).isEqualTo(T1);
    }

    @Test
    @DisplayName("compenser → mémorise le code et le message d'erreur pour l'application cliente")
    void compenser_memorise_diagnostic() {
        TraceOperation trace = TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente", "cle-1", T0)
                .compenser(TX, null, "STOCK_INSUFFISANT", "Stock insuffisant pour l'offre X", T1);

        assertThat(trace.codeErreur()).isEqualTo("STOCK_INSUFFISANT");
        assertThat(trace.messageErreur()).isEqualTo("Stock insuffisant pour l'offre X");
    }

    @Test
    @DisplayName("la clé d'idempotence est obligatoire")
    void cle_obligatoire() {
        assertThatThrownBy(() -> TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente", " ", T0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
