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

import os
import re
import socket
import time

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core import sudo
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


PORT_KEY = "{{spark-hive-site-override/hive.server2.thrift.port}}"
HTTP_PORT_KEY = "{{spark-hive-site-override/hive.server2.thrift.http.port}}"
TRANSPORT_KEY = "{{spark-hive-site-override/hive.server2.transport.mode}}"
ENDPOINT_KEY = "{{spark-hive-site-override/hive.server2.thrift.http.path}}"
SSL_KEY = "{{spark-hive-site-override/hive.server2.use.SSL}}"
SECURITY_KEY = "{{cluster-env/security_enabled}}"
SEARCH_PATHS_KEY = "{{kerberos-env/executable_search_paths}}"
SPARK_USER_KEY = "{{spark-env/spark_user}}"
KEYTAB_KEY = "{{spark-hive-site-override/hive.server2.authentication.kerberos.keytab}}"
PRINCIPAL_KEY = "{{spark-hive-site-override/hive.server2.authentication.kerberos.principal}}"
TIMEOUT_KEY = "check.command.timeout"
_HOST_PATTERN = re.compile(r"[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?", re.ASCII)
_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  return (
    PORT_KEY,
    HTTP_PORT_KEY,
    TRANSPORT_KEY,
    ENDPOINT_KEY,
    SSL_KEY,
    SECURITY_KEY,
    SEARCH_PATHS_KEY,
    SPARK_USER_KEY,
    KEYTAB_KEY,
    PRINCIPAL_KEY,
  )


def _boolean(value):
  return str(value).strip().lower() == "true"


def _bounded_port(value):
  port = int(value)
  if not 1 <= port <= 65535:
    raise ValueError("port is outside 1..65535")
  return port


def _regular_file(path, name, executable=False):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
    or _PATH_PATTERN.fullmatch(path) is None
  ):
    raise ValueError(f"{name} path is invalid")
  if not sudo.path_lexists(path) or sudo.path_islink(path) or not sudo.path_isfile(path):
    raise ValueError(f"{name} must be a regular non-symlink file")
  if executable and sudo.stat(path).st_mode & 0o111 == 0:
    raise ValueError(f"{name} is not executable")
  return path


def _beeline_url(host, port, transport, endpoint, ssl_enabled, principal):
  if _HOST_PATTERN.fullmatch(host) is None or ".." in host:
    raise ValueError("host name is invalid")
  options = []
  if principal:
    options.append(f"principal={principal.replace('_HOST', host.lower())}")
  options.append(f"transportMode={transport}")
  if transport == "http":
    if not endpoint or "/" in endpoint or any(character.isspace() for character in endpoint):
      raise ValueError("HTTP endpoint is invalid")
    options.append(f"httpPath={endpoint}")
    if ssl_enabled:
      options.append("ssl=true")
  return ";".join((f"jdbc:hive2://{host}:{port}/default",) + tuple(options))


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations=None, parameters=None, host_name=None):
  if not configurations:
    return "UNKNOWN", ["There were no configurations supplied to the script."]
  parameters = parameters or {}
  try:
    host = (host_name or socket.getfqdn()).lower()
    transport = str(configurations.get(TRANSPORT_KEY, "binary")).strip().lower()
    if transport not in ("binary", "http"):
      raise ValueError("transport mode must be binary or http")
    port = _bounded_port(configurations[PORT_KEY if transport == "binary" else HTTP_PORT_KEY])
    timeout = int(float(parameters.get(TIMEOUT_KEY, 60)))
    if not 1 <= timeout <= 300:
      raise ValueError("check timeout must be between 1 and 300 seconds")
    secure = _boolean(configurations.get(SECURITY_KEY, False))
    user = configurations[SPARK_USER_KEY]
    endpoint = configurations.get(ENDPOINT_KEY, "cliservice")
    url = _beeline_url(
      host,
      port,
      transport,
      endpoint,
      _boolean(configurations.get(SSL_KEY, False)),
      configurations.get(PRINCIPAL_KEY) if secure else None,
    )
  except (KeyError, TypeError, ValueError) as error:
    return "UNKNOWN", [f"Invalid Spark Thrift Server alert configuration: {error}"]

  started = time.monotonic()
  try:
    beeline = _regular_file("/usr/lib/spark/bin/beeline", "Spark Beeline", executable=True)
    command = (beeline, "-u", url, "-e", "SELECT 1")
    if secure:
      keytab = configurations.get(KEYTAB_KEY)
      principal = configurations.get(PRINCIPAL_KEY)
      if not keytab or not principal:
        return "UNKNOWN", ["Secure Spark Thrift Server alert requires a principal and keytab."]
      _regular_file(keytab, "Spark Thrift Server keytab")
      with PrivateKerberosCache(
        user,
        temp_dir="/tmp",
        prefix="ambari-spark-thrift-alert-",
      ) as cache:
        cache.kinit(
          get_kinit_path(configurations.get(SEARCH_PATHS_KEY)),
          keytab,
          principal.replace("_HOST", host),
          timeout=30,
        )
        Execute(command, user=user, environment=cache.environment, timeout=timeout)
    else:
      Execute(command, user=user, timeout=timeout)
    elapsed = time.monotonic() - started
    return "OK", [
      f"Spark Thrift Server responded on port {port} in {elapsed:.3f}s"
    ]
  except Exception as error:
    return "CRITICAL", [f"Spark Thrift Server check failed on {host}:{port}: {error}"]
