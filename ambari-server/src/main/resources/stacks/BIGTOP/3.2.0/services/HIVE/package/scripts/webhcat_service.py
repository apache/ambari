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
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import safe_process

from hive_service import (
  expected_process_tokens,
  read_or_discover_hive_process,
  _show_logs_without_masking,
  wait_for_hive_process,
)


def webhcat_service(action="start", upgrade_type=None):
  import params

  if action not in ("start", "stop"):
    raise Fail(f"Unsupported WebHCat service action: {action}")
  role = "webhcat"
  identity = read_or_discover_hive_process(
    params.webhcat_pid_file,
    params.webhcat_user,
    params.user_group,
    role,
  )

  if action == "start":
    if identity is not None:
      Logger.info(f"WebHCat is already running with pid {identity.pid}")
      return
    started_identity = None
    try:
      Execute(
        (os.path.join(params.webhcat_bin_dir, "webhcat_server.sh"), "start"),
        environment={
          "HADOOP_HOME": params.hadoop_home,
          "HIVE_CONF_DIR": params.hive_conf_dir,
          "HIVE_HOME": params.hive_home,
          "JAVA_HOME": params.java64_home,
          "WEBHCAT_CONF_DIR": params.webhcat_conf_dir,
        },
        user=params.webhcat_user,
        cwd=params.hcat_pid_dir,
        logoutput=True,
        timeout=60,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
      started_identity = wait_for_hive_process(
        params.webhcat_pid_file,
        params.webhcat_user,
        params.user_group,
        role,
      )
    except Exception:
      if started_identity is not None:
        try:
          safe_process.terminate_process(
            started_identity,
            params.webhcat_user,
            expected_process_tokens(role),
            term_wait_attempts=10,
            term_wait_sleep=1,
            kill_wait_attempts=10,
            kill_wait_sleep=1,
          )
          safe_process.remove_pid_file_if_stopped(
            params.webhcat_pid_file,
            started_identity.pid,
            expected_user=params.webhcat_user,
            expected_cmdline=expected_process_tokens(role),
          )
        except Exception as cleanup_error:
          Logger.error(f"Could not roll back failed WebHCat start: {cleanup_error}")
      _show_logs_without_masking(
        params.hcat_log_dir, params.webhcat_user, "WebHCat"
      )
      raise
    return

  if identity is None:
    Logger.info("No running WebHCat process was found")
    return
  tokens = expected_process_tokens(role)
  try:
    safe_process.terminate_process(
      identity,
      params.webhcat_user,
      tokens,
      term_wait_attempts=30,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    safe_process.remove_pid_file_if_stopped(
      params.webhcat_pid_file,
      identity.pid,
      expected_user=params.webhcat_user,
      expected_cmdline=tokens,
    )
  except Exception:
    _show_logs_without_masking(
      params.hcat_log_dir, params.webhcat_user, "WebHCat"
    )
    raise
