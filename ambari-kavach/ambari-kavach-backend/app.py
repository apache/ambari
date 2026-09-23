import ast
import json
import logging
import random
import re
import urllib.parse
import os
import string
import bcrypt
import requests
from datetime import datetime, timedelta
from flask import Flask, jsonify, request, send_from_directory
import mysql.connector
from mysql.connector import Error
from apscheduler.schedulers.background import BackgroundScheduler
from flask_cors import CORS, cross_origin
from cryptography.fernet import Fernet

from configparser import ConfigParser, NoSectionError, NoOptionError
from flask_jwt_extended import create_access_token, JWTManager, verify_jwt_in_request, get_jwt_identity
import flask


app = Flask(__name__)
######## Starting reading configs ############
config_file="ambari_kavach.ini"
config = ConfigParser()
try:
    config.read("./ambari_kavach.ini")
except:
    print("Not to able to find the Ambari Kavach Config File")


######## Starting Log Modules ##############

class FileLogger(logging.Logger):
    def __init__(self, name, filename, mode='w', level=logging.INFO, fformatter=None, log_to_console=False, sformatter=None):
        super().__init__(name, level)

    # Create a custom file handler
        self.file_handler = logging.FileHandler(filename=filename, mode=mode)

    # Set the formatter for the file handler
        if fformatter is not None:
            self.file_handler.setFormatter(fformatter)

    # Add the file handler to the logger
        self.addHandler(self.file_handler)

        if log_to_console:
            # Create a console handler
            self.console_handler = logging.StreamHandler()  # Prints to the console

        # Set the formatter for the console handler
            if not sformatter:
                sformatter = fformatter
            self.console_handler.setFormatter(sformatter)

        # Add the console handler to the logger
            self.addHandler(self.console_handler)



    def fdebug(self, msg, pre_msg=''):
        if pre_msg:
            print(pre_msg)
        self.debug(msg)

    def finfo(self, msg):
        self.info(msg)

    def fwarn(self, msg):
        self.warning(msg)

    def ferror(self, msg):
        self.error(msg)

    def fcritical(self, msg):
        self.critical(msg)

log_level_str = config["Kavachlog"]["file_log_level"]
file_log_level = getattr(logging, log_level_str.upper(), None)
log_format = config["Kavachlog"]["log_format"]
log_fp = config["Kavachlog"]["ambari_user_audit_file"]
general_log_fp = config["Kavachlog"]["kavach_server_log_file"]

formatter = logging.Formatter(log_format, "%d-%m-%Y %H:%M:%S")
fLogger = FileLogger(__name__, log_fp, mode='a', level=file_log_level, fformatter=formatter)
fl = FileLogger(__name__, general_log_fp, mode='a', level=file_log_level, fformatter=formatter)

############# Connectiong to DB ##################
DB_CONFIG = {
    "host": config["KavachDB"]["mysql_hostname"],
    "database": config["KavachDB"]["kavach_database"],
    "user": config["KavachDB"]["kavach_db_user_name"],
    "password": config["KavachDB"]["kavach_db_password"]
}

def get_mysql_connection():
    global fLogger,fl
    """Establishes a MySQL database connection."""
    try:
        connection = mysql.connector.connect(**DB_CONFIG)
        return connection
    except Error as e:
        print("Error connecting to MySQL:", e)
        fl.ferror(f"Connection to mysql failed with this error: {e} - Please check weather MYSQL is running")
        return None

def run_mysql_commands(sql_check, params=None, fetch_method=None, commit=False):
    connection = get_mysql_connection()
    if not connection:
        fl.ferror(f" [ERROR] Unable to Connect to the Database")
        return False, "{Error: Unable to connect to the Database. Connection is not able to be established}"
    try:
        with connection.cursor() as cur:
            if commit:
                connection.start_transaction()
            if params is not None:
                cur.execute(sql_check, params)
            else:
                cur.execute(sql_check)
            if commit:
                connection.commit()
                return True, "Successfully committed the message"
            if fetch_method == "one":
                return True, cur.fetchone()
            elif fetch_method == "all":
                return True, cur.fetchall()
            else:
                return True, None
    except Exception as e:
        if commit:
            connection.rollback()
        fl.ferror(f" [ERROR] There is an error occurred while running the SQL query {str(e)}")
        return False, str(e)
    finally:
        connection.close()
    
##################### Safe_guard_passwords ###################

_raw_enc_key = os.environ.get("KAVACH_ENCRYPTION_KEY")
if not _raw_enc_key:
    raise RuntimeError(
        "KAVACH_ENCRYPTION_KEY environment variable is not set. "
        "Generate one with: python3 -c \"from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())\""
    )
ENCRYPTION_KEY = _raw_enc_key.encode() if isinstance(_raw_enc_key, str) else _raw_enc_key
cipher_suite = Fernet(ENCRYPTION_KEY)

def generate_password(length=21):
    """Generates a secure random password."""
    characters = string.ascii_letters + string.digits
    return ''.join(random.choices(characters, k=length))

def hash_password(password):
    """Hashes a password using bcrypt."""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')
def encrypt_password(password):
    """Encrypts the password using Fernet symmetric encryption."""
    return cipher_suite.encrypt(password.encode()).decode()

def decrypt_password(encrypted_password):
    """Decrypts the password using Fernet symmetric encryption."""
    return cipher_suite.decrypt(encrypted_password.encode()).decode()


# ============================================================
# Email validation and role-permission helpers
# ============================================================

_EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$')

def is_valid_email(email):
    """Returns True if email passes basic RFC-like format validation."""
    return bool(_EMAIL_REGEX.match(str(email).strip()))

def get_super_admins():
    """Return list of super-admin emails, re-reading the config file on every call.
    This ensures changes to ambari_kavach.ini take effect immediately without a server restart.
    Reads super_admin_list; falls back to manager_list for backward compat."""
    try:
        fresh = ConfigParser()
        fresh.read("./ambari_kavach.ini")
        key = "super_admin_list" if fresh.has_option("sudo_powers", "super_admin_list") else "manager_list"
        raw = ast.literal_eval(fresh["sudo_powers"][key])
        return [str(e).strip().lower() for e in raw if e]
    except (ValueError, SyntaxError, KeyError):
        return []

def get_cluster_managers(ambari_server):
    """Return per-cluster manager email list from DB for the given server."""
    ok, row = run_mysql_commands(
        "SELECT manager_emails FROM ambari_onboarding WHERE ambari_server = %s",
        (ambari_server,), "one", False
    )
    if not ok or not row or not row[0]:
        return []
    try:
        emails = json.loads(row[0])
        return [str(e).strip().lower() for e in emails if e]
    except (json.JSONDecodeError, TypeError):
        return []

def is_super_admin(email):
    """Returns True if the given email belongs to the global super-admin list."""
    return email.strip().lower() in get_super_admins()

def has_manager_access(email, ambari_server=None):
    """Returns True if email is a super-admin, or a cluster manager for the given server."""
    e = email.strip().lower()
    if e in get_super_admins():
        return True
    if ambari_server and e in get_cluster_managers(ambari_server):
        return True
    return False

def write_audit_log(actor, event, entity):
    """Insert an audit record into the major audit table."""
    run_mysql_commands(
        "INSERT INTO ambari_vault_major_audit (actor_email, audit_event, impact_entity) VALUES (%s, %s, %s)",
        (actor, event, entity), "", True
    )


################## Creating user in AMBARI FUNCTION ##########################
def create_ambari_user(ambari_server, user_name, password, ambari_username, ambari_password, admin=False, role='CLUSTER.ADMINISTRATOR', http_method='http', port=8888):
    global fLogger, fl
    headers = {"X-Requested-By": "ambari", "Content-Type": "application/json"}

    # Create user in Ambari
    url = f"{http_method}://{ambari_server}:{port}/api/v1/users"
    payload = {"Users": {"user_name": user_name, "password": password, "active": True, "admin": admin}}
    try:
        response = requests.post(url, json=payload, headers=headers, auth=(ambari_username, ambari_password), timeout=30)
    except requests.exceptions.RequestException as e:
        fl.ferror(f" [ERROR] Connection error creating user {user_name} on {ambari_server}: {e}")
        return False, f"Connection error to Ambari: {str(e)}"

    if response.status_code != 201:
        fl.ferror(f" [ERROR] Failed to create user {user_name} on {ambari_server}: HTTP {response.status_code} - {response.text}")
        return False, f"Failed to create user in Ambari (HTTP {response.status_code})"

    # Fetch cluster name to assign role
    cluster_url = f"{http_method}://{ambari_server}:{port}/api/v1/clusters"
    try:
        cluster_response = requests.get(cluster_url, headers=headers, auth=(ambari_username, ambari_password), timeout=30)
    except requests.exceptions.RequestException as e:
        fl.ferror(f" [ERROR] Connection error fetching cluster for {ambari_server}: {e}")
        return False, "Connection error fetching cluster name"

    if cluster_response.status_code != 200:
        fl.ferror(f" [ERROR] Unable to fetch cluster name from {ambari_server}")
        return False, "Failed to fetch cluster name"

    cluster_data = cluster_response.json()
    if "items" not in cluster_data or not cluster_data["items"]:
        fl.ferror(f" [ERROR] No cluster found on Ambari server {ambari_server}")
        return False, "No cluster found on this Ambari server"

    cluster_name = cluster_data["items"][0]["Clusters"]["cluster_name"]

    # Assign cluster role
    role_url = f"{http_method}://{ambari_server}:{port}/api/v1/clusters/{cluster_name}/privileges"
    role_payload = {
        "PrivilegeInfo": {
            "principal_name": user_name,
            "principal_type": "USER",
            "permission_name": role
        }
    }
    try:
        role_response = requests.post(role_url, json=role_payload, headers=headers, auth=(ambari_username, ambari_password), timeout=30)
    except requests.exceptions.RequestException as e:
        fl.ferror(f" [ERROR] Connection error assigning role to {user_name} on {ambari_server}: {e}")
        return False, "Connection error assigning role"

    if role_response.status_code not in [200, 201]:
        fl.ferror(f" [ERROR] Unable to assign role '{role}' to user {user_name} on {ambari_server}: HTTP {role_response.status_code}")
        return False, f"Failed to assign role '{role}' to user"

    return True, "User created successfully"

###################### CREATING TEMPORARY USERS FUNCTION IN PYTHON ##########################

@app.route('/create_user', methods=['POST'])
@cross_origin(supports_credentials=True)
def create_user():
    global fLogger,fl
    """Creates a new user in Ambari and assigns Cluster Administrator role."""
    try:
        data = request.get_json()
        ambari_server = data.get("ambari_server")
        request_time = int(data.get("request_time", 5))
        active = data.get("active", True)
        admin = data.get("admin", False)
        role = data.get("role")
        allowed_roles = ("CLUSTER.ADMINISTRATOR", "CLUSTER.OPERATOR", "CLUSTER.USER")
        if not role:
            return jsonify({"error": "role is required"}), 400
        if role not in allowed_roles:
            return jsonify({"error": f"Invalid role '{role}'. Must be one of: {', '.join(allowed_roles)}"}), 400
        email = request.headers.get("X-Email")
        if not email:
            fLogger.ferror(" [ERROR] Aborting as EMAIL is not provided in the Request")
            fl.ferror(" [ERROR] Email is not provided in the request.. Aborting ....")
            return jsonify({"error": "Missing email in request headers"}), 400

        # Generate username based on email
        user_name = "u_a_" + email.split("@")[0] +"-"+str(request_time)+ "_"+generate_password(4)

        if not ambari_server:
            fl.ferror(f" [ERROR] Ambari Server is not provided in the Request.. Aborting ... ")
            return jsonify({"error": "Missing Ambari server"}), 400

        password = generate_password(21)
        hashed_password = encrypt_password(password)

        # Fetch cluster settings (vault creds + access policy + DR status)
        sql_check = """
            SELECT vault_password, http_method, port, single_user_mode, dr_compromised FROM ambari_onboarding
            WHERE ambari_server = %s
        """
        query_status, server_creds = run_mysql_commands(sql_check, (ambari_server,), "one")
        if not query_status or not server_creds:
            # Fallback: column may not exist yet
            sql_check = """
                SELECT vault_password, http_method, port FROM ambari_onboarding
                WHERE ambari_server = %s
            """
            query_status, server_creds = run_mysql_commands(sql_check, (ambari_server,), "one")
            if not query_status or not server_creds:
                fl.ferror(f" [ERROR] Ambari server {ambari_server} not found in onboarding")
                return jsonify({"error": "Ambari server not registered"}), 404
            server_creds = (server_creds[0], server_creds[1], server_creds[2], 0, 0)  # default: multi-user, not compromised

        single_user_mode_flag = bool(server_creds[3]) if len(server_creds) > 3 else False
        dr_compromised_flag = bool(server_creds[4]) if len(server_creds) > 4 else False

        # Block all user creation if DR password has been exposed via building_on_fire.py
        if dr_compromised_flag:
            fl.ferror(f" [ERROR] Cluster '{ambari_server}' is DR_COMPROMISED — blocking user creation")
            return jsonify({"error": f"Cluster '{ambari_server}' has been flagged as DR_COMPROMISED. All Kavach user creation is blocked until a super admin re-registers the cluster."}), 403

        # Enforce single-user policy only when enabled for this cluster
        if single_user_mode_flag:
            sq = "SELECT user_name FROM ambari_manager_users WHERE ambari_server = %s AND pass_flag = 1 AND expire_time > NOW()"
            q_ok, existing_user = run_mysql_commands(sq, (ambari_server,), "one")
            if q_ok and existing_user:
                fl.ferror(f"An active user: {existing_user[0]} is already available in the requested cluster: {ambari_server}")
                return jsonify({"error": f"Cluster '{ambari_server}' is in single-user mode — only one active temporary user is allowed at a time. Active user: {existing_user[0]}. Wait for it to expire or ask a manager to delete it."}), 409
            if not q_ok:
                fl.fwarn(f" [WARN] Could not verify single-user mode for {ambari_server}: {existing_user} — proceeding with user creation")

        # Per-email, per-cluster, per-role guard (applies to ALL clusters, including multi-user mode).
        # A requester cannot create a second user for the same cluster+role while their first is still active.
        sq_active = """
            SELECT user_name, expire_time FROM ambari_manager_users
            WHERE ambari_server = %s AND email = %s AND role = %s
              AND pass_flag = 1 AND expire_time > NOW()
            LIMIT 1
        """
        q_ok2, active_own = run_mysql_commands(sq_active, (ambari_server, email, role), "one")
        if q_ok2 and active_own:
            fl.ferror(f" [ERROR] {email} already has active user {active_own[0]} for cluster {ambari_server} role {role}, expiring at {active_own[1]}")
            return jsonify({"error": f"You already have an active '{role}' user on cluster '{ambari_server}': '{active_own[0]}' (expires at {active_own[1]}). Wait for it to expire or ask a manager to delete it."}), 409

        vault_password_enc = server_creds[0]
        http_method = server_creds[1] or "http"
        port = server_creds[2] or 8888
        success, message = create_ambari_user(ambari_server, user_name, password, "vault", decrypt_password(vault_password_enc), False, role, http_method, port)
        if not success:
            return jsonify({"error": message}), 500

        else:
            # Insert user into MySQL, including email and role
            expire_time = datetime.now() + timedelta(minutes=request_time)
            sql_insert = """
                INSERT INTO ambari_manager_users (ambari_server, user_name, email, hash_password, expire_time, pass_flag, created_at, role)
                VALUES (%s, %s, %s, %s, %s, 1, NOW(), %s)
                ON DUPLICATE KEY UPDATE hash_password = VALUES(hash_password), expire_time = VALUES(expire_time), pass_flag = 1, created_at = NOW(), role = VALUES(role)
            """
            query_status, message = run_mysql_commands(sql_insert,(ambari_server, user_name, email, hashed_password, expire_time, role),"",True)
            write_audit_log(email, 'USER_CREATED', f"{user_name}@{ambari_server}")
            #send_notification(ambari_server,str(request_time),user_name,password,email)
            fl.finfo(f" [INFO] Username creation successful! user: {user_name} in cluster:{ambari_server} ")
            fLogger.finfo(f"Ambari User: {user_name} is created for user {email} in ambari-server: {ambari_server} for {request_time} minutes, The user {user_name} will expire at {expire_time}")
            return jsonify({"username": user_name, "email": email, "password": password, "role": role}), 201

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

################################ SUDO POWER USER DELETION ###########################
@app.route('/api/manager/delete_user', methods=['POST'])
@app.route('/manager/delete_user', methods=['POST'])  # legacy
@cross_origin(supports_credentials=True)
def delete_user():
    global fLogger,fl
    """Deletes a user from both Ambari and MySQL."""
    try:

        data = request.get_json()
        user_name = data.get("user_name")
        email = (request.headers.get("X-Email") or "").strip().lower()

        if not email:
            fLogger.ferror(" [ERROR] Aborting as EMAIL is not provided in the Request")
            fl.ferror(" [ERROR] Email is not provided in the request.. Aborting ....")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not user_name:
            fl.ferror(f" [ERROR] User name is not provided By the manager to delete the user Requested")
            return jsonify({"error": "Username is required"}), 400

        # Look up which cluster this user belongs to first (needed for cluster-manager permission check)
        success, row = run_mysql_commands(
            "SELECT ambari_server FROM ambari_manager_users WHERE user_name = %s AND pass_flag = 1",
            (user_name,), "one", False
        )
        if not success:
            return jsonify({"error": "Database error looking up user"}), 500

        # Determine cluster for permission check (may be None if user not found — will 404 below)
        target_cluster = row[0] if row else None
        if not has_manager_access(email, target_cluster):
            fLogger.ferror(" [ERROR] Aborting: user requesting delete is not a manager for this cluster")
            fl.ferror(" [ERROR] Aborting: user requesting delete is not a manager for this cluster")
            return jsonify({"error": "You do not have manager permissions for this cluster."}), 403

        if target_cluster:
            ambari_server = target_cluster
            sql_check = """
                SELECT vault_password, http_method, port FROM ambari_onboarding
                WHERE ambari_server = %s
            """
            success, server_creds = run_mysql_commands(sql_check, (ambari_server,), "one")
            if success and server_creds:
                vault_pwd = decrypt_password(server_creds[0])
                http_method = server_creds[1] or "http"
                port = server_creds[2] or 8888
                if delete_user_from_ambari(user_name, ambari_server, "vault", vault_pwd, http_method, port):
                    run_mysql_commands("UPDATE ambari_manager_users SET pass_flag = 0, deleted_at = NOW() WHERE user_name = %s", (user_name,), "", True)
                    write_audit_log(email, 'MANAGER_VETO_DELETE', user_name)
                    fl.finfo(f" [INFO] The username {user_name} in {ambari_server} is deleted by the Manager {email} FORCEFULLY")
                    fLogger.finfo(f" [INFO] The username {user_name} in {ambari_server} is deleted by the Manager {email} FORCEFULLY")
                    return jsonify({"message": f"User '{user_name}' deleted successfully"}), 200
                else:
                    fl.ferror(f" [ERROR] Failed to delete {user_name} from Ambari - server {ambari_server} may be down or unreachable")
                    return jsonify({"error": f"Failed to delete user from Ambari. Cluster {ambari_server} may be down or unreachable."}), 503
        else:
            fl.ferror(f" [ERROR]The username {user_name} which is trying to be deleted by the manager is already Deleted or not found")
            return jsonify({"error": "User not found or already deleted"}), 404
    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500


################################# EXPIRY USER DELETION #######################

def remove_expired_users():
    global fLogger,fl
    """Removes expired users from MySQL and Ambari."""
    try:
        # Fetch expired users along with their Ambari servers
        s, expired_users = run_mysql_commands("SELECT user_name, ambari_server, email FROM ambari_manager_users WHERE expire_time < NOW() AND pass_flag = 1", None, "all", False)
        if not s or not expired_users:
            return
        for row in expired_users:
            user_name, ambari_server, user_email = row[0], row[1], row[2] if len(row) > 2 else "system"
            if ambari_server:
                fl.finfo(f" [INFO] Scheduler: removing expired user {user_name} from {ambari_server}")
                sql_check = """
                    SELECT vault_password, http_method, port FROM ambari_onboarding
                    WHERE ambari_server = %s
                """
                s, server_creds = run_mysql_commands(sql_check, (ambari_server,), "one", False)
                if not s or not server_creds:
                    fl.finfo(f" [INFO] Skipping {user_name} - Ambari server {ambari_server} not found in onboarding")
                    continue
                try:
                    decrypted_password = decrypt_password(server_creds[0])
                    http_method = server_creds[1] or "http"
                    port = server_creds[2] or 8888
                    if delete_user_from_ambari(user_name, ambari_server, "vault", decrypted_password, http_method, port):
                        run_mysql_commands("UPDATE ambari_manager_users SET pass_flag = 0, deleted_at = NOW() WHERE user_name = %s", (user_name,), "", True)
                        write_audit_log(user_email or "system", 'USER_EXPIRED_AUTO_DELETED', f"{user_name}@{ambari_server}")
                        fLogger.finfo(f"Successfully auto-deleted expired user {user_name} from {ambari_server}")
                    else:
                        fl.ferror(f" [ERROR] Failed to delete expired user {user_name} from Ambari API ({ambari_server})")
                except Exception as dec_err:
                    fl.ferror(f" [ERROR] Decryption failed for expired user {user_name}: {dec_err}")

    except Exception as e:
        fl.ferror(f" [ERROR] remove_expired_users scheduler error: {str(e)}")


def delete_user_from_ambari(user_name, ambari_server, ambari_username, ambari_password, http_method='http', port=8888):
    """Deletes a user from Ambari for a given server."""
    url = f"{http_method}://{ambari_server}:{port}/api/v1/users/{user_name}"
    print(f"Making DELETE request to: {url}")
    headers = {"X-Requested-By": "ambari"}
    try:
        response = requests.delete(url, auth=(ambari_username, ambari_password), headers=headers)
    except Exception as e:
        print(f"Looks Like Ambari Server {ambari_server}is DOWN or URL {url}is not available")
        fl.finfo(f"Looks Like Ambari Server {ambari_server}is DOWN or URL {url}is not available")
        fLogger.finfo(f"Looks Like Ambari Server {ambari_server}is DOWN or URL {url}is not available")
        return False
    if response.status_code in [200, 204]:
        print(f"Successfully deleted {user_name} from {ambari_server}")
        fl.finfo(f"Successfully deleted {user_name} from {ambari_server}")
        return True
    else:
        print(f"Failed to delete {user_name} from {ambari_server}: {response.text}")
        fl.finfo(f"Failed to delete {user_name} from {ambari_server}: {response.text}")
        return False

def get_ambari_users(ambari_server, user_name, password, http_method='http', port=8888):
    """Fetch all users from Ambari server."""
    url = f"{http_method}://{ambari_server}:{port}/api/v1/users"
    try:
        response = requests.get(url, auth=(user_name, password))
        if response.status_code != 200:
            fl.ferror(f" [ERROR] Not able fetch the available users in the cluster")
            return []

        #response.raise_for_status()

        users_data = response.json()
        users = [user["Users"]["user_name"] for user in users_data.get("items", [])]

        return users
    except requests.exceptions.RequestException as e:
        print(f"Error fetching users: {e}")
        return []


########################## SIGNIN WITH GOOGLE ############################

@app.route('/auth/google-login', methods=['POST'])
@cross_origin(supports_credentials=True) # Apply CORS specifically for this route
def login():
    data = request.get_json()
    user_data = data.get("userData")

    if not user_data:
        return jsonify({'message': 'No user data provided.'}), 400

    email = user_data.get("email")
    hd = user_data.get('hd') # Hosted Domain
    name = user_data.get('name')
    picture = user_data.get('picture') # Google profile picture URL
    given_name = user_data.get('given_name')
    family_name = user_data.get('family_name')

    # Input validation — email is mandatory; hd is only present for Google Workspace accounts
    if not email:
        return jsonify({'message': 'Email is missing from Google user data.'}), 400

    # Derive the effective domain:
    # - Google Workspace accounts supply `hd` (e.g. "company.com")
    # - Personal Gmail accounts do NOT supply `hd`, so fall back to the email domain
    effective_domain = hd if hd else (email.split('@')[1] if '@' in email else '')

    fl.finfo(f" [INFO] Google login attempt for email: {email}, effective domain: {effective_domain}")

    # Domain restriction
    try:
        allowed = config.get("auth", "allowed_domains")
    except (NoSectionError, NoOptionError):
        allowed = "gmail.com"
    allowed_domains = [d.strip() for d in allowed.split(",") if d.strip()]
    if effective_domain not in allowed_domains:
        return jsonify({'message': f'Access denied: Domain "{effective_domain}" is not allowed. Permitted domains: {", ".join(allowed_domains)}'}), 403

    # Use effective_domain in the response so the frontend stores the correct value
    hd = effective_domain

    # Create access token using email as identity
    access_token = create_access_token(identity=email)
    fl.finfo(f" [INFO] JWT token issued for {email}")

    # --- Constructing the Response ---
    # Return all the user_data you received, plus the access_token and a message.
    response_data = {
        "access_token": access_token,
        "message": f"Successfully signed in as {name} ({email})",
        "user": {
            "email": email,
            "name": name,
            "hd": hd,
            "picture": picture,
            "given_name": given_name,
            "family_name": family_name
            # Add any other user_data fields you need on the frontend
            # e.g., "locale": user_data.get("locale"),
            #        "id": user_data.get("sub") # Google's unique user ID
        }
    }

    return jsonify(response_data), 200

############################ REGISTER AN AMBARI SERVER ###################
@app.route('/api/register', methods=['POST'])
@cross_origin(supports_credentials=True)
def register_server():
    """Creates and registers the New Ambari Server Cluster"""
    try:
        data = request.get_json()
        ambari_server = data.get("ambari_server")
        port = data.get("port", 8888)
        http_method = data.get("http_method","http")
        manager_emails_raw = data.get("manager_emails", [])
        single_user_mode = 1 if data.get("single_user_mode") else 0

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari Server is not provided in the request. Aborting...")
            return jsonify({"error": "Missing Ambari server"}), 400

        # Validate and deduplicate manager emails
        validated_managers = []
        seen = set()
        for e in (manager_emails_raw or []):
            e = str(e).strip().lower()
            if not e:
                continue
            if not is_valid_email(e):
                return jsonify({"error": f"Invalid manager email format: '{e}'"}), 400
            if e not in seen:
                seen.add(e)
                validated_managers.append(e)
        manager_emails_json = json.dumps(validated_managers)

        sql_check = "SELECT ambari_server FROM ambari_onboarding WHERE ambari_server = %s"
        query_status, message = run_mysql_commands(sql_check,(ambari_server,),"one")
        
        if message:
            fl.ferror(f"An active cluster {ambari_server} is already registered in the Ambari Vault")
            return jsonify({"error": f"Ambari Server already exists in Ambari Vault, policy violation for: {ambari_server}"}), 409
   

        # Validate admin/admin credential in Ambari cluster
        initial_ambari_username = "admin"
        initial_ambari_password = "admin"
        check_url = f"{http_method}://{ambari_server}:{port}/api/v1/clusters"
        headers = {"X-Requested-By": "ambari", "Content-Type": "application/json"}
        check_response = requests.get(check_url, headers=headers, auth=(initial_ambari_username, initial_ambari_password))

        if check_response.status_code != 200:
            fl.ferror(" [ERROR] The initial credential admin/admin is not correctly present")
            return jsonify({"error": "[POLICY] The initial credential admin/admin is missing in the Ambari Server"}), 500

        users_existed = get_ambari_users(ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        # Create Vault user
        if "vault" in users_existed:
            delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        vault_password = generate_password(12)
        vault_hashed_password = encrypt_password(vault_password)
        vault_failure, vault_message = create_ambari_user(ambari_server, "vault", vault_password, initial_ambari_username, initial_ambari_password, True, 'CLUSTER.ADMINISTRATOR', http_method, port)

        if not vault_failure:
            fl.ferror(" [ERROR] Error while creating the Vault Ambari user")
            return jsonify({"error": vault_message}), 500

        # Create Admin DR user
        if "ambari_admin_dr" in users_existed:
            delete_user_from_ambari("ambari_admin_dr", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        dr_password = generate_password(12)
        dr_hashed_password = encrypt_password(dr_password)
        dr_failure, dr_message = create_ambari_user(ambari_server, "ambari_admin_dr", dr_password, initial_ambari_username, initial_ambari_password, True, 'CLUSTER.ADMINISTRATOR', http_method, port)

        if not dr_failure:
            delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
            fl.ferror(" [ERROR] Error while creating the Ambari Admin DR user")
            return jsonify({"error": dr_message}), 500

        # Create CLUSTER USERS (sre_ro and dev_ro) with generated passwords
        sre_ro_password = generate_password(16)
        dev_ro_password = generate_password(16)
        create_ambari_user(ambari_server, "sre_ro", sre_ro_password, initial_ambari_username, initial_ambari_password, False, "CLUSTER.USER", http_method, port)
        create_ambari_user(ambari_server, "dev_ro", dev_ro_password, initial_ambari_username, initial_ambari_password, False, "CLUSTER.USER", http_method, port)

        # Delete users other than Vault, Admin DR, Admin, sre_ro, dev_ro
        filtered_users_list = [u for u in users_existed if u not in ("vault", "ambari_admin_dr", "admin", "sre_ro", "dev_ro")]

        for u in filtered_users_list:
            success = delete_user_from_ambari(u, ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
            if not success:
                delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
                delete_user_from_ambari("ambari_admin_dr", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
                return jsonify({"error": f"Failed to delete {u} from {ambari_server}, which violates the registration policy"}), 500

        delete_user_from_ambari("admin", ambari_server, "vault", vault_password, http_method, port)

        # Store hashed passwords, cluster managers, and access policy in MySQL
        # Try full INSERT first; auto-migrate missing columns as needed
        sql_insert_full = """
                    INSERT INTO ambari_onboarding (ambari_server, http_method, port, vault_password, admin_dr_password, manager_emails, single_user_mode)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """
        sql_insert_mgr_only = """
                    INSERT INTO ambari_onboarding (ambari_server, http_method, port, vault_password, admin_dr_password, manager_emails)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """
        query_status, message = run_mysql_commands(
            sql_insert_full,
            (ambari_server, http_method, port, vault_hashed_password, dr_hashed_password, manager_emails_json, single_user_mode),
            "", True
        )
        if not query_status:
            # single_user_mode column may not exist yet — auto-migrate
            fl.fwarn(" [WARN] single_user_mode or manager_emails column missing during register — auto-migrating")
            run_mysql_commands("ALTER TABLE ambari_onboarding ADD COLUMN manager_emails TEXT DEFAULT NULL", None, "", True)
            run_mysql_commands("ALTER TABLE ambari_onboarding ADD COLUMN single_user_mode TINYINT(1) DEFAULT 0", None, "", True)
            query_status, message = run_mysql_commands(
                sql_insert_full,
                (ambari_server, http_method, port, vault_hashed_password, dr_hashed_password, manager_emails_json, single_user_mode),
                "", True
            )
            if not query_status:
                # Last-resort fallback: insert without new columns
                query_status, message = run_mysql_commands(
                    sql_insert_mgr_only,
                    (ambari_server, http_method, port, vault_hashed_password, dr_hashed_password, manager_emails_json),
                    "", True
                )
                if not query_status:
                    return jsonify({"error": f"Registration failed: {message}"}), 500

        reg_email = (request.headers.get("X-Email") or "").strip() or "system"
        write_audit_log(reg_email, 'CLUSTER_REGISTERED', ambari_server)

        return jsonify({"message": "Registration DONE Successfully"}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

############################ RE - REGISTER AN AMBARI SERVER  By manager ###################

@app.route('/api/re-register', methods=['POST'])
@cross_origin(supports_credentials=True)
def re_register_server():
    """Re-registers an existing Ambari Server Cluster - Manager Only"""
    global fLogger, fl
    try:
        data = request.get_json()
        ambari_server = data.get("ambari_server")
        port = data.get("port", 8888)
        http_method = data.get("http_method", "http")
        email = (request.headers.get("X-Email") or "").strip().lower()

        if not email:
            fLogger.ferror(" [ERROR] Aborting as EMAIL is not provided in the Request")
            fl.ferror(" [ERROR] Email is not provided in the request.. Aborting ....")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not has_manager_access(email, ambari_server):
            fLogger.ferror(" [ERROR] Aborting: user requesting re-register is not a manager for this cluster")
            fl.ferror(" [ERROR] Aborting: user requesting re-register is not a manager for this cluster")
            return jsonify({"error": "You do not have manager permissions for this cluster."}), 403

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari Server is not provided in the request. Aborting...")
            return jsonify({"error": "Missing Ambari server"}), 400

        # Check if the Ambari server exists
        sql_check = "SELECT ambari_server FROM ambari_onboarding WHERE ambari_server = %s"
        query_status, existing_cluster = run_mysql_commands(sql_check, (ambari_server,), "one")

        if not existing_cluster:
            fl.ferror(f"This Ambari is not registered YET. Please Register This ambari Server Through Registration Form")
            return jsonify({"error": f"This Ambari is not registered YET. Please Register This ambari Server Through Registration Form: {ambari_server}"}), 409

        # Validate admin/admin credential in Ambari cluster
        initial_ambari_username = "admin"
        initial_ambari_password = "admin"
        check_url = f"{http_method}://{ambari_server}:{port}/api/v1/clusters"
        headers = {"X-Requested-By": "ambari", "Content-Type": "application/json"}
        check_response = requests.get(check_url, headers=headers, auth=(initial_ambari_username, initial_ambari_password))

        if check_response.status_code != 200:
            fl.ferror(" [ERROR] The initial credential admin/admin is not correctly present")
            return jsonify({"error": "[POLICY] The initial credential admin/admin is missing in the Ambari Server"}), 500

        users_existed = get_ambari_users(ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        # Create Vault user
        if "vault" in users_existed:
            delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        vault_password = generate_password(12)
        vault_hashed_password = encrypt_password(vault_password)
        vault_failure, vault_message = create_ambari_user(ambari_server, "vault", vault_password, initial_ambari_username, initial_ambari_password, True, 'CLUSTER.ADMINISTRATOR', http_method, port)

        if not vault_failure:
            fl.ferror(" [ERROR] Error while creating the Vault Ambari user")
            return jsonify({"error": vault_message}), 500

        # Create Admin DR user
        if "ambari_admin_dr" in users_existed:
            delete_user_from_ambari("ambari_admin_dr", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)

        dr_password = generate_password(12)
        dr_hashed_password = encrypt_password(dr_password)
        dr_failure, dr_message = create_ambari_user(ambari_server, "ambari_admin_dr", dr_password, initial_ambari_username, initial_ambari_password, True, 'CLUSTER.ADMINISTRATOR', http_method, port)

        if not dr_failure:
            delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
            fl.ferror(" [ERROR] Error while creating the Ambari Admin DR user")
            return jsonify({"error": dr_message}), 500

        # Create CLUSTER USERS (sre_ro and dev_ro) with generated passwords
        sre_ro_password = generate_password(16)
        dev_ro_password = generate_password(16)
        create_ambari_user(ambari_server, "sre_ro", sre_ro_password, initial_ambari_username, initial_ambari_password, False, "CLUSTER.USER", http_method, port)
        create_ambari_user(ambari_server, "dev_ro", dev_ro_password, initial_ambari_username, initial_ambari_password, False, "CLUSTER.USER", http_method, port)

        # Delete users other than Vault, Admin DR, Admin, sre_ro, dev_ro
        filtered_users_list = [u for u in users_existed if u not in ("vault", "ambari_admin_dr", "admin", "sre_ro", "dev_ro")]

        for u in filtered_users_list:
            success = delete_user_from_ambari(u, ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
            if not success:
                delete_user_from_ambari("vault", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
                delete_user_from_ambari("ambari_admin_dr", ambari_server, initial_ambari_username, initial_ambari_password, http_method, port)
                return jsonify({"error": f"Failed to delete {u} from {ambari_server}, which violates the registration policy"}), 500

        delete_user_from_ambari("admin", ambari_server, "vault", vault_password, http_method, port)

        # Update hashed passwords in MySQL and clear DR_COMPROMISED flag
        sql_update = """
            UPDATE ambari_onboarding
            SET vault_password = %s, admin_dr_password = %s, http_method = %s, port = %s, dr_compromised = 0
            WHERE ambari_server = %s
        """
        query_status, message = run_mysql_commands(sql_update, (vault_hashed_password, dr_hashed_password, http_method, port, ambari_server), "", True)

        write_audit_log(email, 'MANAGER_REREGISTRATION_DONE', ambari_server)

        fl.finfo(f" [INFO] Re-registration completed successfully for {ambari_server} by manager {email}")
        return jsonify({"message": "RE-Registration DONE Successfully"}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

############################ TEST AMBARI CONNECTION ###################

@app.route('/api/test_connection', methods=['POST'])
@cross_origin(supports_credentials=True)
def test_ambari_connection():
    """Test connectivity to an Ambari server (admin/admin)"""
    global fl
    try:
        data = request.get_json()
        ambari_server = data.get("ambari_server")
        port = int(data.get("port", 8888))
        http_method = data.get("http_method", "http")
        if not ambari_server:
            return jsonify({"error": "Missing ambari_server"}), 400
        url = f"{http_method}://{ambari_server}:{port}/api/v1/clusters"
        headers = {"X-Requested-By": "ambari", "Content-Type": "application/json"}
        r = requests.get(url, headers=headers, auth=("admin", "admin"), timeout=15)
        if r.status_code == 200:
            count = len(r.json().get("items", []))
            return jsonify({"ok": True, "message": f"Connected. Found {count} cluster(s)."})
        elif r.status_code == 401:
            return jsonify({"ok": False, "message": "Authentication failed. Ensure admin/admin is active."})
        return jsonify({"ok": False, "message": f"HTTP {r.status_code}"})
    except requests.exceptions.ConnectionError:
        return jsonify({"ok": False, "message": "Connection refused. Check hostname and port."})
    except requests.exceptions.Timeout:
        return jsonify({"ok": False, "message": "Connection timed out."})
    except Exception as e:
        return jsonify({"ok": False, "message": str(e)})

############################ HEALTH CHECK ###################

@app.route('/api/health', methods=['GET'])
@cross_origin(supports_credentials=True)
def health_check():
    """Simple health check for connectivity validation"""
    return jsonify({"status": "ok", "service": "ambari-kavach"}), 200

############################ GET ALL THE SERVERS REGISTERED ###################

@app.route('/api/servers', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_servers():
    """Fetch all registered Ambari servers with connection details"""
    global fl
    try:
        # Try full query first; fall back gracefully for old deployments missing new columns
        query_status, servers = run_mysql_commands(
            "SELECT ambari_server, http_method, port, manager_emails, single_user_mode FROM ambari_onboarding", None, "all"
        )
        has_extra_cols = True
        has_manager_col = False
        if not query_status:
            # Try with just manager_emails
            query_status, servers = run_mysql_commands(
                "SELECT ambari_server, http_method, port, manager_emails FROM ambari_onboarding", None, "all"
            )
            has_extra_cols = False
            has_manager_col = query_status
            if not query_status:
                fl.fwarn(" [WARN] manager_emails column not found — falling back to base query")
                query_status, servers = run_mysql_commands(
                    "SELECT ambari_server, http_method, port FROM ambari_onboarding", None, "all"
                )

        if not query_status:
            fl.ferror(" [ERROR] Failed to fetch servers from database")
            return jsonify({"error": "Database query failed"}), 500

        server_list = [s[0] for s in servers] if servers else []
        servers_detail = []
        for s in (servers or []):
            mgr_emails = []
            single_user = False
            if has_extra_cols:
                try:
                    mgr_emails = json.loads(s[3]) if s[3] else []
                except (json.JSONDecodeError, TypeError):
                    mgr_emails = []
                single_user = bool(s[4]) if len(s) > 4 and s[4] is not None else False
            elif has_manager_col:
                try:
                    mgr_emails = json.loads(s[3]) if s[3] else []
                except (json.JSONDecodeError, TypeError):
                    mgr_emails = []
            servers_detail.append({
                "host": s[0],
                "http_method": s[1] or "http",
                "port": s[2] or 8888,
                "manager_emails": mgr_emails,
                "single_user_mode": single_user,
            })
        return jsonify({"servers": server_list, "servers_detail": servers_detail}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

############################ GET AUDIT LOGS ###################

@app.route('/api/audit_logs', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_audit_logs():
    """Fetch recent audit logs from ambari_vault_major_audit"""
    global fl
    try:
        email = (request.headers.get("X-Email") or "").strip()
        if not email:
            return jsonify({"error": "Missing email in request headers"}), 400

        try:
            limit = max(1, min(int(request.args.get("limit", 200)), 500))
            offset = max(0, int(request.args.get("offset", 0)))
        except (ValueError, TypeError):
            limit, offset = 200, 0

        ok, rows = run_mysql_commands(
            "SELECT actor_email, audit_event, impact_entity, event_time FROM ambari_vault_major_audit ORDER BY event_time DESC LIMIT %s OFFSET %s",
            (limit, offset), "all"
        )
        if not ok:
            return jsonify({"error": rows if isinstance(rows, str) else "Database query failed"}), 500

        # Get total count for pagination metadata
        ok_count, count_row = run_mysql_commands("SELECT COUNT(*) FROM ambari_vault_major_audit", None, "one")
        total = count_row[0] if ok_count and count_row else 0

        logs = [{"user": r[0], "event": r[1], "entity": r[2], "timestamp": str(r[3]) if r[3] else None} for r in (rows or [])]
        return jsonify({"audit_logs": logs, "total": total, "limit": limit, "offset": offset}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] audit_logs: {str(e)}")
        return jsonify({"error": str(e)}), 500

############################ GET CURRENT USER INFO ###################

@app.route('/api/me', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_current_user():
    """Return current user email, super-admin status, and per-cluster manager status."""
    try:
        email = (request.headers.get("X-Email") or "").strip().lower()
        if not email:
            return jsonify({"error": "Missing email in request headers"}), 400

        user_is_super_admin = is_super_admin(email)

        # Find which clusters this user manages (if not super admin, check per-cluster)
        managed = []
        if not user_is_super_admin:
            ok, rows = run_mysql_commands(
                "SELECT ambari_server, manager_emails FROM ambari_onboarding",
                None, "all", False
            )
            for row in (rows if ok and isinstance(rows, list) else []):
                server, mgr_emails_json = row[0], row[1]
                if not mgr_emails_json:
                    continue
                try:
                    mgr_list = [e.strip().lower() for e in json.loads(mgr_emails_json) if e]
                    if email in mgr_list:
                        managed.append(server)
                except (json.JSONDecodeError, TypeError):
                    pass

        return jsonify({
            "email": email,
            "is_super_admin": user_is_super_admin,
            "managed_clusters": managed,
            "is_manager": user_is_super_admin or len(managed) > 0,
        }), 200
    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

############################ GET ALL THE ACTIVE USERS FOR A GIVEN USER EMAIL ###################

@app.route('/api/active_users', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_active_users():
    """Fetch active users for the requesting email"""
    global fLogger, fl
    try:
        email = request.headers.get("X-Email")
        if not email:
            fLogger.ferror(" [ERROR] Aborting as EMAIL is not provided in the Request")
            fl.ferror(" [ERROR] Email is not provided in the request.. Aborting ....")
            return jsonify({"error": "Missing email in request headers"}), 400

        sql_query = """
            SELECT user_name, hash_password, ambari_server, created_at, expire_time
            FROM ambari_manager_users
            WHERE email = %s AND pass_flag = 1
        """
        query_status, users = run_mysql_commands(sql_query, (email,), "all")

        if not query_status:
            fl.ferror(" [ERROR] Failed to fetch active users from database")
            return jsonify({"error": "Database query failed"}), 500

        if not users:
            return jsonify({"message": "No active users available for the given email"}), 404

        user_list = [
            {
                "username": user[0], 
                "password": decrypt_password(user[1]), 
                "ambari_server": user[2],
                "created_at": str(user[3]) if user[3] else None,
                "expire_time": str(user[4]) if user[4] else None
            } for user in reversed(users)
        ]
        return jsonify({"active_users": user_list}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500



######################## GET ALL THE USERS FOR A GIVEN USER EMAIL ###################

@app.route('/api/expired_users', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_all_users():
    """Fetch all expired users for the requesting email"""
    global fLogger, fl
    try:
        email = request.headers.get("X-Email")
        if not email:
            fLogger.ferror(" [ERROR] Aborting as EMAIL is not provided in the Request")
            fl.ferror(" [ERROR] Email is not provided in the request.. Aborting ....")
            return jsonify({"error": "Missing email in request headers"}), 400

        sql_query = """
            SELECT user_name, ambari_server, created_at, expire_time, deleted_at
            FROM ambari_manager_users
            WHERE email = %s AND pass_flag = 0
        """
        query_status, users = run_mysql_commands(sql_query, (email,), "all")

        if not query_status:
            fl.ferror(" [ERROR] Failed to fetch expired users from database")
            return jsonify({"error": "Database query failed"}), 500

        if not users:
            return jsonify({"message": "No expired users available for the given email"}), 404

        user_list = [
            {
                "username": user[0], 
                "ambari_server": user[1], 
                "created_at": str(user[2]) if user[2] else None,
                "expired_at": str(user[3]) if user[3] else None,
                "deleted_at": str(user[4]) if user[4] else None
            } for user in reversed(users)
        ]
        return jsonify({"all_users": user_list}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500



######################## GET ALL THE USERS FOR A GIVEN CLUSTER ###################

@app.route('/api/cluster_users', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_cluster_users():
    """Fetch all users (active and expired) for a given Ambari server"""
    global fLogger, fl
    try:
        # Get ambari_server from query parameters
        ambari_server = request.args.get("ambari_server")

        # Validate ambari_server parameter
        if not ambari_server:
            fl.ferror(" [ERROR] Ambari server parameter is missing")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        # Check if the Ambari server is registered
        sql_check_server = "SELECT ambari_server FROM ambari_onboarding WHERE ambari_server = %s"
        query_status, server_exists = run_mysql_commands(sql_check_server, (ambari_server,), "one")

        if not query_status:
            fl.ferror(" [ERROR] Failed to check server registration")
            return jsonify({"error": "Database query failed"}), 500

        if not server_exists:
            fl.ferror(f" [ERROR] Ambari server {ambari_server} is not registered")
            return jsonify({"error": f"Ambari server '{ambari_server}' is not registered in the system"}), 404

        # Fetch all users for the given ambari_server
        sql_query = """
            SELECT 
                user_name, 
                email, 
                created_at, 
                expire_time, 
                pass_flag,
                deleted_at,
                hash_password
            FROM ambari_manager_users
            WHERE ambari_server = %s
            ORDER BY created_at DESC
        """
        query_status, users = run_mysql_commands(sql_query, (ambari_server,), "all")

        if not query_status:
            fl.ferror(" [ERROR] Failed to fetch cluster users from database")
            return jsonify({"error": "Database query failed"}), 500

        if not users:
            return jsonify({
                "message": f"No users found for Ambari server '{ambari_server}'",
                "ambari_server": ambari_server,
                "total_users": 0,
                "active_users": 0,
                "expired_users": 0,
                "users": []
            }), 200

        # Process users data
        active_users = []
        expired_users = []
        
        for user in users:
            user_data = {
                "username": user[0],
                "email": user[1],
                "created_at": str(user[2]) if user[2] else None,
                "expire_time": str(user[3]) if user[3] else None,
                "status": "active" if user[4] == 1 else "expired",
                "deleted_at": str(user[5]) if user[5] else None
            }
            
            # Add password only for active users
            if user[4] == 1:  # pass_flag = 1 means active
                user_data["password"] = decrypt_password(user[6])
                active_users.append(user_data)
            else:
                expired_users.append(user_data)

        # Prepare response
        response_data = {
            "ambari_server": ambari_server,
            "total_users": len(users),
            "active_users_count": len(active_users),
            "expired_users_count": len(expired_users),
            "active_users": active_users,
            "expired_users": expired_users,
            "summary": {
                "cluster": ambari_server,
                "total": len(users),
                "active": len(active_users),
                "expired": len(expired_users)
            }
        }

        fl.finfo(f" [INFO] fetched user list for cluster {ambari_server} - Total: {len(users)}, Active: {len(active_users)}, Expired: {len(expired_users)}")
        return jsonify(response_data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500




########################## Adding new thing nelow to test ##################################################


############################ AMBARI CLUSTER INFORMATION APIS ######################

def get_ambari_credentials(ambari_server):
    """Helper function to get Ambari credentials for a registered server"""
    sql_check = "SELECT vault_password, http_method, port FROM ambari_onboarding WHERE ambari_server = %s"
    query_status, server_data = run_mysql_commands(sql_check, (ambari_server,), "one")
    
    if not query_status or not server_data:
        return None, None, None, None
    
    vault_password = decrypt_password(server_data[0])
    http_method = server_data[1] or "http"
    port = server_data[2] or 8888
    
    return "vault", vault_password, http_method, port

def make_ambari_request(ambari_server, endpoint, email):
    """Helper function to make authenticated requests to Ambari"""
    username, password, http_method, port = get_ambari_credentials(ambari_server)
    
    if not username:
        return None, {"error": "Ambari server not registered"}, 404
    
    url = f"{http_method}://{ambari_server}:{port}/api/v1/{endpoint}"
    headers = {"X-Requested-By": "ambari"}
    
    try:
        response = requests.get(url, auth=(username, password), headers=headers, timeout=30)
        
        if response.status_code == 200:
            return response.json(), None, 200
        else:
            fl.ferror(f" [ERROR] Failed to fetch data from {url} - Status: {response.status_code}")
            return None, {"error": f"Failed to fetch data from Ambari: {response.status_code}"}, response.status_code
            
    except requests.exceptions.Timeout:
        fl.ferror(f" [ERROR] Timeout connecting to {ambari_server}")
        return None, {"error": "Connection timeout to Ambari server"}, 500
    except requests.exceptions.ConnectionError:
        fl.ferror(f" [ERROR] Connection error to {ambari_server}")
        return None, {"error": "Connection error to Ambari server"}, 500
    except Exception as e:
        fl.ferror(f" [ERROR] Request failed: {str(e)}")
        return None, {"error": str(e)}, 500

@app.route('/api/ambari/clusters', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_clusters():
    """Fetch all clusters from a given Ambari server"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari server parameter is missing")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        data, error, status_code = make_ambari_request(ambari_server, "clusters", email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched clusters from {ambari_server}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/hosts', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_hosts():
    """Fetch all hosts from a given Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        if cluster_name:
            endpoint = f"clusters/{cluster_name}/hosts"
        else:
            endpoint = "hosts"

        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched hosts from {ambari_server}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/services', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_services():
    """Fetch all services from a given Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        endpoint = f"clusters/{cluster_name}/services"
        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched services from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/components', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_components():
    """Fetch all components from a given service in Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        service_name = request.args.get("service_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        if service_name:
            endpoint = f"clusters/{cluster_name}/services/{service_name}/components"
        else:
            endpoint = f"clusters/{cluster_name}/components"

        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched components from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/host_components', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_host_components():
    """Fetch host components from a given Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        host_name = request.args.get("host_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        if host_name:
            endpoint = f"clusters/{cluster_name}/hosts/{host_name}/host_components"
        else:
            endpoint = f"clusters/{cluster_name}/host_components"

        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched host components from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/configurations', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_configurations():
    """Fetch configurations from a given Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        config_type = request.args.get("config_type")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        if config_type:
            endpoint = f"clusters/{cluster_name}/configurations?type={config_type}"
        else:
            endpoint = f"clusters/{cluster_name}/configurations"

        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched configurations from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/users', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_users():
    """Fetch all users from Ambari server"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari server parameter is missing")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        data, error, status_code = make_ambari_request(ambari_server, "users", email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched Ambari users from {ambari_server}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/groups', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_groups():
    """Fetch all groups from Ambari server"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari server parameter is missing")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        data, error, status_code = make_ambari_request(ambari_server, "groups", email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched Ambari groups from {ambari_server}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/privileges', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_privileges():
    """Fetch privileges from Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        endpoint = f"clusters/{cluster_name}/privileges"
        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched privileges from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/requests', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_requests():
    """Fetch requests/operations from Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        endpoint = f"clusters/{cluster_name}/requests"
        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched requests from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/stack_versions', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_stack_versions():
    """Fetch stack versions from Ambari server"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server:
            fl.ferror(" [ERROR] Ambari server parameter is missing")
            return jsonify({"error": "Missing ambari_server parameter"}), 400

        data, error, status_code = make_ambari_request(ambari_server, "stacks", email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched stack versions from {ambari_server}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/ambari/alerts', methods=['GET'])
@cross_origin(supports_credentials=True)
def fetch_ambari_alerts():
    """Fetch alerts from Ambari cluster"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        endpoint = f"clusters/{cluster_name}/alerts"
        data, error, status_code = make_ambari_request(ambari_server, endpoint, email)
        
        if error:
            return jsonify(error), status_code
            
        fl.finfo(f" [INFO] User {email} fetched alerts from {ambari_server}/{cluster_name}")
        return jsonify(data), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500


########################## @@@@@@@@@@@@@@ #################

############################ AMBARI AGGREGATED ANALYTICS APIS ######################

@app.route('/api/analytics/cluster_overview', methods=['GET'])
@cross_origin(supports_credentials=True)
def get_cluster_overview():
    """Get comprehensive cluster overview with all resources and services"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        # Get clusters data
        clusters_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}", email)
        if error:
            return jsonify(error), status

        # Get hosts data - request total_mem/available_mem for correct memory utilization (fallback if 400)
        hosts_data, error, status = make_ambari_request(
            ambari_server,
            f"clusters/{cluster_name}/hosts?fields=Hosts/total_mem,Hosts/available_mem,Hosts/host_name,Hosts/ip,Hosts/cpu_count,Hosts/disk_info",
            email,
        )
        if error and status == 400:
            hosts_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/hosts", email)
        if error:
            return jsonify(error), status

        # Get services data
        services_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/services", email)
        if error:
            return jsonify(error), status

        # Get host components
        components_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/host_components", email)
        if error:
            return jsonify(error), status

        # Process and aggregate data
        cluster_info = clusters_data.get('Clusters', {})
        hosts = hosts_data.get('items', [])
        services = services_data.get('items', [])
        host_components = components_data.get('items', [])

        # Aggregate host resources
        total_memory = 0
        total_cpu_cores = 0
        total_disk_space = 0
        host_details = []

        used_memory_gb = 0
        for host in hosts:
            host_info = host.get('Hosts', {}) or {}
            memory = host_info.get('total_mem') or 0
            try:
                memory = float(memory) / (1024 * 1024) if memory else 0  # KB to GB
            except (TypeError, ValueError):
                memory = 0
            avail_mem = host_info.get('available_mem') or 0
            try:
                avail_mem = float(avail_mem) / (1024 * 1024) if avail_mem else 0
            except (TypeError, ValueError):
                avail_mem = 0
            used_memory_gb += max(0, memory - avail_mem)
            cpu_count = host_info.get('cpu_count') or host_info.get('ph_cpu_count') or 0
            try:
                cpu_count = int(cpu_count) if cpu_count else 0
            except (TypeError, ValueError):
                cpu_count = 0
            disk_info = host_info.get('disk_info', []) or []
            
            total_memory += memory
            total_cpu_cores += cpu_count
            
            host_disk_total = sum([float(disk.get('size') or 0) for disk in disk_info]) / (1024 * 1024 * 1024)  # Convert bytes to GB
            total_disk_space += host_disk_total

            host_details.append({
                "hostname": host_info.get('host_name'),
                "ip": host_info.get('ip'),
                "memory_gb": round(memory, 2),
                "available_memory_gb": round(avail_mem, 2),
                "cpu_cores": int(cpu_count),
                "disk_gb": round(host_disk_total, 2),
                "os_type": host_info.get('os_type'),
                "rack_info": host_info.get('rack_info'),
                "host_state": host_info.get('host_state')
            })

        # Fallback: if list endpoint returned no metrics, fetch each host for full details
        if total_memory == 0 and total_cpu_cores == 0 and hosts:
            used_memory_gb = 0
            for host in hosts:
                host_info = host.get('Hosts', {})
                host_name = host_info.get('host_name')
                if not host_name:
                    continue
                host_detail_data, err, _ = make_ambari_request(
                    ambari_server, f"clusters/{cluster_name}/hosts/{host_name}", email
                )
                if err:
                    continue
                h = host_detail_data.get('Hosts', {})
                mem_kb = h.get('total_mem', 0) or 0
                mem_gb = (mem_kb / (1024 * 1024)) if mem_kb else 0
                avail_kb = h.get('available_mem', 0) or 0
                avail_gb = (avail_kb / (1024 * 1024)) if avail_kb else 0
                used_memory_gb += max(0, mem_gb - avail_gb)
                cpu = h.get('cpu_count', 0) or h.get('ph_cpu_count', 0) or 0
                total_memory += mem_gb
                total_cpu_cores += cpu
                disk_list = h.get('disk_info', []) or []
                d_gb = sum(float(d.get('size') or 0) for d in disk_list) / (1024 * 1024 * 1024)
                total_disk_space += d_gb
                for hd in host_details:
                    if hd.get('hostname') == host_name:
                        hd['memory_gb'] = round(mem_gb, 2)
                        hd['available_memory_gb'] = round(avail_gb, 2)
                        hd['cpu_cores'] = int(cpu)
                        hd['disk_gb'] = round(d_gb, 2)
                        break

        # Aggregate service information
        service_summary = []
        for service in services:
            service_info = service.get('ServiceInfo', {})
            service_summary.append({
                "service_name": service_info.get('service_name'),
                "state": service_info.get('state'),
                "maintenance_state": service_info.get('maintenance_state')
            })

        # Compute utilization from host data
        utilization = {}
        if total_memory > 0:
            utilization = {
                "used_memory_gb": round(used_memory_gb, 2),
                "available_memory_gb": round(total_memory - used_memory_gb, 2),
                "memory_utilization_percent": round((used_memory_gb / total_memory * 100), 2),
            }

        # Fetch alerts for health summary
        health_summary = {}
        try:
            health_resp, health_err, _ = make_ambari_request(
                ambari_server, f"clusters/{cluster_name}/alerts", email
            )
            if not health_err and health_resp:
                alerts = health_resp.get('items', [])
                critical = sum(1 for a in alerts if a.get('Alert', {}).get('state') == 'CRITICAL')
                warning = sum(1 for a in alerts if a.get('Alert', {}).get('state') == 'WARNING')
                health_summary = {"critical_alerts": critical, "warning_alerts": warning}
        except Exception:
            pass

        # Fetch all users for this cluster - active and expired (no password)
        past_users = []
        try:
            sql_users = """
                SELECT user_name, email, created_at, expire_time, pass_flag
                FROM ambari_manager_users
                WHERE ambari_server = %s
                ORDER BY created_at DESC
            """
            ok, rows = run_mysql_commands(sql_users, (ambari_server,), "all")
            if ok and rows:
                past_users = [
                    {"username": r[0], "email": r[1], "created_at": str(r[2]) if r[2] else None, "expire_time": str(r[3]) if r[3] else None, "status": "active" if r[4] == 1 else "expired"}
                    for r in rows
                ]
            fl.finfo(f" [INFO] Fetched {len(past_users)} users for cluster {ambari_server}")
        except Exception as ex:
            fl.ferror(f" [ERROR] Failed to fetch users for {ambari_server}: {ex}")

        # Create comprehensive overview
        overview = {
            "cluster_info": {
                "cluster_name": cluster_info.get('cluster_name'),
                "version": cluster_info.get('version'),
                "cluster_id": cluster_info.get('cluster_id'),
                "security_type": cluster_info.get('security_type')
            },
            "resource_summary": {
                "total_hosts": len(hosts),
                "total_memory_gb": round(total_memory, 2),
                "total_cpu_cores": int(total_cpu_cores),
                "total_disk_gb": round(total_disk_space, 2),
                "total_services": len(services),
                **utilization,
            },
            "health_summary": health_summary,
            "hosts": host_details,
            "services": service_summary,
            "past_users": past_users,
            "generated_at": datetime.now().isoformat()
        }

        fl.finfo(f" [INFO] User {email} fetched cluster overview for {ambari_server}/{cluster_name}")
        return jsonify(overview), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/host_details', methods=['GET'])
@cross_origin(supports_credentials=True)
def get_host_details():
    """Get detailed information for a specific host including components and resources"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        host_name = request.args.get("host_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name or not host_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing required parameters"}), 400

        # Get host information
        host_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/hosts/{host_name}", email)
        if error:
            return jsonify(error), status

        # Get host components
        components_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/hosts/{host_name}/host_components", email)
        if error:
            return jsonify(error), status

        host_info = host_data.get('Hosts', {})
        components = components_data.get('items', [])

        # Process host metrics
        host_details = {
            "basic_info": {
                "hostname": host_info.get('host_name'),
                "ip_address": host_info.get('ip'),
                "public_hostname": host_info.get('public_host_name'),
                "os_arch": host_info.get('os_arch'),
                "os_type": host_info.get('os_type'),
                "os_family": host_info.get('os_family'),
                "rack_info": host_info.get('rack_info'),
                "host_state": host_info.get('host_state'),
                "health_status": host_info.get('host_health_report', {}).get('HostHealthStatus', {}).get('health_status')
            },
            "resources": {
                "total_memory_kb": host_info.get('total_mem'),
                "total_memory_gb": round(host_info.get('total_mem', 0) / (1024 * 1024), 2),
                "available_memory_kb": host_info.get('available_mem'),
                "available_memory_gb": round(host_info.get('available_mem', 0) / (1024 * 1024), 2),
                "cpu_count": host_info.get('cpu_count'),
                "physical_cpu_count": host_info.get('ph_cpu_count'),
                "load_avg": {
                    "load_one": host_info.get('load_one'),
                    "load_five": host_info.get('load_five'),
                    "load_fifteen": host_info.get('load_fifteen')
                }
            },
            "disk_info": host_info.get('disk_info', []),
            "components": []
        }

        # Process components information
        for component in components:
            comp_info = component.get('HostRoles', {})
            host_details["components"].append({
                "component_name": comp_info.get('component_name'),
                "service_name": comp_info.get('service_name'),
                "state": comp_info.get('state'),
                "desired_state": comp_info.get('desired_state'),
                "maintenance_state": comp_info.get('maintenance_state'),
                "stack_id": comp_info.get('stack_id')
            })

        # Calculate resource utilization percentages
        if host_details["resources"]["total_memory_gb"] > 0:
            memory_used_gb = host_details["resources"]["total_memory_gb"] - host_details["resources"]["available_memory_gb"]
            host_details["resources"]["memory_utilization_percent"] = round((memory_used_gb / host_details["resources"]["total_memory_gb"]) * 100, 2)

        fl.finfo(f" [INFO] User {email} fetched host details for {host_name} in {ambari_server}/{cluster_name}")
        return jsonify(host_details), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/service_distribution', methods=['GET'])
@cross_origin(supports_credentials=True)
def get_service_distribution():
    """Get service distribution across hosts with component mapping"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        # Get services
        services_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/services", email)
        if error:
            return jsonify(error), status

        # Get all host components
        components_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/host_components", email)
        if error:
            return jsonify(error), status

        services = services_data.get('items', [])
        host_components = components_data.get('items', [])

        # Create service distribution map
        service_distribution = {}
        host_service_map = {}

        for service in services:
            service_name = service.get('ServiceInfo', {}).get('service_name')
            service_distribution[service_name] = {
                "service_state": service.get('ServiceInfo', {}).get('state'),
                "components": {},
                "total_components": 0,
                "hosts": set()
            }

        for component in host_components:
            comp_info = component.get('HostRoles', {})
            service_name = comp_info.get('service_name')
            component_name = comp_info.get('component_name')
            host_name = comp_info.get('host_name')

            if service_name in service_distribution:
                if component_name not in service_distribution[service_name]["components"]:
                    service_distribution[service_name]["components"][component_name] = {
                        "hosts": [],
                        "count": 0
                    }

                service_distribution[service_name]["components"][component_name]["hosts"].append({
                    "host_name": host_name,
                    "state": comp_info.get('state'),
                    "desired_state": comp_info.get('desired_state')
                })
                service_distribution[service_name]["components"][component_name]["count"] += 1
                service_distribution[service_name]["total_components"] += 1
                service_distribution[service_name]["hosts"].add(host_name)

                # Build host-service mapping
                if host_name not in host_service_map:
                    host_service_map[host_name] = []
                host_service_map[host_name].append({
                    "service": service_name,
                    "component": component_name,
                    "state": comp_info.get('state')
                })

        # Convert sets to lists for JSON serialization
        for service_name in service_distribution:
            service_distribution[service_name]["hosts"] = list(service_distribution[service_name]["hosts"])
            service_distribution[service_name]["host_count"] = len(service_distribution[service_name]["hosts"])

        distribution_summary = {
            "cluster_name": cluster_name,
            "ambari_server": ambari_server,
            "service_distribution": service_distribution,
            "host_service_mapping": host_service_map,
            "summary": {
                "total_services": len(service_distribution),
                "total_unique_hosts": len(host_service_map),
                "services": list(service_distribution.keys())
            },
            "generated_at": datetime.now().isoformat()
        }

        fl.finfo(f" [INFO] User {email} fetched service distribution for {ambari_server}/{cluster_name}")
        return jsonify(distribution_summary), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/resource_utilization', methods=['GET'])
@cross_origin(supports_credentials=True)
def get_resource_utilization():
    """Get cluster-wide resource utilization metrics"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        # Get hosts data
        hosts_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/hosts", email)
        if error:
            return jsonify(error), status

        hosts = hosts_data.get('items', [])

        # Calculate resource metrics
        cluster_resources = {
            "total_memory_gb": 0,
            "available_memory_gb": 0,
            "used_memory_gb": 0,
            "total_cpu_cores": 0,
            "total_disk_gb": 0,
            "host_count": len(hosts),
            "hosts_by_utilization": [],
            "resource_distribution": {
                "high_memory_hosts": [],
                "high_cpu_hosts": [],
                "low_resource_hosts": []
            }
        }

        for host in hosts:
            host_info = host.get('Hosts', {})
            hostname = host_info.get('host_name', 'unknown')
            
            # Memory calculations
            total_mem_gb = (host_info.get('total_mem', 0) / (1024 * 1024))
            available_mem_gb = (host_info.get('available_mem', 0) / (1024 * 1024))
            used_mem_gb = total_mem_gb - available_mem_gb
            memory_util_percent = (used_mem_gb / total_mem_gb * 100) if total_mem_gb > 0 else 0

            # CPU information
            cpu_cores = host_info.get('cpu_count', 0)
            load_one = host_info.get('load_one', 0)
            cpu_util_percent = (load_one / cpu_cores * 100) if cpu_cores > 0 else 0

            # Disk information
            disk_info = host_info.get('disk_info', [])
            total_disk_gb = sum([float(disk.get('size') or 0) for disk in disk_info]) / (1024 * 1024 * 1024)

            # Aggregate cluster totals
            cluster_resources["total_memory_gb"] += total_mem_gb
            cluster_resources["available_memory_gb"] += available_mem_gb
            cluster_resources["used_memory_gb"] += used_mem_gb
            cluster_resources["total_cpu_cores"] += cpu_cores
            cluster_resources["total_disk_gb"] += total_disk_gb

            host_utilization = {
                "hostname": hostname,
                "memory": {
                    "total_gb": round(total_mem_gb, 2),
                    "used_gb": round(used_mem_gb, 2),
                    "available_gb": round(available_mem_gb, 2),
                    "utilization_percent": round(memory_util_percent, 2)
                },
                "cpu": {
                    "cores": cpu_cores,
                    "load_one": load_one,
                    "utilization_percent": round(cpu_util_percent, 2)
                },
                "disk": {
                    "total_gb": round(total_disk_gb, 2)
                }
            }

            cluster_resources["hosts_by_utilization"].append(host_utilization)

            # Categorize hosts by resource usage
            if memory_util_percent > 80:
                cluster_resources["resource_distribution"]["high_memory_hosts"].append(hostname)
            if cpu_util_percent > 80:
                cluster_resources["resource_distribution"]["high_cpu_hosts"].append(hostname)
            if total_mem_gb < 4 or cpu_cores < 2:  # Low resource threshold
                cluster_resources["resource_distribution"]["low_resource_hosts"].append(hostname)

        # Calculate cluster-wide utilization percentages
        if cluster_resources["total_memory_gb"] > 0:
            cluster_resources["cluster_memory_utilization_percent"] = round(
                (cluster_resources["used_memory_gb"] / cluster_resources["total_memory_gb"]) * 100, 2
            )

        # Round aggregate values
        cluster_resources["total_memory_gb"] = round(cluster_resources["total_memory_gb"], 2)
        cluster_resources["available_memory_gb"] = round(cluster_resources["available_memory_gb"], 2)
        cluster_resources["used_memory_gb"] = round(cluster_resources["used_memory_gb"], 2)
        cluster_resources["total_disk_gb"] = round(cluster_resources["total_disk_gb"], 2)

        utilization_report = {
            "cluster_name": cluster_name,
            "ambari_server": ambari_server,
            "resource_summary": cluster_resources,
            "generated_at": datetime.now().isoformat()
        }

        fl.finfo(f" [INFO] User {email} fetched resource utilization for {ambari_server}/{cluster_name}")
        return jsonify(utilization_report), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/analytics/cluster_health', methods=['GET'])
@cross_origin(supports_credentials=True)
def get_cluster_health():
    """Get comprehensive cluster health status including alerts and service states"""
    global fl
    try:
        ambari_server = request.args.get("ambari_server")
        cluster_name = request.args.get("cluster_name")
        email = request.headers.get("X-Email")

        if not email:
            fl.ferror(" [ERROR] Email is not provided in the request")
            return jsonify({"error": "Missing email in request headers"}), 400

        if not ambari_server or not cluster_name:
            fl.ferror(" [ERROR] Missing required parameters")
            return jsonify({"error": "Missing ambari_server or cluster_name parameter"}), 400

        # Get services
        services_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/services", email)
        if error:
            return jsonify(error), status

        # Get alerts
        alerts_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/alerts", email)
        if error:
            alerts_data = {"items": []}  # Continue without alerts if not available

        # Get hosts
        hosts_data, error, status = make_ambari_request(ambari_server, f"clusters/{cluster_name}/hosts", email)
        if error:
            return jsonify(error), status

        services = services_data.get('items', [])
        alerts = alerts_data.get('items', [])
        hosts = hosts_data.get('items', [])

        # Analyze service health
        service_health = {
            "healthy": [],
            "unhealthy": [],
            "maintenance": [],
            "unknown": []
        }

        for service in services:
            service_info = service.get('ServiceInfo', {})
            service_name = service_info.get('service_name')
            service_state = service_info.get('state', 'UNKNOWN')
            maintenance_state = service_info.get('maintenance_state', 'OFF')

            if maintenance_state != 'OFF':
                service_health["maintenance"].append(service_name)
            elif service_state == 'STARTED':
                service_health["healthy"].append(service_name)
            elif service_state in ['STOPPED', 'INSTALL_FAILED', 'START_FAILED']:
                service_health["unhealthy"].append(service_name)
            else:
                service_health["unknown"].append(service_name)

        # Analyze host health
        host_health = {
            "healthy": [],
            "unhealthy": [],
            "unknown": []
        }

        for host in hosts:
            host_info = host.get('Hosts', {})
            hostname = host_info.get('host_name')
            host_state = host_info.get('host_state', 'UNKNOWN')
            health_report = host_info.get('host_health_report', {})
            health_status = health_report.get('HostHealthStatus', {}).get('health_status', 'UNKNOWN')

            if host_state == 'HEALTHY' and health_status == 'HEALTHY':
                host_health["healthy"].append(hostname)
            elif host_state in ['UNHEALTHY', 'HEARTBEAT_LOST'] or health_status == 'UNHEALTHY':
                host_health["unhealthy"].append(hostname)
            else:
                host_health["unknown"].append(hostname)

        # Analyze alerts by severity
        alert_summary = {
            "critical": [],
            "warning": [],
            "ok": [],
            "unknown": []
        }

        for alert in alerts:
            alert_info = alert.get('Alert', {})
            alert_name = alert_info.get('definition_name', 'Unknown Alert')
            alert_state = alert_info.get('state', 'UNKNOWN')
            
            if alert_state == 'CRITICAL':
                alert_summary["critical"].append({
                    "name": alert_name,
                    "text": alert_info.get('text', ''),
                    "host": alert_info.get('host_name', '')
                })
            elif alert_state == 'WARNING':
                alert_summary["warning"].append({
                    "name": alert_name,
                    "text": alert_info.get('text', ''),
                    "host": alert_info.get('host_name', '')
                })
            elif alert_state == 'OK':
                alert_summary["ok"].append(alert_name)
            else:
                alert_summary["unknown"].append(alert_name)

        # Calculate overall health score
        total_services = len(services)
        healthy_services = len(service_health["healthy"])
        total_hosts = len(hosts)
        healthy_hosts = len(host_health["healthy"])
        total_alerts = len(alerts)
        critical_alerts = len(alert_summary["critical"])

        service_health_score = (healthy_services / total_services * 100) if total_services > 0 else 100
        host_health_score = (healthy_hosts / total_hosts * 100) if total_hosts > 0 else 100
        alert_health_score = ((total_alerts - critical_alerts) / total_alerts * 100) if total_alerts > 0 else 100

        overall_health_score = (service_health_score + host_health_score + alert_health_score) / 3

        health_report = {
            "cluster_name": cluster_name,
            "ambari_server": ambari_server,
            "overall_health_score": round(overall_health_score, 2),
            "health_status": "HEALTHY" if overall_health_score >= 80 else "WARNING" if overall_health_score >= 60 else "CRITICAL",
            "services": {
                "total": total_services,
                "health_breakdown": service_health,
                "health_score": round(service_health_score, 2)
            },
            "hosts": {
                "total": total_hosts,
                "health_breakdown": host_health,
                "health_score": round(host_health_score, 2)
            },
            "alerts": {
                "total": total_alerts,
                "breakdown": alert_summary,
                "critical_count": critical_alerts,
                "health_score": round(alert_health_score, 2)
            },
            "recommendations": [],
            "generated_at": datetime.now().isoformat()
        }

        # Add recommendations based on health analysis
        if critical_alerts > 0:
            health_report["recommendations"].append("Investigate and resolve critical alerts immediately")
        if len(service_health["unhealthy"]) > 0:
            health_report["recommendations"].append(f"Restart or investigate unhealthy services: {', '.join(service_health['unhealthy'])}")
        if len(host_health["unhealthy"]) > 0:
            health_report["recommendations"].append(f"Check unhealthy hosts: {', '.join(host_health['unhealthy'])}")
        if overall_health_score < 60:
            health_report["recommendations"].append("Cluster requires immediate attention - multiple components are unhealthy")

        fl.finfo(f" [INFO] User {email} fetched cluster health for {ambari_server}/{cluster_name}")
        return jsonify(health_report), 200

    except Exception as e:
        fl.ferror(f" [ERROR] The backend ran into error: {str(e)}")
        return jsonify({"error": str(e)}), 500




############################ DELETE A CLUSTER (Super Admin only) ###################

@app.route('/api/clusters/<path:server>', methods=['DELETE'])
@cross_origin(supports_credentials=True)
def delete_cluster(server):
    """Permanently removes a cluster from Kavach. Super admin only."""
    global fl
    try:
        email = (request.headers.get("X-Email") or "").strip().lower()
        if not email:
            return jsonify({"error": "Missing email in request headers"}), 400
        if not is_super_admin(email):
            fl.ferror(f" [SECURITY] Non-super-admin {email} attempted to delete cluster {server}")
            return jsonify({"error": "Only super admins can delete clusters."}), 403

        # Verify cluster exists
        ok, row = run_mysql_commands(
            "SELECT ambari_server, vault_password, http_method, port FROM ambari_onboarding WHERE ambari_server = %s",
            (server,), "one"
        )
        if not ok or not row:
            return jsonify({"error": f"Cluster '{server}' not found."}), 404

        vault_pwd = decrypt_password(row[1])
        http_method = row[2] or "http"
        port = row[3] or 8888

        restore_warnings = []

        # Step 1: Delete all active Kavach temp users from Ambari via vault
        ok2, active_users = run_mysql_commands(
            "SELECT user_name FROM ambari_manager_users WHERE ambari_server = %s AND pass_flag = 1",
            (server,), "all", False
        )
        deleted_count = 0
        for user_row in (active_users if ok2 and isinstance(active_users, list) else []):
            if delete_user_from_ambari(user_row[0], server, "vault", vault_pwd, http_method, port):
                deleted_count += 1

        # Mark all users as deleted in DB
        run_mysql_commands(
            "UPDATE ambari_manager_users SET pass_flag = 0, deleted_at = NOW() WHERE ambari_server = %s AND pass_flag = 1",
            (server,), "", True
        )

        # Step 2: Restore Ambari to original state — recreate admin/admin, remove vault + DR accounts
        # 2a. Create admin user with password 'admin' via vault
        admin_ok, admin_msg = create_ambari_user(
            server, "admin", "admin", "vault", vault_pwd, True, "CLUSTER.ADMINISTRATOR", http_method, port
        )
        if admin_ok:
            fl.finfo(f" [INFO] Restored admin/admin on {server}")
            # 2b. Delete ambari_admin_dr via admin/admin
            if not delete_user_from_ambari("ambari_admin_dr", server, "admin", "admin", http_method, port):
                restore_warnings.append("Could not remove ambari_admin_dr (may not exist)")
            # 2c. Delete vault via admin/admin
            if not delete_user_from_ambari("vault", server, "admin", "admin", http_method, port):
                restore_warnings.append("Could not remove vault user (may not exist)")
        else:
            fl.ferror(f" [ERROR] Failed to restore admin/admin on {server}: {admin_msg}")
            restore_warnings.append(f"Could not restore admin/admin: {admin_msg}")

        # Step 3: Remove cluster from Kavach DB
        run_mysql_commands("DELETE FROM ambari_onboarding WHERE ambari_server = %s", (server,), "", True)

        write_audit_log(email, 'CLUSTER_DELETED', server)
        msg = f"Cluster '{server}' deleted. {deleted_count} temp user(s) removed. Ambari restored to admin/admin."
        if restore_warnings:
            msg += " Warnings: " + "; ".join(restore_warnings)
        fl.finfo(f" [INFO] {msg}")
        return jsonify({"message": msg, "warnings": restore_warnings}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] delete_cluster: {str(e)}")
        return jsonify({"error": str(e)}), 500


############################ UPDATE CLUSTER MANAGERS (Super Admin only) ###################

@app.route('/api/clusters/<path:server>/managers', methods=['PUT'])
@cross_origin(supports_credentials=True)
def update_cluster_managers(server):
    """Update the per-cluster manager list. Super admin only."""
    global fl
    try:
        email = (request.headers.get("X-Email") or "").strip().lower()
        if not email:
            return jsonify({"error": "Missing email in request headers"}), 400
        if not is_super_admin(email):
            fl.ferror(f" [SECURITY] Non-super-admin {email} attempted to update managers for cluster {server}")
            return jsonify({"error": "Only super admins can update cluster managers."}), 403

        # Verify cluster exists
        ok, row = run_mysql_commands(
            "SELECT ambari_server FROM ambari_onboarding WHERE ambari_server = %s",
            (server,), "one"
        )
        if not ok or not row:
            return jsonify({"error": f"Cluster '{server}' not found."}), 404

        data = request.get_json()
        manager_emails_raw = data.get("manager_emails", [])
        single_user_mode = 1 if data.get("single_user_mode") else 0

        # Validate and deduplicate manager emails
        validated = []
        seen = set()
        for e in (manager_emails_raw or []):
            e = str(e).strip().lower()
            if not e:
                continue
            if not is_valid_email(e):
                return jsonify({"error": f"Invalid email format: '{e}'"}), 400
            if e not in seen:
                seen.add(e)
                validated.append(e)

        manager_emails_json = json.dumps(validated)

        # Try full UPDATE (manager_emails + single_user_mode); auto-migrate missing columns
        ok2, _ = run_mysql_commands(
            "UPDATE ambari_onboarding SET manager_emails = %s, single_user_mode = %s WHERE ambari_server = %s",
            (manager_emails_json, single_user_mode, server), "", True
        )
        if not ok2:
            fl.fwarn(" [WARN] Columns missing — running auto-migration and retrying")
            run_mysql_commands("ALTER TABLE ambari_onboarding ADD COLUMN manager_emails TEXT DEFAULT NULL", None, "", True)
            run_mysql_commands("ALTER TABLE ambari_onboarding ADD COLUMN single_user_mode TINYINT(1) DEFAULT 0", None, "", True)
            ok2, _ = run_mysql_commands(
                "UPDATE ambari_onboarding SET manager_emails = %s, single_user_mode = %s WHERE ambari_server = %s",
                (manager_emails_json, single_user_mode, server), "", True
            )
            if not ok2:
                return jsonify({"error": "Database update failed after migration. Please check server logs."}), 500

        write_audit_log(email, 'CLUSTER_MANAGERS_UPDATED', server)
        fl.finfo(f" [INFO] Cluster '{server}' settings updated by {email}: managers={validated}, single_user_mode={single_user_mode}")
        return jsonify({"message": "Cluster settings updated successfully.", "manager_emails": validated, "single_user_mode": bool(single_user_mode)}), 200

    except Exception as e:
        fl.ferror(f" [ERROR] update_cluster_managers: {str(e)}")
        return jsonify({"error": str(e)}), 500


# --- JWT Configuration ---
CORS(app)
_jwt_secret = os.environ.get("JWT_SECRET_KEY")
if not _jwt_secret:
    raise RuntimeError(
        "JWT_SECRET_KEY environment variable is not set. "
        "Generate one with: python3 -c \"import secrets; print(secrets.token_hex(32))\""
    )
app.config["JWT_SECRET_KEY"] = _jwt_secret
app.config["JWT_ACCESS_TOKEN_EXPIRES"] = timedelta(hours=1)
jwt = JWTManager(app)

# --- JWT Authentication Middleware ---
# Public paths that do not require a valid JWT token
_JWT_PUBLIC_PATHS = {'/auth/google-login', '/api/health', '/api/test_connection'}

@app.before_request
def _verify_jwt_auth():
    """Verify JWT on all protected routes and ensure X-Email header matches the token identity."""
    if flask.request.method == 'OPTIONS':
        return None  # Allow CORS preflight through
    if flask.request.path in _JWT_PUBLIC_PATHS:
        return None  # Public endpoints
    # Only protect API-level paths
    is_protected = (
        flask.request.path.startswith('/api/') or
        flask.request.path == '/create_user' or
        flask.request.path.startswith('/manager/')
    )
    if not is_protected:
        return None
    try:
        verify_jwt_in_request()
        token_email = get_jwt_identity()
        flask.g.jwt_email = token_email
    except Exception:
        return jsonify({"error": "Authentication required. Please log in again."}), 401
    # Verify X-Email header (if present) matches JWT identity to prevent spoofing
    header_email = (flask.request.headers.get("X-Email") or "").strip().lower()
    if header_email and token_email and header_email != token_email.lower():
        fl.ferror(f" [SECURITY] X-Email header ({header_email}) does not match JWT identity ({token_email})")
        return jsonify({"error": "Identity mismatch: token and email header do not match"}), 403

# --- SPA fallback: serve frontend for non-API routes (when frontend dist exists) ---
_FRONTEND_DIST = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "ambari-kavach-frontend", "dist"))

@app.route("/", defaults={"path": ""})
@app.route("/<path:path>")
def serve_spa(path):
    """Serve frontend SPA for non-API routes when dist exists."""
    if path.startswith("api/") or path.startswith("auth/") or path == "create_user" or path.startswith("manager/"):
        return jsonify({"error": "Not Found"}), 404
    if os.path.isdir(_FRONTEND_DIST):
        if path and os.path.exists(os.path.join(_FRONTEND_DIST, path)):
            return send_from_directory(_FRONTEND_DIST, path)
        return send_from_directory(_FRONTEND_DIST, "index.html")
    return jsonify({"error": "Not Found"}), 404

# Start background scheduler for expired user cleanup (runs in both dev and production)
_scheduler = BackgroundScheduler()
_scheduler.add_job(remove_expired_users, "interval", minutes=1)
_scheduler.start()

if __name__ == '__main__':
    try:
        app.run(host="0.0.0.0", port=5000)
    finally:
        _scheduler.shutdown()