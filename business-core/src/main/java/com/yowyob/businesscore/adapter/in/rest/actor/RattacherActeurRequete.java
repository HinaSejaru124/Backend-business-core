package com.yowyob.businesscore.adapter.in.rest.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = """
        Rattachement d'un acteur déjà connu à un rôle métier. Business Core ne résout ni ne crée aucune
        identité kernel ici : pour un rôle OPERATEUR, fournir acteurKernelId (identité déjà connue —
        Kernel Core fait autorité ; une personne inconnue doit s'inscrire elle-même via
        POST .../actors:register). Pour un rôle BENEFICIAIRE (RG-04, pas d'identifiants de connexion),
        fournir identifiantPersonne.
        """)
public record RattacherActeurRequete(
        @Schema(description = "Rôle métier cible", example = "00000000-0000-0000-0000-000000000000")
        @NotNull UUID roleMetierId,
        @Schema(description = "Identité kernel déjà connue — requis pour un rôle OPERATEUR")
        UUID acteurKernelId,
        @Schema(description = "Identifiant personne — requis pour un rôle BENEFICIAIRE", example = "client-42")
        String identifiantPersonne
) {
}
