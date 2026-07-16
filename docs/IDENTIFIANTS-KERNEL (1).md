# Identifiants kernel non modélisés — stratégie de résolution

> Le kernel exige des identifiants que **le métier ne connaît pas** : `agencyId`, `registerId`, `cashierId`, `sku`, `familyCode`, `variantLabel`, `currency`… Ce document dit, pour chacun, **d'où vient la valeur** et **qui la résout**.
>
> Règle absolue : **ces identifiants n'apparaissent JAMAIS dans le domaine ni dans l'API exposée au développeur.** Le développeur déclare un métier ; c'est l'**adapter kernel** qui fabrique/résout ces identifiants. À donner en contexte à ton IA avec `REFERENCE-KERNEL.md`.

## Hypothèse de départ : mono-agence / mono-caisse

Au démarrage, **une entreprise = une agence principale + une caisse**. Cela simplifie la résolution : aucun paramètre d'agence/caisse ne circule dans le métier. Le passage au multi-agence se fera plus tard via le **même mécanisme** (le résolveur ci-dessous), sans réécrire le domaine — cohérent avec le principe d'extensibilité (commencer simple, étendre par une interface).

---

## Le mécanisme : un `ResolveurContexteKernel`

Plutôt que chaque adapter aille chercher ces valeurs à sa façon, le **socle** expose un résolveur unique. L'adapter l'interroge pour une entreprise donnée et obtient un petit objet de contexte kernel.

```java
/** Contexte kernel résolu pour une entreprise : les identifiants que le métier ignore. */
public record ContexteKernel(
        UUID organizationId,
        UUID agencyId,        // agence principale (mono-agence au départ)
        UUID businessActorId, // mémorisé à l'onboarding
        UUID registerId,      // caisse principale (config)
        UUID cashierId,       // = acteur courant (BusinessContext)
        String currency       // devise (Configuration), défaut documenté
) {}

public interface ResolveurContexteKernel {
    Mono<ContexteKernel> resoudre(UUID businessId);
}
```

L'implémentation combine **quatre sources**, dans cet ordre de priorité :
1. le `BusinessContext` (acteur courant, entreprise ciblée),
2. nos **entités de liaison** (`Entreprise` : organizationId, businessActorId, agencyId),
3. la **Configuration** de l'entreprise (devise, caisse…),
4. une **résolution kernel auto** (ex. agence principale via `GET agencies`), avec **défaut documenté** en dernier recours.

---

## Tableau de résolution (par identifiant)

| Identifiant | Catégorie | Source exacte | Détail |
|---|---|---|---|
| `organizationId` | liaison | `Entreprise.organizationId` | déjà stocké, toujours dispo |
| `businessActorId` | liaison | `Entreprise.businessActorId` | **à ajouter à l'entité** ; mémorisé à l'onboarding (évite de le re-résoudre) |
| `agencyId` | auto + config | `GET /api/organizations/{id}/agencies` → principale ; surcharge config si besoin | mono-agence : prendre la 1ʳᵉ/principale. **À mémoriser** dans `Entreprise.agencyId` après 1ʳᵉ résolution |
| `cashierId` | contexte | `BusinessContext.actorId()` | le caissier = l'opérateur courant. **Jamais configuré.** |
| `registerId` | config | Configuration entreprise (`parametre_config` clé `caisse_principale`) | choix d'exploitation ; défaut = 1ʳᵉ caisse via `GET /api/cash-registers` |
| `currency` | config | Configuration entreprise (clé `devise`) | défaut documenté : `XAF` |
| `customerThirdPartyId` | auto | port `ResoudreBeneficiaire` | créer/trouver l'actor puis le tiers, au moment de la vente |
| `sku` | dérivation | `DefinitionOffre.code` (+ version) | déterministe, ex. `OFFRE-{code}-V{n}`. **Jamais saisi.** |
| `familyCode` | dérivation | mapping Type Métier → famille, ou `businessDomainId` | défaut documenté `GENERAL` si absent |
| `variantLabel` | dérivation | nom de la capacité/variante de l'offre | défaut `STANDARD` si l'offre n'a pas de variante |
| `unitPrice` | métier | `DefinitionOffre.prix` | vient du socle de l'offre (FIXE) ; SUR_DEVIS → résolu autrement |

---

## Trois catégories, trois traitements

### 1. Configuration (valeur stable, choisie)
`registerId`, `currency`. Ce sont de vrais choix d'exploitation qu'on ne peut pas deviner. Ils vivent dans la **Configuration** de l'entreprise (brique 7), avec un défaut documenté. Le résolveur lit la config, sinon applique le défaut.

> ⚠️ Faux ami : `agencyId` semble configurable mais est surtout **auto-résolvable** (agence principale). `cashierId` n'est **pas** une config : c'est l'acteur courant.

### 2. Résolution auto (déjà possédé ou récupérable)
`organizationId`, `businessActorId` : déjà dans notre entité `Entreprise` (catégorie « liaison » du modèle de données — on stocke les références kernel). `agencyId` : récupéré une fois via `GET agencies` puis **mémorisé**. `customerThirdPartyId` : résolu au vol via le port.

> **À faire** : ajouter `businessActorId` et `agencyId` à l'entité `Entreprise` (+ migration Liquibase + RLS). On stocke la référence, on ne la redemande pas à chaque appel — c'est le principe « entité de liaison ».

### 3. Dérivation (calculé depuis nos données)
`sku`, `familyCode`, `variantLabel` : **jamais saisis**. L'adapter les fabrique de façon déterministe depuis la `DefinitionOffre`. Le développeur déclare une offre métier ; le SKU kernel en découle. `currency` se lit de la config.

---

## Exemple : résolution pour une vente

```java
// Dans l'adapter EnregistrerVente / PorteMonnaie — PAS dans le domaine
return resolveurContexteKernel.resoudre(businessId)
    .flatMap(ctx -> {
        // ctx.organizationId(), ctx.agencyId(), ctx.cashierId(), ctx.registerId(), ctx.currency()
        // sont tous résolus : l'adapter construit les requêtes kernel sans rien demander au métier.
        ...
    });
```

Le use case métier appelle simplement « enregistrer la vente de l'offre X, quantité Y, pour le bénéficiaire Z ». Tous les identifiants kernel sont injectés par l'adapter via le résolveur. **Le domaine reste pur.**

---

## Impacts à prévoir

1. **Entité `Entreprise`** : ajouter `businessActorId` et `agencyId` (références mémorisées). Migration + RLS.
2. **Socle** : créer `ResolveurContexteKernel` + `ContexteKernel` (port interne) et son implémentation (combine BusinessContext + Entreprise + Configuration + appels kernel).
3. **Configuration** : prévoir les clés `devise` (défaut XAF) et `caisse_principale`.
4. **Adapters concernés** : `EnregistrerVente`, `PorteMonnaie`, `GererCatalogueOffre`, `VerifierDisponibilite` passent par le résolveur au lieu d'exiger ces ids du domaine.

## Extension multi-agence (plus tard)
Quand le multi-agence arrivera : `ResolveurContexteKernel.resoudre(businessId, agencyId?)` — l'agence devient un paramètre optionnel de l'opération, résolu à l'agence principale si absent. Le reste du mécanisme ne change pas.
