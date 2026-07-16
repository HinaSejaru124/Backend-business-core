CREATE TABLE fournisseur (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                    VARCHAR(200) NOT NULL,
    contact_nom            VARCHAR(200),
    contact_telephone      VARCHAR(30),
    email                  VARCHAR(200),
    delai_livraison_jours  INTEGER,
    cree_le                TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE medicament ADD COLUMN fournisseur_id UUID REFERENCES fournisseur(id);
