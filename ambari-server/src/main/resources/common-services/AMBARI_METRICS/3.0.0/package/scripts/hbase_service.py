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
from resource_management.libraries.functions.show_logs import show_logs

from metrics_process import (
  hbase_pid_file,
  read_or_discover_hbase_process,
  stop_hbase_process,
  wait_for_hbase_process,
)


def hbase_service(name, action="start"):  # 'start' or 'stop' or 'status'
  import params

  if action not in ("start", "stop"):
    raise Fail(f"Unsupported AMS HBase action: {action}")

  pid_file = hbase_pid_file(params.hbase_pid_dir, params.hbase_user, name)
  identity = read_or_discover_hbase_process(
    pid_file, params.hbase_user, params.user_group, name
  )

  if action == "start":
    if identity is not None:
      Logger.info(f"AMS HBase {name} is already running with pid {identity.pid}")
      return False

    command = (
      params.daemon_script,
      "--config",
      params.hbase_conf_dir,
      "start",
      name,
    )
    try:
      Execute(
        command,
        user=params.hbase_user,
        environment={"JAVA_HOME": params.java64_home},
        timeout=60,
      )
      wait_for_hbase_process(
        pid_file, params.hbase_user, params.user_group, name
      )
    except Exception:
      try:
        stop_hbase_process(
          pid_file,
          params.hbase_user,
          params.user_group,
          name,
          wait_attempts=max(1, int(params.hbase_regionserver_shutdown_timeout)),
        )
      except Exception as cleanup_error:
        Logger.error(f"Failed to roll back AMS HBase {name}: {cleanup_error}")
      show_logs(params.hbase_log_dir, params.hbase_user)
      raise
    return True

  if identity is None:
    Logger.info(f"No running AMS HBase {name} process was found")
    return False

  try:
    wait_attempts = max(1, int(params.hbase_regionserver_shutdown_timeout))
    return stop_hbase_process(
      pid_file,
      params.hbase_user,
      params.user_group,
      name,
      wait_attempts=wait_attempts,
    )
  except Exception:
    show_logs(params.hbase_log_dir, params.hbase_user)
    raise
