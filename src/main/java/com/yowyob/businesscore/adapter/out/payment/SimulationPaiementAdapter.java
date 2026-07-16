package com.yowyob.businesscore.adapter.out.payment;

import com.yowyob.businesscore.domain.port.out.PaiementPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter de paiement de <b>SIMULATION</b> — placeholder de l'API de paiement Kernel Core (indisponible).
 *
 * <p>Confirme immédiatement tout changement de plan, ce qui rend le parcours d'upgrade et le déblocage du
 * quota testables de bout en bout dès maintenant. À remplacer/compléter par un {@code KernelPaiementAdapter}
 * (même port {@link PaiementPort}) quand l'API existera — sélection par profil ou propriété, sans toucher
 * à {@code PlanService}.
 */
@Component
public class SimulationPaiementAdapter implements PaiementPort {

    @Override
    public Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande) {
        return Mono.just(new ResultatPaiement(
                ResultatPaiement.Statut.CONFIRME, null, "SIMULATION-" + UUID.randomUUID()));
    }
}
