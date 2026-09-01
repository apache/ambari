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
import re
import uuid

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.show_logs import show_logs

import flink_process
import flink_utils


_LAUNCHER_DIRECTORY_PATTERN = re.compile(
  r"\.ambari-historyserver-[0-9a-f]{32}", re.ASCII
)


def _launcher_pid_paths(pid_dir):
  flink_utils.validate_service_directory(pid_dir, "Flink PID directory")
  launcher_dir = os.path.join(
    pid_dir, f".ambari-historyserver-{uuid.uuid4().hex}"
  )
  launcher_pid = os.path.join(
    launcher_dir, "flink-ambari-historyserver-historyserver.pid"
  )
  return launcher_dir, launcher_pid


def _cleanup_launcher_pid(launcher_dir, launcher_pid, pid_dir):
  if (
    os.path.dirname(launcher_dir) != pid_dir
    or _LAUNCHER_DIRECTORY_PATTERN.fullmatch(os.path.basename(launcher_dir)) is None
    or os.path.dirname(launcher_pid) != launcher_dir
    or os.path.basename(launcher_pid)
    != "flink-ambari-historyserver-historyserver.pid"
  ):
    raise Fail("Refusing to clean an unexpected Flink launcher PID path")
  File(launcher_pid, action="delete")
  Directory(launcher_dir, action="delete")


def _start_history_server(params):
  identity = flink_process.read_or_recover_process(
    params.flink_history_server_pid_file,
    params.flink_user,
    params.user_group,
    params.flink_config_dir,
  )
  if identity is not None:
    Logger.info(f"Flink History Server is already running with pid {identity.pid}")
    return

  params.HdfsResource(
    params.jobmanager_archive_fs_dir,
    type="directory",
    action="create_on_execute",
    owner=params.flink_user,
    group=params.user_group,
    mode=0o770,
  )
  params.HdfsResource(None, action="execute")

  flink_utils.validate_executable(
    params.historyserver_script, "Flink History Server launcher"
  )
  if params.security_enabled:
    flink_utils.validate_keytab(
      params.security_kerberos_login_keytab, "Flink service keytab"
    )
  hadoop_classpath = flink_utils.resolve_hadoop_classpath(
    params.hadoop_executable,
    params.flink_user,
    params.java_home,
  )
  launcher_dir, launcher_pid = _launcher_pid_paths(params.flink_pid_dir)
  Directory(
    launcher_dir,
    owner=params.flink_user,
    group=params.user_group,
    mode=0o700,
  )

  operation_error = None
  try:
    Execute(
      (params.historyserver_script, "start-foreground"),
      user=params.flink_user,
      environment={
        "FLINK_CONF_DIR": params.flink_config_dir,
        "FLINK_IDENT_STRING": "ambari-historyserver",
        "FLINK_LOG_DIR": params.flink_log_dir,
        "FLINK_PID_DIR": launcher_dir,
        "HADOOP_CLASSPATH": hadoop_classpath,
        "HADOOP_CONF_DIR": params.hadoop_conf_dir,
        "JAVA_HOME": params.java_home,
      },
      wait_for_finish=False,
    )
    flink_process.wait_for_started_process(
      params.flink_history_server_pid_file,
      params.flink_user,
      params.user_group,
      params.flink_config_dir,
    )
  except Exception as error:
    operation_error = error
    try:
      show_logs(params.flink_log_dir, user=params.flink_user)
    except Exception as log_error:
      Logger.warning(f"Could not collect Flink logs after start failure: {log_error}")

  try:
    _cleanup_launcher_pid(launcher_dir, launcher_pid, params.flink_pid_dir)
  except Exception as cleanup_error:
    if operation_error is not None:
      raise Fail(
        f"Flink History Server start failed: {operation_error}; launcher PID "
        f"cleanup also failed: {cleanup_error}"
      ) from operation_error
    raise
  if operation_error is not None:
    raise operation_error


def flink_service(name, upgrade_type=None, action=None):
  if name != "historyserver":
    raise Fail(f"Unsupported Flink service name: {name}")

  if action == "start":
    import params

    _start_history_server(params)
    return

  if action == "stop":
    import status_params as params

    stopped = flink_process.stop_process(
      params.flink_history_server_pid_file,
      params.flink_user,
      params.user_group,
      params.flink_config_dir,
    )
    if not stopped:
      Logger.info("Flink History Server is not running")
    return

  raise Fail(f"Unsupported Flink service action: {action}")
