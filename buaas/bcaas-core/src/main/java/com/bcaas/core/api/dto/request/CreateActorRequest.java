package com.bcaas.core.api.dto.request;

import com.bcaas.core.actor.domain.model.ActorRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateActorRequest(

        @NotBlank(message = "Le prénom est obligatoire")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        String lastName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        String phoneNumber,

        String locale,

        @NotNull(message = "Le rôle est obligatoire")
        ActorRole role
) {
    public CreateActorRequest {
        locale = locale != null ? locale : "fr";
    }
}
