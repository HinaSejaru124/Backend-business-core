-- Initialisation des rôles PostgreSQL pour la défense en profondeur multi-tenant.
-- Exécuté une seule fois par l'image postgres au premier démarrage (superuser).
--
-- Deux rôles distincts, clé de la Barrière 3 (RLS) :
--   bc_owner : propriétaire du schéma, exécute les migrations Liquibase (JDBC). PEUT créer/modifier.
--   bc_app   : rôle applicatif runtime (R2DBC). NON-owner, sans BYPASSRLS : soumis aux policies RLS.
--
-- Un rôle applicatif non-owner est indispensable : un superuser ou le propriétaire d'une table
-- contourne RLS par défaut. bc_app garantit que la base refuse physiquement les lignes d'un autre tenant.

CREATE ROLE bc_owner WITH LOGIN PASSWORD 'bc_owner';
CREATE ROLE bc_app   WITH LOGIN PASSWORD 'bc_app';

-- bc_owner possède le schéma public et peut y créer les tables (PG15+ retire CREATE à PUBLIC).
GRANT CREATE, USAGE ON SCHEMA public TO bc_owner;

-- bc_app peut lire/écrire mais pas créer de structure.
GRANT USAGE ON SCHEMA public TO bc_app;

-- Toute table créée plus tard par bc_owner (via Liquibase) accorde automatiquement le DML à bc_app.
ALTER DEFAULT PRIVILEGES FOR ROLE bc_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bc_app;

ALTER DEFAULT PRIVILEGES FOR ROLE bc_owner IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO bc_app;
