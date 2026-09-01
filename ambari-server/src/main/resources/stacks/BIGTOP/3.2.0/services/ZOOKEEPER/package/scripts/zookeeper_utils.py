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

from resource_management.core import sudo
from resource_management.core.exceptions import Fail


_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)
_CONFIG_KEY_PATTERN = re.compile(r"[A-Za-z0-9_.-]+", re.ASCII)
_HOST_PATTERN = re.compile(r"[A-Za-z0-9_](?:[A-Za-z0-9_.-]*[A-Za-z0-9_])?", re.ASCII)
_PRINCIPAL_PATTERN = re.compile(
  r"[A-Za-z0-9._/-]+@[A-Za-z0-9._-]+", re.ASCII
)
_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_VERSION_PATTERN = re.compile(
  r"[0-9]+(?:\.[0-9]+){1,3}(?:[-.][A-Za-z0-9]+)*", re.ASCII
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


def positive_int(value, name, minimum=1, maximum=None):
  if isinstance(value, bool):
    raise Fail(f"{name} must be an integer")
  try:
    parsed = int(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{name} must be an integer") from error
  if parsed < minimum or (maximum is not None and parsed > maximum):
    expected = f"at least {minimum}"
    if maximum is not None:
      expected = f"between {minimum} and {maximum}"
    raise Fail(f"{name} must be {expected}")
  return parsed


def validate_absolute_path(path, name):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
    or _ABSOLUTE_PATH_PATTERN.fullmatch(path) is None
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return path


def validate_service_directory(path, name):
  validate_absolute_path(path, name)
  protected_trees = (
    "/boot",
    "/dev",
    "/etc",
    "/home",
    "/proc",
    "/root",
    "/sys",
    "/usr",
  )
  protected_directories = {
    "/",
    "/bin",
    "/data",
    "/home",
    "/lib",
    "/lib64",
    "/mnt",
    "/opt",
    "/run",
    "/sbin",
    "/srv",
    "/tmp",
    "/var",
    "/var/lib",
    "/var/log",
    "/var/run",
  }
  if path in protected_directories or any(
    path == root or path.startswith(root + os.path.sep) for root in protected_trees
  ):
    raise Fail(f"{name} must not use a protected system directory")
  return path


def validate_user(value, name):
  if not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid local user or group name")
  return value


def validate_host(value, name):
  if (
    not isinstance(value, str)
    or len(value) > 253
    or ".." in value
    or _HOST_PATTERN.fullmatch(value) is None
  ):
    raise Fail(f"{name} is not a safe ZooKeeper host name")
  return value.lower()


def validate_principal(value, name):
  if not isinstance(value, str) or _PRINCIPAL_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid Kerberos principal")
  return value


def validate_keytab(path, name):
  validate_absolute_path(path, name)
  if not path.endswith(".keytab"):
    raise Fail(f"{name} must end with .keytab")
  if not sudo.path_lexists(path):
    raise Fail(f"{name} {path} does not exist")
  if sudo.path_islink(path) or not sudo.path_isfile(path):
    raise Fail(f"{name} {path} must be a regular non-symlink file")
  return path


def validate_executable(path, name):
  validate_absolute_path(path, name)
  if not sudo.path_lexists(path):
    raise Fail(f"{name} {path} does not exist")
  if sudo.path_islink(path) or not sudo.path_isfile(path):
    raise Fail(f"{name} {path} must be a regular non-symlink file")
  if (sudo.stat(path).st_mode & 0o111) == 0:
    raise Fail(f"{name} {path} is not executable")
  return path


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("ZooKeeper scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def sanitize_zoo_cfg(properties):
  if not isinstance(properties, dict):
    raise Fail("zoo.cfg must be a property mapping")
  sanitized = {}
  for key, value in properties.items():
    if not isinstance(key, str) or _CONFIG_KEY_PATTERN.fullmatch(key) is None:
      raise Fail(f"zoo.cfg contains an invalid property name: {key!r}")
    if key.startswith("server."):
      raise Fail("zoo.cfg server entries are managed from the Ambari host topology")
    if isinstance(value, bool):
      rendered = str(value).lower()
    elif isinstance(value, (str, int, float)) and not isinstance(value, bool):
      rendered = str(value)
    else:
      raise Fail(f"zoo.cfg/{key} has an unsupported value type")
    if any(ord(character) < 32 or ord(character) == 127 for character in rendered):
      raise Fail(f"zoo.cfg/{key} contains a control character")
    sanitized[key] = rendered

  integer_ranges = {
    "tickTime": (1, None),
    "initLimit": (1, None),
    "syncLimit": (1, None),
    "clientPort": (1, 65535),
    "autopurge.snapRetainCount": (3, None),
    "autopurge.purgeInterval": (0, None),
    "admin.serverPort": (1, 65535),
  }
  for key, (minimum, maximum) in integer_ranges.items():
    if key in sanitized:
      positive_int(sanitized[key], f"zoo.cfg/{key}", minimum, maximum)

  if "admin.enableServer" in sanitized:
    sanitized["admin.enableServer"] = str(
      as_bool(sanitized["admin.enableServer"], "zoo.cfg/admin.enableServer")
    ).lower()

  whitelist = sanitized.get("4lw.commands.whitelist")
  if whitelist is not None:
    commands = {
      command.strip() for command in whitelist.split(",") if command.strip()
    }
    if "*" in commands:
      raise Fail("zoo.cfg/4lw.commands.whitelist must not enable every command")
    if "ruok" not in commands:
      raise Fail("zoo.cfg/4lw.commands.whitelist must enable ruok for the alert")

  client_port = sanitized.get("clientPort")
  admin_port = sanitized.get("admin.serverPort")
  admin_enabled = sanitized.get("admin.enableServer", "true") == "true"
  if admin_enabled and client_port is not None and client_port == admin_port:
    raise Fail("zoo.cfg/admin.serverPort must differ from clientPort")
  return sanitized
