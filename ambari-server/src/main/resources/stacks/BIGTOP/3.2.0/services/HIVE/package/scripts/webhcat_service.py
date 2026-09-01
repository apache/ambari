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

Ambari Agent

"""

import traceback
from functools import partial

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.core.resources.service import Service
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Execute, File

from hive_pid_utils import (
  force_stop_process,
  is_pid_file_process_running,
  read_pid,
)


def webhcat_service(action="start", upgrade_type=None):
  import params

  cmd = format("{webhcat_bin_dir}/webhcat_server.sh")

  if action == "start":
    daemon_cmd = format("cd {hcat_pid_dir} ; {cmd} start")
    process_is_running = partial(
      is_pid_file_process_running, params.webhcat_pid_file, params.webhcat_user
    )
    try:
      Execute(
        daemon_cmd,
        environment={"HIVE_HOME": params.hive_home},
        user=params.webhcat_user,
        not_if=process_is_running,
      )
    except:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      raise
  elif action == "stop":
    try:
      # try stopping WebHCat using its own script
      graceful_stop(cmd)
    except Fail:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      Logger.info(traceback.format_exc())

    try:
      force_stop_process(
        read_pid(params.webhcat_pid_file, fail_on_invalid=True),
        params.webhcat_user,
      )
    except Exception:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      raise

    File(params.webhcat_pid_file, action="delete")


def graceful_stop(cmd):
  """
  Attemps to stop WebHCat using its own shell script. On some versions this may not correctly
  stop the daemon.
  :param cmd: the command to run to stop the daemon
  :return:
  """
  import params

  daemon_cmd = format("{cmd} stop")

  Execute(
    daemon_cmd, environment={"HIVE_HOME": params.hive_home}, user=params.webhcat_user
  )
