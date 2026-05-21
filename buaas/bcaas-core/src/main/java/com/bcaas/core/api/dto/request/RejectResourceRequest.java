package com.bcaas.core.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectResourceRequest(
        @NotBlank(message = "La raison du rejet est obligatoire")
        String reason
) {}
