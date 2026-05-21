package com.bcaas.core.infrastructure.config;

import com.bcaas.core.actor.application.ActorService;
import com.bcaas.core.actor.port.output.ActorEventPublisher;
import com.bcaas.core.actor.port.output.ActorRepository;
import com.bcaas.core.audit.application.AuditService;
import com.bcaas.core.audit.port.output.AuditRepository;
import com.bcaas.core.tenant.application.TenantService;
import com.bcaas.core.tenant.port.output.TenantEventPublisher;
import com.bcaas.core.tenant.port.output.TenantRepository;
import com.bcaas.core.workflow.application.WorkflowService;
import com.bcaas.core.workflow.port.output.WorkflowEventPublisher;
import com.bcaas.core.workflow.port.output.WorkflowRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration du Business Core.
 *
 * Câble les use cases avec leurs ports.
 * Respecte l'inversion de dépendance — le domaine ne connaît pas Spring.
 *
 * Analogie réseau : table de routage — associe chaque port à son adapteur.
 */
@Configuration
public class BcaasCoreAutoConfiguration {

    @Bean
    public TenantService tenantService(TenantRepository tenantRepository,
                                       TenantEventPublisher eventPublisher) {
        return new TenantService(tenantRepository, eventPublisher);
    }

    @Bean
    public ActorService actorService(ActorRepository actorRepository,
                                     ActorEventPublisher eventPublisher,
                                     TenantRepository tenantRepository) {
        return new ActorService(actorRepository, eventPublisher, tenantRepository);
    }

    @Bean
    public WorkflowService workflowService(WorkflowRepository workflowRepository,
                                           WorkflowEventPublisher eventPublisher) {
        return new WorkflowService(workflowRepository, eventPublisher);
    }

    @Bean
    public AuditService auditService(AuditRepository auditRepository) {
        return new AuditService(auditRepository);
    }
}
