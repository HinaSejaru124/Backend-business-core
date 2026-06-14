# ADR-001 — Spring Boot 4 réactif (Java 21, WebFlux, R2DBC)

- Statut : accepté
- Date : 2026-06-14

## Contexte

Le Business Core doit être non bloquant (ENF-03 : ne jamais figer sous charge) et s'appuyer sur une
stack supportée dans la durée. À la mi-2026, Spring Boot 3.3/3.4 sont périmées et 3.5 expire fin juin
2026 ; Spring Boot 4.0 (GA novembre 2025) est la branche supportée.

## Décision

- **Java 21 (LTS)**, **Spring Boot 4.0.x** (Spring Framework 7, Spring Security 7, Jakarta EE 11,
  Jackson 3), **Maven**.
- Pile **réactive** : Spring WebFlux + Spring Data R2DBC. `Mono`/`Flux` partout, aucun `.block()`,
  `WebClient` (pas `RestTemplate`).
- PostgreSQL 16, Redis 7, Kafka 3, Liquibase ; versions transitives gérées par le BOM Spring Boot.
- On fixe **4.0.7** (branche mature) plutôt que 4.1.0 (trop récente pour un travail d'équipe).

## Conséquences

- Performance et résilience natives, alignées sur l'exigence de non-blocage.
- **Modularisation Spring Boot 4** : certaines auto-configurations ne sont plus garanties par la seule
  présence d'une lib. Le socle fournit donc explicitement ce qui manque (`WebClient` via `ObjectProvider`
  avec repli, `KafkaTemplate` via une config dédiée) et utilise **Jackson 3** (`tools.jackson`).
- Liquibase tourne en **JDBC** au démarrage (migrations) pendant que le runtime utilise **R2DBC** : les
  deux configurations de connexion coexistent, c'est normal.
- Un test de démarrage de contexte (`ApplicationContextLoadsTest`) protège des régressions de câblage
  introduites par la modularisation.
