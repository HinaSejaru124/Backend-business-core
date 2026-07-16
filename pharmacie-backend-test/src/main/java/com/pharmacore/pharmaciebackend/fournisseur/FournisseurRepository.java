package com.pharmacore.pharmaciebackend.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FournisseurRepository extends JpaRepository<Fournisseur, UUID> {
}
