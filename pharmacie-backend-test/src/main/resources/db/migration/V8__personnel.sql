CREATE TABLE personnel (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                VARCHAR(100) NOT NULL,
    prenom             VARCHAR(100) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe_hash  TEXT NOT NULL,
    role               VARCHAR(30) NOT NULL,
    acteur_kernel_id   UUID NOT NULL,
    actif              BOOLEAN NOT NULL DEFAULT true,
    cree_le            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_personnel_email ON personnel (email);
