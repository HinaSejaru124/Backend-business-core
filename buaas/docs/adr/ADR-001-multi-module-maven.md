# ADR-001 — Structure Maven Multi-Module

**Date** : 2026-05-16  
**Statut** : Accepté  
**Décideurs** : Équipe BCaaS

## Contexte
Le projet doit produire un noyau générique (bcaas-core) réutilisable par
n'importe quelle application métier, indépendamment du projet d'orientation
(buaas-app) qui en est le premier consommateur.

## Décision
Utiliser un projet Maven multi-module avec un Parent POM commun, structuré
en trois modules principaux :
- `bcaas-core` : noyau générique, publié comme librairie sur Nexus
- `buaas-app` : application d'orientation, consomme bcaas-core
- `bcaas-sdk` : SDK léger pour les intégrateurs tiers (Business Book)

## Conséquences
- Un seul `mvn install` compile tout dans le bon ordre
- Les versions et dépendances sont centralisées dans le Parent POM
- `bcaas-core` peut être publié indépendamment sur Nexus/GitHub Packages
- Toute autre application peut importer `bcaas-core` comme dépendance Maven
- La migration future vers les microservices est facilitée

## Alternatives rejetées
- **Librairie indépendante** : trop de friction pour le développement en équipe
- **Monolithe unique** : impossible de réutiliser le core sans embarquer buaas-app
