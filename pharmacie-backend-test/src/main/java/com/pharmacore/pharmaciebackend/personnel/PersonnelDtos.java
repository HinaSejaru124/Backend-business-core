package com.pharmacore.pharmaciebackend.personnel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class PersonnelDtos {

    private PersonnelDtos() {}

    public record CreerPersonnelRequest(
            @NotBlank String nom,
            @NotBlank String prenom,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "8 caractères minimum") String motDePasse,
            @NotBlank @Pattern(regexp = "PHARMACIEN_RESPONSABLE|CAISSIER",
                    message = "doit être PHARMACIEN_RESPONSABLE ou CAISSIER") String role
    ) {}

    public record PersonnelResponse(
            UUID id, String nom, String prenom, String email, String role, boolean actif, Instant creeLe
    ) {
        public static PersonnelResponse depuis(Personnel p) {
            return new PersonnelResponse(p.getId(), p.getNom(), p.getPrenom(), p.getEmail(), p.getRole(),
                    p.isActif(), p.getCreeLe());
        }
    }
}
