package com.pharmacore.pharmaciebackend.medicament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MedicamentRepository extends JpaRepository<Medicament, UUID> {

    @Query("SELECT m FROM Medicament m WHERE m.stockActuel <= m.seuilAlerte")
    List<Medicament> trouverSousSeuilAlerte();
}
