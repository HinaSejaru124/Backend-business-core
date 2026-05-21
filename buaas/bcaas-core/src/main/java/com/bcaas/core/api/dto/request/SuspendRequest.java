package com.bcaas.core.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SuspendRequest(
        @NotBlank(message = "La raison est obligatoire")
        String reason
) {}
