package com.pharmacore.pharmaciebackend.fournisseur;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fournisseur")
public class Fournisseur {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "contact_nom")
    private String contactNom;

    @Column(name = "contact_telephone")
    private String contactTelephone;

    private String email;

    @Column(name = "delai_livraison_jours")
    private Integer delaiLivraisonJours;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected Fournisseur() {
    }

    public Fournisseur(String nom, String contactNom, String contactTelephone, String email,
                       Integer delaiLivraisonJours) {
        this.nom = nom;
        this.contactNom = contactNom;
        this.contactTelephone = contactTelephone;
        this.email = email;
        this.delaiLivraisonJours = delaiLivraisonJours;
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getContactNom() { return contactNom; }
    public String getContactTelephone() { return contactTelephone; }
    public String getEmail() { return email; }
    public Integer getDelaiLivraisonJours() { return delaiLivraisonJours; }
    public Instant getCreeLe() { return creeLe; }
}
