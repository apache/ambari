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

import time

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import safe_process


LIVY_SERVER_PROCESS_TOKENS = ("org.apache.livy.server.LivyServer",)


def rollback_started_livy_process(pid_file, identity, user):
  safe_process.terminate_process(identity, user, LIVY_SERVER_PROCESS_TOKENS)
  pid = safe_process.read_pid(pid_file)
  if pid == identity.pid:
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      identity.pid,
      expected_user=user,
      expected_cmdline=LIVY_SERVER_PROCESS_TOKENS,
    )
  return True


def read_or_discover_livy_process(
  pid_file, user, group, rollback_discovered=False
):
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.inspect_process(
      pid, user, LIVY_SERVER_PROCESS_TOKENS
    )
    if identity is not None and safe_process.is_process_running(
      pid,
      user,
      LIVY_SERVER_PROCESS_TOKENS,
      identity=identity,
    ):
      return safe_process.publish_pid_file_for_identity(
        pid_file,
        identity,
        user,
        LIVY_SERVER_PROCESS_TOKENS,
        owner=user,
        group=group,
        mode=0o640,
      )
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=LIVY_SERVER_PROCESS_TOKENS,
    )

  identity = safe_process.discover_running_process(
    user, LIVY_SERVER_PROCESS_TOKENS
  )
  if identity is None:
    return None
  try:
    return safe_process.publish_pid_file_for_identity(
      pid_file,
      identity,
      user,
      LIVY_SERVER_PROCESS_TOKENS,
      owner=user,
      group=group,
      mode=0o640,
    )
  except Exception:
    if rollback_discovered:
      try:
        rollback_started_livy_process(pid_file, identity, user)
      except Exception as rollback_error:
        Logger.warning(
          f"Could not roll back failed Livy start: {rollback_error}"
        )
    raise


def wait_for_livy_process(pid_file, user, group, attempts=10, sleep_seconds=1):
  for attempt in range(attempts):
    identity = read_or_discover_livy_process(
      pid_file, user, group, rollback_discovered=True
    )
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail("Livy Server did not start with a valid process identity")


def livy_service(name, upgrade_type=None, action=None):
  import params

  if name != "server":
    raise Fail(f"Unsupported Livy service name: {name}")

  identity = read_or_discover_livy_process(
    params.livy_server_pid_file,
    params.livy_user,
    params.livy_group,
  )

  if action == "start":
    if identity is not None:
      Logger.info(f"Livy Server is already running with pid {identity.pid}")
      return

    process_environment = {"JAVA_HOME": params.java_home}
    if params.security_enabled:
      if not params.livy_server_kerberos_cache_file:
        raise Fail("Secure Livy Server requires a private Kerberos cache path")
      process_environment["KRB5CCNAME"] = (
        f"FILE:{params.livy_server_kerberos_cache_file}"
      )
    File(params.livy_server_kerberos_cache_file, action="delete")
    Execute(
      (params.livy_server_command, "start"),
      user=params.livy_user,
      environment=process_environment,
      logoutput=True,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
    wait_for_livy_process(
      params.livy_server_pid_file,
      params.livy_user,
      params.livy_group,
    )
  elif action == "stop":
    if identity is None:
      File(params.livy_server_kerberos_cache_file, action="delete")
      Logger.info("No running Livy Server process was found")
      return

    safe_process.terminate_process(
      identity,
      params.livy_user,
      LIVY_SERVER_PROCESS_TOKENS,
    )
    safe_process.remove_pid_file_if_stopped(
      params.livy_server_pid_file,
      identity.pid,
      expected_user=params.livy_user,
      expected_cmdline=LIVY_SERVER_PROCESS_TOKENS,
    )
    File(params.livy_server_kerberos_cache_file, action="delete")
  else:
    raise Fail(f"Unsupported Livy service action: {action}")
