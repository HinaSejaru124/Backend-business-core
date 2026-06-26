# Modifications restantes — Business Core

> État après la mise à jour du backend (audit du dépôt). Les étapes 1, 2 et 3 de la remédiation kernel sont largement faites et **tous les endpoints appelés existent dans le kernel**. Ce document liste ce qui reste, par priorité. À donner à ton IA avec `REFERENCE-KERNEL.md` et `IDENTIFIANTS-KERNEL.md`.

## Résumé de l'état

| Étape | État |
|---|---|
| 1. Enveloppe ApiResponse | ✅ fait (KernelClient détecte `{success, data}`, gère errorCode) |
| 2. ResolveurContexteKernel | ✅ créé (port + record + service ; Entreprise enrichie) |
| 3. Adapters (entreprise, produit, bénéficiaire, audit) | ✅ majoritairement fait |
| 4. Paiement | ⚠️ **méthode à revoir** (voir §1 — point critique) |
| 5. Cycle de vie | ❌ à faire |
| Transverse — brancher le résolveur | ❌ à faire (le résolveur n'est injecté nulle part) |

---

## 1. CRITIQUE — Revoir la méthode de paiement

### Le problème
L'adapter `PorteMonnaieMonetaireAdapter` paie via `POST /api/{document}/{id}/payments/cashier`, en mappant le `linkedDocumentType` du bill vers l'un des **7 documents payables**. Or, vérification faite dans la spec kernel :

- Les 7 documents payables sont **tous des documents d'achat/fournisseur** : bon-commande, bons-achat, bons-livraison, facture-fournisseurs, factures-proforma, bon-receptions, note-credits.
- **Aucun n'est une vente client.** Une `sales/order` ne génère pas l'un de ces 7 documents.
- `SalesOrderResponse` **ne contient aucune référence** à un bill ni à un document payable.
- L'appel `GET /api/cashier/bills/{orderId}` suppose que `orderId == billId` — rien ne le garantit ; ce sont deux entités distinctes.

**Conséquence** : pour une vente client, la chaîne actuelle ne trouvera pas de document payable valide → l'encaissement échouera.

### Le chemin correct (vérifié dans la spec)
Pour encaisser une vente client :
```
1. POST /api/accounting/invoices/from-orders/{orderId}   → génère une facture client depuis la commande → invoiceId
2. POST /api/bills/import/accounting-invoices/{invoiceId} → crée le bill cashier depuis la facture → billId
   (ou POST /api/bills avec customerId, reference, totalAmount, currency)
3. POST /api/bills/pay  { amount, sessionId, registerId } → paie le bill
```

### À modifier
- **`EnregistrerVenteKernelAdapter`** : après `confirm`, appeler `POST /api/accounting/invoices/from-orders/{orderId}` pour obtenir l'`invoiceId`, puis créer/importer le bill (`billId`). Renvoyer `billId` (et non un `linkedDocumentType` qui n'existera pas pour une vente).
- **`VenteEnregistree`** : remplacer `payableType`/`payableId` par `billId` (UUID).
- **`PorteMonnaieGenerique` / `ReferencePayable`** : la « référence payable » d'une vente devient le `billId`. L'implémentation monétaire appelle `POST /api/bills/pay { amount, sessionId, registerId }`.
- **`PorteMonnaieMonetaireAdapter`** : remplacer la table des 7 documents par l'appel `bills/pay`. Garder la map des 7 documents **uniquement** si on veut aussi gérer le paiement de documents d'achat (cas distinct, pas la vente) — sinon la supprimer.
- **`sessionId` / `registerId`** : viennent du `ResolveurContexteKernel` (voir §3). `sessionId` = session de caisse ouverte ; politique de session à fixer (cf. §1.1).

### 1.1 Décision à documenter : la session de caisse
`PayBillRequest` accepte `sessionId` (optionnel dans le schéma). Deux options :
- **A.** Ne pas passer `sessionId` et espérer que le kernel rattache une session ouverte par défaut. Simple mais non garanti — **à confirmer en test réel**.
- **B.** Résoudre la session ouverte de la caisse via `GET /api/cashier/sessions` (filtré sur `registerId`), en ouvrir une (`POST /api/sessions`) si aucune n'est active. Robuste.

Recommandation : **B**, avec une politique « une session ouverte par caisse et par jour », gérée par le résolveur ou un petit service de session. Documenter le choix.

---

## 2. IMPORTANT — Compléter VerifierDisponibilite

### Problème
L'adapter envoie seulement `productId` :
```
GET /api/inventory/movements/balance?productId=...
```
Le kernel exige **trois** query params obligatoires : `organizationId`, `agencyId`, `productId`. L'appel échouera (400) en l'état.

### À modifier
- Injecter `ResolveurContexteKernel` dans `VerifierDisponibiliteKernelAdapter`.
- Résoudre le `ContexteKernel` (à partir du `businessId` courant), puis :
```
GET /api/inventory/movements/balance?organizationId={org}&agencyId={agency}&productId={produit}
```
- La signature du port `VerifierDisponibilite.soldeStock` devra peut-être recevoir le `businessId` (ou le résolveur lit le `BusinessContext`).

---

## 3. TRANSVERSE — Brancher le ResolveurContexteKernel

### Problème
Le résolveur est créé mais **injecté nulle part**. Tous les adapters qui ont besoin de `organizationId`, `agencyId`, `currency`, `registerId`, `cashierId` utilisent encore des valeurs incomplètes ou des défauts en dur.

### À modifier (chaque adapter concerné)
Injecter `ResolveurContexteKernel`, appeler `resoudre(businessId)` en tête de la méthode, puis utiliser le `ContexteKernel` pour remplir les champs kernel :

| Adapter | Champs à tirer du résolveur |
|---|---|
| `VerifierDisponibiliteKernelAdapter` | organizationId, agencyId |
| `CatalogueOffreKernelAdapter` (produit) | organizationId, currency |
| `EnregistrerVenteKernelAdapter` | organizationId, agencyId, customerThirdPartyId, currency |
| `PorteMonnaieMonetaireAdapter` | registerId, cashierId, currency, sessionId |
| `PersisterEntrepriseKernelAdapter` | service, rôle (depuis KernelProperties) |

> Note : le `businessId` doit circuler jusqu'à l'adapter. Soit via le `BusinessContext` (lu par le résolveur), soit en paramètre de la méthode du port. Choisir une convention et l'appliquer partout.

---

## 4. Produit — vérifier les champs obligatoires

### À vérifier
`CreateProductRequest` exige : `organizationId*`, `sku*`, `name*`, `familyCode*`, `variantLabel*`, `unitPrice*`, `currency*`. Confirmer que `CatalogueOffreKernelAdapter` les fournit tous, avec dérivation (cf. `IDENTIFIANTS-KERNEL.md`) :
- `sku` = `OFFRE-{code}-V{n}` (dérivé)
- `familyCode` = mapping Type/businessDomain, défaut `GENERAL`
- `variantLabel` = défaut `STANDARD`
- `organizationId`, `currency` = résolveur

---

## 5. Cycle de vie de l'Entreprise (étape 5 — non faite)

### À faire
Mapper `Entreprise.cycleVie` sur les transitions kernel (toutes vérifiées présentes) :

| Transition métier | Endpoint kernel |
|---|---|
| activer | `POST /api/organizations/{id}/approve` |
| suspendre | `POST /api/organizations/{id}/suspend` |
| fermer | `POST /api/organizations/{id}/close` |
| rouvrir | `POST /api/organizations/{id}/reopen` |

- Créer un port (ou étendre `PersisterEntreprise`) avec `changerCycleVie(businessId, cible)`.
- L'endpoint exposé `PUT /v1/businesses/{id}/lifecycle` appelle la transition correspondante puis met à jour `Entreprise.cycleVie`.

---

## 6. Tests — renforcer la couverture

Le dépôt a ~21 classes de test pour 234 de code. Les nouveaux adapters kernel manquent de tests. Pour chaque adapter modifié ci-dessus, ajouter un test **WireMock** vérifiant :
- la/les bonne(s) URL(s) appelée(s) et l'ordre (pour les façades multi-appels),
- les champs obligatoires présents dans le corps,
- le désenveloppage correct (réponse `{success, data}` → objet métier),
- le chemin d'erreur (`errorCode` non nul → erreur).

Priorité : `EnregistrerVente` (chaîne order→invoice→bill), `PorteMonnaie` (bills/pay), `VerifierDisponibilite` (3 params).

---

## Ordre conseillé
1. **§3 brancher le résolveur** (débloque §2 et §1 — fondation).
2. **§2 VerifierDisponibilite** (rapide, échoue sinon).
3. **§1 paiement** (le plus gros — revoir la chaîne order→invoice→bill→pay).
4. **§4 produit** (vérification + dérivation).
5. **§5 cycle de vie** (indépendant).
6. **§6 tests** (au fil de l'eau, sur chaque point ci-dessus).

À chaque point : `mvn test` vert avant de continuer.
