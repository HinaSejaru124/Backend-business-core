package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/** Repository de la fiche produit d'entreprise. RLS garantit l'isolation par tenant. */
public interface EntrepriseProfilRepository
        extends ReactiveCrudRepository<EntrepriseProfilEntity, UUID> {
}
