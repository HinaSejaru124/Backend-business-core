# feat/offers-actors — Offre + Acteurs métier

> **Dev 3.** Briques 2 (Offre) et 3 (Acteurs métier).
> Lis d'abord `README.md`. Donne les deux fichiers à ton IA de code.

## Ta mission en une phrase

Tu construis **les ressources du métier** : ce qu'il propose (Offres) et qui y participe (Acteurs). Tu possèdes aussi l'entité `Entreprise` (l'instance d'un type), centrale pour toute l'équipe.

---

## Branche
```
git checkout main && git pull
git checkout -b feat/offers-actors
```

## Tes packages
```
domain/offer/          domain/actor/          domain/enterprise/  (l'entité Entreprise)
application/usecase/offer/   application/usecase/actor/   application/usecase/enterprise/
adapter/in/rest/offer/   adapter/in/rest/actor/   adapter/in/rest/enterprise/
adapter/out/persistence/offer/   .../actor/   .../enterprise/
adapter/out/kernel/product/   .../actor/   .../thirdparty/   .../organization/
```

> Tu possèdes `Entreprise` car c'est le point de rattachement des Acteurs. Les autres devs **liront** cette entité via un port en lecture que tu exposes (ou via son repository). Tu figes sa forme tôt et tu préviens l'équipe.

---

## Partie A — Entreprise (entité de liaison centrale)

### Concept
Une **Entreprise** est l'instance réelle d'un Type Métier, épinglée à une version. Elle référence une `Organization` du kernel.

### Entité
| `Entreprise` | `id`, `versionTypeId` (FK vers la version épinglée), `organizationId` (réf kernel), `nomLocal`, `cycleVie` (`CycleVie`) |

> `CycleVie` ∈ { ACTIVE, SUSPENDUE, FERMEE } — dans `domain/shared/`.

### Ports de sortie
- `PersisterEntreprise` → `POST /api/organizations` puis `POST /api/organizations/{orgId}/agencies`.

### Endpoints
| Verbe | Chemin |
|---|---|
| POST | `/v1/businesses` |
| GET | `/v1/businesses` |
| GET | `/v1/businesses/{businessId}` |
| PUT | `/v1/businesses/{businessId}/lifecycle` |

---

## Partie B — Offre (brique 2)

### Concept
Une **Offre** est une unité de valeur proposée. Modèle **hybride** : un socle commun (nom, prix, devise) + des **capacités activables combinables** (STOCKABLE, PLANIFIABLE, RESERVABLE, RECURRENT). Le prix a 3 formes (FIXE, GRATUIT, SUR_DEVIS).

### Entités
| `DefinitionOffre` | `id`, `versionTypeId` (FK), `nom`, `formePrix` (`FormePrix`), `prix` (BigDecimal, nullable) |
| `Capacite` | `id`, `definitionOffreId` (FK), `type` (`TypeCapacite`), `active` (bool) |

> `FormePrix` ∈ {FIXE, GRATUIT, SUR_DEVIS} ; `TypeCapacite` ∈ {STOCKABLE, PLANIFIABLE, RESERVABLE, RECURRENT}.

### Point d'attention important (traduction vers le kernel)
Notre Offre est plus riche que le `Product` du kernel. Ton adapter **traduit** :
- offre STOCKABLE → produit avec gestion de stock,
- offre SUR_DEVIS → produit sans prix fixe, etc.

### Port interne
- `FournisseurDeCapacite` : chaque capacité activable est une **stratégie** derrière cette interface (même principe d'extensibilité que les règles N1→N2). Implémente d'abord STOCKABLE, les autres suivent.

### Ports de sortie
- `GererCatalogueOffre` → `POST /api/products`, `GET /api/products/{id}/prices/effective`.
- `VerifierDisponibilite` → `GET /api/inventory/movements/balance` (pour les offres stockables).

### Endpoints
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/offers` |
| GET | `/v1/business-types/{typeId}/versions/{n}/offers` |

---

## Partie C — Acteurs métier (brique 3)

### Concept
Un **Acteur métier** = association (personne + rôle + entreprise). Apport clé : le **rôle métier** (« pharmacien responsable »), distinct du rôle technique kernel. Parapluie commun, **deux sous-catégories étanches** :
- **Opérateur** (interne, accès système) → via `actor-core` + `roles` + `auth`.
- **Bénéficiaire** (externe, pas d'accès back-office) → via `tp` (tiers).

### Entités
| `RoleMetier` | `id`, `versionTypeId` (FK), `code`, `categorie` (`CategorieActeur`) |
| `ActeurMetier` | `id`, `entrepriseId` (FK), `roleMetierId` (FK), `acteurKernelId` (réf Actor ou Tiers), `valideDepuis`, `valideJusqua` |

> `CategorieActeur` ∈ {OPERATEUR, BENEFICIAIRE}.

### Règle métier (sécurité — RG-04)
Opérateur et Bénéficiaire ne vivent pas dans les mêmes cores kernel : **étanches**. On ne transforme jamais un bénéficiaire en opérateur par modification de rôle. Si une personne change de statut, on crée un **nouvel** ActeurMetier.

### Ports de sortie
- `ResoudreBeneficiaire` → `POST /api/third-parties` (bénéficiaire).
- `AppliquerRoleTechnique` → `POST /api/roles` **puis** `POST /api/roles/assignments` (2 appels).
- `RattacherAOrganisation` → `POST /api/organizations/{orgId}/actors`.

> ⚠️ **Évolution depuis ce brief initial** : le port `ResoudrePersonne` (`POST /api/actors`, résolution
> d'un opérateur à partir d'un identifiant texte) a été supprimé. Business Core ne résout/crée plus
> jamais d'identité kernel à partir d'un email pour un OPERATEUR — soit l'identité est déjà connue
> (`acteurKernelId` fourni directement à `POST .../actors`), soit la personne s'inscrit elle-même
> (`POST .../actors:register`, qui délègue entièrement au kernel : login d'abord, sign-up seulement si
> le compte n'existe pas). Le chemin BENEFICIAIRE (`ResoudreBeneficiaire`) est inchangé. Détail complet :
> [`architecture/authentification-trois-flux.md`](../architecture/authentification-trois-flux.md) §3.

### Endpoints (voir le lien ci-dessus pour le détail complet, notamment `:register`/`:login`/`/me`)
| Verbe | Chemin |
|---|---|
| POST | `/v1/business-types/{typeId}/versions/{n}/roles` |
| POST | `/v1/businesses/{businessId}/actors` (identité déjà connue uniquement) |
| GET | `/v1/businesses/{businessId}/actors` |
| PUT | `/v1/businesses/{businessId}/actors/{actorId}` |
| DELETE | `/v1/businesses/{businessId}/actors/{actorId}` |
| POST | `/v1/businesses/{businessId}/actors:register` (inscription libre-service) |
| POST | `/v1/businesses/{businessId}/actors:login` |
| GET | `/v1/businesses/{businessId}/actors/me` |

---

## Migration Liquibase
`db/changelog/features/offers-actors.xml` — tables `entreprise`, `definition_offre`, `capacite`, `role_metier`, `acteur_metier`. IDs préfixés `oa-001`…

## Definition of Done
- [ ] Créer une entreprise instance d'un type+version, vérifier la réf Organization kernel.
- [ ] Déclarer une offre stockable, vérifier la traduction en Product + la lecture de stock.
- [ ] Rattacher un opérateur et un bénéficiaire, vérifier qu'ils passent par des cores kernel différents.
- [ ] Tests unitaires + tests d'intégration sur les 4 ports kernel.
- [ ] Changelog appliqué, CI verte, réactif.

## Suggestions de prompts
- « Implémente l'entité de domaine `DefinitionOffre` avec son socle et une liste de `Capacite`, modèle hybride, en Java 21 pur. »
- « Écris la stratégie `FournisseurDeCapacite` pour STOCKABLE qui, à l'activation, branche la vérification de stock via le port VerifierDisponibilite. »
- « Implémente l'adapter kernel `AppliquerRoleTechnique` qui enchaîne POST /api/roles puis POST /api/roles/assignments avec WebClient réactif. »
