# Résultats du spike Phase 0 — 2026-07-08/09

**Environnement testé** : `backend-business-core` sur `develop2` (commits `c490771`, `f53f367`, `0428bc5`), backend recompilé et relancé après pull, Redis démarré via `docker run -d --name businesscore-redis -p 6379:6379 redis:7-alpine` (aucun Redis local n'existait auparavant — nécessaire au démarrage, la config par défaut pointe sur `localhost:6379`).

**Compte de test** : nouveau compte créé via `POST /v1/registration` avec l'email déjà vérifié `techlan50@gmail.com` (le kernel accepte une ré-inscription sur un email déjà validé). Clé activée après un `POST /v1/auth/login`. `tenantId` (kernel) : `f68788d9-fdfb-4f57-a7a8-00e336995e79`.

---

## Ce qui fonctionne (validé empiriquement, réponses réelles ci-dessous)

| Étape | Requête | Résultat |
|---|---|---|
| Créer le type | `POST /v1/business-types` `{"code":"PHARMA_TEST","nom":"Pharmacie Test"}` | `201` — `{"id":"175a0f0e-...","statut":"BROUILLON"}` |
| Publier le type | `POST /v1/business-types/{id}/publish` | `200` — `"statut":"PUBLIE"` |
| Créer la version 1 | `POST /v1/business-types/{id}/versions` | `201` — `"numero":1,"immuable":false` |
| Publier la version 1 | `POST /v1/business-types/{id}/versions/1/publish` | `200` — `"immuable":true,"publieeLe":"2026-07-08T23:11:56.81Z"` |
| Créer l'offre (médicament) | `POST .../versions/1/offers` `{"nom":"Paracetamol 500mg","formePrix":"FIXE","prix":500,"capacites":["STOCKABLE"]}` | `201` — capacité `STOCKABLE` active confirmée |

**Conclusion partielle** : les briques 1 (Type Métier), 2 (Offre), 7 (Configuration, non testée ici mais même mécanisme) fonctionnent normalement avec une clé API fraîchement créée, **à condition de ne pas fournir `domainCode`/`domainNom`** (voir blocage n°1).

---

## Blocage n°1 — `domainCode`/`domainNom` (paramètres optionnels de `POST /v1/business-types`)

Avec `{"code":"PHARMA_TEST","nom":"Pharmacie Test","domainCode":"SANTE","domainNom":"Sante"}` → `500 Erreur interne`.

**Cause exacte** (log backend) : `TypeMetierService.creer()` appelle `ResoudreBusinessDomain` qui tente d'obtenir un token machine (kernel, grant `client_credentials`) → **`400 Bad Request` du kernel** sur `POST https://kernel-core.yowyob.com/oauth2/token`. Voir blocage n°2 pour la cause racine.

**Contournement retenu pour PharmaCore** : ne jamais envoyer `domainCode`/`domainNom` (champs optionnels). Le type métier n'a alors pas de `businessDomainId` (reste `null`) — sans impact fonctionnel connu pour la suite du spike.

---

## Blocage n°2 (racine, bloquant) — Le Kernel n'accepte plus le grant `client_credentials`

**`POST /v1/businesses` (créer l'entreprise) échoue systématiquement en `500`.**

Cause exacte, vérifiée directement contre le Kernel en dehors de Business Core (donc indépendante de tout bug PharmaCore) :

```
curl -X POST https://kernel-core.yowyob.com/oauth2/token \
  -u "prod-platform-backend:VbWi225xzYPoD8rQ2FRniTAqkylh34XYeWxa9HCU" \
  -d "grant_type=client_credentials"

→ 400 { "error": "invalid_request", "error_description": "Unsupported grant_type." }
```

Ce test reproduit **exactement** la requête envoyée en interne par `KernelTokenService.java` (Basic Auth + `grant_type=client_credentials`, vérifié ligne à ligne dans le code). **Le Kernel a désactivé ce grant type** — ce n'est pas un problème de secret expiré ou d'identifiants faux, c'est le mécanisme d'authentification machine lui-même qui n'est plus supporté côté Kernel.

**Chemin de code impacté** : `KernelCredentialStore.pourTenantCourant()` — utilisé pour **tout appel kernel fait pour un compte qui n'a pas ses propres identifiants kernel délégués** (`kernelClientId == null`), ce qui est le cas de **tous** les comptes créés via le flux unifié actuel `POST /v1/registration` (il ne provisionne pas de credentials kernel délégués — cf. `ProvisionnerAccesDevAdapter`, un stub non implémenté). Concrètement : **tout développeur inscrit aujourd'hui est bloqué dès qu'une action nécessite un appel kernel de type "plateforme"** — notamment `PersisterEntreprise` (création de l'Organization kernel), donc **`POST /v1/businesses` est cassé pour tout le monde en l'état actuel**, pas seulement pour PharmaCore.

**Ce que ça confirme** : ceci correspond exactement à la tâche déjà identifiée par l'équipe (« authentification déléguée : passer du grant `client_credentials` au flux discover-contexts → select-context → refresh », cf. mémoire projet / `docs/AUTHENTIFICATION-DELEGUEE (2).md`). Ce n'est **pas** un problème introduit par PharmaCore — c'est une dépendance bloquante côté socle Business Core, déjà connue de l'équipe.

**Décision** : ne pas contourner ni patcher `KernelCredentialStore`/`KernelAuthAdapter` depuis ce projet (hors périmètre PharmaCore, et changement de fond sur l'authentification qui doit être fait par l'équipe qui possède cette brique). **La suite du spike (entreprise → approve → opération Vendre → exécution) est suspendue tant que ce blocage n'est pas levé côté Business Core.**

---

## Bonus découvert au passage (mineur, à signaler)

`UsageTrackingWebFilter` (nouveau, lié au dashboard d'usage) lève une `UnsupportedOperationException` après **chaque** requête, y compris celles qui réussissent (log : `Error [...UnsupportedOperationException]... but ServerHttpResponse already committed (200 OK)` par exemple). N'empêche pas les réponses déjà émises d'arriver correctement au client, mais pollue les logs et devrait être corrigé côté socle.

---

## Prochaines étapes

1. **Signaler ce blocage à l'équipe** (probablement Miguel / porteur de la tâche authentification déléguée) — c'est un pré-requis pour PharmaCore, mais aussi pour tout usage réel de Business Core par un développeur externe.
2. Une fois `POST /v1/businesses` réparé : reprendre le spike exactement là où il s'est arrêté (`POST /v1/businesses` → `POST .../approve` → déclarer l'opération `Vendre` avec `VERIFIER_STOCK, EVALUER_REGLES, ENREGISTRER_VENTE, ENGAGER_STOCK, ENCAISSER, EMETTRE_EVENEMENT` → exécuter → observer le comportement réel du stock, cf. §0.2 de `backend-test.md`).
3. Le bootstrap réel du backend Pharmacie (§3.4 de `backend-test.md`) rencontrera le même blocage à l'identique tant que le point 1 n'est pas résolu — ne pas commencer l'implémentation du bootstrap avant.
