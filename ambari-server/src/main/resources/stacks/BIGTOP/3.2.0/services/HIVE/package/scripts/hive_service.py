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

# Python Imports
import os
from functools import partial

# Ambari Commons & Resource Management Imports
from ambari_commons.db_connection_helper import verify_db_connection
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core import shell
from resource_management.core import utils
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import File, Execute
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.stack_features import check_stack_feature

from hive_pid_utils import (
  is_pid_file_process_running,
  read_pid,
  terminate_process,
)


def hive_service(name, action="start", upgrade_type=None):
  import params
  import status_params

  if name == "metastore":
    pid_file = status_params.hive_metastore_pid
    cmd = format(
      "{start_metastore_path} {hive_log_dir}/hive.out {hive_log_dir}/hive.err {pid_file} {hive_conf_dir}"
    )
  elif name == "hiveserver2":
    pid_file = status_params.hive_pid
    cmd = format(
      "{start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.err {pid_file} {hive_conf_dir} {tez_conf_dir}"
    )

    if params.security_enabled:
      hive_kinit_cmd = format(
        "{kinit_path_local} -kt {hive_server2_keytab} {hive_principal}; "
      )
      Execute(hive_kinit_cmd, user=params.hive_user)

  process_is_running = partial(
    is_pid_file_process_running, pid_file, params.hive_user
  )

  if action == "start":
    daemon_cmd = cmd
    hadoop_home = params.hadoop_home
    hive_bin = "hive"

    # upgrading hiveserver2 (rolling_restart) means that there is an existing,
    # de-registering hiveserver2; the pid will still exist, but the new
    # hiveserver is spinning up on a new port, so the pid will be re-written
    if upgrade_type == UPGRADE_TYPE_ROLLING:
      process_is_running = None

      if params.version and params.stack_root:
        hadoop_home = format("{stack_root}/{version}/hadoop")
        hive_bin = os.path.join(params.hive_bin_dir, hive_bin)

    Execute(
      daemon_cmd,
      user=params.hive_user,
      environment={
        "HADOOP_HOME": hadoop_home,
        "JAVA_HOME": params.java64_home,
        "HIVE_BIN": hive_bin,
      },
      path=params.execute_path,
      not_if=process_is_running,
    )

    if (
      params.hive_jdbc_driver == "com.mysql.jdbc.Driver"
      or params.hive_jdbc_driver == "org.postgresql.Driver"
      or params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver"
    ):
      validation_called = False

      if params.hive_jdbc_target is not None:
        validation_called = True
        validate_connection(params.hive_jdbc_target, params.hive_lib_dir)

      if not validation_called:
        emessage = "ERROR! DB connection check should be executed at least one time!"
        Logger.error(emessage)

    if name == "hiveserver2":
      wait_for_znode()

  elif action == "stop":
    try:
      pid = read_pid(pid_file, fail_on_invalid=True)
      graceful_wait_attempts = 11 if name == "hiveserver2" else 2
      graceful_wait_sleep = 3 if name == "hiveserver2" else 5
      terminate_process(
        pid,
        params.hive_user,
        graceful_wait_attempts=graceful_wait_attempts,
        graceful_wait_sleep=graceful_wait_sleep,
      )
    except Exception:
      show_logs(params.hive_log_dir, params.hive_user)
      raise

    File(pid_file, action="delete")


def validate_connection(target_path_to_jdbc, hive_lib_path):
  import params

  path_to_jdbc = target_path_to_jdbc
  if not params.jdbc_jar_name:
    path_to_jdbc = (
      format("{hive_lib_path}/")
      + params.default_connectors_map[params.hive_jdbc_driver]
      if params.hive_jdbc_driver in params.default_connectors_map
      else None
    )
    if not os.path.isfile(path_to_jdbc):
      path_to_jdbc = format("{hive_lib_path}/") + "*"
      error_message = (
        "Error! Sorry, but we can't find jdbc driver with default name "
        + params.default_connectors_map[params.hive_jdbc_driver]
        + " in hive lib dir. So, db connection check can fail. Please run 'ambari-server setup --jdbc-db={db_name} --jdbc-driver={path_to_jdbc} on server host.'"
      )
      Logger.error(error_message)

  classpath = os.pathsep.join((format("{check_db_connection_jar}"), path_to_jdbc))
  try:
    verify_db_connection(
      format("{ambari_java_home}/bin/java"),
      classpath,
      params.hive_jdbc_connection_url,
      params.hive_metastore_user_name,
      params.hive_metastore_user_passwd,
      params.hive_jdbc_driver,
      tries=5,
      try_sleep=10,
    )
  except Exception:
    show_logs(params.hive_log_dir, params.hive_user)
    raise


@retry(times=30, sleep_time=10, err_class=Fail)
def wait_for_znode():
  import params
  import status_params

  try:
    check_process_status(status_params.hive_pid)
  except ComponentIsNotRunning:
    raise Exception(
      format("HiveServer2 is no longer running, check the logs at {hive_log_dir}")
    )

  cmd = format(
    "{zk_bin_dir}/zkCli.sh -server {zk_quorum} ls /{hive_server2_zookeeper_namespace} | grep 'serverUri='"
  )
  code, out = shell.call(cmd)
  if code == 1:
    raise Fail(
      format("ZooKeeper node /{hive_server2_zookeeper_namespace} is not ready yet")
    )
