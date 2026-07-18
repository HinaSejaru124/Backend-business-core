package com.yowyob.businesscore.infrastructure.config;

import com.yowyob.businesscore.application.admin.AdminProperties;
import com.yowyob.businesscore.application.billing.BillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Active la liaison de {@link BillingProperties} (catalogue des plans / quotas mensuels)
 * depuis {@code businesscore.billing.*}, de {@link AdminProperties} (console admin)
 * depuis {@code businesscore.admin.*} et de {@link PaymentProperties} (passerelle de paiement Kernel)
 * depuis {@code businesscore.payment.*}.
 */
@Configuration
@EnableConfigurationProperties({BillingProperties.class, AdminProperties.class, PaymentProperties.class})
public class BillingConfig {
}
