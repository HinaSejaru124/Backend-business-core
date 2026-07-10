package com.pharmacore.pharmaciebackend.ordonnance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class OrdonnanceDtos {

    private OrdonnanceDtos() {}

    public record LigneRequest(
            @NotNull UUID medicamentId, @Positive int quantitePrescrite, String posologie
    ) {}

    public record CreerOrdonnanceRequest(
            @NotNull UUID clientId,
            @NotBlank String medecinNom,
            String medecinNumeroOrdre,
            @NotNull LocalDate dateEmission,
            String documentNom,
            String documentContentType,
            /** Contenu du fichier encodé en base64 (lu côté navigateur via FileReader). */
            String documentContenuBase64,
            @NotEmpty @Valid List<LigneRequest> lignes
    ) {}

    public record LigneResponse(UUID id, UUID medicamentId, int quantitePrescrite, String posologie) {
        public static LigneResponse depuis(OrdonnanceLigne l) {
            return new LigneResponse(l.getId(), l.getMedicamentId(), l.getQuantitePrescrite(), l.getPosologie());
        }
    }

    public record OrdonnanceResponse(
            UUID id, UUID clientId, String medecinNom, String medecinNumeroOrdre, LocalDate dateEmission,
            String documentNom, boolean documentDisponible, UUID documentIdBcaas, String statut,
            Instant creeLe, List<LigneResponse> lignes
    ) {
        public static OrdonnanceResponse depuis(Ordonnance o, List<OrdonnanceLigne> lignes) {
            return new OrdonnanceResponse(o.getId(), o.getClientId(), o.getMedecinNom(),
                    o.getMedecinNumeroOrdre(), o.getDateEmission(), o.getDocumentNom(),
                    o.getDocumentContenu() != null && o.getDocumentContenu().length > 0,
                    o.getDocumentIdBcaas(), o.getStatut(), o.getCreeLe(),
                    lignes.stream().map(LigneResponse::depuis).toList());
        }
    }
}
