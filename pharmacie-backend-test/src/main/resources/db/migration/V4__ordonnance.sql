CREATE TABLE ordonnance (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id             UUID NOT NULL REFERENCES client(id),
    medecin_nom           VARCHAR(200) NOT NULL,
    medecin_numero_ordre  VARCHAR(50),
    date_emission         DATE NOT NULL,
    document_nom          VARCHAR(300),
    document_content_type VARCHAR(100),
    document_id_bcaas     UUID,
    statut                VARCHAR(20) NOT NULL DEFAULT 'VALIDE',
    cree_le               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ordonnance_ligne (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordonnance_id       UUID NOT NULL REFERENCES ordonnance(id) ON DELETE CASCADE,
    medicament_id       UUID NOT NULL REFERENCES medicament(id),
    quantite_prescrite  INTEGER NOT NULL,
    posologie           VARCHAR(300)
);

CREATE INDEX idx_ordonnance_client ON ordonnance (client_id);
