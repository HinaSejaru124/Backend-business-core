package com.yowyob.businesscore.domain.port.out;

public record SignUpResult(
        String id,
        String tenantId,
        String status,
        String message
) {}
