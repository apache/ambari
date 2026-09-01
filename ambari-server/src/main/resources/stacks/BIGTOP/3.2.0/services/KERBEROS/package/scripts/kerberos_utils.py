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

import base64
import binascii
import ipaddress
import os
import re
import stat
from collections.abc import Mapping

from resource_management.core import sudo
from resource_management.core.exceptions import Fail


_DNS_LABEL_PATTERN = re.compile(
  r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?", re.ASCII
)
_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_VERSION_PATTERN = re.compile(r"[0-9]+(?:\.[0-9]+){1,3}", re.ASCII)
_MAX_KEYTAB_BYTES = 16 * 1024 * 1024
_MAX_KEYTAB_RECORDS = 10000
_PROTECTED_MANAGED_ROOTS = (
  "/bin",
  "/boot",
  "/dev",
  "/home",
  "/lib",
  "/lib64",
  "/proc",
  "/root",
  "/run",
  "/sbin",
  "/sys",
  "/tmp",
  "/usr/bin",
  "/usr/lib",
  "/usr/lib64",
  "/usr/sbin",
  "/var/tmp",
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


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("Kerberos service scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def validate_user(value, name):
  if not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid local user or group name")
  return value


def validate_host(value, name):
  if not isinstance(value, str) or value.strip() != value:
    raise Fail(f"{name} is invalid")
  if not _is_valid_host(value):
    raise Fail(f"{name} is invalid")
  return value.lower()


def validate_principal(value, name="Kerberos principal"):
  if (
    not isinstance(value, str)
    or not value
    or value.strip() != value
    or any(ord(character) < 32 or ord(character) == 127 for character in value)
  ):
    raise Fail(f"{name} is invalid")
  return value


def validate_realm(value):
  if not isinstance(value, str):
    raise Fail("kerberos-env/realm is invalid")
  value = value.strip()
  if value.endswith(".") or not _is_valid_dns_name(value):
    raise Fail("kerberos-env/realm is invalid")
  return value


def _is_valid_dns_name(value):
  if not value or len(value) > 253:
    return False
  if value.endswith("."):
    value = value[:-1]
  return bool(value) and all(
    _DNS_LABEL_PATTERN.fullmatch(label) is not None for label in value.split(".")
  )


def _is_valid_host(value):
  try:
    ipaddress.IPv4Address(value)
    return True
  except ValueError:
    if all(character in "0123456789." for character in value):
      return False
    return _is_valid_dns_name(value)


def _validate_endpoint(value, name):
  if not isinstance(value, str):
    raise Fail(f"{name} is invalid")
  value = value.strip()
  if not value or any(
    ord(character) < 32 or ord(character) == 127 for character in value
  ):
    raise Fail(f"{name} is invalid")

  host = value
  port = None
  if value.startswith("["):
    closing = value.find("]")
    if closing < 0:
      raise Fail(f"{name} is invalid")
    host = value[1:closing]
    suffix = value[closing + 1 :]
    if suffix:
      if not suffix.startswith(":"):
        raise Fail(f"{name} is invalid")
      port = suffix[1:]
    try:
      ipaddress.IPv6Address(host)
    except ValueError as error:
      raise Fail(f"{name} has an invalid IPv6 address") from error
  elif value.count(":") == 1:
    host, port = value.rsplit(":", 1)
  elif ":" in value:
    raise Fail(f"{name} must bracket an IPv6 address")

  if ":" not in host and not _is_valid_host(host):
    raise Fail(f"{name} has an invalid host")
  if port is not None and (
    not port.isascii() or not port.isdigit() or not 1 <= int(port) <= 65535
  ):
    raise Fail(f"{name} has an invalid port")
  return value


def validate_endpoints(value, name, required=False):
  if value is None:
    value = ""
  if not isinstance(value, str):
    raise Fail(f"{name} is invalid")
  entries = [entry.strip() for entry in value.split(",")]
  if not entries or any(not entry for entry in entries):
    if required:
      raise Fail(f"{name} must contain at least one Kerberos server")
    return ""
  return ",".join(_validate_endpoint(entry, name) for entry in entries)


def validate_endpoint(value, name, required=False):
  if value is None:
    value = ""
  if not isinstance(value, str):
    raise Fail(f"{name} is invalid")
  value = value.strip()
  if not value:
    if required:
      raise Fail(f"{name} must contain a Kerberos server")
    return ""
  return _validate_endpoint(value, name)


def endpoint_without_port(value, name):
  value = validate_endpoint(value, name, required=True)
  if value.startswith("["):
    return value[: value.find("]") + 1]
  if value.count(":") == 1:
    return value.rsplit(":", 1)[0]
  return value


def validate_domains(value):
  if value is None:
    value = ""
  if not isinstance(value, str):
    raise Fail("krb5-conf/domains contains an invalid domain")
  entries = [entry.strip() for entry in value.split(",")]
  if entries == [""]:
    return ""
  if any(
    not _is_valid_dns_name(entry[1:] if entry.startswith(".") else entry)
    for entry in entries
  ):
    raise Fail("krb5-conf/domains contains an invalid domain")
  return ",".join(entries)


def _validate_path_parents(path, name):
  parent = os.path.dirname(path)
  while parent != os.path.sep:
    if sudo.path_lexists(parent) and sudo.path_islink(parent):
      raise Fail(f"{name} parent {parent} must not be a symbolic link")
    parent = os.path.dirname(parent)


def validate_managed_file(path, name, suffix=None):
  if (
    not isinstance(path, str)
    or any(ord(character) < 32 or ord(character) == 127 for character in path)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
    or os.path.dirname(path) == os.path.sep
  ):
    raise Fail(f"{name} must be a safe absolute file path")
  if suffix and not path.endswith(suffix):
    raise Fail(f"{name} must end with {suffix}")
  if any(
    path == protected or path.startswith(protected + os.path.sep)
    for protected in _PROTECTED_MANAGED_ROOTS
  ):
    raise Fail(f"{name} must not use a protected system directory")
  _validate_path_parents(path, name)
  if sudo.path_lexists(path):
    if sudo.path_islink(path):
      raise Fail(f"{name} must not be a symbolic link")
    if not sudo.path_isfile(path):
      raise Fail(f"{name} must be a regular file")
  return path


def validate_executable(path, name):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
  ):
    raise Fail(f"{name} must be an absolute path")
  _validate_path_parents(path, name)
  if (
    not sudo.path_lexists(path)
    or sudo.path_islink(path)
    or not sudo.path_isfile(path)
  ):
    raise Fail(f"{name} must be a regular non-symlink file")
  metadata = sudo.stat(path)
  if metadata.st_uid != 0 or metadata.st_mode & 0o022:
    raise Fail(f"{name} must be root-owned and not group/world writable")
  if stat.S_IMODE(metadata.st_mode) & 0o111 == 0:
    raise Fail(f"{name} is not executable")
  return path


def validate_keytab_records(records, require_content=False):
  if records is None:
    return ()
  if not isinstance(records, (list, tuple)):
    raise Fail("kerberosCommandParams must be a list")
  if len(records) > _MAX_KEYTAB_RECORDS:
    raise Fail("kerberosCommandParams contains too many records")

  keytab_properties = {}
  for item in records:
    if not isinstance(item, Mapping):
      raise Fail("kerberosCommandParams contains an invalid record")
    validate_principal(item.get("principal"))
    validate_managed_file(
      item.get("keytab_file_path"), "Kerberos keytab path", suffix=".keytab"
    )
    for field in ("keytab_file_owner_name", "keytab_file_group_name"):
      if item.get(field):
        validate_user(item[field], field)
    if item.get("keytab_file_owner_access") not in (None, "r", "rw"):
      raise Fail("keytab_file_owner_access must be r or rw")
    if item.get("keytab_file_group_access") not in (None, "", "r", "rw"):
      raise Fail("keytab_file_group_access must be empty, r, or rw")
    keytab_path = item["keytab_file_path"]
    properties = (
      item.get("keytab_file_owner_name"),
      item.get("keytab_file_group_name"),
      item.get("keytab_file_owner_access"),
      item.get("keytab_file_group_access"),
      item.get("keytab_content_base64") if require_content else None,
    )
    previous_properties = keytab_properties.setdefault(keytab_path, properties)
    if previous_properties != properties:
      raise Fail(
        f"Kerberos keytab {keytab_path} has inconsistent content or permissions"
      )
    if require_content:
      content = item.get("keytab_content_base64")
      if not isinstance(content, str) or not content:
        raise Fail("SET_KEYTAB requires non-empty base64 keytab content")
      if len(content) > _MAX_KEYTAB_BYTES * 2:
        raise Fail("SET_KEYTAB keytab content exceeds the supported size")
      try:
        decoded = base64.b64decode(content, validate=True)
      except (binascii.Error, ValueError) as error:
        raise Fail("SET_KEYTAB contains invalid base64 keytab content") from error
      if not decoded:
        raise Fail("SET_KEYTAB requires non-empty keytab content")
      if len(decoded) > _MAX_KEYTAB_BYTES:
        raise Fail("SET_KEYTAB keytab content exceeds the supported size")
  return tuple(records)


def keytab_is_regular_file(path):
  validate_managed_file(path, "Kerberos keytab path", suffix=".keytab")
  if not sudo.path_lexists(path):
    return False
  return True
