package com.pharmacore.pharmaciebackend.medicament;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class MedicamentDtos {

    private MedicamentDtos() {}

    /**
     * {@code categorie} doit être {@code medicament_libre} ou {@code medicament_prescription} —
     * cette valeur est envoyée telle quelle au moteur de règles Business Core (condition
     * {@code CATEGORIE_EGALE}), cf. backend-test.md §3.2 et §1.5.
     */
    public record CreerMedicamentRequest(
            @NotBlank String nom,
            String dci,
            String formeGalenique,
            String codeCip,
            @NotBlank String categorie,
            boolean ordonnanceRequise,
            @NotNull @PositiveOrZero BigDecimal prixUnitaire,
            @PositiveOrZero int stockInitial,
            @PositiveOrZero int seuilAlerte,
            UUID fournisseurId
    ) {}

    public record MedicamentResponse(
            UUID id, UUID offreId, String nom, String dci, String formeGalenique, String codeCip,
            String categorie, boolean ordonnanceRequise, BigDecimal prixUnitaire,
            int stockActuel, int seuilAlerte, UUID fournisseurId, String statut, Instant creeLe
    ) {
        public static MedicamentResponse depuis(Medicament m) {
            return new MedicamentResponse(m.getId(), m.getOffreId(), m.getNom(), m.getDci(),
                    m.getFormeGalenique(), m.getCodeCip(), m.getCategorie(), m.isOrdonnanceRequise(),
                    m.getPrixUnitaire(), m.getStockActuel(), m.getSeuilAlerte(), m.getFournisseurId(),
                    m.getStatut(), m.getCreeLe());
        }
    }
}
