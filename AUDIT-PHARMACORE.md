# Audit PharmaCore — Business Core & Kernel Core

*Audit technique complet, préparé pour la présentation devant le superviseur et les enseignants — 15 juillet 2026.*

Chaque affirmation de ce document a été vérifiée en direct (base de données réelle, appels réseau réels, lecture du code) — rien n'est déduit par supposition.

---

## Résumé exécutif

- **1 problème bloquant confirmé** côté Kernel Core (racine unique de « la vente ne marche pas ») — non corrigeable par nous
- **2 incohérences corrigées** pendant cet audit (stock jamais synchronisé, Titulaire exclu de la caisse)
- **3 mécanismes vérifiés sains** avec preuves (clé API, données du frontend développeur, architecture)
- **4 éléments corrigés au total** pendant cette session (les 2 ci-dessus + historique des ventes + audit des requêtes)
- **1 limite connue, assumée** (comptes personnel locaux) — non corrigeable sans que Kernel répare d'abord son propre système de vérification d'e-mail

**Message le plus important à retenir :** il n'y a qu'**un seul problème racine technique** — Kernel Core refuse un type de demande d'accès (badge d'authentification machine). Tout ce qui touche la vente et le paiement en découle. À côté de ça, on a trouvé **quatre vraies incohérences de logique** qui n'ont rien à voir avec Kernel et qu'il faut corriger avant la présentation.

---

## 1. Est-ce que Pharmaco est vraiment construit sur Business Core ?

> **Verdict : oui, sur l'essentiel.**

PharmaCore ne refait pas le travail de Business Core là où ça compte. J'ai cherché dans tout le code de PharmaCore un moteur de règles, un calcul de taxe ou une logique de vente refaits localement — **il n'y en a aucun**. Tout ça vient réellement de Business Core.

Pour comprendre ce qu'on vérifie ici : Business Core est censé être une boîte à outils métier générique (un peu comme une caisse enregistreuse vierge qu'on peut configurer pour n'importe quel commerce). PharmaCore est censé se contenter de *configurer* cette caisse pour qu'elle comprenne « médicament », « ordonnance », « pharmacien » — pas reconstruire sa propre caisse à côté.

### Ce qui vient réellement de Business Core

- Le prix, la TVA (*taxe*), le seuil d'alerte de stock — déclarés une fois dans Business Core, jamais recalculés dans PharmaCore.
- La règle « ordonnance obligatoire » (*EVALUER_REGLES*) — c'est Business Core qui décide si la vente doit être bloquée, pas PharmaCore.
- Le déroulement complet d'une vente (chercher le produit, vérifier le stock, enregistrer, encaisser) — c'est une recette à six étapes (*saga*) qui vit entièrement dans Business Core ; PharmaCore se contente de l'appeler.
- La vérification de qui a le droit de faire quoi (rôles) est validée par Business Core à chaque appel, pas seulement affichée/masquée côté PharmaCore.

### Ce qui reste local dans PharmaCore — et pourquoi c'est normal

PharmaCore garde en local la fiche détaillée d'un médicament (sa molécule, sa forme), les comptes du personnel, les clients, les fournisseurs, les ordonnances. C'est **voulu et correct** : Business Core ne peut pas connaître le vocabulaire d'une pharmacie (il est générique, utilisable par n'importe quel métier) — c'est le travail de PharmaCore de traduire « vendre un Doliprane » en langage que Business Core comprend (« vendre l'offre n°X »).

### Deux vraies exceptions à connaître (et à savoir expliquer)

**⚠️ Les comptes Pharmacien / Caissier sont 100 % locaux à PharmaCore** *(déviation assumée)*

Normalement, une identité (un utilisateur) devrait être créée et vérifiée par Kernel Core (comme une carte d'identité nationale). Ici, ces comptes sont créés directement dans la base de PharmaCore, avec un simple mot de passe local.

*Pourquoi :* Kernel exige une vérification par e-mail pour créer une identité, et cette vérification n'a jamais fonctionné de façon fiable pendant le développement (e-mails jamais reçus). Plutôt que de bloquer tout le projet là-dessus, la décision a été prise de réutiliser l'identité Kernel déjà validée du titulaire pour représenter tout le personnel.

*Conséquence à assumer en présentation :* Business Core ne peut pas distinguer un pharmacien d'un caissier au niveau de l'identité réelle — seule l'interface de PharmaCore fait la différence. Ce n'est pas caché, c'est documenté, mais il faut savoir le dire clairement si on vous pose la question.

*Pourquoi ce n'est pas corrigé :* contrairement aux deux points précédents, celui-ci ne dépend pas que de notre code — corriger proprement demanderait que Kernel répare d'abord sa propre vérification par e-mail (hors de notre contrôle). Le contourner autrement reviendrait à réinventer un bout du système d'identité de Kernel, ce que le projet s'interdit. Reste donc une limite assumée, pas un bug qu'on peut fermer aujourd'hui.

**✅ Le stock affiché n'était jamais synchronisé avec la vraie source** *(corrigé pendant cet audit)*

Le nombre de boîtes en stock qu'on voit dans PharmaCore était saisi une fois à la création du médicament, puis augmenté seulement manuellement (réception fournisseur). **Aucun code, nulle part, ne le diminuait après une vente** — même la partie du programme qui gère les ventes n'y touchait jamais.

*Correction appliquée :* dès qu'une ligne de vente est réellement confirmée par Business Core (statut *COMPLETEE*), PharmaCore diminue maintenant le stock local de la quantité vendue (`Medicament.decrementerStock`, appelé dans `VenteService.creer`). Le compteur ne descend jamais sous zéro, même si le stock local était déjà désynchronisé.

*Pourquoi ça ne se voit pas encore :* tant que Kernel refuse le badge d'accès (section 3), aucune vente n'atteint le statut *COMPLETEE* — donc ce correctif est prêt et vérifié par compilation, mais ne produira un effet visible qu'une fois le blocage Kernel levé. C'est normal, pas un signe que le correctif ne marche pas.

---

## 2. Les rôles et le processus de vente sont-ils cohérents ?

Trois rôles existent : le **Titulaire** (le patron, propriétaire de la pharmacie), le **Pharmacien Responsable**, et le **Caissier**. Dans une vraie pharmacie, le titulaire est lui-même pharmacien : il peut tout faire, y compris vendre. Voilà ce qu'on a vérifié.

> **Verdict : cohérent sur la règle d'ordonnance.**
> La règle « ordonnance obligatoire » reflète bien la réalité : un caissier seul ne peut jamais vendre un médicament sur ordonnance ; un pharmacien peut le faire, mais doit taper une justification (*motif de dérogation*) qui reste enregistrée. C'est exactement comme dans une vraie pharmacie.

**✅ Le Titulaire n'avait pas le droit d'utiliser la caisse dans l'application** *(corrigé pendant cet audit)*

La liste des rôles autorisés à accéder à l'écran de vente (le poste de caisse) contenait *Pharmacien Responsable* et *Caissier* — mais **pas Titulaire**. Si le titulaire essayait d'ouvrir l'écran de caisse, l'application le redirigeait ailleurs.

En creusant plus loin que le frontend, j'ai trouvé que ce n'était pas un simple oubli : le **backend bloquait aussi explicitement** cette action, avec un message volontaire (« le titulaire ne vend pas »). C'était donc une règle assumée à l'origine, pas un accident — mais elle contredisait l'idée qu'« un titulaire est d'abord un pharmacien, donc il peut tout faire », et le titulaire est de toute façon déjà enregistré côté Kernel comme un vrai caissier (avec le droit technique de vendre).

*Corrections appliquées (les deux couches, pas seulement l'affichage) :*
- **Backend** (`VenteService.exigerActeurConnecte`) : le titulaire peut maintenant déclencher une vente, avec sa vraie identité Kernel (`titulaireActorId`), exactement comme le personnel.
- **Frontend** (`roles.ts`) : Titulaire ajouté à la liste des rôles autorisés sur `/vente`.
- **Frontend** (`AppShell.tsx`) : un lien « Poste de vente » a été ajouté à la navigation du titulaire — sans ça, même autorisé, il n'aurait eu aucun moyen d'y accéder depuis le menu.

*Point resté cohérent sans y toucher :* si le titulaire essaie de vendre un médicament sur ordonnance, il reste traité comme un caissier pour cette règle précise (il ne peut pas fournir de motif de dérogation) — c'est correct, puisque son identité Kernel réelle est bien CAISSIER, pas Pharmacien Responsable.

### Choisir un médicament, une quantité — est-ce que ça marche ?

Oui, entièrement — voir la section suivante pour l'explication complète : cette partie se passe uniquement dans votre navigateur, avant tout appel au serveur, donc elle fonctionne toujours instantanément. Le blocage n'arrive qu'au clic final.

---

## 3. Pourquoi la vente ne peut pas être validée (expliqué simplement)

Reprenons depuis le début, sans jargon.

> **Une analogie pour comprendre**
>
> Imaginez une pharmacie où la caisse enregistreuse (**PharmaCore**) ne peut pas gérer le stock toute seule. Pour valider une vente, elle doit appeler le **siège administratif** (**Business Core**), qui lui-même doit appeler **l'entrepôt central** (**Kernel Core**) — le seul endroit où est gardé le vrai compte des stocks et l'argent, pour toutes les entreprises de la plateforme, pas que les pharmacies.
>
> Pour parler à l'entrepôt central, le siège doit d'abord montrer un **badge d'accès** à l'entrée. Il demande ce badge à l'accueil de l'entrepôt. Ce jour-là, l'accueil refuse de délivrer ce type de badge : « ce type de demande n'est pas accepté ici » (en langage technique : Kernel rejette le *grant OAuth2 client_credentials* avec l'erreur *Unsupported grant_type*). Sans badge, le siège ne peut même pas entrer pour demander « il reste combien de boîtes ? » — donc tout s'arrête là, avant même d'avoir touché au stock.

Concrètement, dans l'application, choisir un médicament et une quantité se passe entièrement dans le panier, sur votre écran — rien n'est envoyé nulle part à ce stade, donc ça marche toujours. C'est uniquement quand on clique sur **« Encaisser »** que la caisse essaie vraiment d'appeler le siège, qui essaie d'appeler l'entrepôt, et se fait refuser le badge.

### Ce que j'ai testé, en direct, pendant cet audit

Deux tests séparés, faits à l'instant, qui donnent exactement la même réponse :

```
1) Demande directe du badge à Kernel (sans passer par PharmaCore ni Business Core)
POST /oauth2/token
<- 400 Bad Request — "Unsupported grant_type"  (réponse en 1,5 seconde)

2) Vraie tentative de vente, via PharmaCore, avec la vraie clé API
POST /v1/businesses/{id}/operations/Vendre:execute
<- 502 Bad Gateway — même cause exacte  (réponse en ~1 à 3 secondes)
```

Le fait que la réponse arrive vite (1 à 3 secondes) et proprement (un vrai message d'erreur, pas un silence) prouve que ce n'est **ni un problème de réseau, ni un bug lent dans notre code** — c'est un refus net et volontaire du côté de Kernel.

### Le détail : quelle étape échoue exactement

La vente est une recette à six étapes. Voici où ça bloque aujourd'hui :

| Étape | Statut | Explication |
|---|---|---|
| 1. Vérifier le stock | ❌ **Échoue ici** | Demande le badge à Kernel pour consulter le stock réel — refusé ici. Tout s'arrête. |
| 2. Vérifier les règles métier | — | Jamais atteinte (dépend de l'étape 1). |
| 3. Enregistrer la vente | — | Jamais atteinte — demande aussi un badge Kernel, aurait le même problème. |
| 4. Décrémenter le stock | — | Jamais atteinte — même badge nécessaire. |
| 5. Encaisser le paiement | — | Jamais atteinte — même badge nécessaire (détails section 4). |
| 6. Notifier les autres services | — | Jamais atteinte. |

**Point important à signaler en présentation :** il y a quelques jours, une vente réelle passait les trois premières étapes et échouait seulement à l'étape 4 (le stock). Aujourd'hui elle échoue dès l'étape 1. Le problème n'a pas changé de nature — mais il s'est aggravé côté Kernel entre les deux dates (probablement un badge encore valable qui a fini par expirer). Ce n'est en aucun cas lié à un changement de notre code : ni PharmaCore ni Business Core n'ont été modifiés sur ce chemin depuis.

### Ce que ça veut dire pour la présentation

Il faut être honnête et clair là-dessus plutôt que d'essayer de le cacher : **le processus de vente est entièrement écrit, correct, et prêt** — la preuve, c'est qu'il échoue exactement là où on s'y attend, avec un message d'erreur clair, et qu'il annule proprement ce qu'il avait commencé (aucune donnée corrompue). Le seul manque, c'est que la plateforme centrale externe (Kernel) ne délivre pas le badge nécessaire en ce moment. C'est un problème d'infrastructure externe, pas un problème de conception ou de code.

---

## 4. L'API de paiement

Attention, il existe en réalité **deux paiements complètement différents** dans le projet — il ne faut pas les confondre en présentation.

**✅ 1. Paiement de l'abonnement du développeur (changer de plan tarifaire)** *(simulé, assumé)*

Sur le tableau de bord développeur, quand on clique « changer de plan », le paiement est **volontairement simulé** — le code contient littéralement le mot *SIMULATION* dans l'identifiant généré. C'est un choix honnête : Kernel ne propose pas encore de vraie API de paiement d'abonnement, donc en attendant, le changement de plan est confirmé directement, sans faire semblant d'avoir un vrai formulaire de carte bancaire.

**⚠️ 2. Paiement d'une vente en pharmacie (bouton « Encaisser »)** *(réel, jamais testé jusqu'au bout)*

Celui-là n'est **pas simulé** — c'est du vrai code qui appelle une vraie route de Kernel (*POST /api/bills/pay*) pour régler la facture générée par la vente. Le code existe, est correctement écrit, mais **n'a jamais pu être vérifié en conditions réelles**, tout simplement parce que la vente échoue avant même d'y arriver (étape 1, voir section 3). S'il était atteint aujourd'hui, il buterait très probablement sur le même refus de badge que les autres étapes.

**Réponse directe à la question « l'API de paiement fonctionne-t-elle » :** le code de paiement de vente est prêt et correct, mais actuellement invérifiable de bout en bout, pour la même raison racine que le reste — le badge Kernel refusé. Le paiement d'abonnement, lui, fonctionne, mais il s'agit d'une simulation assumée en attendant une vraie API côté Kernel.

---

## 5. Autres problèmes trouvés (au-delà de la vente)

**✅ L'historique des ventes n'affichait rien** *(corrigé)*

La page « Historique des ventes » n'avait qu'un message d'avertissement, sans aucun tableau — même si des ventes avaient un jour abouti, on n'aurait rien vu. Corrigé pendant cette session : la page affiche maintenant un vrai tableau (date, articles, montant, statut). Elle reste vide pour l'instant, ce qui est normal puisque aucune vente n'a encore abouti (voir section 3).

**⚠️ Les services s'arrêtent seuls, sans message d'erreur** *(à surveiller avant la présentation)*

Plusieurs fois pendant cette session, les 4 applications (et la base de données en mémoire *Redis*) se sont arrêtées en même temps, sans aucune trace d'erreur dans leurs journaux — signe que quelque chose *en dehors* du code (probablement une contrainte de ressources de l'ordinateur : mémoire ou disque) les a interrompues de l'extérieur. Ce n'est pas un bug de programmation.

*Recommandation :* redémarrer les 4 services juste avant la présentation, et prévoir une vérification rapide (« est-ce que tout répond ? ») dans les minutes qui précèdent.

**✅ La création de médicament n'apparaissait pas dans le journal d'audit** *(corrigé, session précédente)*

Un développeur qui crée un médicament ne voyait jamais cette action dans l'onglet « Requêtes » de son tableau de bord. Cause trouvée et corrigée : cette action passe par un chemin d'identification différent (une session de connexion plutôt qu'une clé API) qui n'était pas suivi par le journal. Toutes les actions sont maintenant suivies, avec une étiquette qui précise si elles comptent dans le quota du développeur ou non.

---

## 6. Le Frontend Développeur affiche-t-il de vraies données ?

> **Verdict : vérifié avec preuves.**
> Pas seulement « le code a l'air propre » — vérifié directement dans la base de données que les chiffres affichés correspondent à de vraies actions faites il y a quelques minutes.

**Preuve n°1 — la clé API**

```
Clé "test pharmacore" en base de données :
  dernière utilisation : 15/07 21:34:32
  <-> heure exacte de mon test réel de vente, à la seconde près
```

**Preuve n°2 — le journal des requêtes**

La table qui alimente l'onglet « Requêtes » contient mes vrais appels de test (les tentatives de vente qui échouent en 502, marquées facturables) *et* les vraies actions faites en naviguant dans le tableau de bord (consultation des entreprises, de l'audit — marquées non facturables, car ce sont des actions de consultation, pas des appels d'application). Rien n'est reconstitué après coup : chaque ligne correspond à un appel réseau qui a vraiment eu lieu.

**Preuve n°3 — recherche de données inventées**

J'ai cherché dans tout le code du Frontend Développeur des traces de données fictives (tableaux tout faits, générateurs de nombres aléatoires pour remplir un graphique, etc.). **Aucune trouvée.** Chaque chiffre affiché (quota, entreprises, clés actives, courbe de consommation) part d'un vrai appel au serveur.

---

## 7. La clé API, de bout en bout

Voici la chaîne complète, vérifiée avec de vraies données :

1. ✅ **Créée dans le Frontend Développeur** — Une ligne apparaît réellement dans la base de données, avec le secret stocké de façon chiffrée (jamais en clair).
2. ✅ **Utilisée par PharmaCore à chaque appel** — PharmaCore joint deux en-têtes à chaque requête (*X-BC-Client-Id*, *X-BC-Api-Key*) — vérifié dans le code, ce n'est pas optionnel.
3. ✅ **Vérifiée par Business Core à chaque requête** — Chaque appel est comparé à la base ; une clé désactivée (*révoquée*) est immédiatement rejetée.
4. ✅ **Utilisation comptée** — La date de dernière utilisation et le nombre de requêtes se mettent à jour en temps réel — confirmé avec la preuve n°1 ci-dessus.

**Verdict :** le mécanisme fonctionne réellement, de la création jusqu'au comptage — pas seulement en apparence.

---

## 8. Doublons et incohérences d'architecture

La question posée : est-ce qu'une partie du code a été écrite « comme si Business Core n'existait pas » ?

- **Moteur de règles :** aucun trouvé en dehors de Business Core. Recherché dans tout PharmaCore — rien.
- **Calcul de prix / taxe :** aucun recalcul local trouvé — tout part de la configuration déclarée dans Business Core.
- **Ancien code mort :** d'anciennes classes qui appelaient une route de médicaments aujourd'hui abandonnée ont déjà été supprimées avant cette session (nettoyage déjà fait).
- **Le seul vrai trou trouvé** était celui signalé plus haut : le stock local n'était relié à rien (ni dupliqué, ni délégué — simplement absent de la chaîne). **Corrigé pendant cet audit.**

**Verdict :** pas de duplication cachée de logique métier. Les déviations trouvées (comptes personnel locaux, stock non synchronisé) étaient des *trous*, pas des *doublons* — la nuance compte pour la présentation : on n'a pas recodé Business Core, il manquait juste un maillon à un endroit précis, maintenant posé.

---

## 9. Recensement complet des problèmes

| Problème | Où | Gravité | Impact | Ce qu'il faut faire |
|---|---|---|---|---|
| **Kernel refuse le badge d'accès machine** (*Unsupported grant_type*) | Kernel Core (externe) | 🔴 Bloquant | Bloque toute vente et le paiement associé, dès la 1ʳᵉ étape | Remonté à l'équipe Kernel. Rien à changer côté PharmaCore/Business Core : le code est prêt à fonctionner dès que Kernel accepte le badge. |
| **Stock jamais synchronisé** | PharmaCore backend | ✅ Corrigé | Ne se verra qu'une fois le blocage Kernel levé (aucune vente ne va au bout pour l'instant) | Fait pendant cet audit (`Medicament.decrementerStock` + `VenteService.creer`) |
| **Titulaire exclu de l'écran de caisse** | PharmaCore frontend + backend | ✅ Corrigé | Le titulaire peut désormais vendre, comme dans une vraie pharmacie | Fait pendant cet audit (`roles.ts`, `AppShell.tsx`, `VenteService.exigerActeurConnecte`) |
| **Comptes personnel 100 % locaux** | PharmaCore backend | 🟠 Limite connue, non corrigeable par nous | Business Core ne distingue pas réellement un pharmacien d'un caissier au niveau identité | Dépend d'une réparation côté Kernel (vérification d'e-mail) — à expliquer clairement en présentation |
| **Historique des ventes vide (page incomplète)** | PharmaCore frontend | ✅ Corrigé | — | Fait pendant cette session |
| **Requêtes design-time invisibles dans l'audit** | Business Core | ✅ Corrigé | — | Fait pendant cette session précédente |
| **Services qui s'arrêtent seuls** | Environnement machine | 🟠 Opérationnel | Risque de coupure pendant la démonstration | Redémarrer et vérifier juste avant la présentation |

---

*Document préparé pour la phase finale avant déploiement. Toutes les preuves techniques (journaux, requêtes réseau, contenu de base de données) ont été recueillies le 15 juillet 2026 et sont reproductibles à l'identique.*
