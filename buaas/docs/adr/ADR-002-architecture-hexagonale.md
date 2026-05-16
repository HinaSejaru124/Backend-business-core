# ADR-002 — Architecture Hexagonale (Ports & Adapters)

**Date** : 2026-05-16  
**Statut** : Accepté  
**Décideurs** : Équipe BCaaS

## Contexte
Le noyau BCaaS doit être indépendant de toute technologie d'infrastructure
(base de données, broker de messages, cache) pour rester générique et
testable unitairement sans démarrer de serveur.

## Décision
Adopter l'architecture hexagonale (Ports & Adapters) de Alistair Cockburn :
- Le **domaine** contient les règles métier pures, sans aucune dépendance externe
- Les **ports** sont des interfaces Java définissant les contrats
- Les **adapters** sont les implémentations concrètes (PostgreSQL, Kafka, Redis)
- Les dépendances pointent toujours vers le centre (inversion de dépendance)

## Conséquences
- Le domaine est testable sans base de données ni Kafka
- On peut changer PostgreSQL pour MongoDB sans toucher au domaine
- Chaque couche a une responsabilité unique (principe SOLID)
- La lisibilité et la maintenabilité du code sont améliorées

## Alternatives rejetées
- **Architecture en couches classique** : couplage fort entre couches
- **Transaction Script** : pas adapté à un domaine riche et générique
