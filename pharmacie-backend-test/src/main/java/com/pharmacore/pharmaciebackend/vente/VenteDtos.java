package com.pharmacore.pharmaciebackend.vente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VenteDtos {

    private VenteDtos() {}

    public record LigneRequest(@NotNull UUID medicamentId, @Positive int quantite) {}

    /**
     * {@code modePaiement} (ESPECES/MOBILE_MONEY/CARTE) est une métadonnée locale PharmaCore — le
     * payload {@code parametres} envoyé à {@code Vendre:execute} ne connaît pas ce concept
     * (backend-test.md §2.5).
     */
    /**
     * {@code motifDerogation} : requis par Business Core pour vendre un médicament sur ordonnance en
     * tant que Pharmacien Responsable (effet DEROGER de la règle « ordonnance requise ») — ignoré si le
     * panier ne contient aucun médicament sur ordonnance, rejeté par la plateforme si le rôle connecté
     * n'y est pas autorisé (Caissier).
     */
    public record CreerVenteRequest(
            UUID clientId,
            UUID ordonnanceId,
            @NotBlank String modePaiement,
            String motifDerogation,
            @NotEmpty @Valid List<LigneRequest> lignes
    ) {}

    public record LigneResponse(UUID id, UUID medicamentId, int quantite, BigDecimal prixUnitaireFacture) {
        public static LigneResponse depuis(VenteLigne l) {
            return new LigneResponse(l.getId(), l.getMedicamentId(), l.getQuantite(), l.getPrixUnitaireFacture());
        }
    }

    public record VenteResponse(
            UUID id, UUID businessId, UUID clientId, UUID ordonnanceId, BigDecimal montantTotal, String devise,
            String modePaiement, String statutBcaas, String transactionKernelId, UUID traceId,
            UUID idempotencyKey, Instant creeLe, List<LigneResponse> lignes
    ) {
        public static VenteResponse depuis(Vente v, List<VenteLigne> lignes) {
            return new VenteResponse(v.getId(), v.getBusinessId(), v.getClientId(), v.getOrdonnanceId(),
                    v.getMontantTotal(), v.getDevise(), v.getModePaiement(), v.getStatutBcaas(),
                    v.getTransactionKernelId(), v.getTraceId(), v.getIdempotencyKey(), v.getCreeLe(),
                    lignes.stream().map(LigneResponse::depuis).toList());
        }
    }
}
