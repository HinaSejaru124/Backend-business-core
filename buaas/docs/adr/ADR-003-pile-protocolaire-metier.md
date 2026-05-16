# ADR-003 — Pile Protocolaire Métier (inspirée OSI/TCP-IP)

**Date** : 2026-05-16  
**Statut** : Accepté  
**Décideurs** : Équipe BCaaS

## Contexte
Le Business Core doit gérer de manière cohérente et découplée :
le routage multi-tenant, les politiques de sécurité, les règles métier
et l'infrastructure technique. Ces préoccupations transverses ne doivent
pas polluer le code métier.

## Décision
Structurer le Business Core comme une pile de 5 couches inspirée du modèle
OSI/TCP-IP, où chaque couche offre des services à la couche supérieure et
cache sa complexité à la couche inférieure :

| Couche | Nom | Responsabilité |
|--------|-----|----------------|
| 5 | Business Capabilities | Acteurs, ressources, opérations, workflow, audit |
| 4 | Context & Policy | Identité, permissions, règles, SLA, saga |
| 3 | Tenant & Routing | Résolution du tenant, discovery, versioning |
| 2 | Transport & Messaging | REST/Kafka, retry, timeout, idempotence |
| 1 | Infrastructure | DB, cache, bus, stockage, observabilité |

## Le "Paquet Métier"
Chaque requête traversant le système est encapsulée avec :
- Header transport : trace_id, correlation_id, timestamp
- Header contexte : tenant_id, actor_id, role_scope, locale, policy_level
- Payload métier : contenu fonctionnel de la requête

## Conséquences
- Séparation nette entre control plane et data plane
- Le multi-tenant est traité comme un réseau virtuel (analogie VLAN)
- Les services peuvent traiter la donnée sans connaître toute l'application
- Les contrats inter-services sont explicites, testables et versionnables

## Alternatives rejetées
- **Middleware unique** : point de défaillance unique, pas scalable
- **Annotations Spring partout** : couplage fort au framework
