package com.yowyob.businesscore.application.capacite;

import com.yowyob.businesscore.domain.port.internal.offer.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Résout la stratégie FournisseurDeCapacite à partir du TypeCapacite.
 * Spring injecte toutes les implémentations présentes (STOCKABLE livré, autres à venir).
 */
@Component
public class RegistreCapacites {

    private final Map<TypeCapacite, FournisseurDeCapacite> parType;

    public RegistreCapacites(List<FournisseurDeCapacite> fournisseurs) {
        this.parType = fournisseurs.stream()
                .collect(Collectors.toMap(FournisseurDeCapacite::type, Function.identity()));
    }

    public Optional<FournisseurDeCapacite> pour(TypeCapacite type) {
        return Optional.ofNullable(parType.get(type));
    }
}