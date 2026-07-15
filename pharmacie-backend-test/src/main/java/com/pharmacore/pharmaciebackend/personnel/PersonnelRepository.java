package com.pharmacore.pharmaciebackend.personnel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonnelRepository extends JpaRepository<Personnel, UUID> {
    Optional<Personnel> findByEmailAndActifTrue(String email);
    boolean existsByEmail(String email);
    List<Personnel> findAllByOrderByCreeLeDesc();

    /** Identité kernel déjà résolue pour ce rôle, s'il existe déjà un membre du personnel — pour ne
     * jamais réinscrire un acteur kernel par employé (une seule identité kernel par rôle, réutilisée). */
    Optional<Personnel> findFirstByRoleOrderByCreeLeAsc(String role);
}
