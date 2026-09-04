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
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


OK_MESSAGE = "HTTP OK - {0:.3f}s response on port {1}"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1} ({2})"

LIVY_SERVER_HOST_KEY = "{{livy-conf/livy.server.host}}"
LIVY_SERVER_PORT_KEY = "{{livy-conf/livy.server.port}}"
SECURITY_ENABLED_KEY = "{{cluster-env/security_enabled}}"
SMOKEUSER_KEYTAB_KEY = "{{cluster-env/smokeuser_keytab}}"
SMOKEUSER_PRINCIPAL_KEY = "{{cluster-env/smokeuser_principal_name}}"
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
USER_GROUP_KEY = "{{cluster-env/user_group}}"
LIVY_KEYSTORE_KEY = "{{livy-conf/livy.keystore}}"
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = (
  "{{kerberos-env/executable_search_paths}}"
)

CHECK_COMMAND_TIMEOUT_KEY = "check.command.timeout"
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0
_HOST_PATTERN = re.compile(r"[A-Za-z0-9_.:-]+", re.ASCII)


def _livy_url(scheme, host, port):
  host = str(host).strip()
  if host.startswith("[") and host.endswith("]"):
    host = host[1:-1]
  if not host or not _HOST_PATTERN.fullmatch(host):
    raise Fail(f"Invalid Livy server host: {host!r}")
  url_host = f"[{host}]" if ":" in host else host
  return f"{scheme}://{url_host}:{port}/sessions"


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  return (
    LIVY_SERVER_HOST_KEY,
    LIVY_SERVER_PORT_KEY,
    SECURITY_ENABLED_KEY,
    SMOKEUSER_KEYTAB_KEY,
    SMOKEUSER_PRINCIPAL_KEY,
    SMOKEUSER_KEY,
    USER_GROUP_KEY,
    LIVY_KEYSTORE_KEY,
    KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
  )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations=None, parameters=None, host_name=None):
  if configurations is None:
    return ("UNKNOWN", ["There were no configurations supplied to the script."])
  parameters = parameters or {}

  try:
    port = int(configurations.get(LIVY_SERVER_PORT_KEY, 8999))
    if not 0 < port <= 65535:
      raise ValueError("port must be between 1 and 65535")

    check_timeout = float(
      parameters.get(
        CHECK_COMMAND_TIMEOUT_KEY, CHECK_COMMAND_TIMEOUT_DEFAULT
      )
    )
    if not math.isfinite(check_timeout) or check_timeout <= 0:
      raise ValueError("check timeout must be a positive finite number")

    target_host = configurations.get(LIVY_SERVER_HOST_KEY) or host_name
    target_host = target_host or socket.getfqdn()
    if str(target_host).strip() in ("0.0.0.0", "::", "[::]"):
      target_host = socket.getfqdn()
    smoke_user = configurations.get(SMOKEUSER_KEY)
    if not str(smoke_user or "").strip():
      raise ValueError("smoke user is not configured")
    user_group = configurations.get(USER_GROUP_KEY)
    if not str(user_group or "").strip():
      raise ValueError("cluster user group is not configured")

    security_enabled = (
      str(configurations.get(SECURITY_ENABLED_KEY, "false")).strip().lower()
      == "true"
    )
    scheme = (
      "https"
      if str(configurations.get(LIVY_KEYSTORE_KEY, "")).strip()
      else "http"
    )
    livy_url = _livy_url(scheme, target_host, port)

    cache_context = nullcontext(None)
    kinit_path = None
    smoke_keytab = None
    smoke_principal = None
    if security_enabled:
      smoke_keytab = configurations.get(SMOKEUSER_KEYTAB_KEY)
      smoke_principal = configurations.get(SMOKEUSER_PRINCIPAL_KEY)
      if not all(
        str(value or "").strip()
        for value in (smoke_keytab, smoke_principal)
      ):
        raise ValueError(
          "secure alert requires a smoke user principal and keytab"
        )
      smoke_principal = smoke_principal.replace(
        "_HOST", socket.getfqdn().lower()
      )
      kinit_path = get_kinit_path(
        configurations.get(KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY)
      )
      cache_context = PrivateKerberosCache(
        smoke_user,
        user_group,
        prefix="ambari-livy-alert-",
      )
  except Exception as error:
    return ("UNKNOWN", [f"Invalid Livy alert configuration: {error}"])

  start_time = time.monotonic()
  try:
    with cache_context as kerberos_cache:
      kerberos_environment = None
      if kerberos_cache is not None:
        kerberos_environment = kerberos_cache.environment
        kerberos_cache.kinit(
          kinit_path,
          smoke_keytab,
          smoke_principal,
          timeout=30,
        )

      curl_argv = [
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
        curl_argv.extend(("--negotiate", "--user", ":"))
      curl_argv.extend(("--url", livy_url))

      execute_options = {}
      if kerberos_environment is not None:
        execute_options["environment"] = kerberos_environment
      Execute(
        tuple(curl_argv),
        timeout=check_timeout + 5,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        logoutput=True,
        user=smoke_user,
        **execute_options,
      )
  except Exception as error:
    return (
      "CRITICAL",
      [CRITICAL_MESSAGE.format(target_host, port, str(error))],
    )

  total_time = time.monotonic() - start_time
  return ("OK", [OK_MESSAGE.format(total_time, port)])
