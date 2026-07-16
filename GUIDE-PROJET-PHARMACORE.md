# Guide du projet — Comprendre Business Core et l'application test PharmaCore

Ce document répond à une question simple : **« comment est-ce que tout s'articule, et où intervient la clé API du développeur ? »** Il est écrit pour être lu une fois, tranquillement, avant d'attaquer les deux cahiers des charges (`pharmacie-backend-test/backend-test.md` et `pharmacie-frontend-test/frontend-test.md`).

---

## 1. Le projet en une phrase

**Business Core** (aussi appelé **BCaaS** — Business Core as a Service) est un moteur générique qui permet à un développeur de **déclarer un métier en données** (pas en code) — types de métier, offres, règles, opérations — et d'obtenir automatiquement une API REST fonctionnelle pour ce métier, sécurisée et multi-tenant, adossée au **Kernel** (RT-Comops, la plateforme d'infrastructure de Yowyob : organisations, comptes, produits, stock, ventes, paiements...).

**PharmaCore** (le projet dont on parle maintenant) n'est **pas un nouveau produit** — c'est une **application exemple complète** (frontend + backend) qui joue le rôle d'un vrai développeur externe : elle prend une clé API Business Core et construit dessus une vraie application de gestion de pharmacie. Son but est de **prouver, de bout en bout, que toute l'architecture qu'on a construite fonctionne réellement** — pas en théorie, en conditions réelles.

---

## 2. Les quatre couches du système

```
┌─────────────────────────────┐
│  Kernel RT-Comops (Yowyob)  │  ← infrastructure : comptes, organisations, produits,
│  kernel-core.yowyob.com     │     stock, ventes, paiements, rôles, tiers...
└──────────────▲──────────────┘
               │ appels HTTP internes (credentials plateforme OU credentials du tenant)
┌──────────────┴──────────────┐
│   Business Core (BCaaS)      │  ← LE PROJET QU'ON A CONSTRUIT.
│   backend-business-core/     │     Moteur générique en 7 briques (voir §5).
│   :8081 en local              │     Traduit « métier » en appels Kernel.
└──────────────▲──────────────┘
               │ HTTP + authentification (JWT **ou** clé API — voir §4)
┌──────────────┴──────────────┐
│  Backend métier du dévelop-  │  ← CE QU'ON CONSTRUIT MAINTENANT (pharmacie-backend-test/).
│  peur (ex : PharmaCore)      │     Un backend Spring Boot classique, écrit par un
│  :9090 (proposé)              │     développeur EXTERNE qui ne connaît que l'API BCaaS.
└──────────────▲──────────────┘
               │ HTTP (API propre à la pharmacie, RIEN à voir avec BCaaS)
┌──────────────┴──────────────┐
│  Frontend métier              │  ← CE QU'ON CONSTRUIT MAINTENANT (pharmacie-frontend-test/).
│  (ex : PharmaCore UI)         │     Interface web du pharmacien / caissier.
│  Next.js                      │
└───────────────────────────────┘
```

Il existe **déjà** deux pièces construites avant PharmaCore, avec un rôle différent :

- **`frontend-developpeur/`** : la console **du développeur lui-même** — celle où on crée son compte, on récupère sa clé API, on consulte sa documentation, ses types métier, son audit. C'est le **point d'entrée humain** dans l'écosystème Business Core.
- **`backend-business-core/`** : le moteur générique (BCaaS) lui-même.

**PharmaCore vient après** : c'est ce que le développeur construit **une fois qu'il a sa clé**, en dehors de la console. La console ne sert plus à rien pour lui à ce stade — il code directement contre l'API BCaaS.

---

## 3. Le principe fondateur : « déclarer, pas coder »

Business Core répond à la question *« c'est quoi un métier ? »* avec un modèle générique en **sept briques** (voir `backend-business-core/business-core/docs/architecture/sept-briques.md`, le document source) :

| # | Brique | Question | Rôle |
|---|--------|----------|------|
| 1 | **Type Métier** | Quel est le modèle global ? | Gabarit réutilisable, versionné (ex. « PHARMACIE ») |
| 2 | **Offre** | Quoi ? | Ce qui est vendu/proposé (ex. un médicament), avec des capacités activables (stock, réservation...) |
| 3 | **Acteurs métier** | Qui ? | Les rôles (pharmacien, caissier, client) et les personnes qui les occupent |
| 4 | **Règles** | Quelles contraintes ? | Déclencheur → condition → effet (ex. « ordonnance requise ») |
| 5 | **Opérations** | Quels actes ? | Le verbe du métier (ex. « Vendre »), un workflow déclaré en étapes |
| 6 | **Transactions** | Quels échanges de valeur ? | Trace financière, lue depuis le Kernel |
| 7 | **Configuration** | Avec quelles valeurs ? | Paramètres de réglage (devise, seuils...) |

**Point capital, et ce n'est pas un hasard : l'exemple qui a servi à concevoir CHAQUE brique, dès le premier jour du projet, est la pharmacie.** On le voit très concrètement dans le code et la documentation source :

- Le catalogue fermé des étapes d'opération (`TypeEtape`) contient littéralement : `VERIFIER_STOCK`, `EVALUER_REGLES`, `ENREGISTRER_VENTE`, `ENCAISSER`, `EMETTRE_EVENEMENT`, `ATTACHER_DOCUMENT`.
- Le vocabulaire des déclencheurs de règles (`Declencheur`) contient : `AVANT_VENTE`, `APRES_VENTE`, `AVANT_ENCAISSEMENT`, `APRES_ENCAISSEMENT`, `AVANT_LIVRAISON`.
- L'exemple pédagogique **officiel** de la brique Règles (fichier `docs/guides/feat-rules.md`, écrit pour l'équipe elle-même) est : *« déclencheur = avant la vente ; condition = catégorie == médicament sur ordonnance ; effet = EXIGER un document, sinon BLOQUER »*.
- Les tests unitaires du moteur de règles utilisent littéralement la valeur `medicament_prescription`.
- L'exemple d'erreur dans la spécification OpenAPI du projet est : *« Un document ordonnance est requis pour cette vente »*, avec `violatedRule: ORDONNANCE_REQUISE`.
- Le rôle métier cité en exemple partout dans la documentation est `pharmacien-responsable`.

**Conclusion : construire PharmaCore, ce n'est pas « choisir un exemple parmi d'autres ». C'est littéralement rejouer le cas d'usage que l'équipe a utilisé pour concevoir le moteur.** C'est exactement pour ça que c'est le meilleur test possible de l'architecture : si PharmaCore fonctionne de bout en bout, ça valide que le modèle en 7 briques tient vraiment la route pour un cas réel — pas seulement en théorie.

---

## 4. Où intervient précisément la clé API du développeur

C'est la question que tu posais directement — voici le mécanisme exact, vérifié dans le code (`ApiKeyReactiveAuthenticationManager`, `ApiKeyService`, `RegistrationService`, `AuthentificationService`, `SecurityConfig`).

> ⚠️ Cette section a été mise à jour le 2026-07-08 : un pull récent sur `develop2` a changé en profondeur le modèle de compte développeur (plusieurs clés par développeur désormais, plus une seule). Ce qui suit est la version à jour.

### 4.1 — Obtenir une clé (l'inscription)
Le développeur (via `frontend-developpeur`, ou directement en curl) appelle :
```
POST /v1/registration
{ "firstName": "...", "lastName": "...", "email": "...", "password": "...", "planCode": "FREE" }

→ 201 { "clientId": "bck_xxxxx", "apiKey": "yyyyy...", "plan": "FREE" }
```
Ce seul appel fait **trois choses** côté Business Core :
1. Crée un compte sur le **Kernel** (identité réelle, vérification e-mail).
2. Crée une ligne `developer_account` (juste l'email + le plan à ce stade — **pas encore de tenant connu**).
3. Émet une **première clé API** (« Default ») dans une table séparée `api_key` — le `clientId` renvoyé est en réalité un **`prefix`** public (ex. `bck_xxxxx`), et l'`apiKey` est le secret, affiché **une seule fois**, stocké haché (BCrypt) côté serveur.

**Un développeur peut avoir plusieurs clés actives** (jusqu'à 5 par défaut) — un vrai portail existe : `GET /v1/api-keys` (lister), `PATCH /v1/api-keys/{id}` (renommer), `POST /v1/api-keys/{id}:revoke` (révoquer). Utile par exemple si PharmaCore veut sa propre clé, distincte de celle de `frontend-developpeur`.

### 4.2 — ⚠️ Piège important : la clé est inerte jusqu'au premier login
**Une clé fraîchement créée ne fonctionne pas immédiatement.** Le `tenantId` (techniquement `kernel_tenant_id`, le `tid` du Kernel) n'est renseigné sur le compte développeur **qu'au premier `POST /v1/auth/login` réussi** — pas à l'inscription. Tant que ce n'est pas fait, tout appel avec la clé échoue avec un message explicite (« *Clé valide mais espace non lié : connectez-vous une fois via `POST /v1/auth/login` pour activer votre clé.* »).

**Concrètement, avant qu'un backend métier (comme PharmaCore) puisse utiliser une clé API, il faut que quelqu'un se soit connecté au moins une fois** avec ce compte (par exemple via `frontend-developpeur`, après avoir vérifié son e-mail). C'est une étape humaine, ponctuelle, à faire une fois pour activer l'espace — ensuite la clé fonctionne pour toujours (sauf révocation).

### 4.3 — Utiliser la clé (à chaque appel machine)
Le backend Pharmacie (PAS le frontend Pharmacie — voir §4.6) envoie sur **chaque requête** vers Business Core :
```
X-BC-Client-Id: bck_xxxxx   (le prefix de la clé)
X-BC-Api-Key: yyyyy...      (le secret)
```
Côté Business Core, `ApiKeyReactiveAuthenticationManager` :
1. Cherche la clé par `prefix`, vérifie qu'elle est active.
2. Vérifie que le secret fourni correspond au hash stocké (BCrypt).
3. Retrouve le compte développeur propriétaire, vérifie qu'il a déjà un `kernel_tenant_id` (sinon → §4.2).
4. Construit un **`BusinessContext`** avec `tenantId = kernelTenantId` — **jamais fourni par le client, toujours dérivé de la clé**. C'est exactement le même `tenantId` qu'obtiendrait ce développeur en se connectant par JWT : **les deux modes d'authentification résolvent le même espace**, c'est la garantie de cohérence multi-tenant.
5. Tous les appels suivants (créer un type métier, une offre, une entreprise, exécuter une opération...) sont automatiquement filtrés/écrits sous ce `tenantId`.

### 4.4 — Suivre son usage
`GET /v1/dashboard` (avec JWT ou clé API) renvoie désormais un vrai tableau de bord : nombre de requêtes aujourd'hui/ce mois, taux d'erreur, historique 30 jours, et la liste des clés du compte. C'est réel, alimenté par des compteurs Redis flushés quotidiennement — plus une estimation.

### 4.5 — Le double mécanisme d'authentification (important à comprendre)
Business Core accepte **deux façons distinctes** de s'authentifier sur les **mêmes endpoints** :
- **JWT Bearer** (`Authorization: Bearer ...`) — c'est ce qu'utilise `frontend-developpeur` après un `POST /v1/auth/login`. Pensé pour un **humain** qui navigue dans une console.
- **Clé API** (`X-BC-Client-Id` + `X-BC-Api-Key`) — c'est ce qu'utilisera **le backend Pharmacie**. Pensé pour une **machine** (un serveur qui tourne sans utilisateur derrière, 24/7).

Dans `SecurityConfig`, le JWT est la voie principale, la clé API est le **repli** (« *quand il n'y a pas de Bearer* »). Les deux mènent au même résultat : un `BusinessContext` avec un `tenantId`.

### 4.6 — Où la clé ne doit JAMAIS apparaître
La clé API (`X-BC-Api-Key`) est un secret **serveur-à-serveur**. Elle doit vivre **uniquement** dans le backend Pharmacie (variable d'environnement, jamais commitée). Le **frontend Pharmacie ne doit jamais la connaître** : il parle uniquement au backend Pharmacie, qui lui-même parle à Business Core. C'est exactement le même principe que `frontend-developpeur` qui ne stocke jamais de secret Kernel — il ne connaît que son propre JWT.

### 4.7 — Un acteur asserté (« on-behalf-of »)
Le backend Pharmacie peut, en plus de la clé, ajouter un en-tête optionnel :
```
X-BC-On-Behalf-Of: <uuid de l'acteur kernel>
```
pour dire « cette action est faite par tel pharmacien/caissier précis ». Business Core **fait confiance** à cette assertion (modèle documenté « A2 ») : c'est au backend Pharmacie de s'assurer que la bonne personne est bien connectée chez lui avant d'asserter son identité.

---

## 5. Le flux complet d'une vente, illustré

Voici, concrètement, ce qui se passe quand un caissier vend un médicament dans PharmaCore — ça illustre tout le reste du document d'un coup :

1. **Frontend Pharmacie** : le caissier clique « Vendre » sur un médicament, quantité 2, sélectionne le client. Le frontend appelle **son propre backend** (`POST /api/ventes` par exemple — endpoint propre à PharmaCore, pas du tout un endpoint Business Core).
2. **Backend Pharmacie** : reçoit la demande, retrouve l'`offreId` Business Core correspondant à ce médicament (mémorisé lors de la création du catalogue), puis appelle :
   ```
   POST /v1/businesses/{businessId}/operations/Vendre:execute
   X-BC-Client-Id: ...
   X-BC-Api-Key: ...
   Idempotency-Key: <uuid généré par le backend Pharmacie>

   { "parametres": { "offreId": "...", "quantite": 2, "beneficiaireId": "..." } }
   ```
3. **Business Core** exécute le workflow déclaré pour l'opération « Vendre » (étape par étape, moteur Saga) :
   - `VERIFIER_STOCK` → vérifie le solde réel sur le Kernel (inventaire).
   - `EVALUER_REGLES` → vérifie les règles déclarées (ex. « ordonnance requise si catégorie = médicament sur ordonnance ») ; si bloquant → réponse `422` immédiate, rien n'est engagé.
   - `ENREGISTRER_VENTE` → crée réellement une commande de vente côté Kernel.
   - `ENCAISSER` → enregistre le paiement côté Kernel.
   - `EMETTRE_EVENEMENT` → publie un événement (Kafka) pour traçabilité/découplage.
   - Si une étape échoue après que `ENREGISTRER_VENTE` a réussi, le moteur **annule automatiquement** la vente côté Kernel (compensation) — la vente n'est jamais « à moitié faite ».
4. Business Core répond `200 OK` (immédiat) avec le résultat, ou `202 Accepted` (si l'opération est différée) avec une URL de suivi.
5. **Backend Pharmacie** : reçoit la réponse, met à jour **sa propre base de données** (historique de vente lisible, alertes stock locales...), répond au frontend.
6. **Frontend Pharmacie** : affiche le ticket de vente.

**Rien de tout ceci n'est inventé pour PharmaCore : c'est le comportement réel, déjà codé, du moteur d'opérations de Business Core** (`MoteurOperation.java`, `EnregistrerVenteExecuteur.java`, etc.). PharmaCore ne fait que l'utiliser comme n'importe quel développeur externe le ferait.

---

## 6. Une limite réelle et honnête, à connaître avant de commencer

En creusant le code pour préparer ce projet, une limite concrète est apparue, et il faut la connaître **avant** de construire PharmaCore dessus (pas de surprise en cours de route) :

**Business Core sait *lire* le solde de stock d'un produit (via le Kernel), mais n'expose aujourd'hui aucun endpoint pour *l'alimenter*.** Concrètement : quand une offre STOCKABLE (un médicament) est créée, le produit correspondant est créé côté Kernel avec un stock à zéro, et il n'existe pas de route Business Core pour dire « ajoute 100 boîtes en stock ». Cela veut dire qu'une vente réelle via `VERIFIER_STOCK` risque fort de refuser (« stock insuffisant ») tant que ce point n'est pas clarifié.

**Ce n'est pas un problème à cacher — c'est exactement le genre de chose que ce projet-test est censé révéler.** Le cahier des charges backend (`pharmacie-backend-test/backend-test.md`) prévoit donc, en **toute première étape**, un test isolé (« spike ») qui vérifie ce comportement réel avant de construire le reste — pour confirmer, contourner proprement (stock géré localement le temps que Business Core expose l'écriture), ou remonter le besoin à l'équipe Business Core.

---

## 7. Lexique rapide

| Terme | Signification |
|---|---|
| **Kernel / RT-Comops** | La plateforme d'infrastructure Yowyob (comptes, organisations, produits, stock, ventes, paiements). Business Core est bâti *au-dessus*. |
| **Business Core / BCaaS** | Le moteur générique qu'on a construit (`backend-business-core/`). Traduit un métier déclaré en données en appels Kernel. |
| **Tenant** | Un développeur inscrit (`developer_account`). Frontière d'isolation stricte entre développeurs. |
| **Type Métier** | Le gabarit d'un métier (« PHARMACIE »), versionné, publié. |
| **Entreprise** | L'instance réelle d'un Type Métier pour un tenant (« Pharmacie du Centre »), épinglée à une version précise. |
| **Offre** | Une unité vendable (un médicament), avec des capacités (STOCKABLE...). |
| **Acteur métier** | Une personne rattachée à une entreprise via un rôle (pharmacien, caissier, client). |
| **Opération** | Un acte métier déclaré (« Vendre »), exécuté comme un workflow d'étapes. |
| **Trace** | L'enregistrement d'une exécution d'opération (statut, dates). |
| **Transaction** | L'échange de valeur (financier) né d'une opération, lu depuis le Kernel. |
| **Clé API (X-BC-Client-Id / X-BC-Api-Key)** | Le secret qui identifie un tenant pour les appels serveur-à-serveur. |

---

## 8. Ce que prouve PharmaCore, une fois terminé

Si PharmaCore fonctionne de bout en bout — inscription développeur → clé API → déclaration du type PHARMACIE → offres, règles, opérations → création d'une entreprise → vente réelle avec vérification de règle (ordonnance) → trace consultable — alors ce test démontre concrètement que :
1. Un développeur externe, qui ne connaît **que** l'API publique de Business Core (aucun accès au code), peut construire un vrai métier dessus.
2. Le modèle en 7 briques est suffisant pour représenter un métier réel, pas juste un cas de test artificiel.
3. La chaîne complète Kernel ↔ Business Core ↔ Backend métier ↔ Frontend métier tient — c'est le test d'intégration ultime du projet.

C'est exactement l'objectif que tu as formulé : *« tester à 100 % toute l'architecture qu'on a construite en background »*.
