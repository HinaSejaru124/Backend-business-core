package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import com.yowyob.businesscore.domain.enterprise.EntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseContrat;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adapter R2DBC — implémente {@link DepotEntrepriseContrat}. RLS isole le tenant. */
@Component
public class EntrepriseContratPersistenceAdapter implements DepotEntrepriseContrat {

    private final EntrepriseContratRepository repository;

    public EntrepriseContratPersistenceAdapter(EntrepriseContratRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<EntrepriseContrat> sauvegarder(EntrepriseContrat contrat) {
        return repository.findById(contrat.entrepriseId())
                .map(existant -> appliquer(existant, contrat))
                .switchIfEmpty(Mono.fromSupplier(() -> versEntityNouveau(contrat)))
                .flatMap(repository::save)
                .map(this::versDomaine);
    }

    @Override
    public Mono<EntrepriseContrat> trouverParEntreprise(UUID entrepriseId) {
        return repository.findById(entrepriseId).map(this::versDomaine);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private EntrepriseContratEntity versEntityNouveau(EntrepriseContrat c) {
        return EntrepriseContratEntity.nouveau(c.entrepriseId(), c.tenantId(), c.clePublique(),
                c.callbackUrl(), c.successUrl(), c.errorUrl(), c.cancelUrl(), c.mettreAJourLe());
    }

    private EntrepriseContratEntity appliquer(EntrepriseContratEntity e, EntrepriseContrat c) {
        e.setCallbackUrl(c.callbackUrl());
        e.setSuccessUrl(c.successUrl());
        e.setErrorUrl(c.errorUrl());
        e.setCancelUrl(c.cancelUrl());
        e.setMisAJourLe(c.mettreAJourLe());
        return e;
    }

    private EntrepriseContrat versDomaine(EntrepriseContratEntity e) {
        return new EntrepriseContrat(e.getEntrepriseId(), e.getTenantId(), e.getClePublique(),
                e.getCallbackUrl(), e.getSuccessUrl(), e.getErrorUrl(), e.getCancelUrl(), e.getMisAJourLe());
    }
}
