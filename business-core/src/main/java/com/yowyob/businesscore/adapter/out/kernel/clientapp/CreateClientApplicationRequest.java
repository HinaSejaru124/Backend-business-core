package com.yowyob.businesscore.adapter.out.kernel.clientapp;

import java.util.List;

/** Corps de POST /api/client-applications (kernel). */
public record CreateClientApplicationRequest(
        String clientId,
        String name,
        String description,
        String clientSecret,
        String planCode,
        List<String> allowedServices
) {
}
