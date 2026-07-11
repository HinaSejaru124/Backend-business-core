package com.pharmacore.pharmaciebackend.fournisseur.commande;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CommandeFournisseurDtos {

    private CommandeFournisseurDtos() {}

    public record LigneRequest(
            @NotNull UUID medicamentId, @Positive int quantiteCommandee,
            @NotNull @PositiveOrZero BigDecimal prixUnitaireAchat
    ) {}

    public record CreerCommandeRequest(
            @NotNull UUID fournisseurId,
            @NotNull LocalDate dateCommande,
            LocalDate dateReceptionPrevue,
            @NotEmpty @Valid List<LigneRequest> lignes
    ) {}

    /** Réception : par défaut, quantité reçue = quantité commandée pour chaque ligne non précisée. */
    public record ReceptionLigne(@NotNull UUID ligneId, @PositiveOrZero int quantiteRecue) {}

    public record ReceptionRequest(List<ReceptionLigne> lignes) {}

    public record LigneResponse(UUID id, UUID medicamentId, int quantiteCommandee,
                                Integer quantiteRecue, BigDecimal prixUnitaireAchat) {
        public static LigneResponse depuis(CommandeFournisseurLigne l) {
            return new LigneResponse(l.getId(), l.getMedicamentId(), l.getQuantiteCommandee(),
                    l.getQuantiteRecue(), l.getPrixUnitaireAchat());
        }
    }

    public record CommandeResponse(UUID id, UUID fournisseurId, String statut, LocalDate dateCommande,
                                   LocalDate dateReceptionPrevue, LocalDate dateReceptionReelle,
                                   Instant creeLe, List<LigneResponse> lignes) {
        public static CommandeResponse depuis(CommandeFournisseur c, List<CommandeFournisseurLigne> lignes) {
            return new CommandeResponse(c.getId(), c.getFournisseurId(), c.getStatut(),
                    c.getDateCommande(), c.getDateReceptionPrevue(), c.getDateReceptionReelle(),
                    c.getCreeLe(), lignes.stream().map(LigneResponse::depuis).toList());
        }
    }
}
