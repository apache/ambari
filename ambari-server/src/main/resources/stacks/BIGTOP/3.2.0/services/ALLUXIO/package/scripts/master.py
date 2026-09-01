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
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions import safe_process
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script

from alluxio_utils import rollback_started_process


class AlluxioMaster(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params

    env.set_params(params)

    Directory(
      [
        params.alluxio_pid_dir,
        params.alluxio_master_metastore_dir,
      ],
      owner=params.alluxio_user,
      group=params.alluxio_group,
      mode=0o770,
      create_parents=True,
    )

    Directory(
      [params.alluxio_log_dir, os.path.join(params.alluxio_log_dir, "user")],
      owner=params.alluxio_user,
      group=params.alluxio_group,
      mode=0o770,
      create_parents=True,
    )

    # create alluxio-site.properties in alluxio install dir
    File(
      os.path.join(params.alluxio_conf_dir, "alluxio-site.properties"),
      owner="root",
      group=params.alluxio_group,
      content=InlineTemplate(params.alluxio_site_properties),
      mode=0o644,
    )

    # create alluxio-env.sh in alluxio install dir
    File(
      os.path.join(params.alluxio_conf_dir, "alluxio-env.sh"),
      owner="root",
      group=params.alluxio_group,
      content=InlineTemplate(params.alluxio_env_sh),
      mode=0o640,
    )

    # create log4j2.properties alluxio install dir
    File(
      os.path.join(params.alluxio_conf_dir, "log4j.properties"),
      owner="root",
      group=params.alluxio_group,
      content=InlineTemplate(params.alluxio_log4j2_properties),
      mode=0o644,
    )

    File(
      os.path.join(params.alluxio_conf_dir, "metrics.properties"),
      owner="root",
      group=params.alluxio_group,
      content=InlineTemplate(params.alluxio_metrics_properties),
      mode=0o644,
    )

    # masters
    File(
      format("{alluxio_conf_dir}/masters"),
      owner="root",
      group=params.alluxio_group,
      mode=0o644,
      content=Template("masters.j2", conf_dir=params.alluxio_conf_dir),
    )

    # workers
    File(
      format("{alluxio_conf_dir}/workers"),
      owner="root",
      group=params.alluxio_group,
      mode=0o644,
      content=Template("workers.j2", conf_dir=params.alluxio_conf_dir),
    )

    # hdfs dir
    params.HdfsResource(
      params.alluxio_hdfs_user_dir,
      type="directory",
      action="create_on_execute",
      owner=params.alluxio_user,
      mode=0o755,
    )
    params.HdfsResource(
      params.underfs_hdfs_addr,
      type="directory",
      action="create_on_execute",
      owner=params.alluxio_user,
      mode=0o755,
    )
    params.HdfsResource(None, action="execute")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    self.configure(env)

    pid_in_file = safe_process.read_pid(params.alluxio_master_pid_file)
    if pid_in_file is None:
      identity = safe_process.discover_running_process(
        params.alluxio_user, params.alluxio_master_process_class
      )
      if identity is not None:
        safe_process.create_pid_file_for_identity(
          params.alluxio_master_pid_file,
          identity,
          params.alluxio_user,
          params.alluxio_master_process_class,
          params.alluxio_user,
          params.alluxio_group,
          mode=0o640,
        )
        return
    else:
      identity = safe_process.read_running_process(
        params.alluxio_master_pid_file,
        params.alluxio_user,
        params.alluxio_master_process_class,
      )
      if identity is None:
        raise Fail(
          f"Alluxio master PID file refers to a stale process {pid_in_file}"
        )
      return

    if params.security_enabled:
      Execute(
        (
          params.kinit_path_local,
          "-kt",
          params.alluxio_service_kerberos_keytab,
          params.kinit_principal,
        ),
        user=params.alluxio_user,
        timeout=30,
      )

    try:
      Execute(
        params.alluxio_master_start_cmd,
        user=params.alluxio_user,
        environment={"JAVA_HOME": params.java_home},
        timeout=60,
      )

      identity = safe_process.wait_for_discovered_process(
        params.alluxio_user,
        params.alluxio_master_process_class,
        attempts=60,
        sleep_seconds=1,
      )
      pid_in_file = safe_process.read_pid(params.alluxio_master_pid_file)
      if pid_in_file is None:
        safe_process.create_pid_file_for_identity(
          params.alluxio_master_pid_file,
          identity,
          params.alluxio_user,
          params.alluxio_master_process_class,
          params.alluxio_user,
          params.alluxio_group,
          mode=0o640,
        )
      elif pid_in_file != identity.pid:
        raise Fail("Alluxio master PID file does not match the started process")

      stored_identity = safe_process.read_running_process(
        params.alluxio_master_pid_file,
        params.alluxio_user,
        params.alluxio_master_process_class,
      )
      if stored_identity is None or not identity.matches(stored_identity):
        raise Fail(
          "Alluxio master process changed while its PID file was being stored"
        )
    except Exception:
      try:
        rollback_started_process(
          params.alluxio_master_pid_file,
          params.alluxio_user,
          params.alluxio_master_process_class,
        )
      except Exception as cleanup_error:
        Logger.warning(
          f"Could not roll back failed Alluxio master start: {cleanup_error}"
        )
      raise

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    pid_in_file = safe_process.read_pid(params.alluxio_master_pid_file)
    if pid_in_file is None:
      identity = safe_process.discover_running_process(
        params.alluxio_user, params.alluxio_master_process_class
      )
    else:
      identity = safe_process.read_running_process(
        params.alluxio_master_pid_file,
        params.alluxio_user,
        params.alluxio_master_process_class,
      )
      if identity is None:
        raise Fail(
          f"Alluxio master PID file refers to a stale process {pid_in_file}"
        )
    if identity is None:
      return

    safe_process.terminate_process(
      identity,
      params.alluxio_user,
      params.alluxio_master_process_class,
    )
    safe_process.remove_pid_file_if_stopped(
      params.alluxio_master_pid_file,
      identity.pid,
      params.alluxio_user,
      params.alluxio_master_process_class,
    )

  def status(self, env):
    import params

    env.set_params(params)
    pid_in_file = safe_process.read_pid(params.alluxio_master_pid_file)
    if pid_in_file is None:
      identity = safe_process.discover_running_process(
        params.alluxio_user, params.alluxio_master_process_class
      )
    else:
      identity = safe_process.read_running_process(
        params.alluxio_master_pid_file,
        params.alluxio_user,
        params.alluxio_master_process_class,
      )
      if identity is None:
        raise Fail(
          f"Alluxio master PID file refers to a stale process {pid_in_file}"
        )
    if identity is None:
      raise ComponentIsNotRunning("Alluxio master is not running")

  def get_user(self):
    import params

    return params.alluxio_user

  def get_pid_files(self):
    import params

    return [params.alluxio_master_pid_file]


if __name__ == "__main__":
  AlluxioMaster().execute()
