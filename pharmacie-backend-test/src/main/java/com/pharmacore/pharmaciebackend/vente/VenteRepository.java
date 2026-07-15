package com.pharmacore.pharmaciebackend.vente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VenteRepository extends JpaRepository<Vente, UUID> {

    List<Vente> findAllByOrderByCreeLeDesc();

    long countByCreeLeAfter(Instant instant);

    @Query("SELECT COALESCE(SUM(v.montantTotal), 0) FROM Vente v WHERE v.creeLe >= :instant")
    BigDecimal sommeMontantDepuis(Instant instant);
}
