# Parcours de test E2E manuel — Business Core

> Script automatisé : [`../../scripts/e2e-parcours.sh`](../../scripts/e2e-parcours.sh)

## Prérequis

- Application BC démarrée : `mvn spring-boot:run` (port **8080** par défaut)
- Infra locale : `docker compose up -d` dans `business-core/`
- Kernel joignable (`KERNEL_BASE_URL` dans `application.yml`)
- Outils : `curl`, `jq`

## Variables d'environnement

```bash
export BC_URL=http://localhost:8080
export EMAIL="test-$(date +%s)@example.com"
export PASSWORD="MotDePasse1!Secur"
# Renseignées au fur et à mesure :
# JWT, TYPE_ID, VERSION, OFFRE_ID, ROLE_CAISSIER_ID, ROLE_CLIENT_ID
# BUSINESS_ID, ORG_ID, TRACE_ID
```

## Règle d'authentification (important)

| Surface | Headers | Usage |
| ------- | ------- | ----- |
| **Console dev** | `Authorization: Bearer <JWT>` | `/v1/api-keys`, `/v1/dashboard`, `/v1/auth/me` |
| **API intégration** | Bearer **obligatoire** + `X-BC-Client-Id` / `X-BC-Api-Key` **recommandés** | Types métier, entreprises, opérations… |
| **Public** | aucun | `/health`, `/v1/registration`, `/v1/auth/login` |

Le **Bearer JWT** délègue l'identité utilisateur au kernel (création d'organisation, exécution d'opérations).
Les headers **`X-BC-*`** identifient la clé API du développeur (suivi d'usage, acteur `X-BC-On-Behalf-Of`).
Ils se complètent : Bearer seul suffit pour tester ; en production le backend du dev envoie les deux.

Dans Swagger : **Authorize** → JWT, puis renseigner `X-BC-*` dans Try it out sur les routes d'intégration.

**À partir de l'étape 3**, toutes les routes protégées utilisent au minimum :

```bash
-H "Authorization: Bearer $JWT"
```

---

## Phase 0 — Santé

```bash
curl -s $BC_URL/health | jq .
```

**Attendu** : HTTP 200, `"status": "UP"`.

---

## Phase 1 — Inscription (`POST /v1/registration`)

Route publique, sans auth.

```bash
curl -s -X POST $BC_URL/v1/registration \
  -H "Content-Type: application/json" \
  -d "{
    \"firstName\": \"Test\",
    \"lastName\": \"Dev\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"planCode\": \"FREE\"
  }" | jq .
```

**Attendu** :

- BC appelle le kernel `POST /api/auth/sign-up`
- HTTP 201 : `{ "clientId": "bck_...", "apiKey": "...", "plan": "FREE" }`
- Conserver les clés (optionnel pour ce parcours JWT) :

```bash
export BC_CLIENT="<clientId>"
export BC_KEY="<apiKey>"
```

**Si 500** : logs `mvn spring-boot:run` — souvent kernel injoignable ou `KERNEL_CLIENT_ID`/`SECRET` invalides.

---

## Phase 2 — Login (`POST /v1/auth/login`)

```bash
curl -s -X POST $BC_URL/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"principal\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | jq .
```

**Attendu** :

- BC → kernel discover-contexts puis select-context
- HTTP 200 : `accessToken`, `expiresInSeconds`, `authorities`, `organisations`, `owner`

```bash
export JWT="<accessToken>"
```

**Si 401** : email non vérifié côté kernel ou mauvais mot de passe.

---

## Phase 3 — Identité (`GET /v1/auth/me`)

```bash
curl -s $BC_URL/v1/auth/me \
  -H "Authorization: Bearer $JWT" | jq .
```

**Attendu** : HTTP 200, `tenantId`, `actorId`, `permissions`.

---

## Phase 4–12 — Modèle métier (JWT uniquement)

> Toutes les commandes ci-dessous : `-H "Authorization: Bearer $JWT"`

### Étape 4 — Créer un type métier

Sans `domainCode` pour éviter un appel kernel domaine au premier test :

```bash
curl -s -X POST $BC_URL/v1/business-types \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"code":"PHARM","nom":"Pharmacie"}' | jq .

export TYPE_ID="<id>"
```

**Attendu** : HTTP 201, statut `BROUILLON`.

### Étape 5 — Lister / consulter

```bash
curl -s $BC_URL/v1/business-types -H "Authorization: Bearer $JWT" | jq .
curl -s $BC_URL/v1/business-types/$TYPE_ID -H "Authorization: Bearer $JWT" | jq .
```

### Étape 6 — Publier le type

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/publish \
  -H "Authorization: Bearer $JWT" | jq .
```

**Attendu** : statut `PUBLIE`.

### Étape 7 — Créer et publier la version 1

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions \
  -H "Authorization: Bearer $JWT" | jq .

curl -s $BC_URL/v1/business-types/$TYPE_ID/versions \
  -H "Authorization: Bearer $JWT" | jq .

curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/1/publish \
  -H "Authorization: Bearer $JWT" | jq .

export VERSION=1
```

### Étape 8 — Déclarer une offre

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/offers \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"nom":"Paracétamol","formePrix":"FIXE","prix":1500,"capacites":["STOCKABLE"]}' | jq .

export OFFRE_ID="<id>"
```

### Étape 9 — Déclarer des rôles métier

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/roles \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"code":"caissier","categorie":"OPERATEUR"}' | jq .

export ROLE_CAISSIER_ID="<id>"

curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/roles \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"code":"client","categorie":"BENEFICIAIRE"}' | jq .

export ROLE_CLIENT_ID="<id>"
```

### Étape 10 — Déclarer une règle (optionnel)

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/rules \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"declencheur":"AVANT_VENTE","condition":"QUANTITE_MAX:10","effet":"ALERTER","rolesAutorisesADeroger":[]}' | jq .
```

### Étape 11 — Déclarer l'opération vente

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/operations \
  -H "Authorization: Bearer $JWT" \
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
  }' | jq .
```

### Étape 12 — Configuration (devise + caisse)

```bash
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/config \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"cle":"devise","valeur":"XAF","verrouille":false}' | jq .

# Remplacer par l'UUID d'une caisse kernel (voir phase kernel)
curl -s -X POST $BC_URL/v1/business-types/$TYPE_ID/versions/$VERSION/config \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"cle":"caisse_principale","valeur":"<UUID_CAISSE_KERNEL>","verrouille":false}' | jq .
```

---

## Phase kernel (manuel, hors BC)

Après `POST /v1/businesses`, BC provisionne automatiquement : approbation, services et agence principale.
Il reste en général **manuel** sur le kernel :

| Action                           | Pourquoi                              |
| -------------------------------- | ------------------------------------- |
| Créer une caisse (cash register) | UUID pour `caisse_principale` en config |
| (Optionnel) Ouvrir session caisse | Si `bills/pay` exige une session active |

---

## Phase 13–19 — Entreprise + exécution (JWT)

### Étape 13 — Créer une entreprise

```bash
curl -s -X POST $BC_URL/v1/businesses \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d "{\"typeId\":\"$TYPE_ID\",\"versionNumber\":$VERSION,\"nom\":\"Pharmacie Test\"}" | jq .

export BUSINESS_ID="<id>"
export ORG_ID="<organizationId>"
```

Puis configurer la caisse kernel (`caisse_principale`) — voir phase kernel ci-dessus.

```bash
curl -s $BC_URL/v1/businesses -H "Authorization: Bearer $JWT" | jq .
curl -s $BC_URL/v1/businesses/$BUSINESS_ID -H "Authorization: Bearer $JWT" | jq .
```

### Étape 14 — Cycle de vie

```bash
curl -s -X PUT $BC_URL/v1/businesses/$BUSINESS_ID/lifecycle \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"cycleVie":"SUSPENDUE"}' | jq .

curl -s -X PUT $BC_URL/v1/businesses/$BUSINESS_ID/lifecycle \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"cycleVie":"ACTIVE"}' | jq .
```

### Étape 15 — Acteurs

```bash
curl -s -X POST $BC_URL/v1/businesses/$BUSINESS_ID/actors \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d "{\"roleMetierId\":\"$ROLE_CLIENT_ID\",\"identifiantPersonne\":\"$EMAIL\"}" | jq .
```

### Étape 16 — Règle locale (optionnel)

```bash
curl -s -X POST $BC_URL/v1/businesses/$BUSINESS_ID/rules \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"declencheur":"AVANT_VENTE","condition":"QUANTITE_MAX:5","effet":"BLOQUER","rolesAutorisesADeroger":[]}' | jq .
```

### Étape 17 — Lister les opérations

```bash
curl -s $BC_URL/v1/businesses/$BUSINESS_ID/operations \
  -H "Authorization: Bearer $JWT" | jq .
```

### Étape 18 — Exécuter la vente

```bash
curl -s -X POST "$BC_URL/v1/businesses/$BUSINESS_ID/operations/vente:execute" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: vente-test-001" \
  -d "{\"parametres\":{\"offreId\":\"$OFFRE_ID\",\"quantite\":1}}" | jq .

export TRACE_ID="<traceId>"
```

**Attendu** : HTTP 200, `"statut": "COMPLETEE"`, `transactionId`, `traceId`.

Rejouer la même `Idempotency-Key` : pas de double vente.

### Étape 19 — Traces

```bash
curl -s $BC_URL/v1/businesses/$BUSINESS_ID/traces \
  -H "Authorization: Bearer $JWT" | jq .

curl -s $BC_URL/v1/businesses/$BUSINESS_ID/traces/$TRACE_ID \
  -H "Authorization: Bearer $JWT" | jq .
```

---

## Dépannage

| Symptôme                                      | Cause fréquente                                        |
| --------------------------------------------- | ------------------------------------------------------ |
| 404 version introuvable (création entreprise) | Mélange clé BC + JWT dans le même parcours             |
| 401 sur routes protégées                      | JWT expiré ou absent                                   |
| 500 à l'inscription                           | Kernel injoignable ou credentials BC invalides         |
| 422 à l'exécution vente                       | Règle bloquante, org non approuvée, services manquants |
| Échec ENCAISSER                               | `caisse_principale` absent ou session caisse inactive  |
| Clé BC « espace non lié »                     | Faire un `POST /v1/auth/login` au moins une fois       |
