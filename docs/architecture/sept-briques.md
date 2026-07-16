# Le modèle métier — sept briques

Le Business Core répond à « c'est quoi un métier ? » par un modèle générique en **sept briques**,
déclaré en **données** (metadata-driven) et interprété par un moteur générique. On modélise sur deux
niveaux : le **Type Métier** (le modèle, déclaré une fois) et l'**Entreprise** (l'instance).

| # | Brique | Statut | Question | Rôle |
|---|--------|--------|----------|------|
| 1 | Type Métier | Nouveau | Quel est le modèle global ? | Gabarit réutilisable, versionné par épinglage |
| 2 | Offre | Mixte | Quoi ? | Socle + capacités activables |
| 3 | Acteurs métier | Réutilise | Qui ? | Opérateur / Bénéficiaire, étanches |
| 4 | Règles | Nouveau | Quelles contraintes ? | Déclencheur → condition → effet |
| 5 | Opérations | Mixte | Quels actes ? | Workflow déclaré, moteur Saga |
| 6 | Transactions | Mixte | Quels échanges de valeur ? | Façade financière unifiée |
| 7 | Configuration | Mixte | Avec quelles valeurs ? | Property bag à deux niveaux |

## Détail

1. **Type Métier** — description réutilisable d'une catégorie d'activité (« Pharmacie »). Versionné par
   **épinglage** : une version publiée est immuable, chaque entreprise reste figée sur sa version
   jusqu'à migration explicite. Enum `StatutType` (BROUILLON, PUBLIE, ARCHIVE).

2. **Offre** — unité de valeur. Modèle hybride : socle commun (nom, prix, devise) + **capacités
   activables combinables** (`TypeCapacite` : STOCKABLE, PLANIFIABLE, RESERVABLE, RECURRENT). Le prix
   a trois formes (`FormePrix` : FIXE, GRATUIT, SUR_DEVIS).

3. **Acteurs métier** — association personne + rôle métier + entreprise. Deux sous-catégories étanches
   (`CategorieActeur` : OPERATEUR via actor/auth/roles-core, BENEFICIAIRE via tp-core). Le rôle métier
   (« pharmacien responsable ») est distinct du rôle technique kernel.

4. **Règles** — contrainte déclarée (jamais codée). Anatomie universelle **déclencheur → condition →
   effet**. Six effets en trois familles (`Effet`) :
   - Bloquants : `BLOQUER`, `EXIGER`, `VALIDER`
   - Mutateur : `AJUSTER` (jamais silencieux, toujours tracé)
   - Traçants : `ALERTER`, `DEROGER` (limité aux rôles autorisés)

5. **Opérations** — le verbe du métier. Workflow déclaré = séquence d'**étapes-types** (`TypeEtape`,
   catalogue fermé) exécutée par le moteur Saga (compensation gratuite). Immédiat (200) ou différé
   (202 + trace) via Kafka.

6. **Transactions** — trace d'un échange de valeur (≠ paiement). Façade unifiée sur les cores
   financiers du kernel. Échange générique, le monétaire étant le cas courant. Naît d'une Opération.

7. **Configuration** — paramètres de réglage. Sépare « quoi faire » (Règle) de « avec quelle valeur »
   (Config). Deux niveaux : défaut au Type (verrouillé ou ajustable) + surcharge à l'Entreprise.

## Emboîtements clés

- Les **déclencheurs** de Règles (`Declencheur`) sont les points d'ancrage dans les étapes d'Opérations.
- Les effets `DEROGER`/`VALIDER` et le rôle déclencheur d'une Opération utilisent les **rôles métier**.
- Une **Transaction** naît d'une **Opération** (RG-05).
- Les conditions de Règles lisent des propriétés d'Offre ; les seuils viennent de la Configuration.

## Entités et stockage

- **Entités propres** (n'existent que dans le Business Core) : `TypeMetier`, `VersionType`,
  `DefinitionOffre`, `Capacite`, `RoleMetier`, `RegleMetier`, `DefinitionOperation`, `EtapeOperation`,
  `ParametreConfig`.
- **Entités de liaison** (référence kernel + métadonnées) : `Entreprise` (réf. Organization),
  `ActeurMetier` (réf. Actor/Tiers), `TraceOperation` (réf. transaction kernel).
- **Jamais** les données opérationnelles du kernel : lues à la demande, mises en cache Redis (TTL),
  jamais copiées en base.
