-- Catalogue des médicaments. offre_id référence l'Offre déclarée côté Business Core
-- (POST /v1/business-types/{typeId}/versions/{n}/offers) — pas de FK physique (systèmes séparés).
CREATE TABLE medicament (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offre_id            UUID NOT NULL,
    nom                 VARCHAR(200) NOT NULL,
    dci                 VARCHAR(200),
    forme_galenique     VARCHAR(100),
    code_cip            VARCHAR(50),
    categorie           VARCHAR(50) NOT NULL,
    ordonnance_requise  BOOLEAN NOT NULL DEFAULT FALSE,
    prix_unitaire       NUMERIC(12,2) NOT NULL,
    stock_actuel        INTEGER NOT NULL DEFAULT 0,
    seuil_alerte        INTEGER NOT NULL DEFAULT 10,
    statut              VARCHAR(20) NOT NULL DEFAULT 'ACTIF',
    cree_le             TIMESTAMPTZ NOT NULL DEFAULT now(),
    maj_le              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_medicament_code_cip ON medicament (code_cip) WHERE code_cip IS NOT NULL;
CREATE INDEX idx_medicament_offre_id ON medicament (offre_id);
CREATE INDEX idx_medicament_categorie ON medicament (categorie);
