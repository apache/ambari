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

import ipaddress
import json
import os
import re

from resource_management.core.exceptions import Fail


_HOST_PATTERN = re.compile(
  r"[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?", re.ASCII
)
_PRINCIPAL_PATTERN = re.compile(r"[A-Za-z0-9._/-]+@[A-Za-z0-9._-]+", re.ASCII)
_REALM_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*", re.ASCII)
_SOLR_NAME_PATTERN = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]*", re.ASCII)
_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_ZNODE_PATTERN = re.compile(
  r"/(?:[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*)?", re.ASCII
)


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


def validate_user(value, name):
  if not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid user, group, or role name")
  return value


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


def service_user(principal, name, required=True):
  if principal is None or (isinstance(principal, str) and not principal.strip()):
    if required:
      raise Fail(f"{name} is required")
    return None
  if not isinstance(principal, str):
    raise Fail(f"{name} must be a Kerberos principal")
  primary = principal.strip().split("/", 1)[0].split("@", 1)[0]
  return validate_user(primary, name)


def configured_principal_users(configurations, config_types, property_name):
  users = []
  for config_type in config_types:
    properties = configurations.get(config_type)
    if not isinstance(properties, dict):
      continue
    principal = properties.get(property_name)
    if principal is None or (isinstance(principal, str) and not principal.strip()):
      continue
    user = service_user(principal, f"{config_type}/{property_name}")
    if user not in users:
      users.append(user)
  return tuple(users)


def parse_users(value, name):
  if value is None:
    return ()
  if not isinstance(value, str):
    raise Fail(f"{name} must be a comma-separated list")
  users = tuple(
    validate_user(item.strip(), name)
    for item in value.split(",")
    if item.strip()
  )
  if len(set(users)) != len(users):
    raise Fail(f"{name} must not contain duplicate users")
  return users


def validate_service_directory(value, name):
  if (
    not isinstance(value, str)
    or not os.path.isabs(value)
    or value.startswith("//")
    or os.path.normpath(value) != value
    or any(ord(character) < 32 for character in value)
  ):
    raise Fail(f"{name} must be a safe absolute directory")
  protected = {
    "/", "/bin", "/etc", "/lib", "/lib64", "/opt", "/run", "/sbin",
    "/srv", "/tmp", "/usr", "/var", "/var/lib", "/var/log", "/var/run",
  }
  if value in protected or value.startswith(
    ("/boot/", "/dev/", "/proc/", "/root/", "/sys/", "/usr/")
  ):
    raise Fail(f"{name} must be a dedicated service directory")
  return value


def validate_absolute_path(value, name):
  if (
    not isinstance(value, str)
    or not os.path.isabs(value)
    or value == "/"
    or value.startswith("//")
    or os.path.normpath(value) != value
    or any(ord(character) < 32 for character in value)
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return value


def validate_secret(value, name):
  if (
    not isinstance(value, str)
    or not value
    or any(character in value for character in ("\x00", "\r", "\n"))
  ):
    raise Fail(f"{name} must be non-empty and contain no control characters")
  return value


def validate_json_object(value, name, allow_empty=False):
  if allow_empty and (value is None or (isinstance(value, str) and not value.strip())):
    return ""
  if not isinstance(value, str):
    raise Fail(f"{name} must be JSON text")
  try:
    document = json.loads(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{name} must be valid JSON") from error
  if not isinstance(document, dict):
    raise Fail(f"{name} must contain one JSON object")
  return value


def validate_znode(value):
  if not isinstance(value, str):
    raise Fail("Solr ZooKeeper znode is invalid")
  normalized = value.rstrip("/") or "/"
  if normalized == "/" or _ZNODE_PATTERN.fullmatch(normalized) is None:
    raise Fail("Solr ZooKeeper znode must be a safe non-root path")
  return normalized


def validate_zookeeper_quorum(value):
  if not isinstance(value, str) or not value:
    raise Fail("Solr ZooKeeper quorum is required")
  endpoints = []
  for endpoint in value.split(","):
    if endpoint.startswith("["):
      closing_bracket = endpoint.find("]")
      separator = endpoint[closing_bracket + 1 : closing_bracket + 2]
      if closing_bracket < 2 or separator != ":":
        raise Fail(f"Invalid Solr ZooKeeper endpoint {endpoint!r}")
      host = endpoint[1:closing_bracket]
      port = endpoint[closing_bracket + 2 :]
      try:
        ipaddress.IPv6Address(host)
      except ValueError as error:
        raise Fail(f"Invalid Solr ZooKeeper endpoint {endpoint!r}") from error
      normalized_host = f"[{host.lower()}]"
    else:
      host, separator, port = endpoint.rpartition(":")
      if not separator or _HOST_PATTERN.fullmatch(host) is None or ".." in host:
        raise Fail(f"Invalid Solr ZooKeeper endpoint {endpoint!r}")
      normalized_host = host.lower()
    if not port:
      raise Fail(f"Invalid Solr ZooKeeper endpoint {endpoint!r}")
    port_number = bounded_int(port, "ZooKeeper port", 1, 65535)
    endpoints.append(f"{normalized_host}:{port_number}")
  return ",".join(endpoints)
