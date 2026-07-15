package com.pharmacore.pharmaciebackend.vente;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vente_ligne")
public class VenteLigne {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vente_id", nullable = false)
    private UUID venteId;

    @Column(name = "medicament_id", nullable = false)
    private UUID medicamentId;

    @Column(nullable = false)
    private int quantite;

    @Column(name = "prix_unitaire_facture", nullable = false)
    private BigDecimal prixUnitaireFacture;

    protected VenteLigne() {
    }

    public VenteLigne(UUID venteId, UUID medicamentId, int quantite, BigDecimal prixUnitaireFacture) {
        this.venteId = venteId;
        this.medicamentId = medicamentId;
        this.quantite = quantite;
        this.prixUnitaireFacture = prixUnitaireFacture;
    }

    public UUID getId() { return id; }
    public UUID getVenteId() { return venteId; }
    public UUID getMedicamentId() { return medicamentId; }
    public int getQuantite() { return quantite; }
    public BigDecimal getPrixUnitaireFacture() { return prixUnitaireFacture; }
}
