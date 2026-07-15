package com.pharmacore.pharmaciebackend.medicament;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medicament")
public class Medicament {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "offre_id", nullable = false)
    private UUID offreId;

    @Column(nullable = false)
    private String nom;

    private String dci;

    @Column(name = "forme_galenique")
    private String formeGalenique;

    @Column(name = "code_cip")
    private String codeCip;

    @Column(nullable = false)
    private String categorie;

    @Column(name = "ordonnance_requise", nullable = false)
    private boolean ordonnanceRequise;

    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prixUnitaire;

    @Column(name = "stock_actuel", nullable = false)
    private int stockActuel;

    @Column(name = "seuil_alerte", nullable = false)
    private int seuilAlerte;

    @Column(name = "fournisseur_id")
    private UUID fournisseurId;

    @Column(nullable = false)
    private String statut = "ACTIF";

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    @Column(name = "maj_le", nullable = false)
    private Instant majLe = Instant.now();

    protected Medicament() {
    }

    public Medicament(UUID offreId, String nom, String dci, String formeGalenique, String codeCip,
                      String categorie, boolean ordonnanceRequise, BigDecimal prixUnitaire,
                      int stockActuel, int seuilAlerte, UUID fournisseurId) {
        this.offreId = offreId;
        this.nom = nom;
        this.dci = dci;
        this.formeGalenique = formeGalenique;
        this.codeCip = codeCip;
        this.categorie = categorie;
        this.ordonnanceRequise = ordonnanceRequise;
        this.prixUnitaire = prixUnitaire;
        this.stockActuel = stockActuel;
        this.seuilAlerte = seuilAlerte;
        this.fournisseurId = fournisseurId;
    }

    /** Augmente le stock local (réception fournisseur) — cf. limite : pas d'équivalent BCaaS. */
    public void reapprovisionner(int quantite) {
        this.stockActuel += quantite;
        this.majLe = Instant.now();
    }

    /** Retire la fiche du catalogue actif sans la supprimer — nécessaire quand une ordonnance la référence. */
    public void retirer() {
        this.statut = "RETIRE";
        this.majLe = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOffreId() { return offreId; }
    public String getNom() { return nom; }
    public String getDci() { return dci; }
    public String getFormeGalenique() { return formeGalenique; }
    public String getCodeCip() { return codeCip; }
    public String getCategorie() { return categorie; }
    public boolean isOrdonnanceRequise() { return ordonnanceRequise; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public int getStockActuel() { return stockActuel; }
    public int getSeuilAlerte() { return seuilAlerte; }
    public UUID getFournisseurId() { return fournisseurId; }
    public String getStatut() { return statut; }
    public Instant getCreeLe() { return creeLe; }
    public Instant getMajLe() { return majLe; }
}
