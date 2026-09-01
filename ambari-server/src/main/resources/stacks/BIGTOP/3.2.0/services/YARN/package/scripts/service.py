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

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.logger import Logger
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

import yarn_process_utils


def _process_spec(component_name, service_name, params, status_params):
  if service_name == "mapreduce" and component_name == "historyserver":
    return {
      "binary": f"{params.mapred_container_bin}/mapred",
      "command_user": params.mapred_user,
      "expected_user": params.mapred_user,
      "log_dir": params.mapred_log_dir,
      "pid_dir": params.mapred_pid_dir,
      "pid_file": status_params.mapred_historyserver_pid_file,
      "pid_owner": params.mapred_user,
      "pid_group": params.user_group,
      "privileged": False,
    }

  privileged_registry_dns = (
    component_name == "registrydns"
    and status_params.registry_dns_needs_privileged_access
  )
  return {
    "binary": f"{params.yarn_container_bin}/yarn",
    "command_user": (
      status_params.root_user if privileged_registry_dns else params.yarn_user
    ),
    "expected_user": params.yarn_user,
    "pid_owner": (
      status_params.root_user if privileged_registry_dns else params.yarn_user
    ),
    "pid_group": params.user_group,
    "privileged": privileged_registry_dns,
    "log_dir": params.yarn_log_dir,
    "pid_dir": (
      f"{params.yarn_pid_dir_prefix}/{status_params.root_user}"
      if privileged_registry_dns
      else params.yarn_pid_dir
    ),
    "pid_file": (
      status_params.yarn_registry_dns_secure_pid_file
      if privileged_registry_dns
      else f"{params.yarn_pid_dir}/hadoop-{params.yarn_user}-{component_name}.pid"
    ),
  }


def _daemon_environment(spec, params):
  return {
    "HADOOP_LIBEXEC_DIR": params.hadoop_libexec_dir,
    "HADOOP_PID_DIR": spec["pid_dir"],
    "HADOOP_SECURE_PID_DIR": spec["pid_dir"],
    "HADOOP_LOG_DIR": spec["log_dir"],
    "HADOOP_SECURE_LOG_DIR": spec["log_dir"],
  }


def _stop_registry_dns_processes(params, status_params, keep_pid_file=None):
  processes = (
    (
      status_params.yarn_registry_dns_pid_file,
      params.yarn_user,
      params.yarn_user,
      False,
    ),
    (
      status_params.yarn_registry_dns_secure_pid_file,
      params.yarn_user,
      status_params.root_user,
      True,
    ),
    (
      status_params.yarn_registry_dns_wrapper_pid_file,
      status_params.root_user,
      status_params.root_user,
      True,
    ),
  )
  cleanup_errors = []
  for pid_file, expected_user, owner, privileged in processes:
    if pid_file == keep_pid_file:
      continue
    try:
      yarn_process_utils.stop_process(
        pid_file,
        expected_user,
        "registrydns",
        owner,
        params.user_group,
        privileged,
      )
    except Exception as error:
      cleanup_errors.append(f"{pid_file}: {error}")
  if cleanup_errors:
    raise RuntimeError(
      "Failed to stop Registry DNS processes: " + "; ".join(cleanup_errors)
    )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def service(componentName, action="start", serviceName="yarn"):
  import params
  import status_params

  spec = _process_spec(componentName, serviceName, params, status_params)
  daemon_environment = _daemon_environment(spec, params)

  if action == "start":
    running = yarn_process_utils.recover_running_process(
      spec["pid_file"],
      spec["expected_user"],
      componentName,
      spec["pid_owner"],
      spec["pid_group"],
      spec["privileged"],
    )
    if running is not None:
      Logger.info(f"YARN {componentName} is already running as pid {running.pid}")
      return

    if componentName == "registrydns":
      _stop_registry_dns_processes(
        params, status_params, keep_pid_file=spec["pid_file"]
      )

    try:
      Execute(
        (
          spec["binary"],
          "--config",
          params.hadoop_conf_dir,
          "--daemon",
          "start",
          componentName,
        ),
        user=spec["command_user"],
        environment=daemon_environment,
        timeout=300,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
      yarn_process_utils.wait_for_running_process(
        spec["pid_file"],
        spec["expected_user"],
        componentName,
        spec["pid_owner"],
        spec["pid_group"],
        spec["privileged"],
      )
    except Exception as error:
      cleanup_errors = []
      try:
        if componentName == "registrydns":
          _stop_registry_dns_processes(params, status_params)
        else:
          yarn_process_utils.stop_process(
            spec["pid_file"],
            spec["expected_user"],
            componentName,
            spec["pid_owner"],
            spec["pid_group"],
            spec["privileged"],
          )
      except Exception as cleanup_error:
        cleanup_errors.append(f"startup rollback failed: {cleanup_error}")
      try:
        show_logs(spec["log_dir"], spec["expected_user"])
      except Exception as log_error:
        cleanup_errors.append(f"log collection failed: {log_error}")
      if cleanup_errors:
        raise RuntimeError(
          f"{error}; additionally {'; '.join(cleanup_errors)}"
        ) from error
      raise

  elif action == "stop":
    if componentName == "registrydns":
      _stop_registry_dns_processes(params, status_params)
    else:
      try:
        yarn_process_utils.stop_process(
          spec["pid_file"],
          spec["expected_user"],
          componentName,
          spec["pid_owner"],
          spec["pid_group"],
          spec["privileged"],
        )
      except Exception:
        show_logs(spec["log_dir"], spec["expected_user"])
        raise

  elif action == "refreshQueues":
    command = (
      spec["binary"],
      "--config",
      params.hadoop_conf_dir,
      "rmadmin",
      "-refreshQueues",
    )
    if params.security_enabled:
      with PrivateKerberosCache(
        spec["expected_user"],
        params.user_group,
        prefix="ambari-yarn-refresh-",
      ) as kerberos_cache:
        kerberos_cache.kinit(
          params.kinit_path_local, params.rm_keytab, params.rm_principal_name
        )
        _execute_refresh_queues(
          command,
          spec["expected_user"],
          kerberos_cache.merge_environment(daemon_environment),
        )
    else:
      _execute_refresh_queues(command, spec["expected_user"], daemon_environment)


def _execute_refresh_queues(command, user, environment):
  Execute(
    command,
    user=user,
    environment=environment,
    timeout=20,
    tries=5,
    try_sleep=5,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
  )
