#!/usr/bin/env python3
"""
╔══════════════════════════════════════════════════════════════════════════════╗
║            AMBARI KAVACH — EMERGENCY DISASTER-RECOVERY TOOL                 ║
║                      building_on_fire.py                                    ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  Use this script when Kavach server is DOWN and you need the Ambari         ║
║  Admin DR password to manually recover a cluster.                           ║
║                                                                             ║
║  What this script does:                                                     ║
║    1. Decrypts and displays the ambari_admin_dr password for a cluster      ║
║    2. Flags the cluster as DR_COMPROMISED in the Kavach database            ║
║    3. Writes an AMBARI_DR_COMPROMISED audit event                           ║
║    4. Blocks ALL new temporary user creation on that cluster via Kavach     ║
║       until a super admin re-registers the cluster                          ║
║                                                                             ║
║  Usage:                                                                     ║
║    python3 building_on_fire.py                                              ║
║        --db-host localhost --db-name ambari_kavach                          ║
║        --db-user kavach_user --db-password secret                           ║
║        --encryption-key <fernet-key>                                        ║
║        [--cluster <hostname>] [--operator <email>]                          ║
╚══════════════════════════════════════════════════════════════════════════════╝
"""

import argparse
import getpass
import json
import os
import sys
from datetime import datetime

# ── third-party deps (same as app.py) ─────────────────────────────────────────
try:
    import mysql.connector
    from mysql.connector import Error
except ImportError:
    print("[FATAL] mysql-connector-python not installed.")
    print("        Run: pip install mysql-connector-python")
    sys.exit(1)

try:
    from cryptography.fernet import Fernet, InvalidToken
except ImportError:
    print("[FATAL] cryptography not installed.")
    print("        Run: pip install cryptography")
    sys.exit(1)

# ── ANSI colours ──────────────────────────────────────────────────────────────
RED    = "\033[91m"
YELLOW = "\033[93m"
GREEN  = "\033[92m"
CYAN   = "\033[96m"
BOLD   = "\033[1m"
RESET  = "\033[0m"

def banner():
    print(f"""
{RED}{BOLD}
  ╔══════════════════════════════════════════════════════════╗
  ║        🔥  BUILDING ON FIRE — KAVACH EMERGENCY  🔥       ║
  ╚══════════════════════════════════════════════════════════╝
{RESET}""")

def warn(msg):  print(f"{YELLOW}[WARN]  {msg}{RESET}")
def info(msg):  print(f"{CYAN}[INFO]  {msg}{RESET}")
def ok(msg):    print(f"{GREEN}[OK]    {msg}{RESET}")
def error(msg): print(f"{RED}[ERROR] {msg}{RESET}")

# ── Config resolution ─────────────────────────────────────────────────────────

def resolve_db_config(args) -> dict:
    base = {
        "host":     args.db_host,
        "database": args.db_name,
        "user":     args.db_user,
        "password": args.db_password,
    }

    missing = [k for k in ("host", "database", "user", "password") if not base.get(k)]
    if missing:
        print(f"\n{YELLOW}Some database credentials are missing. Please enter them:{RESET}")
        if "host" in missing:
            base["host"] = input("  MySQL host     : ").strip() or "localhost"
        if "database" in missing:
            base["database"] = input("  Database name  : ").strip() or "ambari_kavach"
        if "user" in missing:
            base["user"] = input("  DB username    : ").strip()
        if "password" in missing:
            base["password"] = getpass.getpass("  DB password    : ")

    return base


def resolve_cipher(args) -> "Fernet":
    key = args.encryption_key or os.environ.get("KAVACH_ENCRYPTION_KEY")
    if not key:
        error("No encryption key provided. Use --encryption-key or set KAVACH_ENCRYPTION_KEY env var.")
        sys.exit(1)
    return Fernet(key.encode() if isinstance(key, str) else key)

# ── DB helpers ────────────────────────────────────────────────────────────────

def connect(db_cfg: dict):
    try:
        conn = mysql.connector.connect(**db_cfg)
        return conn
    except Error as e:
        error(f"Cannot connect to MySQL: {e}")
        sys.exit(1)


def fetch_all_clusters(conn) -> list[dict]:
    """Return list of cluster rows, tolerating missing new columns."""
    cursor = conn.cursor()
    try:
        cursor.execute(
            "SELECT ambari_server, http_method, port, admin_dr_password, "
            "manager_emails, single_user_mode, dr_compromised "
            "FROM ambari_onboarding"
        )
        rows = cursor.fetchall()
        return [
            {
                "host":            r[0],
                "http_method":     r[1] or "http",
                "port":            r[2] or 8888,
                "admin_dr_enc":    r[3],
                "manager_emails":  _safe_json(r[4]),
                "single_user_mode": bool(r[5]) if r[5] is not None else False,
                "dr_compromised":  bool(r[6]) if r[6] is not None else False,
            }
            for r in rows
        ]
    except Error:
        # Fallback: columns may not exist yet
        cursor.execute(
            "SELECT ambari_server, http_method, port, admin_dr_password "
            "FROM ambari_onboarding"
        )
        rows = cursor.fetchall()
        return [
            {
                "host":            r[0],
                "http_method":     r[1] or "http",
                "port":            r[2] or 8888,
                "admin_dr_enc":    r[3],
                "manager_emails":  [],
                "single_user_mode": False,
                "dr_compromised":  False,
            }
            for r in rows
        ]
    finally:
        cursor.close()


def _safe_json(raw) -> list:
    if not raw:
        return []
    try:
        return json.loads(raw)
    except Exception:
        return []


def ensure_dr_column(conn):
    """Auto-add dr_compromised column if not present — idempotent."""
    cursor = conn.cursor()
    try:
        cursor.execute(
            "ALTER TABLE ambari_onboarding "
            "ADD COLUMN dr_compromised TINYINT(1) DEFAULT 0"
        )
        conn.commit()
        info("Auto-migrated: added dr_compromised column to ambari_onboarding.")
    except Error as e:
        if "Duplicate column" in str(e):
            pass  # already exists — fine
        else:
            warn(f"Could not add dr_compromised column: {e}")
    finally:
        cursor.close()


def mark_dr_compromised(conn, host: str):
    ensure_dr_column(conn)
    cursor = conn.cursor()
    try:
        cursor.execute(
            "UPDATE ambari_onboarding SET dr_compromised = 1 WHERE ambari_server = %s",
            (host,)
        )
        conn.commit()
        ok(f"Cluster '{host}' flagged as DR_COMPROMISED in database.")
    except Error as e:
        error(f"Failed to set dr_compromised flag: {e}")
    finally:
        cursor.close()


def write_audit_event(conn, host: str, operator: str):
    """Write AMBARI_DR_COMPROMISED event to audit log."""
    cursor = conn.cursor()
    try:
        cursor.execute(
            "INSERT INTO ambari_vault_major_audit "
            "(actor_email, audit_event, impact_entity) VALUES (%s, %s, %s)",
            (operator, "AMBARI_DR_COMPROMISED", host)
        )
        conn.commit()
        ok(f"Audit event AMBARI_DR_COMPROMISED written for '{host}'.")
    except Error as e2:
        warn(f"Could not write audit event: {e2}")
    finally:
        cursor.close()


# ── Core flow ─────────────────────────────────────────────────────────────────

def select_cluster(clusters: list[dict], arg_host: str | None) -> dict:
    if arg_host:
        match = next((c for c in clusters if c["host"] == arg_host), None)
        if not match:
            error(f"Cluster '{arg_host}' not found in Kavach database.")
            print("  Available clusters:")
            for c in clusters:
                print(f"    • {c['host']}")
            sys.exit(1)
        return match

    print(f"\n{BOLD}Registered clusters:{RESET}")
    for i, c in enumerate(clusters, 1):
        status = f"{RED}DR COMPROMISED{RESET}" if c["dr_compromised"] else f"{GREEN}OK{RESET}"
        print(f"  {BOLD}{i}.{RESET} {c['host']}  [{c['http_method']}:{c['port']}]  {status}")

    while True:
        try:
            choice = input(f"\nSelect cluster number (1-{len(clusters)}): ").strip()
            idx = int(choice) - 1
            if 0 <= idx < len(clusters):
                return clusters[idx]
        except (ValueError, KeyboardInterrupt):
            pass
        print("  Invalid choice. Try again.")


def confirm(prompt: str) -> bool:
    while True:
        ans = input(f"\n{YELLOW}{BOLD}{prompt}{RESET} [yes/no]: ").strip().lower()
        if ans in ("yes", "y"):
            return True
        if ans in ("no", "n"):
            return False


def reveal_password(cluster: dict, cipher: Fernet) -> str:
    try:
        plain = cipher.decrypt(cluster["admin_dr_enc"].encode()).decode()
        return plain
    except InvalidToken:
        error("Decryption failed — encryption key does not match the one used during cluster registration.")
        sys.exit(1)
    except Exception as e:
        error(f"Unexpected decryption error: {e}")
        sys.exit(1)


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Kavach emergency DR password recovery tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Example:
  python3 building_on_fire.py --db-host localhost --db-name ambari_kavach --db-user kavach_user --db-password secret --encryption-key <fernet-key> --cluster prod-ambari.internal.example.com --operator oncall@example.com
        """
    )

    parser.add_argument("--cluster",        metavar="HOSTNAME",  help="Target Ambari server hostname (skip interactive selection)")
    parser.add_argument("--operator",       default="emergency-script", help="Your email / identifier for the audit log")
    parser.add_argument("--db-host",        metavar="HOST",      help="MySQL hostname or IP")
    parser.add_argument("--db-name",        metavar="DATABASE",  help="Kavach database name")
    parser.add_argument("--db-user",        metavar="USER",      help="MySQL username")
    parser.add_argument("--db-password",    metavar="PASSWORD",  help="MySQL password")
    parser.add_argument("--encryption-key", metavar="KEY",       help="Fernet encryption key (or set KAVACH_ENCRYPTION_KEY env var)")

    args = parser.parse_args()

    banner()

    # ── Resolve config & connect ───────────────────────────────────────────────
    db_cfg = resolve_db_config(args)
    cipher = resolve_cipher(args)

    info(f"Connecting to MySQL at {db_cfg['host']} / {db_cfg['database']} …")
    conn = connect(db_cfg)
    ok("Connected.")

    # ── Fetch clusters ─────────────────────────────────────────────────────────
    clusters = fetch_all_clusters(conn)
    if not clusters:
        error("No clusters registered in Kavach database.")
        conn.close()
        sys.exit(1)

    # ── Select target cluster ──────────────────────────────────────────────────
    cluster = select_cluster(clusters, args.cluster)

    print(f"""
{BOLD}Target cluster:{RESET}
  Host       : {cluster['host']}
  Connection : {cluster['http_method']}://{cluster['host']}:{cluster['port']}
  Managers   : {', '.join(cluster['manager_emails']) or 'None assigned'}
  DR status  : {'⚠️  ALREADY COMPROMISED' if cluster['dr_compromised'] else '✅  Not yet flagged'}
""")

    if cluster["dr_compromised"]:
        warn("This cluster is already flagged as DR_COMPROMISED.")
        warn("New Kavach user creation is already blocked on this cluster.")
        if not confirm("Do you still want to reveal the DR password?"):
            print("Aborted.")
            conn.close()
            sys.exit(0)
        dr_password = reveal_password(cluster, cipher)
        print(f"\n{RED}{BOLD}  ambari_admin_dr password:{RESET}  {BOLD}{dr_password}{RESET}\n")
        conn.close()
        return

    # ── Safety confirmation ────────────────────────────────────────────────────
    print(f"""{YELLOW}{BOLD}
  ⚠️  WARNING — This operation will:
     1. Reveal the ambari_admin_dr password for '{cluster['host']}'
     2. Flag this cluster as DR_COMPROMISED in Kavach
     3. BLOCK all new temporary user creation via Kavach on this cluster
     4. Write an AMBARI_DR_COMPROMISED audit event
     5. The cluster CANNOT be used via Kavach until a super admin re-registers it
{RESET}""")

    if not confirm(f"Proceed with DR recovery for '{cluster['host']}'?"):
        print("Aborted. No changes made.")
        conn.close()
        sys.exit(0)

    # ── Decrypt & display DR password ─────────────────────────────────────────
    dr_password = reveal_password(cluster, cipher)

    print(f"""
{GREEN}{BOLD}╔═══════════════════════════════════════════════════╗
║        AMBARI ADMIN DR PASSWORD                   ║
╚═══════════════════════════════════════════════════╝{RESET}

  Cluster   : {BOLD}{cluster['host']}{RESET}
  Username  : {BOLD}ambari_admin_dr{RESET}
  Password  : {RED}{BOLD}{dr_password}{RESET}
  Retrieved : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

{YELLOW}  ⚠  Store this password securely. Do NOT share it unnecessarily.{RESET}
""")

    # ── Flag cluster & write audit ─────────────────────────────────────────────
    mark_dr_compromised(conn, cluster["host"])
    write_audit_event(conn, cluster["host"], args.operator)

    # ── Next steps ─────────────────────────────────────────────────────────────
    print(f"""
{BOLD}Next steps:{RESET}
  1. Use the password above to log into Ambari as  ambari_admin_dr
  2. Fix or investigate the issue on the cluster
  3. Once stable, log into Kavach as a Super Admin
  4. Go to  Admin Panel → Configure → Delete Cluster  then  Register Cluster
  5. After re-registration, Kavach user creation will resume automatically

{RED}{BOLD}  Until the cluster is re-registered, NO temporary users can be created
  via Kavach for '{cluster['host']}'.{RESET}
""")

    conn.close()


if __name__ == "__main__":
    main()
