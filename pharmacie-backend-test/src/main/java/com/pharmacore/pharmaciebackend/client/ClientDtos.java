package com.pharmacore.pharmaciebackend.client;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class ClientDtos {

    private ClientDtos() {}

    public record CreerClientRequest(
            @NotBlank String nom, String prenom, String telephone, String email, String adresse
    ) {}

    public record ClientResponse(
            UUID id, String nom, String prenom, String telephone, String email,
            String adresse, UUID beneficiaireId, Instant creeLe
    ) {
        public static ClientResponse depuis(Client c) {
            return new ClientResponse(c.getId(), c.getNom(), c.getPrenom(), c.getTelephone(),
                    c.getEmail(), c.getAdresse(), c.getBeneficiaireId(), c.getCreeLe());
        }
    }
}
