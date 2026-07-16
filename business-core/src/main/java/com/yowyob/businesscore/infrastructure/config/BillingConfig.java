package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.application.admin.AdminProperties;
import com.yowyob.businesscore.application.billing.BillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Active la liaison de {@link BillingProperties} (catalogue des plans / quotas mensuels)
 * depuis {@code businesscore.billing.*} et de {@link AdminProperties} (console admin)
 * depuis {@code businesscore.admin.*}.
 */
@Configuration
@EnableConfigurationProperties({BillingProperties.class, AdminProperties.class})
public class BillingConfig {
}
