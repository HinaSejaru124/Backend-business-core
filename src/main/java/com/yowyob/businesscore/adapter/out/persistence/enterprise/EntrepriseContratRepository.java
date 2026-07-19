package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/** Repository du contrat technique d'entreprise. RLS garantit l'isolation par tenant. */
public interface EntrepriseContratRepository
        extends ReactiveCrudRepository<EntrepriseContratEntity, UUID> {
}
