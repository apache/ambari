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

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.script.script import Script

import infra_solr_process
import infra_solr_utils
from setup_infra_solr import setup_infra_solr, setup_solr_znode_env


class InfraSolr(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    setup_infra_solr(name="server")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    setup_solr_znode_env()

    identity = infra_solr_process.read_or_recover_process(
      params.infra_solr_pidfile,
      params.infra_solr_user,
      params.user_group,
      params.infra_solr_port,
      params.infra_solr_datadir,
    )
    if identity is not None:
      Logger.info(f"Infra Solr is already running with pid {identity.pid}")
      return

    infra_solr_utils.validate_executable(
      params.solr_executable, "Infra Solr launcher"
    )
    start_command = [
      params.solr_executable,
      "start",
      "-cloud",
      "-noprompt",
      "-p",
      str(params.infra_solr_port),
      "-s",
      params.infra_solr_datadir,
      "-z",
      f"{params.zk_quorum}{params.infra_solr_znode}",
    ]
    if params.security_enabled:
      start_command.append(
        f"-Dsolr.kerberos.name.rules={params.infra_solr_kerberos_name_rules}"
      )

    try:
      Execute(
        tuple(start_command),
        environment={"SOLR_INCLUDE": params.infra_solr_include},
        user=params.infra_solr_user,
        timeout=120,
        logoutput=True,
      )
      infra_solr_process.wait_for_started_process(
        params.infra_solr_pidfile,
        params.infra_solr_user,
        params.user_group,
        params.infra_solr_port,
        params.infra_solr_datadir,
      )
    except Exception:
      try:
        infra_solr_process.stop_process(
          params.infra_solr_pidfile,
          params.infra_solr_user,
          params.user_group,
          params.infra_solr_port,
          params.infra_solr_datadir,
        )
      except Exception as cleanup_error:
        Logger.warning(f"Infra Solr startup cleanup failed: {cleanup_error}")
      show_logs(params.infra_solr_log_dir, params.infra_solr_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    try:
      stopped = infra_solr_process.stop_process(
        params.infra_solr_pidfile,
        params.infra_solr_user,
        params.user_group,
        params.infra_solr_port,
        params.infra_solr_datadir,
      )
      if not stopped:
        Logger.info("No running Infra Solr process was found")
    except Exception:
      show_logs(params.infra_solr_log_dir, params.infra_solr_user)
      raise

  def status(self, env):
    import status_params

    env.set_params(status_params)
    identity = infra_solr_process.read_or_recover_process(
      status_params.infra_solr_pidfile,
      status_params.infra_solr_user,
      status_params.user_group,
      status_params.infra_solr_port,
      status_params.infra_solr_datadir,
    )
    if identity is None:
      raise ComponentIsNotRunning()

  def disable_security(self, env):
    import params

    env.set_params(params)
    if not params.infra_solr_znode:
      raise Fail("Infra Solr ZooKeeper znode is required to disable security")
    zkmigrator = ZkMigrator(
      zk_host=params.zk_quorum,
      java_exec=params.ambari_java_exec,
      java_home=params.ambari_java_home,
      jaas_file=params.infra_solr_jaas_file,
      user=params.infra_solr_user,
    )
    zkmigrator.set_acls(params.infra_solr_znode, "world:anyone:crdwa")

  def get_log_folder(self):
    import params

    return params.infra_solr_log_dir

  def get_user(self):
    import params

    return params.infra_solr_user

  def get_pid_files(self):
    import status_params

    return [status_params.infra_solr_pidfile]


if __name__ == "__main__":
  InfraSolr().execute()
