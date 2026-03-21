# Ambari Kavach — Backend

Flask-based REST API server that manages Ambari cluster registrations, temporary user lifecycle, authentication, and the audit trail.

---

## Directory Layout

```
ambari-kavach-backend/
├── app.py                     # Main Flask application
├── building_on_fire.py        # Emergency DR recovery CLI tool
├── schema.sql                 # MySQL DDL — initialise the database
├── requirements.txt           # Python dependencies
├── ambari_kavach.ini.example  # Configuration template
└── README.md                  # This file
```

Logs (gitignored, created at runtime):
```
logs/
├── ambari_kavach.log          # Server + error log
└── ambari_user_audit.log      # Per-user action log
```

---

## Setup

### 1. Python virtual environment

```bash
python3 -m venv venv
source venv/bin/activate      # macOS / Linux
pip install -r requirements.txt
```

### 2. Database

```bash
mysql -u root -p < schema.sql
```

Create a dedicated DB user:

```sql
CREATE USER 'kavach'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON ambari_kavach.* TO 'kavach'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Generate encryption key

```bash
python3 -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
```

Store this key securely — it encrypts all Ambari passwords at rest. Losing it means losing access to all registered clusters.

### 4. Configuration

```bash
cp ambari_kavach.ini.example ambari_kavach.ini
# Edit ambari_kavach.ini — see Configuration section below
```

### 5. Set required environment variables

Both variables are **required**. The server will refuse to start if either is missing.

```bash
export KAVACH_ENCRYPTION_KEY="<your-fernet-key>"
export JWT_SECRET_KEY="<random-secret-string>"
```

### 6. Start the server

```bash
mkdir -p logs
python3 app.py
```

Backend runs at `http://localhost:5000`.

---

## Configuration (`ambari_kavach.ini`)

| Section | Key | Description |
|---|---|---|
| `[Kavachlog]` | `file_log_level` | `DEBUG` / `INFO` / `WARNING` / `ERROR` |
| `[Kavachlog]` | `ambari_user_audit_file` | Path for the user-action audit log |
| `[Kavachlog]` | `kavach_server_log_file` | Path for the server log |
| `[KavachDB]` | `mysql_hostname` | MySQL host |
| `[KavachDB]` | `kavach_database` | Database name (default: `ambari_kavach`) |
| `[KavachDB]` | `kavach_db_user_name` | MySQL username |
| `[KavachDB]` | `kavach_db_password` | MySQL password |
| `[sudo_powers]` | `super_admin_list` | JSON array of super admin emails |
| `[auth]` | `allowed_domains` | Comma-separated Google OAuth domains |

---

## Database Schema

Three tables — defined in `schema.sql`:

### `ambari_onboarding`
One row per registered Ambari cluster.

| Column | Type | Description |
|---|---|---|
| `ambari_server` | VARCHAR(255) PK | Ambari server hostname |
| `http_method` | VARCHAR(10) | `http` or `https` |
| `port` | INT | Ambari port (default 8888) |
| `vault_password` | TEXT | Fernet-encrypted vault service account password |
| `admin_dr_password` | TEXT | Fernet-encrypted DR admin password |
| `manager_emails` | TEXT | JSON array of per-cluster manager emails |
| `single_user_mode` | TINYINT(1) | 1 = only one active temp user at a time |
| `dr_compromised` | TINYINT(1) | 1 = DR password exposed; blocks all user creation |

### `ambari_manager_users`
Every temporary Ambari user ever issued through Kavach.

| Column | Type | Description |
|---|---|---|
| `id` | INT PK | Auto-increment |
| `ambari_server` | VARCHAR(255) | Cluster the user belongs to |
| `user_name` | VARCHAR(255) | Ambari username |
| `email` | VARCHAR(255) | Kavach user who requested this account |
| `hash_password` | TEXT | bcrypt hash of the temp password |
| `expire_time` | DATETIME | When the account is auto-deleted |
| `pass_flag` | TINYINT | 1 = active, 0 = expired/deleted |
| `created_at` | DATETIME | Creation timestamp |
| `deleted_at` | DATETIME | Deletion timestamp (NULL if still active) |
| `role` | VARCHAR(64) NOT NULL | Ambari role: `CLUSTER.ADMINISTRATOR`, `CLUSTER.OPERATOR`, or `CLUSTER.USER` |

### `ambari_vault_major_audit`
Immutable event log — rows are never updated or deleted.

| Column | Type | Description |
|---|---|---|
| `id` | INT PK | Auto-increment |
| `actor_email` | VARCHAR(255) | Who triggered the event |
| `audit_event` | VARCHAR(255) | Event type (see below) |
| `impact_entity` | VARCHAR(255) | Cluster hostname or username affected |
| `event_time` | TIMESTAMP | Auto-set on insert |

**Audit event types:**

| Event | Trigger |
|---|---|
| `CLUSTER_REGISTERED` | New cluster registered via `/api/register` |
| `MANAGER_REREGISTRATION_DONE` | Manager re-registered a cluster via `/api/re-register` |
| `AMBARI_DR_COMPROMISED` | DR password revealed via `building_on_fire.py` |
| `USER_CREATED` | Temporary user created via `/create_user` |
| `USER_DELETED` | Manager force-deleted a user via `/manager/delete_user` |

---

## Application Flow

### Cluster Registration (`/api/register`)

Only super admins can register clusters. Registration hardwires the cluster into the Kavach trust model by rotating all credentials and removing the default admin account.

```
Request (super admin)
  │
  ├─ Validate admin/admin credentials on Ambari server
  ├─ Create vault user (CLUSTER.ADMINISTRATOR) — long-lived service account
  ├─ Create ambari_admin_dr user (CLUSTER.ADMINISTRATOR) — break-glass only
  ├─ Create sre_ro user (CLUSTER.USER) — read-only for SRE
  ├─ Create dev_ro user (CLUSTER.USER) — read-only for developers
  ├─ Delete all other pre-existing users
  ├─ Delete admin account (using vault credentials)
  ├─ Encrypt vault + DR passwords with Fernet
  ├─ INSERT into ambari_onboarding
  └─ Write CLUSTER_REGISTERED audit event
```

After registration the cluster has no default admin. All access goes through Kavach.

### Temporary User Creation (`/create_user`)

```
Request (authenticated user)
  │
  ├─ Validate JWT + verify X-Email header matches JWT identity
  ├─ Check email domain against allowed_domains allowlist
  ├─ Check dr_compromised flag → 403 if set (cluster locked down)
  ├─ Check single_user_mode → 409 if another active user exists
  ├─ Generate username (user_<epoch>) + random 12-char password
  ├─ Create user on Ambari via REST API (vault credentials used)
  ├─ Assign requested role (CLUSTER.ADMINISTRATOR / OPERATOR / USER)
  ├─ bcrypt-hash password, INSERT into ambari_manager_users
  └─ Return plaintext credentials to caller (shown once only)
```

### Background Expiry Scheduler

APScheduler runs every 60 seconds to enforce time-bounded access:

```
For each row WHERE pass_flag=1 AND expire_time < NOW():
  ├─ Delete user from Ambari via REST API
  ├─ UPDATE pass_flag=0, deleted_at=NOW()
  └─ Log deletion
```

Users are removed automatically — no manual cleanup required.

### Re-registration (`/api/re-register`)

Used to rotate credentials on a cluster or recover from a DR compromise event. Clears the `dr_compromised` flag.

```
Request (cluster manager)
  │
  ├─ Validate manager has authority over this cluster
  ├─ Verify admin/admin is active on the cluster
  ├─ Rotate vault + DR passwords (delete existing accounts, create new)
  ├─ Recreate sre_ro and dev_ro users
  ├─ Delete all other stale users
  ├─ Delete admin account (via new vault credentials)
  ├─ UPDATE ambari_onboarding (new encrypted passwords, dr_compromised=0)
  └─ Write MANAGER_REREGISTRATION_DONE audit event
```

---

## Security Design

### Secrets never touch disk in plaintext
All Ambari passwords stored in the database are Fernet-encrypted. The encryption key lives exclusively in the `KAVACH_ENCRYPTION_KEY` environment variable. The server refuses to start if this variable is absent — there is no insecure fallback.

### Credentials shown exactly once
When a temporary user is created, the plaintext password is returned in the API response and never stored. Only a bcrypt hash is persisted. There is no mechanism to retrieve a password after the creation response is consumed.

### JWT identity binding
Every protected endpoint validates that the `X-Email` header matches the identity embedded in the JWT. A valid token cannot be used to act as a different user.

### Role enforcement is server-side
Valid roles (`CLUSTER.ADMINISTRATOR`, `CLUSTER.OPERATOR`, `CLUSTER.USER`) are enforced in `app.py`. Any request with an unrecognised role is rejected with HTTP 400 before touching Ambari.

### DR lockdown
When `building_on_fire.py` reveals the DR password, it immediately sets `dr_compromised=1` on that cluster. All subsequent user-creation requests for that cluster return HTTP 403 until a manager re-registers the cluster and rotates credentials. The lock cannot be cleared without a full credential rotation.

### Domain-restricted SSO
Only email addresses from domains listed in `allowed_domains` can authenticate. Google OAuth is the only supported identity provider — there are no local passwords.

---

## API Endpoints

See the [root README API Reference](../README.md#api-reference) for the full endpoint table.

---

## `building_on_fire.py` — Emergency DR Tool

Use when the Kavach server is **down** and direct Ambari admin access is required immediately.

```bash
python3 building_on_fire.py \
    --db-host localhost \
    --db-name ambari_kavach \
    --db-user kavach \
    --db-password <db_password> \
    --encryption-key <fernet_key> \
    --cluster <ambari_hostname> \
    --operator <your_email>
```

The tool connects directly to MySQL (no running Kavach server needed), decrypts and prints the `ambari_admin_dr` password, sets `dr_compromised=1` on the cluster, and writes an `AMBARI_DR_COMPROMISED` audit record.

**After the incident:** re-register the cluster from the Admin Panel to rotate all credentials and lift the lockdown.
