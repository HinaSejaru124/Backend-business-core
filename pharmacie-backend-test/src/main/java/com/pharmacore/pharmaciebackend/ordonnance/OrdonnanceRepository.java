package com.pharmacore.pharmaciebackend.ordonnance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrdonnanceRepository extends JpaRepository<Ordonnance, UUID> {
}
