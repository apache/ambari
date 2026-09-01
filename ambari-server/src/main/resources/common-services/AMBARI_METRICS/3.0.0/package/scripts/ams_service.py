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
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.show_logs import show_logs
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from hbase_service import hbase_service
from metrics_process import (
  hbase_pid_file,
  read_or_discover_ams_process,
  read_or_discover_hbase_process,
  stop_ams_process,
  wait_for_ams_process,
  wait_for_hbase_process,
)


_AMS_START_TIMEOUTS = {"collector": 420, "monitor": 60}


def _component_details(name, params):
  if name == "collector":
    return (
      params.ams_collector_script,
      params.ams_collector_conf_dir,
      os.path.join(params.ams_collector_pid_dir, "ambari-metrics-collector.pid"),
      params.ams_collector_log_dir,
    )
  if name == "monitor":
    return (
      params.ams_monitor_script,
      params.ams_monitor_conf_dir,
      os.path.join(params.ams_monitor_pid_dir, "ambari-metrics-monitor.pid"),
      params.ams_monitor_log_dir,
    )
  raise Fail(f"Unsupported Ambari Metrics component: {name}")


def _stop_component(name, pid_file, params):
  stopped = stop_ams_process(
    pid_file, params.ams_user, params.user_group, name
  )
  if not stopped:
    Logger.info(f"No running Ambari Metrics {name} process was found")
  return stopped


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def ams_service(name, action):
  import params

  if action not in ("start", "stop"):
    raise Fail(f"Unsupported Ambari Metrics action: {action}")

  script, config_dir, pid_file, log_dir = _component_details(name, params)

  if action == "stop":
    _stop_component(name, pid_file, params)
    if name == "collector":
      hbase_service("regionserver", action="stop")
      hbase_service("master", action="stop")
      File(
        os.path.join(
          params.ams_collector_pid_dir, "ambari-metrics-collector.krb5cc"
        ),
        action="delete",
      )
    elif (
      name == "monitor"
      and params.security_enabled
      and params.monitor_kinit_cmd
    ):
      File(params.monitor_credential_cache, action="delete")
    return

  existing_identity = read_or_discover_ams_process(
    pid_file, params.ams_user, params.user_group, name
  )
  if existing_identity is not None:
    Logger.info(
      f"Ambari Metrics {name} is already running with pid {existing_identity.pid}"
    )
    return

  command = [script, "--config", config_dir]
  started_hbase_roles = []
  environment = {"JAVA_HOME": params.java64_home}
  if name == "monitor":
    environment["PYTHON"] = params.python_binary
    if params.security_enabled and params.monitor_kinit_cmd:
      environment["KRB5CCNAME"] = params.monitor_cache_name

  command.append("start")
  embedded_master_was_running = False
  master_pid_file = None
  try:
    if name == "monitor":
      Execute(
        (
          params.python_binary,
          "-c",
          "import sys; raise SystemExit(0 if sys.version_info >= (3, 9, 2) else 1)",
        ),
        user=params.ams_user,
        timeout=15,
      )
    if name == "collector":
      if params.is_hbase_distributed:
        for role in ("master", "regionserver"):
          if hbase_service(role, action="start"):
            started_hbase_roles.append(role)
        command.insert(-1, "--distributed")
      else:
        master_pid_file = hbase_pid_file(
          params.hbase_pid_dir, params.hbase_user, "master"
        )
        embedded_master_was_running = read_or_discover_hbase_process(
          master_pid_file, params.hbase_user, params.user_group, "master"
        ) is not None
        hbase_service("regionserver", action="stop")
        for config_file in ("core-site.xml", "hdfs-site.xml"):
          File(os.path.join(config_dir, config_file), action="delete")

      if params.security_enabled:
        kerberos_cache = os.path.join(
          params.ams_collector_pid_dir, "ambari-metrics-collector.krb5cc"
        )
        File(kerberos_cache, action="delete")
        Execute(
          (
            params.kinit_path_local,
            "-c",
            f"FILE:{kerberos_cache}",
            "-kt",
            params.ams_collector_keytab_path,
            params.ams_collector_jaas_princ,
          ),
          user=params.ams_user,
          timeout=30,
        )
        File(
          kerberos_cache,
          owner=params.ams_user,
          group=params.user_group,
          mode=0o600,
        )
        environment["KRB5CCNAME"] = f"FILE:{kerberos_cache}"

    Execute(
      tuple(command),
      user=params.ams_user,
      environment=environment,
      timeout=_AMS_START_TIMEOUTS[name],
    )
    wait_for_ams_process(
      pid_file, params.ams_user, params.user_group, name
    )
    if name == "collector" and not params.is_hbase_distributed:
      wait_for_hbase_process(
        master_pid_file,
        params.hbase_user,
        params.user_group,
        "master",
      )
  except Exception:
    try:
      _stop_component(name, pid_file, params)
    except Exception as cleanup_error:
      Logger.error(f"Failed to roll back Ambari Metrics {name}: {cleanup_error}")
    if name == "collector":
      rollback_roles = (
        reversed(started_hbase_roles)
        if params.is_hbase_distributed
        else (() if embedded_master_was_running else ("master",))
      )
      for role in rollback_roles:
        try:
          hbase_service(role, action="stop")
        except Exception as cleanup_error:
          Logger.error(f"Failed to roll back AMS HBase {role}: {cleanup_error}")
      if params.security_enabled:
        File(
          os.path.join(
            params.ams_collector_pid_dir, "ambari-metrics-collector.krb5cc"
          ),
          action="delete",
        )
    elif params.security_enabled and params.monitor_kinit_cmd:
      File(params.monitor_credential_cache, action="delete")
    show_logs(log_dir, params.ams_user)
    raise
