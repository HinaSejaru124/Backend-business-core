package com.pharmacore.pharmaciebackend.ordonnance;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ordonnance_ligne")
public class OrdonnanceLigne {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "ordonnance_id", nullable = false)
    private UUID ordonnanceId;

    @Column(name = "medicament_id", nullable = false)
    private UUID medicamentId;

    @Column(name = "quantite_prescrite", nullable = false)
    private int quantitePrescrite;

    private String posologie;

    protected OrdonnanceLigne() {
    }

    public OrdonnanceLigne(UUID ordonnanceId, UUID medicamentId, int quantitePrescrite, String posologie) {
        this.ordonnanceId = ordonnanceId;
        this.medicamentId = medicamentId;
        this.quantitePrescrite = quantitePrescrite;
        this.posologie = posologie;
    }

    public UUID getId() { return id; }
    public UUID getOrdonnanceId() { return ordonnanceId; }
    public UUID getMedicamentId() { return medicamentId; }
    public int getQuantitePrescrite() { return quantitePrescrite; }
    public String getPosologie() { return posologie; }
}
