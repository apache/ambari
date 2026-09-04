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

from resource_management import Script, Execute
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.show_logs import show_logs
from status import check_service_status
from ams import ams
from metrics_process import (
  read_or_discover_ams_process,
  stop_ams_identity,
  stop_ams_process,
  wait_for_ams_process,
)


class AmsGrafana(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)
    self.configure(env)  # for security

  def configure(self, env, action=None):
    import params

    env.set_params(params)
    ams(name="grafana", action=action)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if (
      not params.ams_grafana_host
      or not isinstance(params.ams_grafana_admin_pwd, str)
      or len(params.ams_grafana_admin_pwd) < 4
      or any(character in params.ams_grafana_admin_pwd for character in "\r\n\x00")
    ):
      raise Fail(
        "Grafana requires a host and an administrator password of at least 4 characters"
      )
    self.configure(env, action="start")

    pid_file = params.grafana_pid_file
    existing_identity = read_or_discover_ams_process(
      pid_file, params.ams_user, params.user_group, "grafana"
    )
    started_identity = None

    try:
      if existing_identity is None:
        Execute(
          (params.ams_grafana_script, "start"),
          user=params.ams_user,
          environment={"JAVA_HOME": params.java64_home},
          timeout=150,
          timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        )
        started_identity = wait_for_ams_process(
          pid_file, params.ams_user, params.user_group, "grafana"
        )

      from metrics_grafana_util import (
        create_ams_datasource,
        create_ams_dashboards,
        create_grafana_admin_pwd,
      )

      create_grafana_admin_pwd()
      create_ams_datasource()
      create_ams_dashboards()
    except Exception:
      if started_identity is not None:
        try:
          stop_ams_identity(
            started_identity, pid_file, params.ams_user, "grafana"
          )
        except Exception as cleanup_error:
          Logger.error(f"Failed to roll back Ambari Metrics Grafana: {cleanup_error}")
      try:
        show_logs(params.ams_grafana_log_dir, params.ams_user)
      except Exception as log_error:
        Logger.warning(f"Unable to show Ambari Metrics Grafana logs: {log_error}")
      raise

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env, action="stop")
    stop_ams_process(
      params.grafana_pid_file,
      params.ams_user,
      params.user_group,
      "grafana",
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_service_status(env, name="grafana")

  def get_pid_files(self):
    import status_params

    return [status_params.grafana_pid_file]


if __name__ == "__main__":
  AmsGrafana().execute()
