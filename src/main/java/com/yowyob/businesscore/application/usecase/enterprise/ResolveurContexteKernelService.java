package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.configuration.ConfigurationService;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Résout le {@link ContexteKernel} d'une entreprise en combinant, dans l'ordre :
 * <ol>
 *   <li>l'entité {@code Entreprise} (organizationId, businessActorId, agencyId mémorisés) ;</li>
 *   <li>le {@code BusinessContext} (cashierId = acteur courant) ;</li>
 *   <li>la Configuration ({@code devise} → currency, défaut {@code XAF} ; {@code caisse_principale}
 *       → registerId) ;</li>
 *   <li>une résolution auto : si l'agence est absente, {@code GET /api/organizations/{id}/agencies}
 *       fournit la principale, qui est <b>mémorisée</b> dans l'entité.</li>
 * </ol>
 */
@Service
public class ResolveurContexteKernelService implements ResolveurContexteKernel {

    private static final String CLE_DEVISE = "devise";
    private static final String CLE_CAISSE = "caisse_principale";
    private static final String DEVISE_DEFAUT = "XAF";

    private final LireEntreprise lireEntreprise;
    private final DepotEntreprise depotEntreprise;
    private final PersisterEntreprise persisterEntreprise;
    private final ConfigurationService configurationService;

    public ResolveurContexteKernelService(LireEntreprise lireEntreprise,
                                          DepotEntreprise depotEntreprise,
                                          PersisterEntreprise persisterEntreprise,
                                          ConfigurationService configurationService) {
        this.lireEntreprise = lireEntreprise;
        this.depotEntreprise = depotEntreprise;
        this.persisterEntreprise = persisterEntreprise;
        this.configurationService = configurationService;
    }

    @Override
    public Mono<ContexteKernel> resoudre(UUID businessId) {
        return lireEntreprise.parId(businessId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Application introuvable : " + businessId)))
                .flatMap(entreprise -> Mono.zip(
                                resoudreAgence(entreprise),
                                devise(entreprise),
                                registre(entreprise),
                                cashier())
                        .map(t -> new ContexteKernel(
                                entreprise.organizationId(),
                                t.getT1().orElse(null),
                                entreprise.businessActorId(),
                                t.getT3().orElse(null),
                                t.getT4().orElse(null),
                                t.getT2())));
    }

    /** Agence mémorisée si présente, sinon résolue auprès du kernel et persistée dans l'entité. */
    private Mono<Optional<UUID>> resoudreAgence(Entreprise e) {
        if (e.agencyId() != null) {
            return Mono.just(Optional.of(e.agencyId()));
        }
        if (e.organizationId() == null) {
            return Mono.just(Optional.empty());
        }
        return persisterEntreprise.trouverAgencePrincipale(e.organizationId())
                .flatMap(agence -> depotEntreprise.sauvegarder(e.avecAgence(agence)).thenReturn(agence))
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<String> devise(Entreprise e) {
        return configurationService.resoudreValeur(e.id(), e.versionTypeId(), CLE_DEVISE)
                .onErrorResume(ex -> Mono.just(DEVISE_DEFAUT))
                .defaultIfEmpty(DEVISE_DEFAUT);
    }

    private Mono<Optional<UUID>> registre(Entreprise e) {
        return configurationService.resoudreValeur(e.id(), e.versionTypeId(), CLE_CAISSE)
                .map(valeur -> Optional.of(UUID.fromString(valeur)))
                .onErrorResume(ex -> Mono.just(Optional.<UUID>empty()))
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<Optional<UUID>> cashier() {
        return BusinessContextHolder.currentContext()
                .map(ctx -> Optional.ofNullable(ctx.actorId()))
                .defaultIfEmpty(Optional.empty());
    }
}
