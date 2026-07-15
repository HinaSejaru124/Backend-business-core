package com.pharmacore.pharmaciebackend.vente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenteLigneRepository extends JpaRepository<VenteLigne, UUID> {
    List<VenteLigne> findByVenteId(UUID venteId);
}
