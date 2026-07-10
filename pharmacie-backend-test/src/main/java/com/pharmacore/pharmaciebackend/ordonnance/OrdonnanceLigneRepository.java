package com.pharmacore.pharmaciebackend.ordonnance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrdonnanceLigneRepository extends JpaRepository<OrdonnanceLigne, UUID> {
    List<OrdonnanceLigne> findByOrdonnanceId(UUID ordonnanceId);
}
