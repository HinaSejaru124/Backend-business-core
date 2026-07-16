# ADR-004 — Modèle metadata-driven en sept briques

- Statut : accepté
- Date : 2026-06-14

## Contexte

Le Business Core doit permettre à n'importe quel développeur de construire le backend de son métier —
quel qu'il soit — sans que l'équipe du Business Core intervienne pour chaque nouveau métier (ENF-01).

## Décision

- **Piloté par les métadonnées** : les métiers, leurs offres, règles et opérations sont des **données
  déclarées** que le Business Core interprète, pas du code. Critère de validation : « peut-on ajouter un
  métier entièrement nouveau sans recompiler ? » → oui.
- **Modélisation à deux niveaux** : Type Métier (modèle) et Entreprise (instance), à la manière de la
  classe et de l'objet.
- **Sept briques génériques** : Type Métier, Offre, Acteurs, Règles, Opérations, Transactions,
  Configuration (voir [sept-briques](../architecture/sept-briques.md)).
- **Logique spécifique externalisée** : la logique métier très spécifique d'un développeur vit dans son
  **backend externe**, qui appelle le Business Core via API — pas dans le Business Core ni le kernel.
- **Extensibilité par stratégies** : les choix variables (évaluation de règles, étapes d'opération,
  capacités d'offre, moyen de paiement) passent par des **ports internes** ; le niveau 1 (paramétré) est
  une implémentation, le niveau 2 (composable) en sera une autre, sans réécriture.

## Conséquences

- Un nouveau métier = de la donnée déclarée, zéro recompilation (généricité, ENF-01).
- Le moteur reste générique ; le sens est confié au développeur.
- Les enums du vocabulaire fermé (`StatutType`, `FormePrix`, `TypeCapacite`, `CategorieActeur`, `Effet`,
  `CycleVie`, `StatutTrace`, `Declencheur`, `TypeEtape`) sont **figés par le socle** : ce sont des
  contrats partagés entre features (notamment `Declencheur` entre Règles et Opérations).
- Le niveau 3 (règles scriptables) est **écarté** (risques de sécurité et de complexité).
