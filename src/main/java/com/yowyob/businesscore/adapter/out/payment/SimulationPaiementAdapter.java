package com.yowyob.businesscore.adapter.out.payment;

import com.yowyob.businesscore.domain.port.out.PaiementPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter de paiement de <b>SIMULATION</b> — confirme immédiatement tout changement de plan.
 *
 * <p><b>Non câblé</b> (pas de {@code @Component}) : l'implémentation active de {@link PaiementPort} est
 * {@code KernelPaiementAdapter} (vrai paiement mobile money). Cette classe est conservée pour les tests et
 * comme repli éventuel (déblocage instantané sans débit réel) — la réactiver = ajouter {@code @Component}
 * et retirer celui de {@code KernelPaiementAdapter}.
 */
public class SimulationPaiementAdapter implements PaiementPort {

    @Override
    public Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande) {
        return Mono.just(new ResultatPaiement(
                ResultatPaiement.Statut.CONFIRME, null, "SIMULATION-" + UUID.randomUUID()));
    }

    @Override
    public Mono<ResultatPaiement> verifierStatut(String reference) {
        return Mono.just(new ResultatPaiement(ResultatPaiement.Statut.CONFIRME, null, reference));
    }
}
