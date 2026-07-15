package com.pharmacore.pharmaciebackend.personnel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Membre du personnel (Pharmacien Responsable ou Caissier) — compte propre à PharmaCore.
 * {@code acteurKernelId} est résolu une seule fois à la création ({@link PersonnelService#creer}) et
 * ne change plus jamais : la connexion quotidienne ne touche que {@code motDePasseHash}, en local.
 */
@Entity
@Table(name = "personnel")
public class Personnel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "acteur_kernel_id", nullable = false)
    private UUID acteurKernelId;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected Personnel() {
    }

    public Personnel(String nom, String prenom, String email, String motDePasseHash, String role,
                     UUID acteurKernelId) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.role = role;
        this.acteurKernelId = acteurKernelId;
    }

    public void desactiver() {
        this.actif = false;
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getMotDePasseHash() { return motDePasseHash; }
    public String getRole() { return role; }
    public UUID getActeurKernelId() { return acteurKernelId; }
    public boolean isActif() { return actif; }
    public Instant getCreeLe() { return creeLe; }
}
