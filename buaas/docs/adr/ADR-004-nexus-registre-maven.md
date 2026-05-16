# ADR-004 — Nexus comme Registre Maven Privé

**Date** : 2026-05-16  
**Statut** : Accepté  
**Décideurs** : Équipe BCaaS

## Contexte
bcaas-core doit être consommable par n'importe quelle équipe externe
sans qu'elle ait accès au code source, exactement comme spring-boot-starter.

## Décision
Déployer Sonatype Nexus 3 dans l'infrastructure Docker comme registre
Maven privé. Chaque `mvn deploy` de bcaas-core publie l'artefact sur Nexus.
Les consommateurs ajoutent simplement Nexus comme repository Maven.

En production : migration vers GitHub Packages ou Nexus cloud.

## Conséquences
- bcaas-core est consommable via 3 lignes de pom.xml
- Le code source du core reste privé
- Le versioning sémantique (1.0.0, 1.1.0...) est géré par Nexus
- Les snapshots de développement sont séparés des releases stables
