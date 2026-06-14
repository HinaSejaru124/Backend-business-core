# ADR-003 — Façade kernel à double clé

- Statut : accepté
- Date : 2026-06-14

## Contexte

Le kernel RT-Comops est un projet distinct déjà déployé, accessible uniquement via son API REST. Le
Business Core doit l'orchestrer en façade (le développeur dit « enregistre une vente », le Business
Core appelle product + inventory + sales + cashier) sans exposer le secret kernel ni réimplémenter la
gouvernance des accès.

## Décision

- **Une `ClientApplication` kernel par développeur**, provisionnée automatiquement à l'inscription
  (`POST /api/client-applications`). Le kernel applique alors nativement quotas et isolation par
  développeur — gouvernance récupérée gratuitement.
- **Chaîne à deux niveaux de clés** :
  - le développeur s'authentifie auprès du Business Core avec sa **clé Business Core** ;
  - le Business Core détient la **clé kernel** du développeur (stockée chiffrée), l'échange contre un
    **JWT court** (`/oauth2/token`) mis en cache Redis, et agit au nom du développeur auprès du kernel.
- **Ports nommés par capacité métier**, jamais par core kernel (`EnregistrerVente`, pas `SalesCorePort`)
  pour rester stable si le kernel évolue. Le client kernel de base (`KernelClient`) est fourni par le
  socle ; chaque feature ajoute ses appels dans son adapter.

## Conséquences

- Le développeur ne gère qu'**une seule clé** ; le secret kernel ne lui est jamais exposé (ENF-04).
- Le Business Core garde le contrôle (révocation, plan, supervision) sans dépendre du kernel.
- La connaissance du kernel (URL, clés, traduction d'API) est **confinée à `adapter/out/kernel/`** ;
  le reste du projet l'ignore. Changer l'URL du kernel (mock WireMock en dev → kernel réel en prod) ne
  touche qu'une propriété.
- **Modèle d'auth réel du kernel** (confirmé par la doc d'exploitation) : chaque appel `/api/**` porte
  `X-Client-Id` + `X-Api-Key` (la ClientApplication), plus `Authorization: Bearer` sur les endpoints
  protégés et `X-Organization-Id` pour les opérations d'entreprise. Deux identités côté Business Core :
  une ClientApplication **plateforme** (`KERNEL_CLIENT_ID`/`KERNEL_CLIENT_SECRET`, pour provisionner) et
  une ClientApplication **par développeur** (pour agir en son nom). Géré par `KernelClient`.
- L'adapter **traduit** la richesse du domaine vers le kernel (ex. une offre STOCKABLE → produit avec
  gestion de stock ; une offre SUR_DEVIS → produit sans prix fixe).
