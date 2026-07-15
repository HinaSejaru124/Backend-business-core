CREATE TABLE vente (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id            UUID NOT NULL,
    client_id              UUID REFERENCES client(id),
    ordonnance_id          UUID REFERENCES ordonnance(id),
    montant_total          NUMERIC(12,2) NOT NULL,
    devise                 VARCHAR(3) NOT NULL DEFAULT 'XAF',
    mode_paiement          VARCHAR(20) NOT NULL,
    statut_bcaas           VARCHAR(20) NOT NULL,
    transaction_kernel_id  TEXT,
    trace_id               UUID,
    idempotency_key        UUID NOT NULL,
    cree_le                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vente_ligne (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vente_id                UUID NOT NULL REFERENCES vente(id) ON DELETE CASCADE,
    medicament_id           UUID NOT NULL REFERENCES medicament(id),
    quantite                INTEGER NOT NULL,
    prix_unitaire_facture   NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_vente_client ON vente (client_id);
CREATE INDEX idx_vente_cree_le ON vente (cree_le);
