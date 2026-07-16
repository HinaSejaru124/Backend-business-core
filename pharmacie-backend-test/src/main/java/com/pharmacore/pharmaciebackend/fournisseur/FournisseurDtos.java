package com.pharmacore.pharmaciebackend.fournisseur;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class FournisseurDtos {

    private FournisseurDtos() {}

    public record CreerFournisseurRequest(
            @NotBlank String nom, String contactNom, String contactTelephone,
            String email, Integer delaiLivraisonJours
    ) {}

    public record FournisseurResponse(
            UUID id, String nom, String contactNom, String contactTelephone,
            String email, Integer delaiLivraisonJours, Instant creeLe
    ) {
        public static FournisseurResponse depuis(Fournisseur f) {
            return new FournisseurResponse(f.getId(), f.getNom(), f.getContactNom(),
                    f.getContactTelephone(), f.getEmail(), f.getDelaiLivraisonJours(), f.getCreeLe());
        }
    }
}
