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

from contextlib import contextmanager
import grp
import json
import os
import pwd
import stat
import tempfile

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


def strict_bool(value, property_name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{property_name} must be true or false")


def strict_yes_no(value, property_name):
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "yes":
      return True
    if normalized == "no":
      return False
  raise Fail(f"{property_name} must be Yes or No")


def require_nonempty_secret(value, property_name):
  if not isinstance(value, str) or not value.strip():
    raise Fail(f"{property_name} must be explicitly configured and non-empty")
  return value


@contextmanager
def private_secret_file(directory, owner, group, value, json_value=False):
  if not isinstance(directory, str) or not os.path.isabs(directory):
    raise Fail("Private secret directory must be an absolute path")
  if os.path.islink(directory) or not os.path.isdir(directory):
    raise Fail("Private secret directory must be a real directory")
  try:
    uid = pwd.getpwnam(owner).pw_uid
    gid = grp.getgrnam(group).gr_gid
  except (KeyError, TypeError) as error:
    raise Fail("Could not resolve private secret file ownership") from error
  directory_stat = os.stat(directory, follow_symlinks=False)
  if (
    directory_stat.st_uid != uid
    or not stat.S_ISDIR(directory_stat.st_mode)
    or stat.S_IMODE(directory_stat.st_mode) & 0o022
  ):
    raise Fail("Private secret directory ownership or permissions are unsafe")

  payload = (
    json.dumps(value).encode("utf-8")
    if json_value
    else str(value).encode("utf-8")
  )
  if len(payload) > 65536:
    raise Fail("Private secret payload exceeds 64 KiB")

  descriptor = None
  path = None
  file_identity = None
  primary_error = None
  try:
    descriptor, path = tempfile.mkstemp(prefix=".ambari-ranger-secret-", dir=directory)
    descriptor_stat = os.fstat(descriptor)
    if not stat.S_ISREG(descriptor_stat.st_mode) or descriptor_stat.st_nlink != 1:
      raise Fail("Private secret file must be a singly linked regular file")
    file_identity = (descriptor_stat.st_dev, descriptor_stat.st_ino)
    os.fchmod(descriptor, 0o600)
    remaining = memoryview(payload)
    while remaining:
      written = os.write(descriptor, remaining)
      if written <= 0:
        raise Fail("Could not write private secret file")
      remaining = remaining[written:]
    os.fsync(descriptor)
    os.fchown(descriptor, uid, gid)
    descriptor_stat = os.fstat(descriptor)
    if (descriptor_stat.st_dev, descriptor_stat.st_ino) != file_identity:
      raise Fail("Private secret file changed while it was being written")
    os.close(descriptor)
    descriptor = None
    yield path
  except BaseException as error:
    primary_error = error
    raise
  finally:
    cleanup_errors = []
    if descriptor is not None:
      try:
        os.close(descriptor)
      except OSError as error:
        cleanup_errors.append(error)
    if path is not None and os.path.lexists(path):
      try:
        path_stat = os.lstat(path)
        if stat.S_ISREG(path_stat.st_mode) and (
          path_stat.st_dev,
          path_stat.st_ino,
        ) == file_identity:
          os.unlink(path)
        else:
          cleanup_errors.append(
            Fail("Private secret file was replaced before cleanup")
          )
      except OSError as error:
        cleanup_errors.append(error)
    if cleanup_errors:
      cleanup_details = "; ".join(str(error) for error in cleanup_errors)
      if primary_error is not None:
        Logger.warning(
          "Could not clean up private secret file after operation failure: "
          f"{cleanup_details}"
        )
      else:
        raise Fail(
          f"Could not clean up private secret file: {cleanup_details}"
        ) from cleanup_errors[0]
