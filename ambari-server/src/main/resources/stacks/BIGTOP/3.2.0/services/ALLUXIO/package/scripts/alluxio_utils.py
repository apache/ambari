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
import posixpath
import re
from urllib.parse import urlsplit, urlunsplit

from resource_management.libraries.functions import safe_process


_PRINCIPAL_PATTERN = re.compile(
  r"[A-Za-z0-9._/-]+@[A-Za-z0-9._-]+", re.ASCII
)
_SERVICE_ACCOUNT_PATTERN = re.compile(
  r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII
)
_DATA_SIZE_PATTERN = re.compile(
  r"[1-9][0-9]*(?:[KMGTPE]B)", re.IGNORECASE | re.ASCII
)
_PROTECTED_DIRECTORY_PREFIXES = (
  "/bin/",
  "/boot/",
  "/dev/",
  "/etc/",
  "/lib/",
  "/lib64/",
  "/proc/",
  "/root/",
  "/sbin/",
  "/sys/",
  "/tmp/",
  "/usr/",
)


def _safe_absolute_path(value):
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and value != "/"
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and not any(ord(character) < 32 for character in value)
  )


def validate_directory_path(value, name):
  protected = {
    "/bin",
    "/etc",
    "/lib",
    "/lib64",
    "/opt",
    "/run",
    "/sbin",
    "/srv",
    "/tmp",
    "/usr",
    "/var",
    "/var/lib",
    "/var/log",
    "/var/run",
  }
  if (
    not _safe_absolute_path(value)
    or value in protected
    or value.startswith(_PROTECTED_DIRECTORY_PREFIXES)
  ):
    raise ValueError(f"{name} must be a dedicated absolute directory")
  return value


def validate_service_account(value, name):
  if value == "root" or _SERVICE_ACCOUNT_PATTERN.fullmatch(str(value)) is None:
    raise ValueError(f"{name} must be a non-root service account")
  return value


def as_bool(value, name):
  if isinstance(value, bool):
    return value
  normalized = str(value).strip().lower()
  if normalized == "true":
    return True
  if normalized == "false":
    return False
  raise ValueError(f"{name} must be true or false")


def validate_port(value, name):
  if isinstance(value, bool):
    parsed = None
  elif isinstance(value, int):
    parsed = value
  elif isinstance(value, str):
    normalized = value.strip()
    parsed = (
      int(normalized)
      if normalized.isascii() and normalized.isdecimal()
      else None
    )
  else:
    parsed = None
  if parsed is None or not 0 < parsed <= 65535:
    raise ValueError(f"{name} must be an integer between 1 and 65535")
  return str(parsed)


def validate_data_size(value, name):
  normalized = str(value).strip()
  if _DATA_SIZE_PATTERN.fullmatch(normalized) is None:
    raise ValueError(f"{name} must be a positive data size")
  return normalized


def resolve_master_metastore_dir(site_properties, alluxio_data_dir):
  configured_dir = (
    site_properties.get("alluxio.master.metastore.dir")
    if site_properties
    else None
  )
  return validate_directory_path(
    configured_dir or os.path.join(alluxio_data_dir, "metastore"),
    "Alluxio master metastore directory",
  )


def resolve_underfs_address(default_fs, configured_path):
  if not isinstance(default_fs, str) or not default_fs:
    raise ValueError("HDFS default filesystem is not configured")
  if (
    not isinstance(configured_path, str)
    or not configured_path.startswith("/")
    or configured_path == "/"
    or configured_path.startswith("//")
    or posixpath.normpath(configured_path) != configured_path
    or any(ord(character) < 32 for character in configured_path)
  ):
    raise ValueError("Alluxio UnderFS path must be a normalized absolute path")

  parsed = urlsplit(default_fs)
  if parsed.scheme not in ("hdfs", "viewfs") or parsed.query or parsed.fragment:
    raise ValueError("Alluxio UnderFS requires an HDFS or ViewFS filesystem URI")
  base_path = parsed.path or "/"
  joined_path = posixpath.join(base_path, configured_path.lstrip("/"))
  return urlunsplit((parsed.scheme, parsed.netloc, joined_path, "", ""))


def validate_keytab_path(value):
  if (
    not _safe_absolute_path(value)
    or not value.endswith(".keytab")
  ):
    raise ValueError("Alluxio service keytab must be a safe absolute .keytab path")
  return value


def validate_principal(value):
  if not isinstance(value, str) or _PRINCIPAL_PATTERN.fullmatch(value) is None:
    raise ValueError("Alluxio service principal is invalid")
  return value


def rollback_started_process(pid_file, identity, user, process_class):
  safe_process.terminate_process(identity, user, process_class)
  pid = safe_process.read_pid(pid_file)
  if pid == identity.pid:
    safe_process.remove_pid_file_if_stopped(
      pid_file, identity.pid, user, process_class
    )
