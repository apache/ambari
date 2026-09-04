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

import glob
import os
import re

from resource_management.core import sudo
from resource_management.core.exceptions import Fail


_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_VERSION_PATTERN = re.compile(
  r"[0-9]+(?:\.[0-9]+){1,3}(?:[-.][A-Za-z0-9]+)*", re.ASCII
)
_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./*+@=-]*", re.ASCII)


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


def validate_user(value, name):
  if not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None:
    raise Fail(f"{name} is not a valid local user or group name")
  return value


def validate_bigtop_stack(stack_name, stack_version):
  if stack_name != "BIGTOP":
    raise Fail("TEZ scripts only support the BIGTOP stack")
  if (
    not isinstance(stack_version, str)
    or _VERSION_PATTERN.fullmatch(stack_version) is None
  ):
    raise Fail("BIGTOP stack version is invalid")
  return stack_version


def validate_absolute_path(path, name):
  if (
    not isinstance(path, str)
    or not os.path.isabs(path)
    or os.path.normpath(path) != path
    or _ABSOLUTE_PATH_PATTERN.fullmatch(path) is None
  ):
    raise Fail(f"{name} must be a safe absolute path")
  return path


def validate_principal(value, name):
  if (
    not isinstance(value, str)
    or not value
    or any(character.isspace() for character in value)
  ):
    raise Fail(f"{name} must be a non-empty Kerberos principal")
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


def find_unique_examples_jar(pattern):
  validate_absolute_path(pattern, "Tez examples JAR pattern")
  matches = [
    path
    for path in sorted(glob.glob(pattern))
    if sudo.path_isfile(path) and not sudo.path_islink(path)
  ]
  if len(matches) != 1:
    raise Fail(
      f"Expected exactly one regular Tez examples JAR for {pattern}, "
      f"found {len(matches)}"
    )
  return matches[0]
