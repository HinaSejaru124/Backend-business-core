CREATE TABLE client (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom            VARCHAR(150) NOT NULL,
    prenom         VARCHAR(150),
    telephone      VARCHAR(30),
    email          VARCHAR(200),
    adresse        VARCHAR(300),
    beneficiaire_id UUID,
    cree_le        TIMESTAMPTZ NOT NULL DEFAULT now()
);
