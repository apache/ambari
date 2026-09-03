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


KAFKA_MAIN_CLASS = "kafka.Kafka"


def validate_pid_file(pid_file):
  if (
    not isinstance(pid_file, str)
    or not os.path.isabs(pid_file)
    or os.path.normpath(pid_file) != pid_file
    or os.path.dirname(pid_file) == os.path.sep
    or os.path.basename(pid_file) != "kafka.pid"
  ):
    raise Fail(f"Kafka PID file {pid_file!r} is not a safe absolute path")
  parent = os.path.dirname(pid_file)
  protected_trees = ("/boot", "/dev", "/etc", "/proc", "/sys", "/usr")
  protected_directories = {
    "/bin",
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
  if parent in protected_directories or any(
    parent == root or parent.startswith(root + os.path.sep)
    for root in protected_trees
  ):
    raise Fail(f"Kafka PID file {pid_file!r} is inside a protected directory")


def expected_process_tokens(server_properties):
  return (KAFKA_MAIN_CLASS, server_properties)


def _publish_identity(pid_file, identity, user, group, expected_tokens):
  return safe_process.publish_pid_file_for_identity(
    pid_file,
    identity,
    expected_user=user,
    expected_cmdline=expected_tokens,
    owner=user,
    group=group,
    mode=0o640,
  )


def read_or_recover_process(pid_file, user, group, server_properties):
  validate_pid_file(pid_file)
  expected_tokens = expected_process_tokens(server_properties)
  recorded_pid = safe_process.read_pid(pid_file)
  if recorded_pid is not None:
    identity = safe_process.read_running_process(pid_file, user, expected_tokens)
    if identity is not None:
      return _publish_identity(pid_file, identity, user, group, expected_tokens)
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
  pid_file,
  user,
  group,
  server_properties,
  attempts=30,
  sleep_seconds=1,
):
  validate_pid_file(pid_file)
  expected_tokens = expected_process_tokens(server_properties)
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
      rollback_started_process(pid_file, identity, user, server_properties)
    except Exception as error:
      Logger.warning(f"Could not roll back Kafka after PID publication failure: {error}")
    raise


def rollback_started_process(pid_file, identity, user, server_properties):
  validate_pid_file(pid_file)
  expected_tokens = expected_process_tokens(server_properties)
  safe_process.terminate_process(
    identity,
    user,
    expected_tokens,
    term_wait_attempts=30,
    term_wait_sleep=1,
    kill_wait_attempts=5,
    kill_wait_sleep=1,
  )
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=expected_tokens,
  )


def stop_process(pid_file, user, group, server_properties):
  expected_tokens = expected_process_tokens(server_properties)
  identity = read_or_recover_process(
    pid_file, user, group, server_properties
  )
  if identity is None:
    return False

  safe_process.terminate_process(
    identity,
    user,
    expected_tokens,
    term_wait_attempts=120,
    term_wait_sleep=1,
    kill_wait_attempts=10,
    kill_wait_sleep=1,
  )
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=expected_tokens,
  )
  return True
