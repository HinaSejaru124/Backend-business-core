package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.EtapeOperation;
import com.yowyob.businesscore.domain.operation.OperationAvecEtapes;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.shared.Declencheur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Use case — déclarer une opération (et ses étapes ordonnées) sous une version de Type.
 *
 * <p>Le tenant est lu du {@link BusinessContext} (jamais du payload). La version cible est résolue par
 * (typeId, numéro) via le port socle {@link PersisterVersionType} (la RLS revérifie l'appartenance).
 */
@Service
public class DeclarerOperationService {

    private final PersisterOperation persisterOperation;
    private final PersisterVersionType persisterVersionType;

    public DeclarerOperationService(PersisterOperation persisterOperation,
                                    PersisterVersionType persisterVersionType) {
        this.persisterOperation = persisterOperation;
        this.persisterVersionType = persisterVersionType;
    }

    public Mono<OperationAvecEtapes> declarer(UUID typeId, int numeroVersion, String nom,
                                              String roleDeclencheur, Declencheur declencheurRegles,
                                              boolean differe, List<EtapeDeclaration> etapes,
                                              BusinessContext ctx) {
        return persisterVersionType.trouverParTypeEtNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMap(version -> {
                    version.verifierAppartenance(ctx.tenantId());
                    UUID versionTypeId = version.id();
                    return persisterOperation.trouverParVersionEtNom(versionTypeId, nom)
                            .flatMap(existante -> Mono.<OperationAvecEtapes>error(ProblemException.conflict(
                                            "Une opération nommée '" + nom + "' existe déjà pour cette version.")
                                    .violatedRule("OPERATION_NOM_UNIQUE_PAR_VERSION")))
                            .switchIfEmpty(Mono.defer(() -> creerEtSauver(
                                    versionTypeId, nom, roleDeclencheur, declencheurRegles, differe, etapes, ctx)));
                });
    }

    private Mono<OperationAvecEtapes> creerEtSauver(UUID versionTypeId, String nom, String roleDeclencheur,
                                                    Declencheur declencheurRegles, boolean differe,
                                                    List<EtapeDeclaration> etapes, BusinessContext ctx) {
        DefinitionOperation definition = DefinitionOperation.creer(
                ctx.tenantId(), versionTypeId, nom, roleDeclencheur, declencheurRegles, differe);

        return persisterOperation.sauvegarderDefinition(definition).flatMap(sauvegardee -> {
            List<EtapeOperation> etapesDomaine = etapes.stream()
                    .sorted(Comparator.comparingInt(etape -> etape.ordre()))
                    .map(e -> EtapeOperation.creer(sauvegardee.id(), e.ordre(), e.typeEtape()))
                    .toList();
            if (etapesDomaine.isEmpty()) {
                return Mono.just(new OperationAvecEtapes(sauvegardee, List.of()));
            }
            return persisterOperation.sauvegarderEtapes(ctx.tenantId(), etapesDomaine)
                    .collectList()
                    .map(etapesSauvees -> new OperationAvecEtapes(sauvegardee, etapesSauvees));
        });
    }
}
