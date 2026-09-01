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

import errno
import pwd
import re
import signal
import time

from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


_PID_PATTERN = re.compile(r"[0-9]+", re.ASCII)


def _invalid_pid(pid_file, reason, fail_on_invalid):
  message = f"Pid file {pid_file} {reason}"
  Logger.info(message)
  if fail_on_invalid:
    raise Fail(message)
  return None


def read_pid(pid_file, fail_on_invalid=False):
  """Read a PID file without allowing its contents to become shell input."""
  if not pid_file:
    Logger.info(f"Pid file {pid_file!s} is empty or does not exist")
    return None

  try:
    exists = sudo.path_exists(pid_file)
    is_file = sudo.path_isfile(pid_file) if exists else False
  except (OSError, Fail):
    return _invalid_pid(pid_file, "could not be inspected", fail_on_invalid)
  if not exists:
    Logger.info(f"Pid file {pid_file!s} is empty or does not exist")
    return None
  if not is_file:
    return _invalid_pid(pid_file, "is not a regular file", fail_on_invalid)

  try:
    value = sudo.read_file(pid_file)
  except Exception:
    return _invalid_pid(pid_file, "could not be read", fail_on_invalid)

  try:
    value = value.decode("ascii") if isinstance(value, bytes) else value
  except UnicodeDecodeError:
    value = ""

  value = value.strip() if isinstance(value, str) else ""
  if not _PID_PATTERN.fullmatch(value):
    return _invalid_pid(
      pid_file, "does not contain a valid process id", fail_on_invalid
    )

  pid = int(value)
  if pid <= 0:
    return _invalid_pid(
      pid_file, "does not contain a positive process id", fail_on_invalid
    )

  return pid


def _is_process_owned_by_user(pid, expected_user, fail_on_mismatch=False):
  if pid is None:
    return False
  if not expected_user:
    message = f"Unable to validate owner of pid {pid}: service user is empty"
    Logger.warning(message)
    if fail_on_mismatch:
      raise Fail(message)
    return False

  process_path = f"/proc/{pid}"
  try:
    if not sudo.path_exists(process_path):
      return False
    process_uid = sudo.stat(process_path).st_uid
  except (OSError, Fail):
    if not sudo.path_exists(process_path):
      return False
    message = f"Unable to inspect owner of pid {pid}"
    Logger.warning(message)
    if fail_on_mismatch:
      raise Fail(message)
    return False

  try:
    expected_uid = pwd.getpwnam(expected_user).pw_uid
  except KeyError:
    message = f"Unable to validate owner of pid {pid}: user {expected_user} not found"
    Logger.warning(message)
    if fail_on_mismatch:
      raise Fail(message)
    return False

  if process_uid != expected_uid:
    message = (
      f"Refusing to signal pid {pid}: process owner does not match {expected_user}"
    )
    Logger.warning(message)
    if fail_on_mismatch:
      raise Fail(message)
    return False

  return True


def is_process_running(pid, expected_user, fail_on_owner_mismatch=False):
  if not _is_process_owned_by_user(
    pid, expected_user, fail_on_mismatch=fail_on_owner_mismatch
  ):
    return False

  try:
    sudo.kill(pid, 0)
  except OSError as error:
    if error.errno in (errno.ENOENT, errno.ESRCH) or not sudo.path_exists(
      f"/proc/{pid}"
    ):
      return False
    if fail_on_owner_mismatch:
      raise Fail(f"Unable to inspect running state of pid {pid}")
    return False

  return True


def is_pid_file_process_running(pid_file, expected_user):
  return is_process_running(
    read_pid(pid_file, fail_on_invalid=True),
    expected_user,
    fail_on_owner_mismatch=True,
  )


def wait_for_process_stopped(pid, expected_user, attempts, sleep_seconds):
  for attempt in range(attempts):
    if not is_process_running(pid, expected_user, fail_on_owner_mismatch=True):
      return True
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)

  return False


def terminate_process(
  pid,
  expected_user,
  graceful_wait_attempts,
  graceful_wait_sleep,
  force_wait_attempts=21,
  force_wait_sleep=3,
):
  if not is_process_running(pid, expected_user, fail_on_owner_mismatch=True):
    return

  if not _is_process_owned_by_user(pid, expected_user, fail_on_mismatch=True):
    return
  sudo.kill(pid, signal.SIGTERM)
  if wait_for_process_stopped(
    pid, expected_user, graceful_wait_attempts, graceful_wait_sleep
  ):
    return

  force_stop_process(pid, expected_user, force_wait_attempts, force_wait_sleep)


def force_stop_process(pid, expected_user, wait_attempts=21, wait_sleep=3):
  if not is_process_running(pid, expected_user, fail_on_owner_mismatch=True):
    return

  if not _is_process_owned_by_user(pid, expected_user, fail_on_mismatch=True):
    return

  sudo.kill(pid, signal.SIGKILL)
  if not wait_for_process_stopped(pid, expected_user, wait_attempts, wait_sleep):
    raise Fail(f"Process with pid {pid} did not stop")
