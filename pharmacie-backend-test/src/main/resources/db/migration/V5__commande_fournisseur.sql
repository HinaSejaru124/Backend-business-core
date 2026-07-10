CREATE TABLE commande_fournisseur (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fournisseur_id          UUID NOT NULL REFERENCES fournisseur(id),
    statut                  VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    date_commande           DATE NOT NULL,
    date_reception_prevue   DATE,
    date_reception_reelle   DATE,
    cree_le                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE commande_fournisseur_ligne (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commande_fournisseur_id  UUID NOT NULL REFERENCES commande_fournisseur(id) ON DELETE CASCADE,
    medicament_id            UUID NOT NULL REFERENCES medicament(id),
    quantite_commandee       INTEGER NOT NULL,
    quantite_recue           INTEGER,
    prix_unitaire_achat      NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_commande_fournisseur_fournisseur ON commande_fournisseur (fournisseur_id);
