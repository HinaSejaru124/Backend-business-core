package com.pharmacore.pharmaciebackend.fournisseur.commande;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "commande_fournisseur")
public class CommandeFournisseur {

    public static final String BROUILLON = "BROUILLON";
    public static final String ENVOYEE = "ENVOYEE";
    public static final String RECUE = "RECUE";
    public static final String ANNULEE = "ANNULEE";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "fournisseur_id", nullable = false)
    private UUID fournisseurId;

    @Column(nullable = false)
    private String statut = BROUILLON;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @Column(name = "date_reception_prevue")
    private LocalDate dateReceptionPrevue;

    @Column(name = "date_reception_reelle")
    private LocalDate dateReceptionReelle;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected CommandeFournisseur() {
    }

    public CommandeFournisseur(UUID fournisseurId, LocalDate dateCommande, LocalDate dateReceptionPrevue) {
        this.fournisseurId = fournisseurId;
        this.dateCommande = dateCommande;
        this.dateReceptionPrevue = dateReceptionPrevue;
    }

    public void marquerRecue(LocalDate dateReception) {
        this.statut = RECUE;
        this.dateReceptionReelle = dateReception;
    }

    public UUID getId() { return id; }
    public UUID getFournisseurId() { return fournisseurId; }
    public String getStatut() { return statut; }
    public LocalDate getDateCommande() { return dateCommande; }
    public LocalDate getDateReceptionPrevue() { return dateReceptionPrevue; }
    public LocalDate getDateReceptionReelle() { return dateReceptionReelle; }
    public Instant getCreeLe() { return creeLe; }
}
