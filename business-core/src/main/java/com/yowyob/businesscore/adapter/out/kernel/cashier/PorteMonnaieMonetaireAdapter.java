package com.yowyob.businesscore.adapter.out.kernel.cashier;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.PorteMonnaieGenerique;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stratégie (port interne) {@link PorteMonnaieGenerique} — implémentation <b>monétaire</b> de départ.
 *
 * <p>Le monétaire n'est qu'une forme d'échange de valeur ; troc/don pourront suivre comme d'autres
 * implémentations sans toucher au domaine. Enregistre l'encaissement sur le core cashier du kernel
 * ({@code POST /api/cashier/payments}) via {@link KernelClient}.
 */
@Component
public class PorteMonnaieMonetaireAdapter implements PorteMonnaieGenerique {

    private final KernelClient kernel;

    public PorteMonnaieMonetaireAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Mono<UUID> enregistrerEchange(BigDecimal montant, String devise) {
        PaiementRequest requete = new PaiementRequest(montant, devise);
        return kernel.post("/api/cashier/payments", requete, PaiementResponse.class)
                .map(PaiementResponse::id);
    }
}

record PaiementRequest(BigDecimal amount, String currency) {
}

record PaiementResponse(UUID id) {
}
