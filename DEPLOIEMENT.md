# Guide de déploiement — GesY

Ce document décrit les étapes complètes pour déployer GesY sur un VPS Ubuntu 24.04 LTS.

**Configuration cible :**
- 32 GB RAM, 8 CPU, 400 GB SSD
- Ubuntu 24.04 LTS
- PostgreSQL installé sur l'hôte (hors Docker)
- Application (API + Frontend + Traefik) en Docker

---

## 1. Architecture

```
                    ┌─────────────────────────────────────┐
                    │              TRAEFIK                │
                    │  (reverse proxy, HTTPS, Let's Encrypt) │
                    │  Ports 80 (redirect) + 443 (HTTPS)   │
                    └──────────────┬──────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    │
    ┌─────────────────┐  ┌─────────────────┐           │
    │  sfb-petroleum  │  │ api.sfb-         │           │
    │  .net           │  │ petroleum.net    │           │
    │  → gesy-app     │  │ → gesy-api       │           │
    │  (Nginx)        │  │ (Spring Boot)    │           │
    └─────────────────┘  └────────┬────────┘           │
                                   │                    │
                                   ▼                    │
                          ┌─────────────────┐           │
                          │  PostgreSQL 16.11 │◄──────────┘
                          │  (sur l'hôte)   │   172.17.0.1 (gateway Docker)
                          └─────────────────┘
```

---

## 2. Prérequis DNS

> **À faire en premier** — La propagation DNS peut prendre du temps.

### 2.1 Enregistrements à créer

| Type | Nom (sous-domaine) | Valeur | TTL |
|------|--------------------|--------|-----|
| **A** | `@` (ou vide) | `IP_DU_VPS` | 300 |
| **A** | `api` | `IP_DU_VPS` | 300 |

Exemple pour `sfb-petroleum.net` :
- `sfb-petroleum.net` → IP du VPS
- `api.sfb-petroleum.net` → IP du VPS

### 2.2 Vérification

```bash
# Vérifier que les DNS sont propagés
dig sfb-petroleum.net +short
dig api.sfb-petroleum.net +short
```

---

## 3. Installation de Docker

### 3.1 Installation de Docker Engine


```bash
# Suppression d'anciennes versions éventuelles
sudo apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

# Installation des dépendances
sudo apt update
sudo apt install -y ca-certificates curl gnupg

# Clé GPG officielle Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Dépôt Docker
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Installation
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 3.2 Vérification

```bash
sudo docker run hello-world
docker compose version
```

---

## 4. Installation et configuration de PostgreSQL

### 4.1 Installation de PostgreSQL

> **Version :** PostgreSQL 16.11 (Ubuntu 24.04)

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib -y

# Vérifier l'installation
sudo systemctl status postgresql
sudo -u postgres psql -c "SELECT version();"
```

### 4.2 Création de la base et de l'utilisateur

```bash
# Connexion en tant que superutilisateur PostgreSQL
sudo -u postgres psql

-- Dans le shell psql :
CREATE USER gesy WITH PASSWORD 'VOTRE_MOT_DE_PASSE_SECURISE';
CREATE DATABASE gesy OWNER gesy;
GRANT ALL PRIVILEGES ON DATABASE gesy TO gesy;
\q
```

> Remplacez `VOTRE_MOT_DE_PASSE_SECURISE` par un mot de passe fort.

### 4.3 Autoriser les connexions depuis Docker

> **Note :** L'IP du gateway Docker (`172.17.0.1`) sera configurée dans `.env` (section 6). Elle s'obtient avec `docker network inspect bridge | grep Gateway` après l'installation de Docker.

**Étape 1 — postgresql.conf :**

```bash
sudo nano /etc/postgresql/16/main/postgresql.conf
```

```conf
# Écouter sur toutes les interfaces (nécessaire pour Docker)
listen_addresses = '*'
```

**Étape 2 — pg_hba.conf :**

```bash
sudo nano /etc/postgresql/16/main/pg_hba.conf
```

Ajoutez cette ligne (après les lignes localhost) :

```conf
# TYPE  DATABASE  USER    ADDRESS           METHOD
local   all       all                       peer
host    all       all     127.0.0.1/32      scram-sha-256
host    all       all     ::1/128           scram-sha-256

# Autoriser les connexions depuis tous les réseaux Docker (172.16.0.0/12)
host    gesy      gesy    172.16.0.0/12     scram-sha-256
```

**Étape 3 — Redémarrer PostgreSQL :**

```bash
sudo systemctl restart postgresql@16-main
sudo systemctl enable postgresql@16-main

# Vérifier que PostgreSQL écoute bien
sudo ss -tlnp | grep 5432
# Doit afficher : 0.0.0.0:5432

sudo -u postgres psql -c "SHOW listen_addresses;"
# Doit afficher : *
```

---

## 5. Transférer les fichiers sur le VPS

Avant de configurer les variables d'environnement, les fichiers du projet doivent être présents sur le serveur.

```bash
# Depuis votre machine locale (exemple avec scp)
scp -r gesy user@IP_DU_VPS:/opt/apps/
```

Ou utilisez `rsync`, Git, SFTP, etc.

**Structure attendue sur le serveur :**

```
/opt/apps/gesy/
├── compose.yaml
├── .env.example
├── ApiGesy-main/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── gesyApp-main/
    ├── Dockerfile
    ├── nginx.conf
    ├── package.json
    ├── angular.json
    └── src/
```

---

## 6. Configuration des variables d'environnement

### 6.1 Créer le fichier .env

```bash
docker network inspect bridge | grep Gateway   # → "Gateway": "172.17.0.1"
cd /opt/apps/gesy
cp .env.example .env
nano .env
```

### 6.2 Variables à configurer

| Variable | Description | Exemple |
|----------|-------------|---------|
| `DATABASE_HOST` | Hôte PostgreSQL (gateway Docker bridge) | `172.17.0.1` |
| `DATABASE_PORT` | Port PostgreSQL | `5432` |
| `DATABASE_NAME` | Nom de la base | `gesy` |
| `DATABASE_USER` | Utilisateur PostgreSQL | `gesy` |
| `DATABASE_PASSWORD` | Mot de passe (obligatoire) | Votre mot de passe |
| `MAIL_HOST` | Serveur SMTP/IMAP | `mail.sfb-petroleum.com` |
| `MAIL_PORT` | Port SMTP | `465` |
| `MAIL_USERNAME` | Email d'envoi | `votre_email@sfb-petroleum.com` |
| `MAIL_PASSWORD` | Mot de passe email | Votre mot de passe |
| `MAIL_IMAP_HOST` | Serveur IMAP (si différent) | `mail.sfb-petroleum.com` |
| `DOMAIN` | Domaine principal | `sfb-petroleum.net` |
| `ACME_EMAIL` | Email Let's Encrypt | `admin@sfb-petroleum.net` |
| `API_URL` | URL de l'API (build frontend) | `https://api.sfb-petroleum.net/api` |

### 6.3 Exemple de .env complet

```env
# DATABASE_HOST = gateway Docker (docker network inspect bridge | grep Gateway)
DATABASE_HOST=172.17.0.1
DATABASE_PORT=5432
DATABASE_NAME=gesy
DATABASE_USER=gesy
DATABASE_PASSWORD=MonMotDePasseSecurise123!

# Mail SMTP/IMAP (envoi et réception d'emails)
MAIL_HOST=mail.sfb-petroleum.com
MAIL_PORT=465
MAIL_USERNAME=votre_email@sfb-petroleum.com
MAIL_PASSWORD=VotreMotDePasseEmail
MAIL_IMAP_HOST=mail.sfb-petroleum.com
MAIL_IMAP_PORT=993

DOMAIN=sfb-petroleum.net
ACME_EMAIL=admin@sfb-petroleum.net
API_URL=https://api.sfb-petroleum.net/api

TZ=UTC
```

> Si `DATABASE_PASSWORD` contient `@`, `#`, `$`, etc., utilisez des guillemets : `DATABASE_PASSWORD="@motdepasse$123"`

---

## 7. Déploiement de l'application

### 7.1 Lancer le déploiement

```bash
cd /opt/apps/gesy
docker compose build --no-cache && docker compose up -d --remove-orphans
```

### 7.2 Vérification

```bash
docker compose ps
```

Les 3 services doivent être `running`. Accès : https://sfb-petroleum.net et https://api.sfb-petroleum.net/api

**Commandes utiles :** `docker compose logs -f --tail=200` | `git pull && docker compose build --no-cache && docker compose up -d --remove-orphans` (rebuild) | `docker compose down`

---

## 8. Dépannage

### L'API ne se connecte pas à PostgreSQL

**1. Vérifier listen_addresses :**
```bash
sudo -u postgres psql -c "SHOW listen_addresses;"
# Doit afficher : *
sudo ss -tlnp | grep 5432
# Doit afficher : 0.0.0.0:5432
```

**2. Vérifier pg_hba.conf :**
```bash
sudo grep "172.16" /etc/postgresql/*/main/pg_hba.conf
# Doit afficher la règle avec 172.16.0.0/12
```

**3. Vérifier DATABASE_HOST dans .env :**
```bash
# Identifier le gateway Docker
docker network inspect bridge | grep Gateway
# Utiliser cette IP dans .env (généralement 172.17.0.1)
```

**4. Test de connectivité depuis un conteneur :**
```bash
docker run --rm --network bridge busybox:latest nc -zv 172.17.0.1 5432
# Doit afficher : open
```

**5. Test de connexion depuis l'hôte :**
```bash
psql -h 127.0.0.1 -U gesy -d gesy -c "SELECT 1;"
```

### Les certificats HTTPS ne se génèrent pas

1. Vérifiez que les ports 80 et 443 sont ouverts (via votre fournisseur).

2. Vérifiez que les DNS pointent bien vers le VPS.

3. Consultez les logs Traefik :
   ```bash
   docker compose logs traefik
   ```

### Erreur "DATABASE_PASSWORD requis"

Le fichier `.env` doit contenir `DATABASE_PASSWORD` avec une valeur non vide. Vérifiez qu'il est bien à la racine du dossier `gesy/` et qu'il est chargé par Docker Compose.

### Redémarrer un service

```bash
docker compose restart api
```
