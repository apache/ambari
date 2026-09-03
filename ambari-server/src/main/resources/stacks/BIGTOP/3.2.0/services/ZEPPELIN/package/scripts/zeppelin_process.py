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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import safe_process


ZEPPELIN_PROCESS_TOKENS = ("org.apache.zeppelin.server.ZeppelinServer",)


def _publish_zeppelin_process(pid_file, identity, user, group):
  return safe_process.publish_pid_file_for_identity(
    pid_file,
    identity,
    user,
    ZEPPELIN_PROCESS_TOKENS,
    owner=user,
    group=group,
    mode=0o640,
  )


def read_or_discover_zeppelin_process(pid_file, user, group):
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.inspect_process(pid, user, ZEPPELIN_PROCESS_TOKENS)
    if identity is not None and safe_process.is_process_running(
      pid,
      user,
      ZEPPELIN_PROCESS_TOKENS,
      identity=identity,
    ):
      return _publish_zeppelin_process(pid_file, identity, user, group)
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=ZEPPELIN_PROCESS_TOKENS,
    )

  identity = safe_process.discover_running_process(user, ZEPPELIN_PROCESS_TOKENS)
  if identity is None:
    return None
  return _publish_zeppelin_process(pid_file, identity, user, group)


def wait_for_zeppelin_process(pid_file, user, group, attempts=30, sleep_seconds=1):
  identity = safe_process.wait_for_discovered_process(
    user,
    ZEPPELIN_PROCESS_TOKENS,
    attempts=attempts,
    sleep_seconds=sleep_seconds,
  )
  try:
    return _publish_zeppelin_process(pid_file, identity, user, group)
  except Exception:
    try:
      rollback_started_zeppelin_process(pid_file, identity, user)
    except Exception as error:
      Logger.warning(
        f"Could not roll back Zeppelin after PID publication failure: {error}"
      )
    raise


def rollback_started_zeppelin_process(pid_file, identity, user):
  safe_process.terminate_process(identity, user, ZEPPELIN_PROCESS_TOKENS)
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=ZEPPELIN_PROCESS_TOKENS,
  )


def start_zeppelin(daemon, conf_dir, pid_file, user, group, java_home):
  identity = read_or_discover_zeppelin_process(pid_file, user, group)
  if identity is not None:
    Logger.info(f"Zeppelin Server is already running with pid {identity.pid}")
    return identity

  Execute(
    (daemon, "--config", conf_dir, "start"),
    user=user,
    environment={"JAVA_HOME": java_home},
    timeout=60,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    logoutput=True,
  )
  return wait_for_zeppelin_process(pid_file, user, group)


def stop_zeppelin(pid_file, user, group):
  identity = read_or_discover_zeppelin_process(pid_file, user, group)
  if identity is None:
    Logger.info("No running Zeppelin Server process was found")
    return

  safe_process.terminate_process(identity, user, ZEPPELIN_PROCESS_TOKENS)
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=ZEPPELIN_PROCESS_TOKENS,
  )
