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

from contextlib import nullcontext
import math
import re
import socket
import time

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


ZEPPELIN_PORT_KEY = "{{zeppelin-site/zeppelin.server.port}}"
ZEPPELIN_PORT_SSL_KEY = "{{zeppelin-site/zeppelin.server.ssl.port}}"
SECURITY_ENABLED_KEY = "{{cluster-env/security_enabled}}"
ZEPPELIN_KEYTAB_KEY = "{{zeppelin-site/zeppelin.server.kerberos.keytab}}"
ZEPPELIN_PRINCIPAL_KEY = "{{zeppelin-site/zeppelin.server.kerberos.principal}}"
ZEPPELIN_USER_KEY = "{{zeppelin-env/zeppelin_user}}"
ZEPPELIN_GROUP_KEY = "{{zeppelin-env/zeppelin_group}}"
UI_SSL_ENABLED_KEY = "{{zeppelin-site/zeppelin.ssl}}"
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = "{{kerberos-env/executable_search_paths}}"
CHECK_COMMAND_TIMEOUT_KEY = "check.command.timeout"
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0
_HOST_PATTERN = re.compile(r"[A-Za-z0-9_.:-]+", re.ASCII)

RESULT_CODE_OK = "OK"
RESULT_CODE_CRITICAL = "CRITICAL"
RESULT_CODE_UNKNOWN = "UNKNOWN"


def _parse_boolean(value, name):
  normalized = str(value).strip().lower()
  if normalized not in ("true", "false"):
    raise ValueError(f"{name} must be true or false")
  return normalized == "true"


def _build_url(ssl_enabled, host, port):
  host = str(host).strip()
  if host.startswith("[") and host.endswith("]"):
    host = host[1:-1]
  if not host or not _HOST_PATTERN.fullmatch(host):
    raise ValueError(f"invalid Zeppelin server host: {host!r}")
  port = int(port)
  if not 0 < port <= 65535:
    raise ValueError("Zeppelin server port must be between 1 and 65535")
  url_host = f"[{host}]" if ":" in host else host
  scheme = "https" if ssl_enabled else "http"
  return f"{scheme}://{url_host}:{port}/api/version"


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  return (
    ZEPPELIN_USER_KEY,
    ZEPPELIN_GROUP_KEY,
    UI_SSL_ENABLED_KEY,
    SECURITY_ENABLED_KEY,
    ZEPPELIN_KEYTAB_KEY,
    ZEPPELIN_PRINCIPAL_KEY,
    KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
    ZEPPELIN_PORT_KEY,
    ZEPPELIN_PORT_SSL_KEY,
  )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations=None, parameters=None, host_name=None):
  if configurations is None:
    return (RESULT_CODE_UNKNOWN, ["No configurations were supplied to the script"])
  parameters = parameters or {}

  try:
    zeppelin_user = str(configurations.get(ZEPPELIN_USER_KEY, "")).strip()
    zeppelin_group = str(configurations.get(ZEPPELIN_GROUP_KEY, "")).strip()
    if not zeppelin_user or not zeppelin_group:
      raise ValueError("Zeppelin user and group are required")

    ssl_enabled = _parse_boolean(
      configurations.get(UI_SSL_ENABLED_KEY, "false"), "zeppelin.ssl"
    )
    security_enabled = _parse_boolean(
      configurations.get(SECURITY_ENABLED_KEY, "false"), "security_enabled"
    )
    port_key = ZEPPELIN_PORT_SSL_KEY if ssl_enabled else ZEPPELIN_PORT_KEY
    port = configurations.get(port_key)
    target_host = host_name or socket.getfqdn()
    url = _build_url(ssl_enabled, target_host, port)

    check_timeout = float(
      parameters.get(CHECK_COMMAND_TIMEOUT_KEY, CHECK_COMMAND_TIMEOUT_DEFAULT)
    )
    if not math.isfinite(check_timeout) or check_timeout <= 0:
      raise ValueError("check timeout must be a positive finite number")

    cache_context = nullcontext(None)
    keytab = None
    principal = None
    kinit_path = None
    if security_enabled:
      keytab = configurations.get(ZEPPELIN_KEYTAB_KEY)
      principal = configurations.get(ZEPPELIN_PRINCIPAL_KEY)
      if not all(str(value or "").strip() for value in (keytab, principal)):
        raise ValueError(
          "secure Zeppelin alert requires a service principal and keytab"
        )
      principal = principal.replace("_HOST", str(target_host).lower())
      kinit_path = get_kinit_path(
        configurations.get(KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY)
      )
      cache_context = PrivateKerberosCache(
        zeppelin_user,
        zeppelin_group,
        prefix="ambari-zeppelin-alert-",
      )
  except Exception as error:
    return (RESULT_CODE_UNKNOWN, [f"Invalid Zeppelin alert configuration: {error}"])

  start_time = time.monotonic()
  try:
    with cache_context as kerberos_cache:
      environment = None
      if kerberos_cache is not None:
        kerberos_cache.kinit(kinit_path, keytab, principal, timeout=30)
        environment = kerberos_cache.environment

      command = [
        "curl",
        "--disable",
        "--silent",
        "--show-error",
        "--fail",
        "--output",
        "/dev/null",
        "--connect-timeout",
        str(min(5.0, check_timeout)),
        "--max-time",
        str(check_timeout),
      ]
      if security_enabled:
        command.extend(("--negotiate", "--user", ":"))
      command.extend(("--url", url))

      execute_options = {}
      if environment is not None:
        execute_options["environment"] = environment
      Execute(
        tuple(command),
        timeout=check_timeout + 5,
        user=zeppelin_user,
        logoutput=True,
        **execute_options,
      )
  except Exception as error:
    return (RESULT_CODE_CRITICAL, [f"Zeppelin request failed: {error}"])

  elapsed = time.monotonic() - start_time
  return (RESULT_CODE_OK, [f"Zeppelin responded in {elapsed:.3f}s"])
