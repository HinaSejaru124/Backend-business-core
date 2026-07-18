package com.yowyob.businesscore.adapter.out.payment;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.PaiementPort;
import com.yowyob.businesscore.infrastructure.config.PaymentProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Paiement d'un upgrade de plan via la passerelle d'encaissement <b>Kernel Core</b> (mobile money,
 * MyCoolPay). Implémentation active de {@link PaiementPort}.
 *
 * <p>Le paiement est <b>asynchrone</b> : {@link #demanderPaiement} ouvre un ordre
 * ({@code POST /api/payments/orders}) qui revient presque toujours en {@code PENDING} avec une
 * {@code redirectUrl} MyCoolPay ; le développeur valide sur son téléphone, puis {@link #verifierStatut}
 * ({@code POST /api/payments/orders/{id}/refresh}) donne l'issue réelle. Le plan n'est activé par
 * {@code PlanService} que sur un statut succès <b>reconnu</b> ({@link #STATUTS_SUCCES}) ; tout statut
 * inconnu est traité comme {@code EN_ATTENTE} (fail-safe : jamais de déblocage sur un statut non maîtrisé).
 *
 * <p>L'authentification et le {@code X-Tenant-Id} sont gérés par {@link KernelClient} : appelé dans le
 * flux console (JWT du développeur présent), il passe en mode délégué et envoie le Bearer + le tenant du
 * développeur — exactement la combinaison validée manuellement sur cet endpoint.
 */
@Component
public class KernelPaiementAdapter implements PaiementPort {

    private static final String CHEMIN_ORDRES = "/api/payments/orders";

    /** Statuts kernel considérés comme un paiement réussi (activation du plan). */
    private static final Set<String> STATUTS_SUCCES = Set.of("SUCCESS", "SUCCEEDED", "PAID", "COMPLETED");
    /** Statuts kernel considérés comme un échec définitif. */
    private static final Set<String> STATUTS_ECHEC = Set.of("FAILED", "CANCELLED", "CANCELED", "REJECTED", "EXPIRED");

    private final KernelClient kernel;
    private final PaymentProperties properties;

    public KernelPaiementAdapter(KernelClient kernel, PaymentProperties properties) {
        this.kernel = kernel;
        this.properties = properties;
    }

    @Override
    public Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("clientId", properties.clientId());
        corps.put("serviceCode", properties.serviceCodePrefix() + demande.planCible());
        corps.put("idempotencyKey", UUID.randomUUID().toString());
        corps.put("amount", demande.montant());
        corps.put("currency", demande.devise());
        corps.put("provider", properties.provider());
        corps.put("method", properties.method());
        corps.put("payerReference", demande.payerReference());
        corps.put("description", "Upgrade plan " + demande.planCible() + " (Business Core)");
        corps.put("callbackUrl", properties.callbackBaseUrl());

        return kernel.post(CHEMIN_ORDRES, corps, PaymentOrderResponse.class)
                .map(KernelPaiementAdapter::versResultat);
    }

    @Override
    public Mono<ResultatPaiement> verifierStatut(String reference) {
        return kernel.post(CHEMIN_ORDRES + "/" + reference + "/refresh", null, PaymentOrderResponse.class)
                .map(KernelPaiementAdapter::versResultat);
    }

    /** Traduit l'ordre de paiement kernel en issue métier. */
    private static ResultatPaiement versResultat(PaymentOrderResponse ordre) {
        String statut = ordre.status() == null ? "" : ordre.status().trim().toUpperCase(Locale.ROOT);
        if (STATUTS_SUCCES.contains(statut)) {
            return new ResultatPaiement(ResultatPaiement.Statut.CONFIRME, null, ordre.id());
        }
        if (STATUTS_ECHEC.contains(statut)) {
            return new ResultatPaiement(ResultatPaiement.Statut.REFUSE, null, ordre.id());
        }
        // PENDING ou statut inconnu : à finaliser (fail-safe, aucun déblocage).
        return new ResultatPaiement(ResultatPaiement.Statut.EN_ATTENTE, ordre.redirectUrl(), ordre.id());
    }
}
