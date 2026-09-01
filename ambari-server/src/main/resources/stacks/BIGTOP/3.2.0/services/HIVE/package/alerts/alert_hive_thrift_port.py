#!/usr/bin/env python3

"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import logging
import socket
import time
import traceback

from resource_management.core import shell
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.core.utils import PasswordString
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

OK_MESSAGE = "HiveServer2 OK - query completed in {0:.3f}s on port {1}"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1} ({2})"

HIVE_SERVER_THRIFT_PORT_KEY = "{{hive-site/hive.server2.thrift.port}}"
HIVE_SERVER_THRIFT_HTTP_PORT_KEY = "{{hive-site/hive.server2.thrift.http.port}}"
HIVE_SERVER_TRANSPORT_MODE_KEY = "{{hive-site/hive.server2.transport.mode}}"
SECURITY_ENABLED_KEY = "{{cluster-env/security_enabled}}"
HIVE_SERVER2_AUTHENTICATION_KEY = "{{hive-site/hive.server2.authentication}}"
HIVE_SERVER_PRINCIPAL_KEY = (
  "{{hive-site/hive.server2.authentication.kerberos.principal}}"
)
SMOKEUSER_KEYTAB_KEY = "{{cluster-env/smokeuser_keytab}}"
SMOKEUSER_PRINCIPAL_KEY = "{{cluster-env/smokeuser_principal_name}}"
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
HIVE_SSL = "{{hive-site/hive.server2.use.SSL}}"
HIVE_SSL_KEYSTORE_PATH = "{{hive-site/hive.server2.keystore.path}}"
HIVE_SSL_KEYSTORE_PASSWORD = "{{hive-site/hive.server2.keystore.password}}"
HIVE_LDAP_USERNAME = "{{hive-env/alert_ldap_username}}"
HIVE_LDAP_PASSWORD = "{{hive-env/alert_ldap_password}}"
HIVE_PAM_USERNAME = "{{hive-env/alert_pam_username}}"
HIVE_PAM_PASSWORD = "{{hive-env/alert_pam_password}}"
HIVE_CUSTOM_USERNAME = "{{hive-env/alert_custom_username}}"
HIVE_CUSTOM_PASSWORD = "{{hive-env/alert_custom_password}}"


# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = "{{kerberos-env/executable_search_paths}}"

THRIFT_PORT_DEFAULT = 10000
HIVE_SERVER_TRANSPORT_MODE_DEFAULT = "binary"
HIVE_SERVER_PRINCIPAL_DEFAULT = "hive/_HOST@EXAMPLE.COM"
HIVE_SERVER2_AUTHENTICATION_DEFAULT = "NOSASL"

# default keytab location
SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY = "default.smoke.keytab"
SMOKEUSER_KEYTAB_DEFAULT = "/etc/security/keytabs/smokeuser.headless.keytab"

# default smoke principal
SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY = "default.smoke.principal"
SMOKEUSER_PRINCIPAL_DEFAULT = "ambari-qa@EXAMPLE.COM"

# default smoke user
SMOKEUSER_SCRIPT_PARAM_KEY = "default.smoke.user"
SMOKEUSER_DEFAULT = "ambari-qa"

HIVE_USER_KEY = "{{hive-env/hive_user}}"
HIVE_USER_DEFAULT = "hive"

CHECK_COMMAND_TIMEOUT_KEY = "check.command.timeout"
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

logger = logging.getLogger("ambari_alerts")


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (
    HIVE_SERVER_THRIFT_PORT_KEY,
    SECURITY_ENABLED_KEY,
    SMOKEUSER_KEY,
    HIVE_SERVER2_AUTHENTICATION_KEY,
    HIVE_SERVER_PRINCIPAL_KEY,
    SMOKEUSER_KEYTAB_KEY,
    SMOKEUSER_PRINCIPAL_KEY,
    HIVE_SERVER_THRIFT_HTTP_PORT_KEY,
    HIVE_SERVER_TRANSPORT_MODE_KEY,
    KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
    HIVE_SSL,
    HIVE_SSL_KEYSTORE_PATH,
    HIVE_SSL_KEYSTORE_PASSWORD,
    HIVE_LDAP_USERNAME,
    HIVE_LDAP_PASSWORD,
    HIVE_USER_KEY,
    HIVE_PAM_USERNAME,
    HIVE_PAM_PASSWORD,
    HIVE_CUSTOM_USERNAME,
    HIVE_CUSTOM_PASSWORD,
  )


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return ("UNKNOWN", ["There were no configurations supplied to the script."])

  transport_mode = str(
    configurations.get(
      HIVE_SERVER_TRANSPORT_MODE_KEY, HIVE_SERVER_TRANSPORT_MODE_DEFAULT
    )
  ).strip().lower()
  if transport_mode not in ("binary", "http"):
    return ("UNKNOWN", [f"Unsupported HiveServer2 transport mode: {transport_mode}"])

  try:
    port_key = (
      HIVE_SERVER_THRIFT_HTTP_PORT_KEY
      if transport_mode == "http"
      else HIVE_SERVER_THRIFT_PORT_KEY
    )
    port = int(configurations.get(port_key, THRIFT_PORT_DEFAULT))
    check_command_timeout = float(
      parameters.get(CHECK_COMMAND_TIMEOUT_KEY, CHECK_COMMAND_TIMEOUT_DEFAULT)
    )
  except (TypeError, ValueError) as exception:
    return ("UNKNOWN", [f"Invalid HiveServer2 alert configuration: {exception}"])
  if not 1 <= port <= 65535 or check_command_timeout <= 0:
    return ("UNKNOWN", ["HiveServer2 alert port and timeout must be positive"])

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == "TRUE"

  hive_server2_authentication = str(
    configurations.get(
      HIVE_SERVER2_AUTHENTICATION_KEY, HIVE_SERVER2_AUTHENTICATION_DEFAULT
    )
  ).strip().upper()
  if hive_server2_authentication not in (
    "CUSTOM",
    "KERBEROS",
    "LDAP",
    "NONE",
    "NOSASL",
    "PAM",
  ):
    return (
      "UNKNOWN",
      [f"Unsupported HiveServer2 authentication: {hive_server2_authentication}"],
    )
  if hive_server2_authentication == "KERBEROS" and not security_enabled:
    return (
      "UNKNOWN",
      ["Kerberos HiveServer2 authentication requires cluster security"],
    )

  hive_ssl = False
  if HIVE_SSL in configurations:
    hive_ssl = str(configurations[HIVE_SSL]).strip().lower() == "true"

  hive_ssl_keystore_path = None
  if HIVE_SSL_KEYSTORE_PATH in configurations:
    hive_ssl_keystore_path = configurations[HIVE_SSL_KEYSTORE_PATH]

  hive_ssl_keystore_password = None
  if HIVE_SSL_KEYSTORE_PASSWORD in configurations:
    hive_ssl_keystore_password = configurations[HIVE_SSL_KEYSTORE_PASSWORD]

  # defaults
  smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
  smokeuser_principal = SMOKEUSER_PRINCIPAL_DEFAULT
  smokeuser = SMOKEUSER_DEFAULT

  # check script params
  if SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY in parameters:
    smokeuser_principal = parameters[SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY]

  if SMOKEUSER_SCRIPT_PARAM_KEY in parameters:
    smokeuser = parameters[SMOKEUSER_SCRIPT_PARAM_KEY]

  if SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY in parameters:
    smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY]

  # check configurations last as they should always take precedence
  if SMOKEUSER_PRINCIPAL_KEY in configurations:
    smokeuser_principal = configurations[SMOKEUSER_PRINCIPAL_KEY]

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  hive_user = HIVE_USER_DEFAULT
  if HIVE_USER_KEY in configurations:
    hive_user = configurations[HIVE_USER_KEY]

  ldap_username = ""
  ldap_password = ""
  if HIVE_LDAP_USERNAME in configurations:
    ldap_username = configurations[HIVE_LDAP_USERNAME]
  if HIVE_LDAP_PASSWORD in configurations:
    ldap_password = configurations[HIVE_LDAP_PASSWORD]

  pam_username = ""
  pam_password = ""
  if HIVE_PAM_USERNAME in configurations:
    pam_username = configurations[HIVE_PAM_USERNAME]
  if HIVE_PAM_PASSWORD in configurations:
    pam_password = configurations[HIVE_PAM_PASSWORD]

  custom_username = ""
  custom_password = ""
  if HIVE_CUSTOM_USERNAME in configurations:
    custom_username = configurations[HIVE_CUSTOM_USERNAME]
  if HIVE_CUSTOM_PASSWORD in configurations:
    custom_password = configurations[HIVE_CUSTOM_PASSWORD]

  credentials = {
    "LDAP": (ldap_username, ldap_password),
    "PAM": (pam_username, pam_password),
    "CUSTOM": (custom_username, custom_password),
  }.get(hive_server2_authentication)
  if credentials is not None and (
    not str(credentials[0]).strip() or not str(credentials[1])
  ):
    return (
      "UNKNOWN",
      [f"{hive_server2_authentication} alert checks require credentials"],
    )

  result_code = None

  if security_enabled:
    hive_server_principal = HIVE_SERVER_PRINCIPAL_DEFAULT
    if HIVE_SERVER_PRINCIPAL_KEY in configurations:
      hive_server_principal = configurations[HIVE_SERVER_PRINCIPAL_KEY]

    if SMOKEUSER_KEYTAB_KEY in configurations:
      smokeuser_keytab = configurations[SMOKEUSER_KEYTAB_KEY]

    # Get the configured Kerberos executable search paths, if any
    if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
      kerberos_executable_search_paths = configurations[
        KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY
      ]
    else:
      kerberos_executable_search_paths = None

    kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
  else:
    hive_server_principal = None

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    start_time = time.time()

    try:
      environment = None
      if security_enabled and hive_server2_authentication == "KERBEROS":
        with PrivateKerberosCache(
          smokeuser,
          temp_dir="/tmp",
          prefix="ambari-hive-thrift-alert-",
        ) as cache:
          cache.kinit(
            kinit_path_local,
            smokeuser_keytab,
            smokeuser_principal,
          )
          _run_beeline_alert(
            host_name,
            port,
            smokeuser,
            hive_user,
            hive_server2_authentication,
            hive_server_principal,
            transport_mode,
            hive_ssl,
            hive_ssl_keystore_path,
            hive_ssl_keystore_password,
            ldap_username,
            ldap_password,
            pam_username,
            pam_password,
            custom_username,
            custom_password,
            int(check_command_timeout),
            cache.environment,
          )
      else:
        _run_beeline_alert(
          host_name,
          port,
          smokeuser,
          hive_user,
          hive_server2_authentication,
          hive_server_principal,
          transport_mode,
          hive_ssl,
          hive_ssl_keystore_path,
          hive_ssl_keystore_password,
          ldap_username,
          ldap_password,
          pam_username,
          pam_password,
          custom_username,
          custom_password,
          int(check_command_timeout),
          environment,
        )
      result_code = "OK"
      total_time = time.time() - start_time
      label = OK_MESSAGE.format(total_time, port)
    except Exception:
      result_code = "CRITICAL"
      label = CRITICAL_MESSAGE.format(host_name, port, traceback.format_exc())

  except Exception as e:
    label = str(e)
    result_code = "UNKNOWN"

  return (result_code, [label])


def _run_beeline_alert(
  host,
  port,
  smokeuser,
  hive_user,
  authentication,
  principal,
  transport_mode,
  ssl_enabled,
  ssl_keystore,
  ssl_password,
  ldap_username,
  ldap_password,
  pam_username,
  pam_password,
  custom_username,
  custom_password,
  timeout,
  environment,
):
  properties = [f"transportMode={transport_mode}"]
  if transport_mode.lower() == "http":
    properties.append("httpPath=cliservice")
  if authentication == "NOSASL":
    properties.append("auth=noSasl")
  elif principal:
    properties.append(f"principal={principal}")
  if ssl_enabled:
    properties.extend(
      (
        "ssl=true",
        f"sslTrustStore={ssl_keystore}",
        f"trustStorePassword={ssl_password}",
      )
    )

  url = f"jdbc:hive2://{host}:{port}/;" + ";".join(properties)
  command = [
    "/usr/lib/hive/bin/beeline",
    "-u",
    PasswordString(url) if ssl_enabled else url,
  ]
  if authentication == "LDAP":
    command.extend(("-n", ldap_username, "-p", PasswordString(ldap_password)))
  elif authentication == "PAM":
    command.extend(("-n", pam_username, "-p", PasswordString(pam_password)))
  elif authentication == "CUSTOM":
    command.extend(("-n", custom_username, "-p", PasswordString(custom_password)))
  else:
    command.extend(("-n", hive_user))
  command.extend(("-e", "show databases"))
  shell.checked_call(
    tuple(command),
    user=smokeuser,
    env=environment,
    timeout=timeout,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_TREE,
  )
