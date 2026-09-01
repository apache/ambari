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
import os
import re
from urllib.parse import urlsplit

from resource_management.core import shell, sudo
from resource_management.core.exceptions import Fail


_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)
_HDFS_AUTHORITY_PATTERN = re.compile(
  r"(?:[A-Za-z0-9_.-]+(?::[0-9]+)?|\[[0-9A-Fa-f:]+\](?::[0-9]+)?)?",
  re.ASCII,
)
_PRINCIPAL_PATTERN = re.compile(r"[A-Za-z0-9._/-]+@[A-Za-z0-9._-]+", re.ASCII)
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


def validate_absolute_path(path, name):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or path.startswith("//")
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


def validate_principal(value, name):
  if not isinstance(value, str) or _PRINCIPAL_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid Kerberos principal")
  return value


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("Flink scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def validate_hdfs_uri(value, name):
  if not isinstance(value, str) or not value or any(
    character.isspace() or ord(character) < 32 or ord(character) == 127
    for character in value
  ):
    raise Fail(f"{name} must be a safe HDFS URI")
  try:
    parsed = urlsplit(value)
    port = parsed.port
  except ValueError as error:
    raise Fail(f"{name} contains an invalid HDFS authority") from error
  if (
    parsed.scheme != "hdfs"
    or _HDFS_AUTHORITY_PATTERN.fullmatch(parsed.netloc) is None
    or (port is not None and not 1 <= port <= 65535)
    or not parsed.path.startswith("/")
    or parsed.query
    or parsed.fragment
    or parsed.username
    or parsed.password
    or any(part == ".." for part in parsed.path.split("/"))
  ):
    raise Fail(f"{name} must be a safe HDFS URI")
  return value


def validate_hdfs_uri_list(value, name):
  if not isinstance(value, str):
    raise Fail(f"{name} must be a comma-separated HDFS URI list")
  entries = [entry.strip() for entry in value.split(",")]
  if not entries or any(not entry for entry in entries):
    raise Fail(f"{name} must be a comma-separated HDFS URI list")
  return ",".join(
    validate_hdfs_uri(entry, f"{name} entry") for entry in entries
  )


def validate_regular_file(path, name):
  validate_absolute_path(path, name)
  if not sudo.path_lexists(path):
    raise Fail(f"{name} {path} does not exist")
  if sudo.path_islink(path) or not sudo.path_isfile(path):
    raise Fail(f"{name} {path} must be a regular non-symlink file")
  return path


def validate_keytab(path, name):
  validate_regular_file(path, name)
  if not path.endswith(".keytab"):
    raise Fail(f"{name} must end with .keytab")
  return path


def validate_executable(path, name):
  validate_regular_file(path, name)
  if (sudo.stat(path).st_mode & 0o111) == 0:
    raise Fail(f"{name} {path} is not executable")
  return path


def yaml_string(value, name):
  if not isinstance(value, str) or not value or "\x00" in value:
    raise Fail(f"{name} must be non-empty text without NUL characters")
  return json.dumps(value, ensure_ascii=True)


def resolve_hadoop_classpath(hadoop_executable, user, java_home, timeout=60):
  validate_executable(hadoop_executable, "Hadoop executable")
  _, output = shell.checked_call(
    (hadoop_executable, "classpath"),
    user=user,
    env={"JAVA_HOME": java_home},
    timeout=timeout,
  )
  classpath = output.strip()
  if (
    not classpath
    or "\n" in classpath
    or "\r" in classpath
    or "\x00" in classpath
  ):
    raise Fail("Hadoop classpath output must contain exactly one non-empty line")
  return classpath
