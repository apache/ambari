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

import grp
import os
import pwd
import re
import signal
import stat
import time
import uuid

from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import File


_PID_PATTERN = re.compile(r"[0-9]+", re.ASCII)


class ProcessIdentity:
  def __init__(self, pid, uid, start_time, argv, state="S"):
    self.pid = pid
    self.uid = uid
    self.start_time = start_time
    self.argv = tuple(argv)
    self.state = state

  @property
  def cmdline(self):
    return " ".join(self.argv)

  def matches(self, other):
    return (
      other is not None
      and self.pid == other.pid
      and self.uid == other.uid
      and self.start_time == other.start_time
      and self.argv == other.argv
    )


def _decode_ascii(value, description):
  try:
    value = value.decode("ascii") if isinstance(value, bytes) else value
  except UnicodeDecodeError as error:
    raise Fail(f"{description} is not ASCII") from error
  if not isinstance(value, str):
    raise Fail(f"{description} is not text")
  return value


def _path_exists(path):
  try:
    return sudo.path_exists(path)
  except Exception as error:
    raise Fail(f"Could not inspect path {path}: {error}") from error


def _path_islink(path):
  try:
    return sudo.path_islink(path)
  except Exception as error:
    raise Fail(f"Could not inspect link status for path {path}: {error}") from error


def read_pid(pid_file, fail_on_invalid=True):
  if not pid_file:
    if fail_on_invalid:
      raise Fail("PID file is not configured")
    return None

  if _path_islink(pid_file):
    raise Fail(f"PID file {pid_file} must not be a symbolic link")
  if not _path_exists(pid_file):
    return None
  try:
    if _path_islink(pid_file) or not sudo.path_isfile(pid_file):
      raise Fail(f"PID file {pid_file} is not a regular file")
    value = sudo.read_file(pid_file)
    if _path_islink(pid_file) or not sudo.path_isfile(pid_file):
      raise Fail(f"PID file {pid_file} was replaced while it was being read")
  except Fail:
    raise
  except Exception as error:
    if not _path_exists(pid_file):
      return None
    raise Fail(f"Could not read PID file {pid_file}: {error}") from error

  try:
    value = _decode_ascii(value, f"PID file {pid_file}").strip()
    if not _PID_PATTERN.fullmatch(value):
      raise Fail(f"PID file {pid_file} does not contain one positive integer")
    pid = int(value)
    if pid <= 0:
      raise Fail(f"PID file {pid_file} does not contain one positive integer")
    return pid
  except Fail:
    if fail_on_invalid:
      raise
    return None


def _read_process_identity(pid):
  process_dir = f"/proc/{pid}"
  if not _path_exists(process_dir):
    return None

  try:
    uid = sudo.stat(process_dir).st_uid
    stat_value = _decode_ascii(
      sudo.read_file(f"{process_dir}/stat"), f"Process status for pid {pid}"
    )
    cmdline_value = sudo.read_file(f"{process_dir}/cmdline")
  except Exception as error:
    if not _path_exists(process_dir):
      return None
    if isinstance(error, Fail):
      raise
    raise Fail(f"Could not inspect process {pid}: {error}") from error

  command_end = stat_value.rfind(")")
  stat_fields = stat_value[command_end + 1 :].split() if command_end >= 0 else []
  if len(stat_fields) <= 19:
    raise Fail(f"Process status for pid {pid} is malformed")
  process_state = stat_fields[0]
  try:
    start_time = int(stat_fields[19])
  except ValueError as error:
    raise Fail(f"Process status for pid {pid} has an invalid start time") from error

  try:
    if isinstance(cmdline_value, bytes):
      argv = tuple(
        item.decode("utf-8", errors="surrogateescape")
        for item in cmdline_value.split(b"\0")
        if item
      )
    elif isinstance(cmdline_value, str):
      argv = tuple(item for item in cmdline_value.split("\0") if item)
    else:
      raise TypeError("command line is not text")
  except (TypeError, UnicodeError) as error:
    raise Fail(f"Could not decode command line for pid {pid}") from error

  return ProcessIdentity(pid, uid, start_time, argv, process_state)


def _expected_uid(expected_user):
  if not expected_user:
    return None
  try:
    return pwd.getpwnam(expected_user).pw_uid
  except (KeyError, OSError) as error:
    raise Fail(f"Could not resolve service user {expected_user}") from error


def _normalize_cmdline_fragments(expected_cmdline):
  if expected_cmdline is None:
    return ()
  if isinstance(expected_cmdline, str):
    expected_cmdline = (expected_cmdline,)
  fragments = tuple(expected_cmdline)
  if not fragments or any(not isinstance(item, str) or not item for item in fragments):
    raise Fail("Expected process command line contains an empty or invalid fragment")
  return fragments


def inspect_process(pid, expected_user=None, expected_cmdline=None):
  identity = _read_process_identity(pid)
  if identity is None or identity.state in ("Z", "X"):
    return None

  expected_uid = _expected_uid(expected_user)
  if expected_uid is not None and identity.uid != expected_uid:
    raise Fail(
      f"Refusing process {pid}: owner does not match service user {expected_user}"
    )

  fragments = _normalize_cmdline_fragments(expected_cmdline)
  missing = [fragment for fragment in fragments if fragment not in identity.argv]
  if missing:
    raise Fail(
      f"Refusing process {pid}: command line does not match expected service identity"
    )
  return identity


def is_process_running(pid, expected_user=None, expected_cmdline=None, identity=None):
  current = inspect_process(pid, expected_user, expected_cmdline)
  if current is None:
    return False
  if identity is not None and not identity.matches(current):
    return False

  try:
    sudo.kill(pid, 0)
  except OSError as error:
    if not _path_exists(f"/proc/{pid}"):
      return False
    raise Fail(f"Could not inspect running state of pid {pid}: {error}") from error
  except Exception as error:
    raise Fail(f"Could not inspect running state of pid {pid}: {error}") from error

  rechecked = inspect_process(pid, expected_user, expected_cmdline)
  if rechecked is None:
    return False
  return current.matches(rechecked) and (identity is None or identity.matches(rechecked))


def read_running_process(pid_file, expected_user=None, expected_cmdline=None):
  pid = read_pid(pid_file)
  if pid is None:
    return None
  identity = inspect_process(pid, expected_user, expected_cmdline)
  if identity is None:
    return None
  return identity if is_process_running(
    pid, expected_user, expected_cmdline, identity=identity
  ) else None


def discover_running_process(expected_user, expected_cmdline):
  if not expected_user:
    raise Fail("A service user is required for process discovery")
  expected_uid = _expected_uid(expected_user)
  fragments = _normalize_cmdline_fragments(expected_cmdline)
  if not fragments:
    raise Fail("Exact command line tokens are required for process discovery")

  try:
    proc_entries = sudo.listdir("/proc")
  except Exception as error:
    raise Fail(f"Could not enumerate processes: {error}") from error

  matches = []
  for entry in proc_entries:
    entry = str(entry)
    if not entry.isascii() or not entry.isdigit():
      continue
    pid = int(entry)
    identity = _read_process_identity(pid)
    if identity is None or identity.uid != expected_uid:
      continue
    if any(fragment not in identity.argv for fragment in fragments):
      continue
    if is_process_running(
      pid, expected_user, fragments, identity=identity
    ):
      matches.append(identity)

  if len(matches) > 1:
    matching_pids = ", ".join(str(identity.pid) for identity in matches)
    raise Fail(
      f"Refusing ambiguous process discovery for {expected_user}: {matching_pids}"
    )
  return matches[0] if matches else None


def wait_for_discovered_process(
  expected_user,
  expected_cmdline,
  attempts=5,
  sleep_seconds=1,
):
  for attempt in range(attempts):
    identity = discover_running_process(expected_user, expected_cmdline)
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail(f"Service process for user {expected_user} was not discovered")


def create_pid_file_for_identity(
  pid_file,
  identity,
  expected_user,
  expected_cmdline,
  owner,
  group,
  mode=0o640,
):
  if not pid_file:
    raise Fail("PID file is not configured")
  if _path_islink(pid_file) or _path_exists(pid_file):
    raise Fail(f"Refusing to replace existing PID file {pid_file}")
  if not is_process_running(
    identity.pid,
    expected_user,
    expected_cmdline,
    identity=identity,
  ):
    raise Fail(f"Process {identity.pid} disappeared before PID file creation")

  temp_pid_file = os.path.join(
    os.path.dirname(pid_file), f".ambari-pid-{uuid.uuid4().hex}"
  )
  published_file_identity = None
  try:
    File(
      temp_pid_file,
      content=f"{identity.pid}\n",
      owner=owner,
      group=group,
      mode=mode,
      replace=False,
    )
    temp_stat = sudo.lstat(temp_pid_file)
    if _path_islink(temp_pid_file) or not stat.S_ISREG(temp_stat.st_mode):
      raise Fail(f"Temporary PID file {temp_pid_file} is not a regular file")
    published_file_identity = (temp_stat.st_dev, temp_stat.st_ino)
    if _path_islink(pid_file) or _path_exists(pid_file):
      raise Fail(f"PID file {pid_file} was created concurrently")
    try:
      sudo.link_exclusive(temp_pid_file, pid_file)
    except Exception as error:
      raise Fail(f"Could not create PID file {pid_file}: {error}") from error
  finally:
    if _path_exists(temp_pid_file):
      sudo.unlink(temp_pid_file)

  try:
    written_identity = read_running_process(
      pid_file, expected_user, expected_cmdline
    )
  except Exception:
    _rollback_pid_file(pid_file, published_file_identity)
    raise
  if written_identity is None or not identity.matches(written_identity):
    _rollback_pid_file(pid_file, published_file_identity)
    raise Fail(
      f"Process identity changed while creating PID file {pid_file}"
    )
  return written_identity


def secure_pid_file_for_identity(
  pid_file,
  identity,
  expected_user,
  expected_cmdline,
  owner,
  group,
  mode=0o640,
):
  """Secure a launcher-created PID file without following a replaced path."""
  if not pid_file:
    raise Fail("PID file is not configured")
  if identity is None or not is_process_running(
    identity.pid,
    expected_user,
    expected_cmdline,
    identity=identity,
  ):
    raise Fail("Process disappeared before securing its PID file")
  if not isinstance(mode, int) or mode < 0 or mode > 0o777:
    raise Fail("PID file mode must contain only permission bits")
  if not hasattr(os, "O_NOFOLLOW"):
    raise Fail("Safe PID file permissions require O_NOFOLLOW support")

  expected_uid = _expected_uid(owner)
  try:
    expected_gid = grp.getgrnam(group).gr_gid
  except (KeyError, TypeError) as error:
    raise Fail(f"Expected PID file group {group!r} does not exist") from error

  flags = os.O_RDONLY | os.O_NONBLOCK | os.O_CLOEXEC | os.O_NOFOLLOW
  descriptor = None
  try:
    descriptor = os.open(pid_file, flags)
    initial_stat = os.fstat(descriptor)
    if not stat.S_ISREG(initial_stat.st_mode) or initial_stat.st_nlink != 1:
      raise Fail(f"PID file {pid_file} must be a singly linked regular file")
    if initial_stat.st_uid != expected_uid:
      raise Fail(
        f"PID file {pid_file} owner does not match user {owner}"
      )

    value = _decode_ascii(
      os.read(descriptor, 64), f"PID file {pid_file}"
    ).strip()
    if _PID_PATTERN.fullmatch(value) is None or int(value) != identity.pid:
      raise Fail(f"PID file {pid_file} does not identify process {identity.pid}")

    os.fchown(descriptor, expected_uid, expected_gid)
    os.fchmod(descriptor, mode)
    secured_stat = os.fstat(descriptor)
    if (
      (secured_stat.st_dev, secured_stat.st_ino)
      != (initial_stat.st_dev, initial_stat.st_ino)
      or secured_stat.st_uid != expected_uid
      or secured_stat.st_gid != expected_gid
      or stat.S_IMODE(secured_stat.st_mode) != mode
    ):
      raise Fail(f"PID file {pid_file} permissions could not be secured")

    path_stat = sudo.lstat(pid_file)
    if (
      not stat.S_ISREG(path_stat.st_mode)
      or path_stat.st_nlink != 1
      or (path_stat.st_dev, path_stat.st_ino)
      != (secured_stat.st_dev, secured_stat.st_ino)
    ):
      raise Fail(f"PID file {pid_file} was replaced while securing permissions")
  except Fail:
    raise
  except Exception as error:
    raise Fail(f"Could not secure PID file {pid_file}: {error}") from error
  finally:
    if descriptor is not None:
      os.close(descriptor)

  published = read_running_process(pid_file, expected_user, expected_cmdline)
  if published is None or not identity.matches(published):
    raise Fail(f"Process identity changed while securing PID file {pid_file}")
  return published


def publish_pid_file_for_identity(
  pid_file,
  identity,
  expected_user,
  expected_cmdline,
  owner,
  group,
  mode=0o640,
):
  """Create a missing PID file or secure a launcher-created one."""
  launcher_pid = read_pid(pid_file)
  if launcher_pid is None:
    return create_pid_file_for_identity(
      pid_file,
      identity,
      expected_user,
      expected_cmdline,
      owner,
      group,
      mode,
    )
  if launcher_pid != identity.pid:
    raise Fail(
      f"PID file {pid_file} identifies process {launcher_pid}, "
      f"expected {identity.pid}"
    )
  return secure_pid_file_for_identity(
    pid_file,
    identity,
    expected_user,
    expected_cmdline,
    owner,
    group,
    mode,
  )


def _rollback_pid_file(pid_file, published_file_identity):
  if published_file_identity is None:
    return
  if _path_islink(pid_file) or not _path_exists(pid_file):
    return
  try:
    current_stat = sudo.lstat(pid_file)
  except Exception as error:
    raise Fail(f"Could not inspect PID file {pid_file} for rollback: {error}") from error
  if not stat.S_ISREG(current_stat.st_mode):
    return
  current_file_identity = (current_stat.st_dev, current_stat.st_ino)
  if current_file_identity != published_file_identity:
    return
  try:
    sudo.unlink(pid_file)
  except Exception as error:
    if not _path_exists(pid_file):
      return
    raise Fail(f"Could not roll back PID file {pid_file}: {error}") from error


def wait_for_running_process(
  pid_file,
  expected_user,
  expected_cmdline,
  attempts=5,
  sleep_seconds=1,
):
  for attempt in range(attempts):
    identity = read_running_process(pid_file, expected_user, expected_cmdline)
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail(f"Service did not start with a valid process in PID file {pid_file}")


def wait_for_process_stopped(
  identity,
  expected_user,
  expected_cmdline,
  attempts,
  sleep_seconds,
):
  for attempt in range(attempts):
    current = inspect_process(identity.pid, expected_user, expected_cmdline)
    if current is None:
      return True
    if not identity.matches(current):
      raise Fail(
        f"Refusing process {identity.pid}: PID was reused while waiting for stop"
      )
    if not is_process_running(
      identity.pid,
      expected_user,
      expected_cmdline,
      identity=identity,
    ):
      return True
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  return False


def _send_signal(identity, expected_user, expected_cmdline, process_signal):
  current = inspect_process(identity.pid, expected_user, expected_cmdline)
  if current is None:
    return False
  if not identity.matches(current):
    raise Fail(f"Refusing process {identity.pid}: PID was reused before signaling")

  try:
    result = sudo.signal_process(
      identity.pid,
      identity.uid,
      identity.start_time,
      _normalize_cmdline_fragments(expected_cmdline),
      process_signal,
    )
  except Exception as error:
    if not _path_exists(f"/proc/{identity.pid}"):
      return False
    raise Fail(f"Could not signal pid {identity.pid}: {error}") from error
  if result == "gone":
    return False
  if result not in ("signaled-pidfd", "signaled-legacy"):
    raise Fail(f"Unexpected signaling result for pid {identity.pid}: {result}")
  return True


def terminate_process(
  identity,
  expected_user,
  expected_cmdline,
  term_wait_attempts=10,
  term_wait_sleep=1,
  kill_wait_attempts=10,
  kill_wait_sleep=1,
):
  if identity is None:
    return
  if not _send_signal(
    identity, expected_user, expected_cmdline, signal.SIGTERM.value
  ):
    return
  if wait_for_process_stopped(
    identity,
    expected_user,
    expected_cmdline,
    term_wait_attempts,
    term_wait_sleep,
  ):
    return

  _send_signal(identity, expected_user, expected_cmdline, signal.SIGKILL.value)
  if not wait_for_process_stopped(
    identity,
    expected_user,
    expected_cmdline,
    kill_wait_attempts,
    kill_wait_sleep,
  ):
    raise Fail(f"Process with pid {identity.pid} did not stop")


def remove_pid_file_if_stopped(
  pid_file,
  expected_pid,
  expected_user=None,
  expected_cmdline=None,
):
  current_pid = read_pid(pid_file)
  if current_pid is None:
    return False
  if current_pid != expected_pid:
    raise Fail(
      f"Refusing to remove PID file {pid_file}: pid changed from "
      f"{expected_pid} to {current_pid}"
    )
  if is_process_running(current_pid, expected_user, expected_cmdline):
    raise Fail(f"Refusing to remove PID file for running process {current_pid}")
  rechecked_pid = read_pid(pid_file)
  if rechecked_pid != expected_pid:
    raise Fail(
      f"Refusing to remove PID file {pid_file}: pid changed from "
      f"{expected_pid} to {rechecked_pid} before cleanup"
    )
  if _path_islink(pid_file) or not sudo.path_isfile(pid_file):
    raise Fail(f"Refusing to remove replaced PID file {pid_file}")
  try:
    sudo.unlink(pid_file)
  except Exception as error:
    if not _path_exists(pid_file):
      return False
    raise Fail(f"Could not remove PID file {pid_file}: {error}") from error
  return True
