#!/usr/bin/env bash
# Parcours de test E2E Business Core — JWT unifié après login.
# Guide : docs/guides/test-e2e-parcours-complet.md
set -euo pipefail

BC_URL="${BC_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-test-$(date +%s)@example.com}"
PASSWORD="${PASSWORD:-MotDePasse1!Secur}"
VERSION="${VERSION:-1}"
SKIP_KERNEL_MANUAL=false
FROM_STEP=0

usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  --skip-kernel-manual   Ne pas pauser pour la phase kernel manuelle
  --from-step N          Reprendre à l'étape N (0-19)
  -h, --help             Afficher cette aide

Variables d'environnement :
  BC_URL, EMAIL, PASSWORD, JWT, TYPE_ID, VERSION, OFFRE_ID,
  ROLE_CAISSIER_ID, ROLE_CLIENT_ID, BUSINESS_ID, ORG_ID, TRACE_ID
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-kernel-manual) SKIP_KERNEL_MANUAL=true; shift ;;
    --from-step) FROM_STEP="${2:?numéro d'étape requis}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Option inconnue: $1" >&2; usage; exit 1 ;;
  esac
done

for cmd in curl jq; do
  command -v "$cmd" >/dev/null || { echo "Requis: $cmd" >&2; exit 1; }
done

auth_header() {
  if [[ -z "${JWT:-}" ]]; then
    echo "JWT non défini — exécuter d'abord l'étape 2 (login)" >&2
    exit 1
  fi
  echo "Authorization: Bearer $JWT"
}

step() {
  echo ""
  echo "=== Étape $1 — $2 ==="
}

expect_http() {
  local expected="$1"
  local actual="$2"
  local label="${3:-requête}"
  if [[ "$actual" != "$expected" ]]; then
    echo "ÉCHEC $label : HTTP $actual (attendu $expected)" >&2
    exit 1
  fi
  echo "OK HTTP $actual"
}

json_field() {
  local json="$1"
  local field="$2"
  echo "$json" | jq -r "$field // empty"
}

should_run() {
  [[ "$1" -ge "$FROM_STEP" ]]
}

# ── Étape 0 — Santé ──────────────────────────────────────────────────────────
if should_run 0; then
  step 0 "Santé"
  RESP=$(curl -s -w "\n%{http_code}" "$BC_URL/health")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 200 "$CODE" "GET /health"
  echo "$BODY" | jq .
  STATUS=$(json_field "$BODY" '.status')
  [[ "$STATUS" == "UP" ]] || { echo "status != UP" >&2; exit 1; }
fi

# ── Étape 1 — Inscription ────────────────────────────────────────────────────
if should_run 1; then
  step 1 "Inscription"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/registration" \
    -H "Content-Type: application/json" \
    -d "{
      \"firstName\": \"Test\",
      \"lastName\": \"Dev\",
      \"email\": \"$EMAIL\",
      \"password\": \"$PASSWORD\",
      \"planCode\": \"FREE\"
    }")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST /v1/registration"
  echo "$BODY" | jq .
  export BC_CLIENT
  BC_CLIENT=$(json_field "$BODY" '.clientId')
  export BC_KEY
  BC_KEY=$(json_field "$BODY" '.apiKey')
  echo "BC_CLIENT=$BC_CLIENT"
  echo "(BC_KEY défini — optionnel pour ce parcours JWT)"
fi

# ── Étape 2 — Login ──────────────────────────────────────────────────────────
if should_run 2; then
  step 2 "Login"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"principal\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 200 "$CODE" "POST /v1/auth/login"
  echo "$BODY" | jq .
  export JWT
  JWT=$(json_field "$BODY" '.accessToken')
  [[ -n "$JWT" ]] || { echo "accessToken absent" >&2; exit 1; }
  echo "JWT défini (${#JWT} caractères)"
fi

# ── Étape 3 — /auth/me ───────────────────────────────────────────────────────
if should_run 3; then
  step 3 "Identité (/v1/auth/me)"
  RESP=$(curl -s -w "\n%{http_code}" "$BC_URL/v1/auth/me" \
    -H "$(auth_header)")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 200 "$CODE" "GET /v1/auth/me"
  echo "$BODY" | jq .
fi

# ── Étape 4 — Type métier ────────────────────────────────────────────────────
if should_run 4; then
  step 4 "Créer un type métier"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"code":"PHARM","nom":"Pharmacie"}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST /v1/business-types"
  echo "$BODY" | jq .
  export TYPE_ID
  TYPE_ID=$(json_field "$BODY" '.id')
  echo "TYPE_ID=$TYPE_ID"
fi

# ── Étape 5 — Lister type ────────────────────────────────────────────────────
if should_run 5; then
  step 5 "Lister / consulter le type"
  curl -s "$BC_URL/v1/business-types" -H "$(auth_header)" | jq .
  curl -s "$BC_URL/v1/business-types/$TYPE_ID" -H "$(auth_header)" | jq .
fi

# ── Étape 6 — Publier type ───────────────────────────────────────────────────
if should_run 6; then
  step 6 "Publier le type"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/publish" \
    -H "$(auth_header)")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 200 "$CODE" "POST publish type"
  echo "$BODY" | jq .
fi

# ── Étape 7 — Version ────────────────────────────────────────────────────────
if should_run 7; then
  step 7 "Créer et publier la version 1"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions" \
    -H "$(auth_header)")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST version"
  echo "$BODY" | jq .
  curl -s "$BC_URL/v1/business-types/$TYPE_ID/versions" -H "$(auth_header)" | jq .
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/publish" \
    -H "$(auth_header)")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 200 "$CODE" "POST publish version"
  echo "$BODY" | jq .
  export VERSION
fi

# ── Étape 8 — Offre ──────────────────────────────────────────────────────────
if should_run 8; then
  step 8 "Déclarer une offre"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/offers" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"nom":"Paracétamol","formePrix":"FIXE","prix":1500,"capacites":["STOCKABLE"]}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST offre"
  echo "$BODY" | jq .
  export OFFRE_ID
  OFFRE_ID=$(json_field "$BODY" '.id')
  echo "OFFRE_ID=$OFFRE_ID"
fi

# ── Étape 9 — Rôles ──────────────────────────────────────────────────────────
if should_run 9; then
  step 9 "Déclarer des rôles métier"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/roles" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"code":"caissier","categorie":"OPERATEUR"}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST rôle caissier"
  echo "$BODY" | jq .
  export ROLE_CAISSIER_ID
  ROLE_CAISSIER_ID=$(json_field "$BODY" '.id')
  echo "ROLE_CAISSIER_ID=$ROLE_CAISSIER_ID"

  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/roles" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"code":"client","categorie":"BENEFICIAIRE"}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST rôle client"
  echo "$BODY" | jq .
  export ROLE_CLIENT_ID
  ROLE_CLIENT_ID=$(json_field "$BODY" '.id')
  echo "ROLE_CLIENT_ID=$ROLE_CLIENT_ID"
fi

# ── Étape 10 — Règle type (optionnel) ────────────────────────────────────────
if should_run 10; then
  step 10 "Déclarer une règle de type (optionnel)"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/rules" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"declencheur":"AVANT_VENTE","condition":"QUANTITE_MAX:10","effet":"ALERTER","rolesAutorisesADeroger":[]}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST règle"
  echo "$BODY" | jq .
fi

# ── Étape 11 — Opération vente ───────────────────────────────────────────────
if should_run 11; then
  step 11 "Déclarer l'opération vente"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/operations" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{
      "nom": "vente",
      "roleDeclencheur": "caissier",
      "declencheurRegles": "AVANT_VENTE",
      "differe": false,
      "etapes": [
        {"ordre": 0, "typeEtape": "ENREGISTRER_VENTE"},
        {"ordre": 1, "typeEtape": "ENCAISSER"}
      ]
    }')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST opération"
  echo "$BODY" | jq .
fi

# ── Étape 12 — Config ────────────────────────────────────────────────────────
if should_run 12; then
  step 12 "Configuration (devise)"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/config" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"cle":"devise","valeur":"XAF","verrouille":false}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST config devise"
  echo "$BODY" | jq .
  if [[ -n "${CAISSE_KERNEL_ID:-}" ]]; then
    curl -s -X POST "$BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/config" \
      -H "$(auth_header)" \
      -H "Content-Type: application/json" \
      -d "{\"cle\":\"caisse_principale\",\"valeur\":\"$CAISSE_KERNEL_ID\",\"verrouille\":false}" | jq .
  else
    echo "(CAISSE_KERNEL_ID non défini — config caisse_principale ignorée)"
  fi
fi

# ── Étape 13 — Entreprise ────────────────────────────────────────────────────
if should_run 13; then
  step 13 "Créer une entreprise"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/businesses" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d "{\"typeId\":\"$TYPE_ID\",\"versionNumber\":$VERSION,\"nom\":\"Pharmacie Test\"}")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST entreprise"
  echo "$BODY" | jq .
  export BUSINESS_ID ORG_ID
  BUSINESS_ID=$(json_field "$BODY" '.id')
  ORG_ID=$(json_field "$BODY" '.organizationId')
  echo "BUSINESS_ID=$BUSINESS_ID"
  echo "ORG_ID=$ORG_ID"

  if [[ "$SKIP_KERNEL_MANUAL" != true ]]; then
    echo ""
    echo "────────────────────────────────────────────────────────────"
    echo "PAUSE — Phase kernel manuelle requise sur ORG_ID=$ORG_ID"
    echo "  1. POST /api/organizations/$ORG_ID/approve"
    echo "  2. POST /api/organizations/$ORG_ID/services (COMMERCIAL, ACCOUNTING, CASHIER, PRODUCT)"
    echo "  3. Créer une caisse → export CAISSE_KERNEL_ID=<uuid> puis relancer --from-step 12"
    echo "────────────────────────────────────────────────────────────"
    read -r -p "Appuyer sur Entrée pour continuer (ou Ctrl-C)..."
  fi
fi

# ── Étape 14 — Lifecycle ─────────────────────────────────────────────────────
if should_run 14; then
  step 14 "Cycle de vie"
  curl -s -X PUT "$BC_URL/v1/businesses/$BUSINESS_ID/lifecycle" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"cycleVie":"SUSPENDUE"}' | jq .
  curl -s -X PUT "$BC_URL/v1/businesses/$BUSINESS_ID/lifecycle" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"cycleVie":"ACTIVE"}' | jq .
fi

# ── Étape 15 — Acteurs ───────────────────────────────────────────────────────
if should_run 15; then
  step 15 "Rattacher un acteur"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/businesses/$BUSINESS_ID/actors" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d "{\"roleMetierId\":\"$ROLE_CLIENT_ID\",\"identifiantPersonne\":\"$EMAIL\"}")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST acteur"
  echo "$BODY" | jq .
fi

# ── Étape 16 — Règle locale (optionnel) ──────────────────────────────────────
if should_run 16; then
  step 16 "Règle locale (optionnel)"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/businesses/$BUSINESS_ID/rules" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -d '{"declencheur":"AVANT_VENTE","condition":"QUANTITE_MAX:5","effet":"BLOQUER","rolesAutorisesADeroger":[]}')
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  expect_http 201 "$CODE" "POST règle locale"
  echo "$BODY" | jq .
fi

# ── Étape 17 — Lister opérations ─────────────────────────────────────────────
if should_run 17; then
  step 17 "Lister les opérations de l'entreprise"
  curl -s "$BC_URL/v1/businesses/$BUSINESS_ID/operations" \
    -H "$(auth_header)" | jq .
fi

# ── Étape 18 — Exécuter vente ────────────────────────────────────────────────
if should_run 18; then
  step 18 "Exécuter la vente"
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BC_URL/v1/businesses/$BUSINESS_ID/operations/vente:execute" \
    -H "$(auth_header)" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: vente-test-001" \
    -d "{\"parametres\":{\"offreId\":\"$OFFRE_ID\",\"quantite\":1}}")
  BODY=$(echo "$RESP" | head -n -1)
  CODE=$(echo "$RESP" | tail -n 1)
  echo "$BODY" | jq .
  if [[ "$CODE" != "200" && "$CODE" != "202" ]]; then
    echo "ÉCHEC exécution vente : HTTP $CODE" >&2
    exit 1
  fi
  echo "OK HTTP $CODE"
  export TRACE_ID
  TRACE_ID=$(json_field "$BODY" '.traceId')
  echo "TRACE_ID=$TRACE_ID"
fi

# ── Étape 19 — Traces ────────────────────────────────────────────────────────
if should_run 19; then
  step 19 "Consulter les traces"
  curl -s "$BC_URL/v1/businesses/$BUSINESS_ID/traces" \
    -H "$(auth_header)" | jq .
  if [[ -n "${TRACE_ID:-}" ]]; then
    curl -s "$BC_URL/v1/businesses/$BUSINESS_ID/traces/$TRACE_ID" \
      -H "$(auth_header)" | jq .
  fi
fi

echo ""
echo "Parcours terminé."
