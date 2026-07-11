package com.pharmacore.pharmaciebackend.client;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nom;

    private String prenom;
    private String telephone;
    private String email;
    private String adresse;

    /** Rempli seulement si un Acteur BENEFICIAIRE a été créé côté BCaaS pour ce client (optionnel). */
    @Column(name = "beneficiaire_id")
    private UUID beneficiaireId;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected Client() {
    }

    public Client(String nom, String prenom, String telephone, String email, String adresse) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getTelephone() { return telephone; }
    public String getEmail() { return email; }
    public String getAdresse() { return adresse; }
    public UUID getBeneficiaireId() { return beneficiaireId; }
    public Instant getCreeLe() { return creeLe; }
}
