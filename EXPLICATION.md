# Comprendre le projet — Business Core, Kernel, PharmaCore

> Document de référence, écrit pour dissiper toutes les confusions accumulées. Tout ce qui est décrit
> ici a été vérifié dans le vrai code et la vraie base de données au moment de l'écriture (12 juillet
> 2026), pas recopié de mémoire.

---

## 1. Le projet en une phrase

**Le Kernel RT-Comops sait gérer n'importe quelle organisation** (créer une entreprise, y rattacher des
gens, suivre des transactions), mais c'est un outil bas niveau, complexe, générique.

**Business Core est une façade** au-dessus du Kernel : il transforme ce langage bas niveau en un
langage métier simple (« déclarer une offre », « exécuter une vente ») pour que **n'importe quel
développeur** puisse construire une application métier (pharmacie, boutique, clinique...) **sans
jamais parler directement au Kernel**.

**PharmaCore est un exemple d'application** construite par-dessus Business Core, pour prouver que ça
marche vraiment — c'est un cas de test, pas un produit final.

```
┌─────────────────────────────────────────────────────────────┐
│  KERNEL RT-COMOPS                                            │
│  Le système bas niveau : identités, organisations,           │
│  stocks, paiements, transactions. Complexe, générique.       │
└───────────────────────────▲───────────────────────────────────┘
                             │ parle un langage technique bas niveau
┌───────────────────────────┴───────────────────────────────────┐
│  BUSINESS CORE (backend-business-core/business-core)         │
│  La façade métier : traduit "Kernel" en "Type Métier,         │
│  Offre, Opération, Règle..." — un vocabulaire que             │
│  n'importe quel développeur comprend sans connaître           │
│  le Kernel.                                                   │
└───────────────────────────▲───────────────────────────────────┘
                             │ appels HTTP authentifiés (clé API ou JWT)
┌───────────────────────────┴───────────────────────────────────┐
│  BACKEND D'UN DÉVELOPPEUR (ex. pharmacie-backend-test)        │
│  Le vrai métier spécifique : "un médicament", "une            │
│  ordonnance", "un client de pharmacie" — des concepts         │
│  que Business Core ne connaît PAS et n'a pas besoin           │
│  de connaître.                                                 │
└───────────────────────────▲───────────────────────────────────┘
                             │
┌───────────────────────────┴───────────────────────────────────┐
│  FRONTEND DU DÉVELOPPEUR (ex. pharmacie-frontend-test)         │
│  L'écran que voit l'utilisateur final (le pharmacien).         │
└─────────────────────────────────────────────────────────────┘
```

Il y a donc **deux frontends et deux backends différents dans ce projet**, et c'est ça qui embrouille :

| | Rôle | Qui l'utilise |
|---|---|---|
| **frontend-developpeur** | Console pour le **développeur** qui construit une app (toi) : créer un compte, une entreprise, une clé API, voir sa consommation | Toi, en tant que développeur |
| **Business Core** (`business-core/`) | Le moteur générique, pas d'interface visuelle propre, juste une API | Personne directement — c'est de l'infrastructure |
| **pharmacie-backend-test** | Le backend métier de "PharmaCore" — code Java qui connaît le concept de médicament, client, ordonnance | Le frontend PharmaCore |
| **pharmacie-frontend-test** | L'écran d'une pharmacie — ce que verrait un pharmacien | Un utilisateur final (fictif, pour le test) |

---

## 2. Ce qu'un développeur construit, et ce qu'il évite de refaire

**Ce qu'il construit lui-même** (son vrai travail, unique à son métier) :
- Ses propres concepts métier : "médicament", "ordonnance", "client de pharmacie" (pour PharmaCore) —
  ou "produit", "panier", "livraison" pour une boutique en ligne, etc.
- Son propre frontend, sa propre base de données locale pour ces concepts.

**Ce qu'il évite de refaire, grâce à Business Core** :
- Créer une organisation légale et la faire reconnaître par le système (ça, c'est fait UNE FOIS par
  `POST /v1/businesses`, qui parle au Kernel pour toi).
- Gérer l'authentification, les utilisateurs, les rôles (pharmacien, caissier...).
- Suivre les transactions financières, les mouvements de stock au niveau système.
- Appliquer des règles métier génériques (« ce produit nécessite une autorisation avant la vente »)
  sans écrire une ligne de code — juste déclarer la règle en donnée.
- Compter son usage, gérer sa facturation.

**Concrètement pour PharmaCore** : sans Business Core, l'équipe qui construit PharmaCore devrait
elle-même écrire tout le code qui parle au Kernel RT-Comops (protocoles d'authentification complexes,
création d'organisation, gestion de stock bas niveau...). Avec Business Core, PharmaCore n'a qu'à
appeler des endpoints simples comme `POST /v1/business-types/{id}/versions/1/offers` pour déclarer un
médicament — Business Core s'occupe de traduire ça en appels Kernel réels.

---

## 3. La clé API — à quoi elle sert, concrètement

La clé API, c'est **la carte d'identité que le backend d'un développeur (une machine) présente à
Business Core** pour prouver « j'ai le droit d'agir pour cette entreprise précise ».

- **`X-BC-Client-Id`** = ton **`developerId`** (stable, le même pour toutes tes entreprises — visible
  via `GET /v1/auth/me`).
- **`X-BC-Api-Key`** = le **secret d'une entreprise précise**. Une entreprise a au maximum **une** clé
  active à la fois.

**Où ça se voit dans le code de PharmaCore** :
- `pharmacie-backend-test/src/main/resources/application.yml` (lignes `pharmacore.bcaas.*`) — déclare
  où sont attendus le client-id et la clé.
- `pharmacie-backend-test/.../config/PharmacoreConfig.java` — pose ces deux valeurs comme en-têtes
  HTTP (`X-BC-Client-Id`, `X-BC-Api-Key`) sur **chaque appel sortant** vers Business Core.
- `pharmacie-backend-test/.../bcaas/BcaasClient.java` — le code qui utilise concrètement ce client HTTP
  pour déclarer une offre (un médicament).

Sans clé valide, Business Core refuse toute requête venant du backend PharmaCore (401).

---

## 4. Glossaire — les mots qui embrouillent, clarifiés un par un

### Organisation (Kernel) vs Entreprise (Business Core) — **c'est la même chose, deux noms**

Le Kernel appelle ça une **« Organization »**. Business Core appelle ça une **« Entreprise »**. C'est
littéralement la même donnée réelle, vue par deux systèmes différents : la table `entreprise` de
Business Core a une colonne `organization_id` qui pointe vers l'Organization réelle côté Kernel.

Quand tu cliques « Créer une entreprise » dans la console développeur, Business Core :
1. crée sa propre ligne `entreprise` (compréhensible pour toi, le développeur) ;
2. **et en même temps**, provisionne une vraie Organization côté Kernel (compréhensible pour le
   système bas niveau) — actor → organisation → approbation → services → agence.

Donc : **« Entreprise »** = le mot que TU utilises. **« Organisation »** = le même objet, vu du Kernel.
Pas deux choses différentes.

### Acteur métier (Business Core) vs Business Actor (Kernel) — **ici, ce sont bien deux choses différentes**

- **« Business Actor »** (Kernel) : rôle technique bas niveau, donné automatiquement au propriétaire
  d'une Organization au moment de sa création (`businessActorRole = OWNER`). Tu ne le gères jamais
  directement.
- **« Acteur métier »** (Business Core, brique 3) : une personne rattachée à TON entreprise avec UN
  rôle métier que TU as défini (« pharmacien responsable », « caissier »). Ça se déclare via
  `POST /v1/businesses/{id}/actors`.

**Constat vérifié** : pour l'entreprise PharmaCore actuelle, **aucun acteur métier n'a encore été
déclaré**. Cette brique existe dans le système mais n'est pas encore utilisée par PharmaCore.

### Type Métier vs Entreprise — le modèle et l'objet

- **Type Métier** = le gabarit, déclaré une fois (« Pharmacie », avec ses règles, ses rôles possibles,
  son catalogue). Comme une **classe** en programmation.
- **Entreprise** = une instance concrète de ce gabarit (« Ma pharmacie du quartier »). Comme un
  **objet** créé à partir de cette classe.

Tu peux créer plusieurs entreprises à partir du même Type Métier (plusieurs pharmacies utilisant le
même modèle), chacune avec sa propre clé API, ses propres données, complètement cloisonnées.

### Offre

Un article ou service vendable, déclaré sous un Type Métier. Pour PharmaCore : **chaque médicament
créé DEVIENT une Offre côté Business Core**. C'est la brique la plus utilisée aujourd'hui par
PharmaCore.

### Opération / Règle / Transaction / Configuration

- **Opération** : un acte métier exécutable, en plusieurs étapes (ex. « Vendre » = vérifier stock →
  évaluer règles → enregistrer vente → encaisser). **Pas encore construit dans PharmaCore** (voir §6).
- **Règle** : une contrainte déclarée sans code (« ce médicament nécessite une ordonnance »). **Pas
  encore déclarée pour PharmaCore.**
- **Transaction** : la trace d'un échange de valeur, née automatiquement d'une Opération exécutée. Ne
  peut pas exister sans Opération — donc **inexistante pour l'instant côté PharmaCore**.
- **Configuration** : des réglages (devise, seuils...) attachés au Type ou à l'Entreprise.

### Le Kernel (RT-Comops)

Le système racine, en dehors de ce dépôt. Il gère les identités réelles (comptes, mots de passe via
Kernel), les Organizations, les mouvements de stock bas niveau, les paiements. Business Core ne
duplique jamais ses données — il les référence et les interroge à la demande.

---

## 5. Contexte réel de tes tests — tes vraies entreprises, vérifiées en base

Tu as actuellement **deux entreprises réelles** sous ton compte `techlan500@gmail.com` (pas trois — j'ai
vérifié en base, la « Test Pharmaco » dont tu parlais n'existe pas encore, c'était une question
hypothétique) :

| Entreprise (nom réel en base) | Type Métier | Clé API active | Créée le |
|---|---|---|---|
| **Pharmacie Test Compte2 E** | PHARMA_TEST2 | `test pharmacore` | 09/07 |
| **Boutique Demo SARL** | BOUTIQUE_DEMO | `test` | 09/07 |

C'est **« Pharmacie Test Compte2 E »** qui est l'entreprise de PharmaCore — c'est sous elle que tous
les médicaments que tu as créés (Doliprane, Postunor 2...) existent réellement comme Offres.

### Réponse directe à ta question : « Si je te donne la clé de cette entreprise, ça a du sens ? »

**Oui, exactement.** Voici pourquoi, précisément :

- PharmaCore (le backend Java `pharmacie-backend-test`) n'est **qu'un client HTTP** — il n'a aucune
  mémoire propre de « quelle entreprise » il représente. Cette information vient **entièrement** de la
  clé API qu'on lui donne en configuration (`.env.local`).
- La clé que j'avais configurée manuellement avant (`bck_RW7hyfIx42bC`) est maintenant **invalide** —
  pas parce que le travail est perdu, mais parce que le **format des clés a changé** (l'équipe a fait
  évoluer le modèle vers « une clé par entreprise » pendant qu'on travaillait). Cette ancienne clé
  n'était d'ailleurs rattachée à aucune entreprise dans l'ancien modèle.
- **Rien n'est perdu** : tous les médicaments déjà créés (Doliprane, Postunor 2...) existent
  **réellement** dans la base de Business Core, rattachés à l'entreprise « Pharmacie Test Compte2 E ».
  Donner la clé ACTIVE de cette même entreprise, c'est simplement redonner à PharmaCore le droit de
  continuer à agir pour cette même entreprise, avec les mêmes données.
- Si à la place tu me donnais la clé de « Boutique Demo SARL », PharmaCore se mettrait à agir pour la
  boutique, pas la pharmacie — mauvaise entreprise, mauvais catalogue.

**Donc : oui, donne-moi la clé active de « Pharmacie Test Compte2 E »** (`test pharmacore`), c'est la
bonne, et ça continue exactement le travail déjà fait.

---

## 6. Bilan honnête — quelles briques PharmaCore utilise VRAIMENT aujourd'hui

Vérifié en base de données, pas supposé :

| Brique | Utilisée par PharmaCore ? | Preuve vérifiée |
|---|---|---|
| 1. Type Métier | ✅ Oui | `PHARMA_TEST2`, publié, version 1 |
| 2. Offre | ✅ Oui | 10 offres réelles déclarées (médicaments) |
| 3. Acteurs métier | ❌ Non | 0 ligne dans `acteur_metier` pour cette entreprise |
| 4. Règles | ❌ Non | 0 ligne dans `regle_metier` |
| 5. Opérations | ❌ Non | 0 ligne dans `definition_operation` — **le module "Vente" n'a jamais été codé** côté PharmaCore |
| 6. Transactions | ❌ Non (dépend des Opérations) | — |
| 7. Configuration | Non vérifié, probablement non utilisé | — |

**Donc PharmaCore, aujourd'hui, ne prouve que 2 des 7 briques** (Type Métier + Offres) — la partie
« catalogue ». Toute la partie « vente réelle » (Opérations, Règles, Acteurs, Transactions), qui est
censée être le vrai test de bout en bout, reste à construire.

**Petite transparence supplémentaire** : parmi les 10 offres, plusieurs (« Test isolation compteur »,
« Isolation directe BC », « Diagnostic trace »...) sont des **offres de test que j'ai créées moi-même
pendant le débogage** du bug de comptage x4, pas de vrais médicaments. Elles ne gênent rien
techniquement, mais je te le signale par honnêteté — ce ne sont pas des données métier réelles de
pharmacie.

---

## 7. Ce qui reste à faire, dans l'ordre logique

1. Tu me donnes la clé active de « Pharmacie Test Compte2 E » → je reconfigure PharmaCore et confirme
   que le catalogue de médicaments fonctionne à nouveau.
2. On construit le module **Vente** (`POST /api/ventes`) côté PharmaCore — c'est la pièce qui manque
   pour exercer réellement les briques Opérations/Règles/Transactions.
3. On déclare au moins un **Acteur métier** (le pharmacien) pour que la Règle « ordonnance requise »
   ait un sens (elle a besoin d'un rôle autorisé à déroger).

---

*Document généré pour clarifier l'architecture — à mettre à jour si le modèle évolue encore.*
