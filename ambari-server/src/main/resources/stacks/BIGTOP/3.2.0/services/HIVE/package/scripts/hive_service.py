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

from ambari_commons.db_connection_helper import verify_db_connection
from resource_management.core import shell
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions import safe_process
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.show_logs import show_logs


HIVE_PROCESS_TOKENS = {
  "hiveserver2": ("org.apache.hive.service.server.HiveServer2",),
  "metastore": ("org.apache.hadoop.hive.metastore.HiveMetaStore",),
  "webhcat": ("org.apache.hive.hcatalog.templeton.Main",),
}


def expected_process_tokens(role):
  try:
    return HIVE_PROCESS_TOKENS[role]
  except KeyError as error:
    raise Fail(f"Unsupported Hive service role: {role}") from error


def read_or_discover_hive_process(pid_file, user, group, role):
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


def wait_for_hive_process(
  pid_file, user, group, role, attempts=30, sleep_seconds=1
):
  for attempt in range(attempts):
    identity = read_or_discover_hive_process(pid_file, user, group, role)
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail(f"Hive {role} did not start with a valid process identity")


def check_hive_process_status(pid_file, user, group, role):
  identity = read_or_discover_hive_process(pid_file, user, group, role)
  if identity is None:
    raise ComponentIsNotRunning(f"Hive {role} is not running")
  return identity


def _pid_file(name, status_params):
  if name == "metastore":
    return status_params.hive_metastore_pid
  if name == "hiveserver2":
    return status_params.hive_pid
  raise Fail(f"Unsupported Hive service role: {name}")


def _start_command(name, params, status_params):
  pid_file = _pid_file(name, status_params)
  if name == "metastore":
    return (
      params.start_metastore_path,
      os.path.join(params.hive_log_dir, "hive.out"),
      os.path.join(params.hive_log_dir, "hive.err"),
      pid_file,
      params.hive_conf_dir,
    )
  return (
    params.start_hiveserver2_path,
    os.path.join(params.hive_log_dir, "hive-server2.out"),
    os.path.join(params.hive_log_dir, "hive-server2.err"),
    pid_file,
    params.hive_conf_dir,
    params.tez_conf_dir,
  )


def _validate_metastore_connection(params):
  if params.hive_jdbc_driver not in params.hive_jdbc_drivers_list:
    return
  validate_connection(params.hive_jdbc_target, params.hive_lib_dir)


def _wait_for_secure_znode(params):
  if not params.security_enabled:
    wait_for_znode()
    return
  required = (
    params.kinit_path_local,
    params.hive_server2_keytab,
    params.hive_principal,
  )
  if not all(str(value or "").strip() for value in required):
    raise Fail("Secure HiveServer2 discovery requires a principal and keytab")
  with PrivateKerberosCache(
    params.hive_user,
    params.user_group,
    temp_dir=params.tmp_dir,
    prefix="ambari-hiveserver2-znode-",
  ) as kerberos_cache:
    kerberos_cache.kinit(*required, timeout=30)
    wait_for_znode(kerberos_cache.environment)


def hive_service(name, action="start", upgrade_type=None):
  import params
  import status_params

  expected_process_tokens(name)
  if action not in ("start", "stop"):
    raise Fail(f"Unsupported Hive service action: {action}")

  pid_file = _pid_file(name, status_params)
  identity = read_or_discover_hive_process(
    pid_file, params.hive_user, params.user_group, name
  )

  if action == "start":
    if identity is not None:
      Logger.info(f"Hive {name} is already running with pid {identity.pid}")
      return
    if name == "metastore":
      _validate_metastore_connection(params)
    started_identity = None
    try:
      Execute(
        _start_command(name, params, status_params),
        user=params.hive_user,
        environment={
          "HADOOP_HOME": params.hadoop_home,
          "JAVA_HOME": params.java64_home,
          "HIVE_BIN": os.path.join(params.hive_bin_dir, "hive"),
        },
        path=params.execute_path,
        logoutput=True,
        timeout=60,
      )
      started_identity = wait_for_hive_process(
        pid_file, params.hive_user, params.user_group, name
      )
      File(
        pid_file,
        owner=params.hive_user,
        group=params.user_group,
        mode=0o640,
      )
      if name == "hiveserver2":
        _wait_for_secure_znode(params)
    except Exception:
      if started_identity is not None:
        try:
          safe_process.terminate_process(
            started_identity,
            params.hive_user,
            expected_process_tokens(name),
            term_wait_attempts=10,
            term_wait_sleep=1,
            kill_wait_attempts=10,
            kill_wait_sleep=1,
          )
          safe_process.remove_pid_file_if_stopped(
            pid_file,
            started_identity.pid,
            expected_user=params.hive_user,
            expected_cmdline=expected_process_tokens(name),
          )
        except Exception as cleanup_error:
          Logger.error(f"Could not roll back failed Hive {name} start: {cleanup_error}")
      show_logs(params.hive_log_dir, params.hive_user)
      raise
    return

  if identity is None:
    Logger.info(f"No running Hive {name} process was found")
    return

  tokens = expected_process_tokens(name)
  try:
    term_wait_attempts = 33 if name == "hiveserver2" else 10
    safe_process.terminate_process(
      identity,
      params.hive_user,
      tokens,
      term_wait_attempts=term_wait_attempts,
      term_wait_sleep=1,
      kill_wait_attempts=10,
      kill_wait_sleep=1,
    )
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      identity.pid,
      expected_user=params.hive_user,
      expected_cmdline=tokens,
    )
  except Exception:
    show_logs(params.hive_log_dir, params.hive_user)
    raise


def validate_connection(target_path_to_jdbc, hive_lib_path):
  import params

  path_to_jdbc = target_path_to_jdbc
  if not params.jdbc_jar_name:
    path_to_jdbc = (
      os.path.join(
        hive_lib_path, params.default_connectors_map[params.hive_jdbc_driver]
      )
      if params.hive_jdbc_driver in params.default_connectors_map
      else None
    )
    if path_to_jdbc is None or not os.path.isfile(path_to_jdbc):
      path_to_jdbc = os.path.join(hive_lib_path, "*")
      Logger.warning(
        "The default Hive metastore JDBC driver was not found; connection "
        "validation will use the Hive library directory."
      )

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
def wait_for_znode(environment=None):
  import params
  import status_params

  check_hive_process_status(
    status_params.hive_pid,
    status_params.hive_user,
    status_params.user_group,
    "hiveserver2",
  )
  namespace = str(params.hive_server2_zookeeper_namespace or "").strip("/")
  if not namespace:
    raise Fail("HiveServer2 ZooKeeper namespace is not configured")
  _, output = shell.checked_call(
    (
      os.path.join(params.zk_bin_dir, "zkCli.sh"),
      "-server",
      str(params.zk_quorum),
      "ls",
      f"/{namespace}",
    ),
    user=params.hive_user,
    env=environment,
    timeout=60,
  )
  if "serverUri=" not in str(output):
    raise Fail(f"ZooKeeper node /{namespace} is not ready yet")
