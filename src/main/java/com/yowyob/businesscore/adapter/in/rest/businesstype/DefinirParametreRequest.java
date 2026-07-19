package com.yowyob.businesscore.adapter.in.rest.businesstype;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Définition d'un paramètre de configuration de version")
public record DefinirParametreRequest(
        @Schema(description = "Clé du paramètre", example = "devise")
        @NotBlank(message = "cle est obligatoire") String cle,
        @Schema(description = "Valeur", example = "XOF")
        @NotBlank(message = "valeur est obligatoire") String valeur,
        @Schema(description = "Non modifiable par les applications", example = "true")
        boolean verrouille
) {}
