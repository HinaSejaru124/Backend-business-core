package com.yowyob.businesscore.application.saga;

/**
 * Clés conventionnelles du {@code ContexteEtape} partagé entre les étapes d'une opération.
 *
 * <p>Le contexte est un sac de données (immuable) que chaque étape lit/enrichit pour la suivante.
 * Centraliser les clés ici évite les fautes de frappe et documente le vocabulaire d'échange.
 */
public final class ClesContexte {

    private ClesContexte() {
    }

    // ─── Entrées (posées par la couche application depuis le payload / l'entreprise) ───
    public static final String TENANT_ID = "tenantId";
    public static final String ENTREPRISE_ID = "entrepriseId";
    public static final String VERSION_TYPE_ID = "versionTypeId";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String OPERATION_NOM = "operationNom";
    public static final String DECLENCHEUR = "declencheur";   // Declencheur pour EVALUER_REGLES
    public static final String ROLES = "roles";               // Set<String> rôles de l'acteur courant
    public static final String OFFRE_ID = "offreId";
    public static final String QUANTITE = "quantite";
    public static final String BENEFICIAIRE_ID = "beneficiaireId";
    public static final String CATEGORIE = "categorie";       // pour la condition de règle CATEGORIE_EGALE
    public static final String MOTIF = "motif";               // motif de dérogation (effet DEROGER)
    public static final String DOCUMENT_NOM = "documentNom";
    public static final String DOCUMENT_CONTENT_TYPE = "documentContentType";
    public static final String DOCUMENT_CONTENU = "documentContenu"; // byte[] ou base64

    // ─── Sorties (posées par les étapes) ───
    public static final String STOCK = "stock";
    public static final String MONTANT = "montant";
    public static final String DEVISE = "devise";
    public static final String TRANSACTION_KERNEL_ID = "transactionKernelId"; // billId cashier — cible de l'encaissement + réf. transaction
    public static final String COMMANDE_ID = "commandeId"; // id commande vente (order) — point de compensation
    public static final String PRODUCT_ID = "productId"; // id produit kernel résolu pour l'offre
    public static final String MOUVEMENT_STOCK_ID = "mouvementStockId";
    public static final String MOUVEMENT_ID = "mouvementId"; // id du paiement enregistré (encaissement)
    public static final String DOCUMENT_ID = "documentId";
    public static final String RESULTAT_REGLES = "resultatRegles"; // List<EffetAAppliquer> (audit)
}
