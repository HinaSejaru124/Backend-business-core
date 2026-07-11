package com.pharmacore.pharmaciebackend.fournisseur.commande;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommandeFournisseurRepository extends JpaRepository<CommandeFournisseur, UUID> {
}
