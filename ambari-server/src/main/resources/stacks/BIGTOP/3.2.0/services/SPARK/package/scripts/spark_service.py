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
from resource_management.core import sudo
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.show_logs import show_logs

import spark_process
import spark_utils


def _component_contract(params, name):
  if name == "jobhistoryserver":
    return params.spark_history_server_pid_file, params.spark_defaults_file
  if name == "sparkthriftserver":
    return params.spark_thrift_server_pid_file, params.spark_defaults_file
  raise Fail(f"Unsupported Spark component: {name}")


def _start(params, name):
  pid_file, conf_file = _component_contract(params, name)
  identity = spark_process.read_or_recover_process(
    name, pid_file, params.spark_user, params.user_group, conf_file
  )
  if identity is not None:
    Logger.info(f"Spark {name} is already running with pid {identity.pid}")
    return

  if params.security_enabled:
    if name == "jobhistoryserver":
      spark_utils.validate_keytab(
        params.spark_history_kerberos_keytab, "Spark History Server keytab"
      )
      spark_utils.validate_keytab(
        params.spnego_keytab, "Spark History Server SPNEGO keytab"
      )
    else:
      spark_utils.validate_principal(
        params.spark_thrift_kerberos_principal,
        "Spark Thrift Server application principal",
      )
      spark_utils.validate_keytab(
        params.spark_thrift_kerberos_keytab,
        "Spark Thrift Server application keytab",
      )
      spark_utils.validate_keytab(
        params.hive_kerberos_keytab, "Spark Thrift Server service keytab"
      )

  if name == "jobhistoryserver":
    params.HdfsResource(
      params.spark_history_dir,
      type="directory",
      action="create_on_execute",
      owner=params.spark_user,
      group=params.user_group,
      mode=0o770,
    )
    params.HdfsResource(None, action="execute")
    spark_utils.validate_executable(params.spark_class, "Spark class launcher")
    command = (
      params.spark_class,
      spark_process.HISTORY_SERVER_MAIN_CLASS,
      "--properties-file",
      conf_file,
    )
  else:
    spark_utils.validate_executable(params.spark_submit, "Spark submit launcher")
    command = (
      params.spark_submit,
      "--class",
      spark_process.THRIFT_SERVER_MAIN_CLASS,
      "--name",
      "Thrift JDBC/ODBC Server",
      "--properties-file",
      conf_file,
    ) + params.spark_thrift_cmd_opts

  try:
    log_file = params.spark_log_dir + (
      "/spark-history-server.log"
      if name == "jobhistoryserver"
      else "/spark-thrift-server.log"
    )
    if sudo.path_lexists(log_file):
      spark_utils.validate_regular_file(log_file, "Spark service log")
    File(log_file, owner=params.spark_user, group=params.user_group, mode=0o640)
    Execute(
      command,
      user=params.spark_user,
      environment={
        "HADOOP_CONF_DIR": params.hadoop_conf_dir,
        "JAVA_HOME": params.java_home,
        "SPARK_CONF_DIR": params.spark_conf_dir,
        "SPARK_LOG_DIR": params.spark_log_dir,
        "SPARK_LOG_FILE": log_file,
      },
      wait_for_finish=False,
    )
    spark_process.wait_for_started_process(
      name, pid_file, params.spark_user, params.user_group, conf_file
    )
  except Exception as start_error:
    cleanup_error = None
    try:
      spark_process.stop_process(name, pid_file, params.spark_user, params.user_group, conf_file)
    except Exception as error:
      cleanup_error = error
    try:
      show_logs(params.spark_log_dir, user=params.spark_user)
    except Exception as error:
      Logger.warning(f"Could not collect Spark logs after start failure: {error}")
    if cleanup_error is not None:
      raise Fail(
        f"Spark {name} start failed: {start_error}; cleanup also failed: "
        f"{cleanup_error}"
      ) from start_error
    raise


def spark_service(name, upgrade_type=None, action=None):
  if action == "start":
    import params

    _start(params, name)
    return
  if action == "stop":
    import status_params as params

    pid_file, conf_file = _component_contract(params, name)
    if not spark_process.stop_process(
      name, pid_file, params.spark_user, params.user_group, conf_file
    ):
      Logger.info(f"Spark {name} is not running")
    return
  raise Fail(f"Unsupported Spark service action: {action}")
