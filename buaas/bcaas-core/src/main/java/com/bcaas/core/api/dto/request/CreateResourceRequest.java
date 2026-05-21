package com.bcaas.core.api.dto.request;

import com.bcaas.core.resource.domain.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateResourceRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
        String title,

        String summary,

        String locale,

        ResourceType type,

        Map<String, String> fields
) {
    public CreateResourceRequest {
        locale = locale != null ? locale : "fr";
        type = type != null ? type : ResourceType.STANDARD;
        fields = fields != null ? fields : Map.of();
    }
}
