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
import time

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import safe_process
from resource_management.libraries.functions.show_logs import show_logs


HBASE_PROCESS_TOKENS = {
  "master": ("org.apache.hadoop.hbase.master.HMaster",),
  "regionserver": ("org.apache.hadoop.hbase.regionserver.HRegionServer",),
  "thrift": ("org.apache.hadoop.hbase.thrift.ThriftServer",),
}


def expected_process_tokens(role):
  try:
    return HBASE_PROCESS_TOKENS[role]
  except KeyError as error:
    raise Fail(f"Unsupported HBase service role: {role}") from error


def pid_file_for_role(pid_dir, user, role):
  expected_process_tokens(role)
  return os.path.join(pid_dir, f"hbase-{user}-{role}.pid")


def read_or_discover_hbase_process(pid_file, user, group, role):
  tokens = expected_process_tokens(role)
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.inspect_process(pid, user, tokens)
    if identity is not None and safe_process.is_process_running(
      pid, user, tokens, identity=identity
    ):
      return identity
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=tokens,
    )

  identity = safe_process.discover_running_process(user, tokens)
  if identity is None:
    return None
  return safe_process.create_pid_file_for_identity(
    pid_file,
    identity,
    user,
    tokens,
    owner=user,
    group=group,
    mode=0o640,
  )


def wait_for_hbase_process(
  pid_file, user, group, role, attempts=10, sleep_seconds=1
):
  for attempt in range(attempts):
    identity = read_or_discover_hbase_process(pid_file, user, group, role)
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail(f"HBase {role} did not start with a valid process identity")


def check_hbase_process_status(pid_file, user, group, role):
  identity = read_or_discover_hbase_process(pid_file, user, group, role)
  if identity is None:
    raise ComponentIsNotRunning(f"HBase {role} is not running")
  return identity


def hbase_service(name, action=None, extra_args=()):
  import params

  tokens = expected_process_tokens(name)
  if action not in ("start", "stop"):
    raise Fail(f"Unsupported HBase service action: {action}")
  if not isinstance(extra_args, (tuple, list)) or any(
    not isinstance(argument, str) or not argument for argument in extra_args
  ):
    raise Fail("HBase daemon arguments must be non-empty argv tokens")

  pid_file = pid_file_for_role(params.pid_dir, params.hbase_user, name)
  identity = read_or_discover_hbase_process(
    pid_file, params.hbase_user, params.user_group, name
  )

  if action == "start":
    if identity is not None:
      Logger.info(f"HBase {name} is already running with pid {identity.pid}")
      return

    command = (
      params.daemon_script,
      "--config",
      params.hbase_conf_dir,
      "start",
      name,
      *extra_args,
    )
    try:
      Execute(
        command,
        user=params.hbase_user,
        environment={"JAVA_HOME": params.java64_home},
        logoutput=True,
        timeout=60,
      )
      wait_for_hbase_process(
        pid_file, params.hbase_user, params.user_group, name
      )
    except Exception:
      show_logs(params.log_dir, params.hbase_user)
      raise
    return

  if identity is None:
    Logger.info(f"No running HBase {name} process was found")
    return

  try:
    wait_attempts = max(1, int(params.hbase_regionserver_shutdown_timeout))
    safe_process.terminate_process(
      identity,
      params.hbase_user,
      tokens,
      term_wait_attempts=wait_attempts,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      identity.pid,
      expected_user=params.hbase_user,
      expected_cmdline=tokens,
    )
  except Exception:
    show_logs(params.log_dir, params.hbase_user)
    raise
