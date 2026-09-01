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

import json
import math
import re
import socket
import urllib.error
import urllib.request
import logging
import traceback
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import (
  DEFAULT_KERBEROS_KINIT_TIMER_MS,
)
from resource_management.libraries.functions.curl_krb_request import (
  KERBEROS_KINIT_TIMER_PARAMETER,
)
from resource_management.core.environment import Environment

RESULT_CODE_OK = "OK"
RESULT_CODE_CRITICAL = "CRITICAL"
RESULT_CODE_UNKNOWN = "UNKNOWN"

NODEMANAGER_HTTP_ADDRESS_KEY = "{{yarn-site/yarn.nodemanager.webapp.address}}"
NODEMANAGER_HTTPS_ADDRESS_KEY = "{{yarn-site/yarn.nodemanager.webapp.https.address}}"
YARN_HTTP_POLICY_KEY = "{{yarn-site/yarn.http.policy}}"

OK_MESSAGE = "NodeManager Healthy"
CRITICAL_CONNECTION_MESSAGE = "Connection failed to {0} ({1})"
CRITICAL_HTTP_STATUS_MESSAGE = "HTTP {0} returned from {1} ({2}) \n{3}"
CRITICAL_NODEMANAGER_STATUS_MESSAGE = (
  'NodeManager returned an unexpected status of "{0}"'
)
KERBEROS_KEYTAB = "{{cluster-env/smokeuser_keytab}}"
KERBEROS_PRINCIPAL = "{{cluster-env/smokeuser_principal_name}}"
SECURITY_ENABLED_KEY = "{{cluster-env/security_enabled}}"
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
EXECUTABLE_SEARCH_PATHS = "{{kerberos-env/executable_search_paths}}"

CONNECTION_TIMEOUT_KEY = "connection.timeout"
CONNECTION_TIMEOUT_DEFAULT = 5.0

logger = logging.getLogger("ambari_alerts")


def _parse_boolean(value, label):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise ValueError(f"{label} must be true or false")


def _valid_host_port(value):
  if not isinstance(value, str):
    return False
  host, separator, port = value.strip().rpartition(":")
  if not separator or not host:
    return False
  if ":" in host and not (host.startswith("[") and host.endswith("]")):
    return False
  if re.fullmatch(r"[0-9]+", port) is None:
    return False
  port_number = int(port)
  return 1 <= port_number <= 65535


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (
    NODEMANAGER_HTTP_ADDRESS_KEY,
    NODEMANAGER_HTTPS_ADDRESS_KEY,
    EXECUTABLE_SEARCH_PATHS,
    YARN_HTTP_POLICY_KEY,
    SMOKEUSER_KEY,
    KERBEROS_KEYTAB,
    KERBEROS_PRINCIPAL,
    SECURITY_ENABLED_KEY,
  )


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  result_code = RESULT_CODE_UNKNOWN

  if configurations is None:
    return (result_code, ["There were no configurations supplied to the script."])

  if host_name is None:
    host_name = socket.getfqdn()

  scheme = "http"
  http_uri = None
  https_uri = None
  http_policy = "HTTP_ONLY"

  smokeuser = configurations.get(SMOKEUSER_KEY)

  executable_paths = None
  if EXECUTABLE_SEARCH_PATHS in configurations:
    executable_paths = configurations[EXECUTABLE_SEARCH_PATHS]

  try:
    security_enabled = _parse_boolean(
      configurations.get(SECURITY_ENABLED_KEY, False),
      "cluster-env/security_enabled",
    )
  except ValueError as error:
    return (RESULT_CODE_UNKNOWN, [str(error)])

  kerberos_keytab = None
  if KERBEROS_KEYTAB in configurations:
    kerberos_keytab = configurations[KERBEROS_KEYTAB]

  kerberos_principal = None
  if KERBEROS_PRINCIPAL in configurations:
    kerberos_principal = configurations[KERBEROS_PRINCIPAL]

  if NODEMANAGER_HTTP_ADDRESS_KEY in configurations:
    http_uri = configurations[NODEMANAGER_HTTP_ADDRESS_KEY]

  if NODEMANAGER_HTTPS_ADDRESS_KEY in configurations:
    https_uri = configurations[NODEMANAGER_HTTPS_ADDRESS_KEY]

  if YARN_HTTP_POLICY_KEY in configurations:
    http_policy = configurations[YARN_HTTP_POLICY_KEY]

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  try:
    if CONNECTION_TIMEOUT_KEY in parameters:
      connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])
  except (TypeError, ValueError):
    return (RESULT_CODE_CRITICAL, ["Connection timeout must be a number"])
  if not math.isfinite(connection_timeout) or connection_timeout <= 0:
    return (RESULT_CODE_CRITICAL, ["Connection timeout must be positive and finite"])

  # determine the right URI and whether to use SSL
  http_policy = str(http_policy).strip().upper()
  if http_policy not in ("HTTP_ONLY", "HTTPS_ONLY"):
    return (
      RESULT_CODE_UNKNOWN,
      [f"Unsupported yarn.http.policy value: {http_policy or '<empty>'}"],
    )
  if http_policy == "HTTPS_ONLY":
    scheme = "https"
    host_port = https_uri
  else:
    host_port = http_uri
  if not _valid_host_port(host_port):
    return (
      RESULT_CODE_UNKNOWN,
      [f"NodeManager {scheme.upper()} web application address is missing"],
    )
  host_port = host_port.strip()

  label = ""
  url_response = None
  node_healthy = "false"

  # replace hostname on host fqdn to make it work on all environments
  if host_port is not None:
    if ":" in host_port:
      _, _, uri_port = host_port.rpartition(":")
      url_host = f"[{host_name}]" if ":" in host_name else host_name
      host_port = f"{url_host}:{uri_port}"
    else:
      host_port = host_name

  query = f"{scheme}://{host_port}/ws/v1/node/info"

  try:
    try:
      if security_enabled:
        if not kerberos_principal or not kerberos_keytab or not smokeuser:
          return (
            RESULT_CODE_CRITICAL,
            ["Kerberos principal, keytab, and smoke user are required"],
          )
        env = Environment.get_instance()
        kinit_timer_ms = parameters.get(
          KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS
        )
        url_response, _, _ = curl_krb_request(
          env.tmp_dir,
          kerberos_keytab,
          kerberos_principal,
          query,
          "nm_health_alert",
          executable_paths,
          False,
          "NodeManager Health",
          smokeuser,
          connection_timeout=connection_timeout,
          kinit_timer_ms=kinit_timer_ms,
        )
        json_response = json.loads(url_response)
      else:
        url_response = urllib.request.urlopen(query, timeout=connection_timeout)
        json_response = json.loads(url_response.read())
    except urllib.error.HTTPError as http_error:
      label = CRITICAL_HTTP_STATUS_MESSAGE.format(
        str(http_error.code), query, str(http_error), traceback.format_exc()
      )
      return (RESULT_CODE_CRITICAL, [label])
    except Exception:
      label = CRITICAL_CONNECTION_MESSAGE.format(query, traceback.format_exc())
      return (RESULT_CODE_CRITICAL, [label])

    try:
      node_healthy = str(json_response["nodeInfo"]["nodeHealthy"])
      node_healthy_report = json_response["nodeInfo"]["healthReport"]
    except Exception:
      return (RESULT_CODE_CRITICAL, [query + "\n" + traceback.format_exc()])
  finally:
    if hasattr(url_response, "close"):
      try:
        url_response.close()
      except Exception:
        logger.debug("Could not close NodeManager health response", exc_info=True)

  # proper JSON received, compare against known value
  if node_healthy.lower() == "true":
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE
  elif node_healthy.lower() == "false":
    result_code = RESULT_CODE_CRITICAL
    label = node_healthy_report
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_NODEMANAGER_STATUS_MESSAGE.format(node_healthy)

  return (result_code, [label])
