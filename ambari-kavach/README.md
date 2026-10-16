# Ambari Kavach

> **"Kavach"** — shield or armour in Sanskrit.

Ambari Kavach is a self-hosted security access-control layer for [Apache Ambari](https://ambari.apache.org/) clusters. It replaces long-lived shared credentials with a fully audited, temporary-user issuance system, accessible through a modern web dashboard backed by Google SSO.

---

## Table of Contents

1. [Why Ambari Kavach?](#why-ambari-kavach)
2. [Architecture](#architecture)
3. [Key Features](#key-features)
4. [Security Design](#security-design)
5. [Role Hierarchy](#role-hierarchy)
6. [Project Structure](#project-structure)
7. [Prerequisites](#prerequisites)
8. [Getting Started](#getting-started)
9. [Configuration Reference](#configuration-reference)
10. [Dashboard Walkthrough](#dashboard-walkthrough)
11. [API Reference](#api-reference)
12. [Emergency DR Recovery](#emergency-dr-recovery)
13. [Production Deployment](#production-deployment)

---

## Why Ambari Kavach?

Apache Ambari ships with a single `admin` account that teams tend to share indefinitely. This creates real operational and security problems:

| Problem | How Kavach solves it |
|---|---|
| Shared `admin/admin` is a single point of compromise | Registration replaces it with a dedicated vault service account; the `admin` account is permanently deleted |
| No audit trail of who accessed what | Every action — cluster events, user creation, DR access — is written to an immutable audit log |
| Access is all-or-nothing, with no time scoping | All issued accounts are temporary (5 min → 12 hr); a background scheduler auto-deletes them on expiry |
| Revoking access requires shared-account coordination | Cluster managers can delete any active user instantly from the dashboard |
| Disaster-recovery credentials are stored ad-hoc | A dedicated `ambari_admin_dr` account is created at registration, its password Fernet-encrypted in the database, and exposed only via a controlled CLI tool that automatically locks the cluster |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                          Browser                             │
│                Vue 3 SPA  —  port 8080                       │
│      Google SSO  →  JWT  →  Axios  →  Dev Proxy             │
└─────────────────────────┬────────────────────────────────────┘
                          │ HTTP  (port 5000)
┌─────────────────────────▼────────────────────────────────────┐
│                  Flask Backend  (app.py)                      │
│                                                              │
│   ┌──────────────┐   ┌───────────────┐   ┌───────────────┐  │
│   │ Google OAuth │   │ Fernet Encrypt│   │ APScheduler   │  │
│   │  + JWT Auth  │   │  (passwords)  │   │ (auto-expiry) │  │
│   └──────────────┘   └───────────────┘   └───────────────┘  │
│                                                              │
└─────────────────────────┬────────────────────────────────────┘
                          │ mysql-connector-python
┌─────────────────────────▼────────────────────────────────────┐
│                     MySQL Database                            │
│   ambari_onboarding  │  ambari_manager_users  │  audit log   │
└─────────────────────────┬────────────────────────────────────┘
                          │ Ambari REST API
┌─────────────────────────▼────────────────────────────────────┐
│               Apache Ambari Clusters                          │
│    ambari-server-1    ambari-server-2    ambari-server-N …    │
└──────────────────────────────────────────────────────────────┘
```

| Layer | Technology |
|---|---|
| Frontend | Vue 3, Vuetify 3, Pinia, Vue Router 4, Axios |
| Backend | Python 3.9+, Flask, flask-jwt-extended, APScheduler |
| Database | MySQL 8.0+ |
| Auth | Google OAuth 2.0 → JWT |
| Encryption | Fernet symmetric encryption (`cryptography` library) |
| Password hashing | bcrypt |

---

## Key Features

### Cluster Hardening at Registration
When a cluster is registered, Kavach performs a one-time hardening sequence via the Ambari REST API:
- Creates a **`vault`** service account (`CLUSTER.ADMINISTRATOR`) — its password Fernet-encrypted in the DB
- Creates **`ambari_admin_dr`** — a disaster-recovery admin account, encrypted separately
- Creates **`sre_ro`** and **`dev_ro`** — read-only cluster user accounts for ops teams
- Deletes all other pre-existing user accounts
- Deletes the `admin` account last, using the freshly created vault credentials

After registration, no human ever uses a long-lived password to log into Ambari directly.

### Temporary User Issuance
- Any authenticated user can self-serve a time-limited Ambari account
- Duration choices: **5 min, 15 min, 30 min, 1 hr, 2 hr, 4 hr, 8 hr, 12 hr**
- Role choices: `CLUSTER.ADMINISTRATOR`, `CLUSTER.OPERATOR`, `CLUSTER.USER` (read-only)
- Credentials are displayed exactly **once** at creation — never stored in plaintext anywhere
- APScheduler polls every 60 seconds and auto-deletes expired accounts from Ambari

### Single-User Mode
Each cluster can be toggled into **single-user mode** — only one active temporary user is permitted at a time. Any request to create a second user while one is active returns a clear error with the active username and its expiry time.

### Fully Audited Access
Every significant action writes to the `ambari_vault_major_audit` table:

| Audit Event | Trigger |
|---|---|
| `CLUSTER_REGISTERED` | New cluster registered |
| `MANAGER_REREGISTRATION_DONE` | Credentials rotated by a manager |
| `AMBARI_DR_COMPROMISED` | Emergency DR password revealed |
| `USER_CREATED` | Temporary user issued |
| `USER_DELETED` | User force-deleted by a manager |

### Cluster Manager Delegation
- Each cluster has its own manager list, set during registration
- Super admins can update manager lists at any time from the Admin Panel
- Managers have authority **only** over their own clusters

### Emergency DR Recovery
`building_on_fire.py` is a standalone CLI tool for when the Kavach server itself is unreachable. It connects directly to MySQL, decrypts and prints the `ambari_admin_dr` password, flags the cluster as `DR_COMPROMISED` (immediately blocking all Kavach user creation), and writes an audit record. See [Emergency DR Recovery](#emergency-dr-recovery).

---

## Security Design

Kavach was designed with the following principles:

### Credentials never leave the server in plaintext
All Ambari service-account passwords are encrypted at rest using **Fernet symmetric encryption** before being written to MySQL. The encryption key is never stored in the database or the application config file — it must be supplied as an environment variable (`KAVACH_ENCRYPTION_KEY`). The server refuses to start if it is absent.

### No long-lived human passwords
Every Ambari account issued to a human is temporary. Passwords are generated randomly (21 characters, mixed alphanumeric), shown once to the requester, and never retrievable afterwards. Only a bcrypt hash is stored in the database.

### Identity verified on every request
The backend validates the `X-Email` request header against the authenticated JWT identity on every protected endpoint. A user cannot impersonate a different email by manipulating headers — the JWT is the source of truth.

### Role-based access enforced server-side
All role checks (super admin, cluster manager, regular user) are enforced in the Flask backend on every API call, independent of what the frontend shows. A non-manager cannot delete users or re-register clusters even if they craft a direct API request.

### DR access is destructive by design
The emergency DR tool (`building_on_fire.py`) automatically sets `dr_compromised = 1` on the cluster row the moment the password is revealed. This immediately blocks all further Kavach user creation on that cluster. Recovery requires a super admin to re-register the cluster, which rotates all credentials and clears the flag — creating a forced review checkpoint after every DR event.

### Domain-restricted Google SSO
Login is restricted to Google accounts from domains listed in `allowed_domains` (configured in the INI file). Accounts from unlisted domains are rejected at the OAuth callback stage regardless of Google authentication outcome.

### Secrets fail loud
Both `KAVACH_ENCRYPTION_KEY` and `JWT_SECRET_KEY` raise a `RuntimeError` at startup if not set as environment variables. There are no hardcoded fallback defaults — a misconfigured deployment fails immediately rather than silently.

---

## Role Hierarchy

```
┌──────────────────────────────────────────────────────┐
│  Super Admin                                         │
│  Configured via super_admin_list in kavach.ini       │
│                                                      │
│  ● Register new Ambari clusters                      │
│  ● Delete cluster registrations                      │
│  ● Update per-cluster manager email lists            │
│  ● All Cluster Manager capabilities                  │
└────────────────────┬─────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────┐
│  Cluster Manager                                     │
│  Assigned per-cluster during registration            │
│                                                      │
│  ● Re-register their cluster (rotates credentials)   │
│  ● Force-delete any active user on their cluster     │
│  ● Create temporary users                            │
└────────────────────┬─────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────┐
│  User                                                │
│  Any authenticated Google SSO user                   │
│                                                      │
│  ● Create temporary Ambari users                     │
│  ● View their own active and expired accounts        │
│  ● View analytics and audit logs                     │
└──────────────────────────────────────────────────────┘
```

---

## Project Structure

```
ambari-kavach/
│
├── ambari-kavach-backend/
│   ├── app.py                       # Flask application — all API endpoints
│   ├── building_on_fire.py          # Emergency DR recovery CLI tool
│   ├── schema.sql                   # MySQL DDL — run once to set up the database
│   ├── requirements.txt             # Python dependencies
│   ├── ambari_kavach.ini.example    # Configuration template (copy → ambari_kavach.ini)
│   └── README.md                    # Backend setup and internals guide
│
├── ambari-kavach-frontend/
│   ├── src/
│   │   ├── views/                   # Page-level Vue components (one per route)
│   │   ├── components/              # Reusable UI components
│   │   ├── stores/auth.js           # Pinia auth store (JWT, email, role flags)
│   │   ├── router/index.js          # Vue Router with authentication guard
│   │   ├── api/client.js            # Axios instance (JWT + X-Email interceptors)
│   │   └── plugins/vuetify.js       # Vuetify 3 theme configuration
│   ├── public/                      # Static assets (logo, favicon)
│   ├── vue.config.js                # Dev-server API proxy configuration
│   ├── package.json
│   └── README.md                    # Frontend setup guide
│
└── README.md                        # This file
```

---

## Prerequisites

| Requirement | Minimum version |
|---|---|
| Python | 3.9 |
| npm | 8 |
| MySQL | 8.0 |
| Optinal - Google Cloud project | OAuth 2.0 Client ID (Web application type) |

---

## Getting Started

### 1 — Database

```bash
mysql -u root -p < ambari-kavach-backend/schema.sql
```

Create a dedicated MySQL user for the application:

```sql
CREATE USER 'kavach'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON ambari_kavach.* TO 'kavach'@'localhost';
FLUSH PRIVILEGES;
```

---

### 2 — Backend

```bash
cd ambari-kavach-backend

python3 -m venv venv
source venv/bin/activate        # macOS / Linux
# venv\Scripts\activate         # Windows

pip install -r requirements.txt
mkdir -p ../logs
cp ambari_kavach.ini.example ambari_kavach.ini
# Edit ambari_kavach.ini — fill in DB credentials, super admin emails, allowed domains
```

Generate a Fernet encryption key (run once and store it safely):

```bash
python3 -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
```

Generate a JWT secret:

```bash
python3 -c "import secrets; print(secrets.token_hex(32))"
```

Export both as environment variables before starting:

```bash
export KAVACH_ENCRYPTION_KEY="<your-fernet-key>"
export JWT_SECRET_KEY="<your-jwt-secret>"

python3 app.py
```

Backend runs at `http://localhost:5000`. Verify: `curl http://localhost:5000/api/health`

---

### 3 — Google OAuth

1. Open [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**
2. **Create Credentials** → **OAuth 2.0 Client ID** → **Web application**
3. Add **Authorised JavaScript origins**: `http://localhost:8080` (plus your production URL)
4. Copy the **Client ID**
5. Create `ambari-kavach-frontend/.env.local`:
   ```
   VUE_APP_GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
   ```

---

### 4 — Frontend

```bash
cd ambari-kavach-frontend
npm install
npm run serve
```

Frontend runs at `http://localhost:8080`. API calls are automatically proxied to `http://localhost:5000`.

---

### 5 — First Login

1. Open `http://localhost:8080`
2. Click **Sign in with Google** using an account from one of your `allowed_domains`
3. You land on the **Dashboard**
4. Users whose email appears in `super_admin_list` in the INI file have Super Admin access immediately

---

## Configuration Reference

`ambari-kavach-backend/ambari_kavach.ini`:

```ini
[Kavachlog]
file_log_level         = INFO
log_format             = [%(asctime)s] - %(levelname)s - %(message)s
ambari_user_audit_file = ../logs/ambari_user_audit.log
kavach_server_log_file = ../logs/ambari_kavach.log

[KavachDB]
mysql_hostname      = 127.0.0.1
kavach_database     = ambari_kavach
kavach_db_user_name = kavach
kavach_db_password  = your_db_password_here

[sudo_powers]
super_admin_list = ["admin@yourcompany.com"]

[auth]
allowed_domains = yourcompany.com,gmail.com
```

| Key | Description |
|---|---|
| `file_log_level` | Python logging level — `DEBUG`, `INFO`, `WARNING`, or `ERROR` |
| `mysql_hostname` | MySQL host (IP or hostname) |
| `kavach_database` | Database name (`ambari_kavach`) |
| `super_admin_list` | JSON array of super admin emails — these users have global admin powers |
| `allowed_domains` | Comma-separated Google OAuth domains allowed to log in |

**Required environment variables** — the server will not start without these:

| Variable | How to generate |
|---|---|
| `KAVACH_ENCRYPTION_KEY` | `python3 -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"` |
| `JWT_SECRET_KEY` | `python3 -c "import secrets; print(secrets.token_hex(32))"` |

---

## Dashboard Walkthrough

### Login
Google SSO sign-in restricted to `allowed_domains`. A JWT is issued on success and stored in the browser.

### Dashboard
Summary cards: registered cluster count, currently active temp users, clusters in DR-compromised state. Recent audit event feed.

### Create User
Choose a cluster, role, and expiry duration. On success, the generated username and one-time password are displayed. **Copy them immediately — they cannot be retrieved again.**

| Role | Ambari access level |
|---|---|
| `CLUSTER.ADMINISTRATOR` | Full administrative access |
| `CLUSTER.OPERATOR` | Can execute operations, cannot change configurations |
| `CLUSTER.USER` | Read-only — view dashboards and service state |

### My Users
Table of all temp accounts created by the logged-in user — active, expired, and deleted — with expiry countdowns.

### Clusters
All registered clusters with HTTP method, port, DR status, single-user mode flag, and manager list.

### Cluster Registration *(Super Admin)*
Register a new Ambari cluster. Requires the cluster to have `admin/admin` credentials active. Kavach performs the full hardening sequence and deletes `admin` as the final step.

### Cluster Manager Panel *(Manager)*
View all active users on a managed cluster. Delete users immediately. Re-register the cluster to rotate all credentials.

### Analytics
Per-cluster charts: users created over time, expiry distribution, usage breakdown by role.

### Audit Logs
Paginated, filterable view of every major system event with actor, event type, affected entity, and timestamp.

### Admin Panel *(Super Admin)*
View and edit per-cluster manager lists. Delete cluster registrations.

### Profile
Current user's email, role, and JWT expiry.

---

## API Reference

All endpoints require `Authorization: Bearer <jwt>` unless marked **Public**.

### Auth & Health

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/google-login` | Public | Exchange Google ID token for a Kavach JWT |
| GET | `/api/health` | Public | Health check — `{"status":"ok"}` |
| GET | `/api/me` | JWT | Current user profile, manager status, and admin flag |

### Clusters

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/servers` | JWT | List all registered clusters |
| POST | `/api/register` | Super Admin | Register and harden a new cluster |
| POST | `/api/re-register` | Manager | Rotate credentials, reset DR flag |
| POST | `/api/test_connection` | JWT | Test connectivity to an Ambari server |
| DELETE | `/api/clusters/<server>` | Super Admin | Delete a cluster registration |
| PUT | `/api/clusters/<server>/managers` | Super Admin | Update cluster manager list |

### Temporary Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/create_user` | JWT | Issue a temporary Ambari user |
| GET | `/api/active_users` | JWT | Caller's active (non-expired) users |
| GET | `/api/expired_users` | JWT | Caller's expired and deleted users |
| GET | `/api/cluster_users` | Manager | All users on a managed cluster |
| POST | `/manager/delete_user` | Manager | Force-delete a user from Ambari |

### Analytics & Audit

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/analytics/cluster_overview` | JWT | Per-cluster activity metrics |
| GET | `/api/audit_logs` | JWT | Paginated major event audit log |
| GET | `/api/ambari/clusters` | JWT | Ambari cluster names for a server |

---

## Emergency DR Recovery

> Use this tool **only** when the Kavach server is unreachable and direct Ambari access is urgently needed.

```bash
cd ambari-kavach-backend
source venv/bin/activate

python3 building_on_fire.py \
    --db-host  localhost \
    --db-name  ambari_kavach \
    --db-user  kavach \
    --db-password  <db_password> \
    --encryption-key  <fernet_key> \
    --cluster  <ambari_hostname> \
    --operator <your_email>
```

**What happens:**
1. Connects directly to MySQL — no running Kavach server required
2. Decrypts and prints the `ambari_admin_dr` password
3. Sets `dr_compromised = 1` — **immediately blocks all Kavach user creation on this cluster**
4. Writes an `AMBARI_DR_COMPROMISED` audit record

**After the incident:**
1. Log into Ambari as `ambari_admin_dr` and fix the issue
2. Log into Kavach as Super Admin → **Admin Panel → Re-register Cluster**
3. Re-registration rotates all credentials and clears the DR flag

---

## Production Deployment

### Backend — Gunicorn + systemd

```bash
pip install gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

Example systemd unit:

```ini
[Unit]
Description=Ambari Kavach Backend
After=network.target mysql.service

[Service]
User=kavach
WorkingDirectory=/opt/ambari-kavach/ambari-kavach-backend
Environment=KAVACH_ENCRYPTION_KEY=<your-fernet-key>
Environment=JWT_SECRET_KEY=<your-jwt-secret>
ExecStart=/opt/ambari-kavach/ambari-kavach-backend/venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

### Frontend — Build + Nginx

```bash
cd ambari-kavach-frontend
echo "VUE_APP_GOOGLE_CLIENT_ID=<client-id>.apps.googleusercontent.com" > .env.production
echo "VUE_APP_API_BASE=https://kavach.yourcompany.com"                 >> .env.production
npm run build
```

Example Nginx config:

```nginx
server {
    listen 80;
    server_name kavach.yourcompany.com;

    root /opt/ambari-kavach/ambari-kavach-frontend/dist;
    index index.html;

    location / { try_files $uri $uri/ /index.html; }

    location /api/     { proxy_pass http://127.0.0.1:5000; proxy_set_header Host $host; }
    location /auth/    { proxy_pass http://127.0.0.1:5000; }
    location /create_user { proxy_pass http://127.0.0.1:5000; }
    location /manager/ { proxy_pass http://127.0.0.1:5000; }
}
```

---
