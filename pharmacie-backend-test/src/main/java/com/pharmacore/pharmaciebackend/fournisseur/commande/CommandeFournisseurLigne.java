package com.pharmacore.pharmaciebackend.fournisseur.commande;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "commande_fournisseur_ligne")
public class CommandeFournisseurLigne {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "commande_fournisseur_id", nullable = false)
    private UUID commandeFournisseurId;

    @Column(name = "medicament_id", nullable = false)
    private UUID medicamentId;

    @Column(name = "quantite_commandee", nullable = false)
    private int quantiteCommandee;

    @Column(name = "quantite_recue")
    private Integer quantiteRecue;

    @Column(name = "prix_unitaire_achat", nullable = false)
    private BigDecimal prixUnitaireAchat;

    protected CommandeFournisseurLigne() {
    }

    public CommandeFournisseurLigne(UUID commandeFournisseurId, UUID medicamentId,
                                    int quantiteCommandee, BigDecimal prixUnitaireAchat) {
        this.commandeFournisseurId = commandeFournisseurId;
        this.medicamentId = medicamentId;
        this.quantiteCommandee = quantiteCommandee;
        this.prixUnitaireAchat = prixUnitaireAchat;
    }

    public void marquerRecue(int quantite) {
        this.quantiteRecue = quantite;
    }

    public UUID getId() { return id; }
    public UUID getCommandeFournisseurId() { return commandeFournisseurId; }
    public UUID getMedicamentId() { return medicamentId; }
    public int getQuantiteCommandee() { return quantiteCommandee; }
    public Integer getQuantiteRecue() { return quantiteRecue; }
    public BigDecimal getPrixUnitaireAchat() { return prixUnitaireAchat; }
}
