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
import shlex

from resource_management.core import sudo
from resource_management.core.exceptions import Fail


_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)
_HOST_PATTERN = re.compile(
  r"[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?", re.ASCII
)
_PRINCIPAL_PATTERN = re.compile(r"[A-Za-z0-9._/-]+@[A-Za-z0-9._-]+", re.ASCII)
_JAVA_OPTION_PATTERN = re.compile(r"-[A-Za-z0-9_./,:+@%=-]+", re.ASCII)
_REALM_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*", re.ASCII)
_SOLR_NAME_PATTERN = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]*", re.ASCII)
_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_VERSION_PATTERN = re.compile(r"[0-9]+(?:\.[0-9]+){1,3}", re.ASCII)
_ZNODE_PATTERN = re.compile(r"/(?:[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*)?", re.ASCII)


def as_bool(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{name} must be true or false")


def bounded_int(value, name, minimum, maximum):
  if isinstance(value, bool):
    raise Fail(f"{name} must be an integer")
  try:
    parsed = int(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{name} must be an integer") from error
  if parsed < minimum or parsed > maximum:
    raise Fail(f"{name} must be between {minimum} and {maximum}")
  return parsed


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("Ambari Infra Solr scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def validate_absolute_path(value, name):
  if (
    not isinstance(value, str)
    or not os.path.isabs(value)
    or value.startswith("//")
    or os.path.normpath(value) != value
    or _ABSOLUTE_PATH_PATTERN.fullmatch(value) is None
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return value


def validate_service_directory(value, name):
  validate_absolute_path(value, name)
  protected = {
    "/", "/bin", "/etc", "/lib", "/lib64", "/opt", "/run", "/sbin",
    "/srv", "/tmp", "/usr", "/var", "/var/lib", "/var/log", "/var/run",
  }
  if value in protected or value.startswith(
    ("/boot/", "/dev/", "/proc/", "/root/", "/sys/", "/usr/")
  ):
    raise Fail(f"{name} must not use a protected system directory")
  return value


def validate_user(value, name):
  if not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid local user or group name")
  return value


def validate_host(value, name):
  if (
    not isinstance(value, str)
    or _HOST_PATTERN.fullmatch(value) is None
    or ".." in value
  ):
    raise Fail(f"{name} is not a valid host name")
  return value.lower()


def validate_principal(value, name):
  if not isinstance(value, str) or _PRINCIPAL_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid Kerberos principal")
  return value


def validate_realm(value):
  if not isinstance(value, str) or _REALM_PATTERN.fullmatch(value) is None:
    raise Fail("Kerberos realm is invalid")
  return value


def validate_solr_name(value, name):
  if not isinstance(value, str) or _SOLR_NAME_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is invalid")
  return value


def validate_znode(value):
  if not isinstance(value, str):
    raise Fail("Infra Solr ZooKeeper znode is invalid")
  normalized = value.rstrip("/") or "/"
  if normalized == "/" or _ZNODE_PATTERN.fullmatch(normalized) is None:
    raise Fail("Infra Solr ZooKeeper znode is invalid")
  return normalized


def validate_zookeeper_quorum(value):
  if not isinstance(value, str) or not value:
    raise Fail("Infra Solr ZooKeeper quorum is required")
  normalized_endpoints = []
  for endpoint in value.split(","):
    host, separator, port = endpoint.rpartition(":")
    if not separator:
      raise Fail(f"ZooKeeper endpoint {endpoint!r} must include a port")
    normalized_host = validate_host(host, "ZooKeeper host")
    normalized_port = bounded_int(port, "ZooKeeper port", 1, 65535)
    normalized_endpoints.append(f"{normalized_host}:{normalized_port}")
  return ",".join(normalized_endpoints)


def validate_text(value, name, maximum=4096):
  if (
    not isinstance(value, str)
    or len(value) > maximum
    or any(character in value for character in ("\x00", "\r", "\n"))
  ):
    raise Fail(f"{name} contains invalid characters")
  return value


def validate_extra_java_options(value):
  value = validate_text(value, "Infra Solr extra Java options")
  try:
    tokens = tuple(shlex.split(value, posix=True))
  except ValueError as error:
    raise Fail("Infra Solr extra Java options contain invalid quoting") from error
  if len(tokens) > 128:
    raise Fail("Infra Solr extra Java options contain too many arguments")
  managed_prefixes = (
    "-Djava.rmi.server.hostname=",
    "-Djava.security.auth.login.config=",
    "-Djetty.port=",
    "-Dlog4j.configurationFile=",
    "-Dsolr.kerberos.",
    "-Dsolr.solr.home=",
    "-Dzookeeper.",
    "-DzkHost=",
  )
  for token in tokens:
    if _JAVA_OPTION_PATTERN.fullmatch(token) is None:
      raise Fail(f"Infra Solr Java option {token!r} contains unsafe characters")
    if token.startswith(managed_prefixes):
      raise Fail(
        f"Infra Solr extra Java options override managed argument {token}"
      )
  return tokens


def validate_regular_file(path, name):
  validate_absolute_path(path, name)
  if (
    not sudo.path_lexists(path)
    or sudo.path_islink(path)
    or not sudo.path_isfile(path)
  ):
    raise Fail(f"{name} {path} must be a regular non-symlink file")
  return path


def validate_keytab(path, name):
  validate_regular_file(path, name)
  if not path.endswith(".keytab"):
    raise Fail(f"{name} must end with .keytab")
  return path


def validate_executable(path, name):
  validate_regular_file(path, name)
  if sudo.stat(path).st_mode & 0o111 == 0:
    raise Fail(f"{name} {path} is not executable")
  return path
