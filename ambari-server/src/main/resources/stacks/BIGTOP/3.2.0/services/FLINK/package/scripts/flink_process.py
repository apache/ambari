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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import safe_process

import flink_utils


HISTORY_SERVER_MAIN_CLASS = "org.apache.flink.runtime.webmonitor.history.HistoryServer"


def validate_pid_file(pid_file):
  flink_utils.validate_absolute_path(pid_file, "Flink History Server PID file")
  if os.path.basename(pid_file) != "flink_history_server.pid":
    raise Fail("Flink History Server PID file must be named flink_history_server.pid")
  flink_utils.validate_service_directory(
    os.path.dirname(pid_file), "Flink PID directory"
  )


def expected_process_tokens(config_dir):
  flink_utils.validate_absolute_path(config_dir, "Flink configuration directory")
  return (HISTORY_SERVER_MAIN_CLASS, "--configDir", config_dir)


def _publish_identity(pid_file, identity, user, group, expected_tokens):
  try:
    return safe_process.create_pid_file_for_identity(
      pid_file,
      identity,
      expected_user=user,
      expected_cmdline=expected_tokens,
      owner=user,
      group=group,
      mode=0o640,
    )
  except Fail:
    current = safe_process.read_running_process(pid_file, user, expected_tokens)
    if current is not None and identity.matches(current):
      return current
    raise


def read_or_recover_process(pid_file, user, group, config_dir):
  validate_pid_file(pid_file)
  expected_tokens = expected_process_tokens(config_dir)
  recorded_pid = safe_process.read_pid(pid_file)
  if recorded_pid is not None:
    identity = safe_process.read_running_process(pid_file, user, expected_tokens)
    if identity is not None:
      return identity
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      recorded_pid,
      expected_user=user,
      expected_cmdline=expected_tokens,
    )

  identity = safe_process.discover_running_process(user, expected_tokens)
  if identity is None:
    return None
  return _publish_identity(pid_file, identity, user, group, expected_tokens)


def wait_for_started_process(
  pid_file, user, group, config_dir, attempts=30, sleep_seconds=1
):
  validate_pid_file(pid_file)
  expected_tokens = expected_process_tokens(config_dir)
  identity = safe_process.wait_for_discovered_process(
    user,
    expected_tokens,
    attempts=attempts,
    sleep_seconds=sleep_seconds,
  )
  try:
    return _publish_identity(pid_file, identity, user, group, expected_tokens)
  except Exception:
    try:
      stop_process(
        pid_file,
        user,
        group,
        config_dir,
        expected_identity=identity,
        allow_discovery=False,
      )
    except Exception as rollback_error:
      Logger.warning(
        f"Could not roll back failed Flink History Server start: {rollback_error}"
      )
    raise


def _read_process_from_pid_file(pid_file, user, expected_tokens):
  identity = safe_process.read_running_process(pid_file, user, expected_tokens)
  return identity


def stop_process(
  pid_file,
  user,
  group,
  config_dir,
  expected_identity=None,
  candidate_pid_file=None,
  allow_discovery=True,
):
  expected_tokens = expected_process_tokens(config_dir)
  rollback_mode = (
    expected_identity is not None
    or candidate_pid_file is not None
    or not allow_discovery
  )
  identity = expected_identity
  if identity is not None:
    current = safe_process.inspect_process(identity.pid, user, expected_tokens)
    if current is None:
      return False
    if not identity.matches(current) or not safe_process.is_process_running(
      identity.pid, user, expected_tokens, identity=identity
    ):
      raise Fail(
        f"Refusing to stop changed Flink History Server pid {identity.pid}"
      )
  elif candidate_pid_file is not None:
    identity = _read_process_from_pid_file(
      candidate_pid_file, user, expected_tokens
    )
  if identity is None and allow_discovery:
    identity = read_or_recover_process(pid_file, user, group, config_dir)
  elif identity is None and candidate_pid_file is None:
    identity = _read_process_from_pid_file(pid_file, user, expected_tokens)
  if identity is None:
    return False
  safe_process.terminate_process(
    identity,
    user,
    expected_tokens,
    term_wait_attempts=30,
    term_wait_sleep=1,
    kill_wait_attempts=10,
    kill_wait_sleep=1,
  )
  if rollback_mode:
    pid = safe_process.read_pid(pid_file)
    if pid != identity.pid:
      return True
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=expected_tokens,
  )
  return True
