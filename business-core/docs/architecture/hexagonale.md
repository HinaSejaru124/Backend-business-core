# Architecture hexagonale

## Principe

Le code métier ne dépend de rien d'extérieur ; c'est l'extérieur qui dépend de lui. Le domaine est
protégé au centre ; les technologies (base de données, web, kernel, messagerie) sont à la périphérie
et interchangeables sans toucher au métier.

**Règle d'or : toutes les flèches de dépendance pointent vers le centre.** Les adapters connaissent
le domaine ; le domaine ne connaît aucun adapter. Le kernel RT-Comops est un adapter de sortie comme
un autre.

## Les trois zones

| Zone | Rôle | Dépendances techniques |
|------|------|------------------------|
| `domain/` | Le métier pur : 7 briques, enums, ports (interfaces) | Aucune |
| `application/` | Orchestration : use cases, `BusinessContext`, saga, sécurité applicative | Spring (injection) uniquement |
| `adapter/` | Implémentations concrètes branchées sur les ports | REST, R2DBC, Redis, Kafka, kernel |

## Découpage des packages

```
com.yowyob.businesscore
├── domain/
│   ├── businesstype/ configuration/ offer/ actor/ enterprise/ rule/ operation/ transaction/
│   ├── shared/                 # enums + value objects partagés (figés)
│   └── port/
│       ├── in/                 # ports d'entrée (use cases)
│       ├── out/                # 17 ports de sortie (vers le kernel / l'infra)
│       └── internal/           # 6 ports internes (stratégies extensibles)
├── application/
│   ├── usecase/                # implémentations des ports d'entrée (par brique)
│   ├── context/                # BusinessContext (figé)
│   ├── security/               # AuthorizationService, TenantGuard
│   ├── saga/                   # dispatchers d'étapes / capacités
│   ├── common/                 # pagination
│   └── error/                  # ProblemException (RFC 7807)
└── adapter/
    ├── in/
    │   ├── rest/               # un sous-package par brique
    │   ├── event/              # consumers Kafka
    │   └── security/           # chaîne Spring Security (clé Business Core)
    └── out/
        ├── kernel/             # client kernel (SEUL endroit qui connaît le kernel)
        ├── persistence/        # repos R2DBC, un sous-package par brique
        ├── cache/              # Redis
        └── messaging/          # publishers Kafka
```

## Ports

### Ports de sortie (17) — vers le kernel et l'infra
Nommés **par capacité métier**, jamais par core du kernel (un port reste stable même si le kernel
renomme/fusionne un core) : `ProvisionnerAccesDev`, `ResoudreBusinessDomain`, `PersisterEntreprise`,
`GererCatalogueOffre`, `VerifierDisponibilite`, `ResoudrePersonne`, `ResoudreBeneficiaire`,
`AppliquerRoleTechnique`, `RattacherAOrganisation`, `EnregistrerVente`, `ExecuterWorkflow`,
`PublierEvenement`, `JournaliserAudit`, `StockerDocument`, `RegistreDeRegles`, `DepotDeConfiguration`,
`VerrouDIdempotence`.

### Ports internes (6) — stratégies extensibles
Partout où le domaine fait un choix qui pourra varier, ce choix passe par une interface dont
l'implémentation de départ est la plus simple ; étendre = ajouter une implémentation :
`EvaluateurDeRegle`, `PlanificateurDOperation`, `ExecuteurDEtape`, `FournisseurDeCapacite`,
`PorteMonnaieGenerique`, `HorlogeSysteme`.

## Réactif de bout en bout

Tout est non bloquant : les méthodes renvoient `Mono<T>` ou `Flux<T>`. `WebClient` pour le kernel
(jamais `RestTemplate`), R2DBC pour la base (jamais JDBC au runtime — JDBC sert uniquement à Liquibase
au démarrage). Aucun `.block()`.

## Correspondance avec les couches réseau (OSI)

Dans l'hexagone, plus on va vers le centre, plus on est « haut » dans les couches. Les couches basses
(infrastructure, transport) sont la périphérie (les adapters) ; les couches hautes (contexte, métier)
sont le centre (le domaine). Monter les couches = entrer vers le centre.
