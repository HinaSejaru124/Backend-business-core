package com.pharmacore.pharmaciebackend.ordonnance;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ordonnance")
public class Ordonnance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "medecin_nom", nullable = false)
    private String medecinNom;

    @Column(name = "medecin_numero_ordre")
    private String medecinNumeroOrdre;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    @Column(name = "document_nom")
    private String documentNom;

    @Column(name = "document_content_type")
    private String documentContentType;

    /** Contenu réel du fichier (scan de l'ordonnance) — stockage local, pas encore transmis à BCaaS. */
    @Column(name = "document_contenu")
    private byte[] documentContenu;

    /** Renseigné après un appel à l'étape ATTACHER_DOCUMENT lors d'une vente (cf. GUIDE §5). */
    @Column(name = "document_id_bcaas")
    private UUID documentIdBcaas;

    @Column(nullable = false)
    private String statut = "VALIDE";

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected Ordonnance() {
    }

    public Ordonnance(UUID clientId, String medecinNom, String medecinNumeroOrdre,
                      LocalDate dateEmission, String documentNom, String documentContentType,
                      byte[] documentContenu) {
        this.clientId = clientId;
        this.medecinNom = medecinNom;
        this.medecinNumeroOrdre = medecinNumeroOrdre;
        this.dateEmission = dateEmission;
        this.documentNom = documentNom;
        this.documentContentType = documentContentType;
        this.documentContenu = documentContenu;
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public String getMedecinNom() { return medecinNom; }
    public String getMedecinNumeroOrdre() { return medecinNumeroOrdre; }
    public LocalDate getDateEmission() { return dateEmission; }
    public String getDocumentNom() { return documentNom; }
    public String getDocumentContentType() { return documentContentType; }
    public byte[] getDocumentContenu() { return documentContenu; }
    public UUID getDocumentIdBcaas() { return documentIdBcaas; }
    public String getStatut() { return statut; }
    public Instant getCreeLe() { return creeLe; }
}
