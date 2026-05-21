package com.bcaas.core.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateResourceRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 3, max = 200)
        String title,

        String summary,

        String locale,

        Map<String, String> fields
) {}
