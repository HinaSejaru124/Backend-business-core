// adapter/out/persistence/rule/RegistreDeReglesAdapter.java
package com.yowyob.businesscore.adapter.out.persistence.rule;

import com.yowyob.businesscore.domain.port.out.RegistreDeRegles;
import com.yowyob.businesscore.domain.port.out.RegleChargee;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Adapter de persistance implémentant le port socle {@link RegistreDeRegles}.
 *
 * <p>Pour un déclencheur donné, fusionne deux ensembles de règles applicables :
 * les règles <b>de Type</b> (toutes celles du tenant courant, isolées par la RLS) et les règles
 * <b>locales</b> de l'entreprise ciblée si un {@code entrepriseId} est fourni. Le résultat est projeté
 * en {@link RegleChargee}, vue minimale et stable consommée par l'évaluateur.
 */
@Component
public class RegistreDeReglesAdapter implements RegistreDeRegles {

    private final RegleMetierRepository repo;

    public RegistreDeReglesAdapter(RegleMetierRepository repo) {
        this.repo = repo;
    }

    @Override
    public Flux<RegleChargee> chargerPourDeclencheur(UUID entrepriseId, Declencheur declencheur) {
        // Règles de Type (porteeType=true) + règles locales de cette entreprise
        Flux<RegleMetierEntity> deType = repo.findByDeclencheurAndVersionTypeIdNotNull(
                declencheur.name());

        Flux<RegleMetierEntity> locales = entrepriseId != null
                ? repo.findByDeclencheurAndEntrepriseId(declencheur.name(), entrepriseId)
                : Flux.empty();

        return Flux.merge(deType, locales).map(this::toRegleChargee);
    }

    private RegleChargee toRegleChargee(RegleMetierEntity e) {
        return new RegleChargee(
                e.getId(),
                Declencheur.valueOf(e.getDeclencheur()),
                e.getCondition(),
                Effet.valueOf(e.getEffet()),
                e.getRolesAutorisesADeroger(),
                e.getVersionTypeId() != null   // porteeType = true si règle de type
        );
    }
}