# Migration de la base de données vers PostgreSQL Railway

Ce guide décrit comment migrer votre base PostgreSQL locale (ou existante) vers la base PostgreSQL hébergée sur Railway.

## Prérequis

- Base source : `postgresql://buyv_admin:buyv123@localhost:5432/buyv_db` (voir `.env`)
- Base cible Railway : `postgresql://postgres:****@switchback.proxy.rlwy.net:25752/railway`
- PostgreSQL client installé en local (pour `pg_dump` / `psql`) ou utiliser le script Python fourni

---

## Étape 1 : Configurer la variable d'environnement sur Railway

Sur Railway, **ne mettez pas** l’URL de la base dans le code. Configurez la variable d’environnement dans le dashboard :

1. Ouvrez votre projet Railway → service backend (Buyv).
2. Onglet **Variables**.
3. Ajoutez ou modifiez :
   - **Nom** : `DATABASE_URL`
   - **Valeur** :  
     `postgresql://postgres:VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm@switchback.proxy.rlwy.net:25752/railway`

Railway fournit parfois une URL au format `postgres://`. L’app la convertit déjà en `postgresql://` dans `app/config.py`, donc les deux formats fonctionnent.

---

## Étape 2 : Migrer les données (source → Railway)

Deux options : **A** avec les outils PostgreSQL (recommandé), **B** avec le script Python.

### Option A : Avec `pg_dump` et `psql` (recommandé)

Sur votre machine, dans un terminal (PowerShell ou CMD), exécutez :

```powershell
# 1. Export de la base SOURCE (structure + données)
# Remplacez par votre chemin PostgreSQL si besoin (ex: "C:\Program Files\PostgreSQL\15\bin\pg_dump.exe")
pg_dump -h localhost -p 5432 -U buyv_admin -d buyv_db -F c -f buyv_backup.dump

# Mot de passe quand demandé : buyv123

# 2. Restauration vers Railway (base CIBLE)
pg_restore -h switchback.proxy.rlwy.net -p 25752 -U postgres -d railway --no-owner --no-acl -v buyv_backup.dump
# Mot de passe quand demandé : VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm
```

- `-F c` : format custom (fichier binaire), adapté à `pg_restore`.
- `--no-owner --no-acl` : évite les erreurs de droits sur Railway (l’utilisateur est `postgres`).

Si vous préférez un fichier SQL (script lisible) :

```powershell
pg_dump -h localhost -p 5432 -U buyv_admin -d buyv_db -f buyv_backup.sql
# Puis :
psql "postgresql://postgres:VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm@switchback.proxy.rlwy.net:25752/railway" -f buyv_backup.sql
```

Vous pouvez supprimer `buyv_backup.dump` ou `buyv_backup.sql` après migration.

### Option B : Script Python (si vous n’avez pas `pg_dump` sous la main)

Un script `migrate_postgres_to_railway.py` est fourni. Il copie les données de votre base source vers Railway.

1. Créez un fichier `.env.migration` (ou exportez les variables) avec :
   - `SOURCE_DATABASE_URL=postgresql://buyv_admin:buyv123@localhost:5432/buyv_db`
   - `TARGET_DATABASE_URL=postgresql://postgres:VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm@switchback.proxy.rlwy.net:25752/railway`

2. Exécutez :
   ```powershell
   cd buyv_kotlin\buyv_backend
   set SOURCE_DATABASE_URL=postgresql://buyv_admin:buyv123@localhost:5432/buyv_db
   set TARGET_DATABASE_URL=postgresql://postgres:VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm@switchback.proxy.rlwy.net:25752/railway
   python migrate_postgres_to_railway.py
   ```

Le script crée les tables sur la cible (schéma SQLAlchemy) puis copie les données. Utilisez Option A si possible pour un clone fidèle (séquences, contraintes, etc.).

---

## Étape 3 : Vérifier après migration

1. **En local** : pointez temporairement vers Railway pour tester :
   - Dans `.env` :  
     `DATABASE_URL=postgresql://postgres:VHePUUyTXmzcopVrFiDBJSCjiBXoWMOm@switchback.proxy.rlwy.net:25752/railway`
   - Lancez : `uvicorn app.main:app --reload`
   - Vérifiez que les utilisateurs, commandes, etc. s’affichent correctement.

2. **Sur Railway** : après déploiement, assurez-vous que la variable `DATABASE_URL` est bien définie (étape 1). Les tables sont créées au démarrage si besoin (`Base.metadata.create_all`), mais après migration elles existent déjà avec les données.

---

## Résumé

| Étape | Action |
|-------|--------|
| 1 | Dans Railway → Variables : `DATABASE_URL` = URL PostgreSQL Railway |
| 2 | Migrer les données : `pg_dump` + `pg_restore` (ou script Python) |
| 3 | Redéployer / lancer l’app et vérifier que tout fonctionne |

Pour garder la base locale pour le dev, laissez dans `.env` l’URL locale (`postgresql://buyv_admin:buyv123@localhost:5432/buyv_db`) et n’utilisez l’URL Railway que dans les variables d’environnement Railway.
