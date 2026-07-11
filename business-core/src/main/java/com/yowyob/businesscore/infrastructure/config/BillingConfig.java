package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.application.billing.BillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Active la liaison de {@link BillingProperties} (catalogue des plans / quotas mensuels)
 * depuis {@code businesscore.billing.*}.
 */
@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingConfig {
}
