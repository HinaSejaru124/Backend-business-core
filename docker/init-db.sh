#!/bin/bash
# Initialise le cluster PostgreSQL au tout premier démarrage du conteneur
# (répertoire $PGDATA vide) : initdb + création des rôles applicatifs.
set -euo pipefail

: "${BC_DB_NAME:=businesscore}"
BC_DB_OWNER_USER=bc_owner
BC_DB_APP_USER=bc_app
: "${BC_DB_OWNER_PASSWORD:?BC_DB_OWNER_PASSWORD est requis}"
: "${BC_DB_APP_PASSWORD:?BC_DB_APP_PASSWORD est requis}"

if [ -s "$PGDATA/PG_VERSION" ]; then
    echo "[init-db] PGDATA déjà initialisé, on passe."
    exit 0
fi

echo "[init-db] Initialisation du cluster PostgreSQL dans $PGDATA"
gosu postgres initdb -D "$PGDATA" --auth=trust --auth-host=trust >/dev/null

echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
echo "listen_addresses = '*'" >> "$PGDATA/postgresql.conf"

gosu postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses='localhost'" -w start

gosu postgres psql -v ON_ERROR_STOP=1 --username postgres <<-SQL
    CREATE ROLE ${BC_DB_OWNER_USER} WITH LOGIN PASSWORD '${BC_DB_OWNER_PASSWORD}';
    CREATE ROLE ${BC_DB_APP_USER}   WITH LOGIN PASSWORD '${BC_DB_APP_PASSWORD}';
    CREATE DATABASE ${BC_DB_NAME} OWNER ${BC_DB_OWNER_USER};
SQL

gosu postgres psql -v ON_ERROR_STOP=1 --username postgres --dbname "${BC_DB_NAME}" \
    -f /docker-init/01-roles.sql

gosu postgres pg_ctl -D "$PGDATA" -m fast -w stop

echo "[init-db] Terminé."
