# Commandes de lancement — Business Core & PharmaCore

Toutes les commandes sont en **PowerShell** (Windows). Un service = un terminal.

> Aucune clé secrète n'est écrite dans ce fichier : le backend PharmaCore charge ses identifiants
> depuis son `.env.local` (voir scénario 1). Ne colle jamais de clé API en dur ici, ce fichier est suivi
> par git.

## Repères

| Service | Port | Dossier |
|---|---|---|
| Backend Business Core | **8081** | `d:\Academique\backend-business\Backend-business-core` |
| Frontend Business Core (console dev) | **3000** | `d:\Academique\frontend-business\Frontend-business-core` |
| Backend PharmaCore | **9090** | `d:\Academique\RESEAU\backend-business-core\pharmacie-backend-test` |
| Frontend PharmaCore | **3001** | `d:\Academique\RESEAU\backend-business-core\pharmacie-frontend-test` |

Java utilisé : `C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe`

**Dépendances externes** (doivent tourner) : PostgreSQL natif sur `5432` et Redis
(conteneur `businesscore-redis`). Vérification : voir scénario 5.

---

## Scénario 1 — Tout lancer (le cas normal)

Ouvre **4 terminaux**, dans cet ordre.

### Terminal 1 — Backend Business Core (8081)

```powershell
cd d:\Academique\backend-business\Backend-business-core
& "C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe" -jar target\business-core-1.0.0-SNAPSHOT.jar --server.port=8081
```

Attends `Started BusinessCoreApplication` (~30 s) **avant** de lancer PharmaCore.

### Terminal 2 — Frontend Business Core (3000)

```powershell
cd d:\Academique\frontend-business\Frontend-business-core
npm run dev
```

### Terminal 3 — Backend PharmaCore (9090)

Ce backend **ne lit pas** `.env.local` tout seul (pas de librairie dotenv) : on charge d'abord ses
variables, puis on démarre.

```powershell
cd d:\Academique\RESEAU\backend-business-core\pharmacie-backend-test
Get-Content .env.local | Where-Object { $_ -match '^\s*[^#\s].*=' } | ForEach-Object {
  $parts = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
}
& "C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot\bin\java.exe" -jar target\pharmacie-backend-0.1.0.jar
```

> Les deux étapes doivent être faites **dans le même terminal** : les variables ne survivent pas d'un
> terminal à l'autre.

### Terminal 4 — Frontend PharmaCore (3001)

```powershell
cd d:\Academique\RESEAU\backend-business-core\pharmacie-frontend-test
npm run dev
```

Le port 3001 est déjà imposé dans son `package.json`, rien à ajouter.

### Accès

- Console développeur : <http://localhost:3000>
- PharmaCore : <http://localhost:3001>
- Swagger Business Core : <http://localhost:8081/swagger-ui.html>

---

## Scénario 2 — Tout arrêter

Dans chaque terminal : `Ctrl + C`. Pour forcer l'arrêt de tout ce qui traîne (processus fantômes) :

```powershell
foreach ($p in 8081,9090,3000,3001,3002) {
  $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
  if ($c) {
    foreach ($procId in ($c.OwningProcess | Select-Object -Unique)) {
      Stop-Process -Id $procId -Force
      Write-Output "PORT $p (PID $procId) arrete"
    }
  } else { Write-Output "PORT $p deja libre" }
}
```

---

## Scénario 3 — J'ai modifié le code **backend**

Le jar est verrouillé tant que le backend tourne : **arrête-le d'abord** (Ctrl+C dans le terminal 1).

```powershell
cd d:\Academique\backend-business\Backend-business-core
mvnd package -DskipTests
```

Puis relance (Terminal 1 du scénario 1).

Avec la suite de tests complète (plus lent, ~4 min, lance Docker/Testcontainers) :

```powershell
mvnd package
```

---

## Scénario 4 — J'ai modifié le code **frontend**

Rien à faire : `npm run dev` recharge à chaud. Pour vérifier avant livraison :

```powershell
cd d:\Academique\frontend-business\Frontend-business-core
npx tsc --noEmit
npx next build
```

---

## Scénario 5 — Vérifier que tout va bien

```powershell
# Backend Business Core
curl http://localhost:8081/actuator/health          # attendu : {"status":"UP"}

# Qui écoute sur quoi
foreach ($p in 8081,9090,3000,3001) {
  $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
  if ($c) { Write-Output "PORT $p : OCCUPE (PID $($c.OwningProcess -join ','))" } else { Write-Output "PORT $p : libre" }
}

# Dépendances externes
docker ps --filter "name=businesscore-redis"        # Redis doit etre "Up"
Get-NetTCPConnection -LocalPort 5432 -State Listen  # PostgreSQL natif
```

Si Redis est arrêté :

```powershell
docker start businesscore-redis
```

---

## Scénario 6 — Lancer les tests backend

```powershell
cd d:\Academique\backend-business\Backend-business-core

mvnd test                                        # toute la suite (~185 tests)
mvnd test -Dtest='PlanServiceTest'               # une seule classe
mvnd test -Dtest='KernelPaiementAdapterTest,PlanServiceTest'   # plusieurs
```

Docker doit tourner (tests d'intégration RLS via Testcontainers).

---

## Scénario 7 — Tester le paiement réel (mobile money)

⚠️ **Débite réellement de l'argent.** Le montant doit rester **≥ 100 XAF** : en dessous, l'opérateur
mobile money refuse la transaction.

1. Lance tout (scénario 1).
2. Connecte-toi sur <http://localhost:3000> avec le compte développeur.
3. Menu **Tarifs & Consommation** → « Passer à PRO » (15 000 XAF).
4. Saisis le numéro mobile money (format `6XXXXXXXX`, ex. `692162333`).
5. Tu es redirigé vers la page de paiement MyCoolPay → valide sur ton téléphone.
6. Au retour sur la console, le plan s'active **uniquement si le paiement est confirmé**.
   S'il traîne, le bouton « J'ai payé » relance la vérification.

Vérifier l'état d'une commande de paiement côté kernel (remplace `<ORDER_ID>`) :

```powershell
curl -X POST "https://kernel-core.yowyob.com/api/payments/orders/<ORDER_ID>/refresh" `
  -H "Authorization: Bearer <TOKEN>" -H "X-Client-Id: business-core" `
  -H "X-Api-Key: <SECRET>" -H "X-Tenant-Id: <TENANT_DU_TOKEN>"
```

> `X-Tenant-Id` doit être le tenant **du token** lui-même, pas celui d'une autre organisation,
> sinon le kernel renvoie 401.

---

## Scénario 8 — Ça ne répond plus / port déjà utilisé

`Port XXXX is in use` ou le backend ne répond plus :

```powershell
# 1. Voir qui occupe le port
Get-NetTCPConnection -LocalPort 8081 -State Listen | Select-Object OwningProcess

# 2. Le tuer
Stop-Process -Id <PID> -Force
```

Puis relancer normalement (scénario 1). Le gel du serveur après quelques requêtes (double abonnement
dans les filtres de sécurité, fuite de connexions R2DBC) a été corrigé — s'il réapparaît, regarde les
logs du terminal 1 : des lignes `Réponse déjà commitée` en rafale sont le symptôme.

---

## Notes de configuration

- Le frontend Business Core lit `NEXT_PUBLIC_API_BASE_URL` dans son `.env.local` : il **doit** valoir
  `http://localhost:8081` (le backend n'est plus sur 8080).
- Le backend Business Core lit `.env` (base de données, kernel). `KERNEL_BASE_URL` ne doit **jamais**
  être vide : une valeur vide retombe sur WireMock (`localhost:8089`) et tous les appels kernel échouent.
- Les identifiants BCaaS de PharmaCore sont dans `pharmacie-backend-test\.env.local` (non versionné).
