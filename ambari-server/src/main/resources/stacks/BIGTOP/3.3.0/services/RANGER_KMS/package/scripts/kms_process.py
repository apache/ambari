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
import pwd
import stat

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import safe_process


_EXPECTED_CMDLINE = ("-Dproc_rangerkms",)


def _validate_pid_file(pid_file):
  if not isinstance(pid_file, str) or not os.path.isabs(pid_file):
    raise Fail("Ranger KMS PID file must be an absolute path")
  normalized = os.path.normpath(pid_file)
  if normalized != pid_file or os.path.basename(pid_file) != "rangerkms.pid":
    raise Fail("Ranger KMS PID file is invalid")
  if not (pid_file.startswith("/run/") or pid_file.startswith("/var/run/")):
    raise Fail("Ranger KMS PID file must be below /run or /var/run")


def _validate_pid_directory(pid_file, user):
  pid_directory = os.path.dirname(pid_file)
  resolved_directory = os.path.realpath(pid_directory)
  if not resolved_directory.startswith("/run/"):
    raise Fail("Ranger KMS PID directory resolves outside /run")
  if os.path.islink(pid_directory) or not os.path.isdir(pid_directory):
    raise Fail("Ranger KMS PID directory must be a real directory")
  try:
    expected_uid = pwd.getpwnam(user).pw_uid
  except (KeyError, TypeError) as error:
    raise Fail(f"Could not resolve Ranger KMS service user {user}") from error
  directory_stat = os.stat(pid_directory, follow_symlinks=False)
  if (
    directory_stat.st_uid != expected_uid
    or not stat.S_ISDIR(directory_stat.st_mode)
    or stat.S_IMODE(directory_stat.st_mode) & 0o022
  ):
    raise Fail("Ranger KMS PID directory ownership or permissions are unsafe")


def _remove_stale_pid_file(pid_file, user):
  pid = safe_process.read_pid(pid_file)
  if pid is None:
    return
  identity = safe_process.inspect_process(pid)
  if identity is not None and identity.state not in ("Z", "X"):
    safe_process.inspect_process(pid, user, _EXPECTED_CMDLINE)
    raise Fail(f"Refusing stale PID file {pid_file}: process {pid} is still running")
  safe_process.remove_pid_file_if_stopped(pid_file, pid, user, _EXPECTED_CMDLINE)


def find_process(pid_file, user, group, publish_discovered=True):
  _validate_pid_file(pid_file)
  _validate_pid_directory(pid_file, user)
  identity = safe_process.read_running_process(pid_file, user, _EXPECTED_CMDLINE)
  if identity is not None:
    return safe_process.publish_pid_file_for_identity(
      pid_file, identity, user, _EXPECTED_CMDLINE, user, group
    )
  _remove_stale_pid_file(pid_file, user)
  identity = safe_process.discover_running_process(user, _EXPECTED_CMDLINE)
  if identity is not None and publish_discovered:
    return safe_process.publish_pid_file_for_identity(
      pid_file, identity, user, _EXPECTED_CMDLINE, user, group
    )
  return identity


def secure_started_process(pid_file, user, group):
  _validate_pid_file(pid_file)
  _validate_pid_directory(pid_file, user)
  identity = safe_process.wait_for_discovered_process(
    user, _EXPECTED_CMDLINE, attempts=30, sleep_seconds=1
  )
  try:
    return safe_process.publish_pid_file_for_identity(
      pid_file, identity, user, _EXPECTED_CMDLINE, user, group
    )
  except Exception:
    try:
      rollback_started_process(pid_file, identity, user)
    except Exception as error:
      Logger.warning(
        f"Could not roll back failed Ranger KMS PID publication: {error}"
      )
    raise


def stop_process(pid_file, user, group):
  identity = find_process(pid_file, user, group)
  if identity is None:
    return
  safe_process.terminate_process(identity, user, _EXPECTED_CMDLINE)
  pid = safe_process.read_pid(pid_file)
  if pid == identity.pid:
    safe_process.remove_pid_file_if_stopped(
      pid_file, identity.pid, user, _EXPECTED_CMDLINE
    )


def rollback_started_process(pid_file, identity, user):
  _validate_pid_file(pid_file)
  safe_process.terminate_process(identity, user, _EXPECTED_CMDLINE)
  try:
    _validate_pid_directory(pid_file, user)
    pid = safe_process.read_pid(pid_file)
  except Fail:
    return
  if pid == identity.pid:
    safe_process.remove_pid_file_if_stopped(
      pid_file, identity.pid, user, _EXPECTED_CMDLINE
    )


def check_process(pid_file, user, group):
  if find_process(pid_file, user, group) is None:
    raise ComponentIsNotRunning()
