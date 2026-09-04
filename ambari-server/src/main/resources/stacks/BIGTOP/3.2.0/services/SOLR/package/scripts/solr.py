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

from resource_management.core.logger import Logger
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.resources.system import Execute
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import safe_process
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature

from setup_solr import setup_solr, setup_solr_znode_env


def solr_process_tokens(port, solr_home):
  port = str(port).strip()
  if not port.isascii() or not port.isdigit() or not 0 < int(port) <= 65535:
    raise Fail(f"Invalid Solr port {port!r}")
  if not solr_home:
    raise Fail("Solr home directory is not configured")
  return (
    f"-Djetty.port={port}",
    f"-Dsolr.solr.home={solr_home}",
    "-jar",
    "start.jar",
  )


def read_or_discover_solr_process(pid_file, user, group, process_tokens):
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.inspect_process(pid, user, process_tokens)
    if identity is not None and safe_process.is_process_running(
      pid,
      user,
      process_tokens,
      identity=identity,
    ):
      return publish_solr_process(
        pid_file, identity, user, group, process_tokens
      )
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=process_tokens,
    )

  identity = safe_process.discover_running_process(user, process_tokens)
  if identity is None:
    return None
  return publish_solr_process(
    pid_file, identity, user, group, process_tokens
  )


def publish_solr_process(pid_file, identity, user, group, process_tokens):
  return safe_process.publish_pid_file_for_identity(
    pid_file,
    identity,
    user,
    process_tokens,
    owner=user,
    group=group,
    mode=0o640,
  )


def rollback_started_solr_process(pid_file, identity, user, process_tokens):
  safe_process.terminate_process(identity, user, process_tokens)
  pid = safe_process.read_pid(pid_file)
  if pid == identity.pid:
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      identity.pid,
      expected_user=user,
      expected_cmdline=process_tokens,
    )
  return True


def wait_for_started_solr_process(
  pid_file,
  user,
  group,
  process_tokens,
  attempts=10,
  sleep_seconds=1,
):
  identity = safe_process.wait_for_discovered_process(
    user,
    process_tokens,
    attempts=attempts,
    sleep_seconds=sleep_seconds,
  )
  try:
    return publish_solr_process(
      pid_file, identity, user, group, process_tokens
    )
  except Exception:
    try:
      rollback_started_solr_process(
        pid_file, identity, user, process_tokens
      )
    except Exception as rollback_error:
      Logger.warning(
        f"Could not roll back failed Solr PID publication: {rollback_error}"
      )
    raise


class Solr(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    setup_solr(name="server")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    process_tokens = solr_process_tokens(params.solr_port, params.solr_datadir)
    read_or_discover_solr_process(
      params.solr_pidfile,
      params.solr_user,
      params.user_group,
      process_tokens,
    )

    self.configure(env)
    setup_solr_znode_env()

    identity = read_or_discover_solr_process(
      params.solr_pidfile,
      params.solr_user,
      params.user_group,
      process_tokens,
    )
    if identity is not None:
      Logger.info(f"Solr is already running with pid {identity.pid}")
      return

    start_argv = [
      f"{params.solr_bindir}/solr",
      "start",
      "-cloud",
      "-noprompt",
      "-p",
      str(params.solr_port),
      "-s",
      str(params.solr_datadir),
      "-z",
      f"{params.zookeeper_quorum}{params.solr_znode}",
    ]
    if params.security_enabled:
      start_argv.append(
        f"-Dsolr.kerberos.name.rules={params.solr_kerberos_name_rules}"
      )

    Execute(
      tuple(start_argv),
      environment={"SOLR_INCLUDE": f"{params.solr_conf}/solr-env.sh"},
      user=params.solr_user,
      logoutput=True,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
    wait_for_started_solr_process(
      params.solr_pidfile,
      params.solr_user,
      params.user_group,
      process_tokens,
      attempts=10,
      sleep_seconds=1,
    )

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    process_tokens = solr_process_tokens(params.solr_port, params.solr_datadir)
    identity = read_or_discover_solr_process(
      params.solr_pidfile,
      params.solr_user,
      params.user_group,
      process_tokens,
    )
    if identity is None:
      Logger.info("No running Solr process was found")
      return

    self.kill_process(
      params.solr_pidfile,
      params.solr_user,
      params.solr_port,
      params.solr_datadir,
      params.solr_log_dir,
      expected_identity=identity,
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)

    process_tokens = solr_process_tokens(
      status_params.solr_port, status_params.solr_datadir
    )
    identity = read_or_discover_solr_process(
      status_params.solr_pidfile,
      status_params.solr_user,
      status_params.user_group,
      process_tokens,
    )
    if identity is None:
      raise ComponentIsNotRunning()

  def kill_process(
    self,
    pid_file,
    user,
    port,
    solr_home,
    log_dir,
    expected_identity=None,
  ):
    """Stop the exact Solr process, falling back from TERM to KILL if needed."""
    process_tokens = solr_process_tokens(port, solr_home)
    current_pid = safe_process.read_pid(pid_file)
    if (
      current_pid is not None
      and expected_identity is not None
      and current_pid != expected_identity.pid
    ):
      raise Fail(
        f"Refusing to signal Solr pid {expected_identity.pid}: "
        f"pid file changed to {current_pid}"
      )
    pid = current_pid
    if pid is None and expected_identity is not None:
      pid = expected_identity.pid
    if pid is None:
      return

    identity = (
      safe_process.inspect_process(pid, user, process_tokens)
      if current_pid is not None
      else expected_identity
    )
    if (
      expected_identity is not None
      and identity is not None
      and not expected_identity.matches(identity)
    ):
      raise Fail(f"Refusing to signal reused Solr pid {pid}")
    if identity is None or not safe_process.is_process_running(
      pid,
      user,
      process_tokens,
      identity=identity,
    ):
      self._delete_stopped_pid_file(pid_file, pid, user, process_tokens)
      return

    try:
      safe_process.terminate_process(identity, user, process_tokens)
    except Exception:
      try:
        show_logs(log_dir, user)
      except Exception as error:
        Logger.warning(f"Could not collect Solr logs after stop failure: {error}")
      raise

    self._delete_stopped_pid_file(pid_file, pid, user, process_tokens)

  def _delete_stopped_pid_file(self, pid_file, pid, user, process_tokens):
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=process_tokens,
    )

  def disable_security(self, env):
    import params

    if not params.solr_znode:
      Logger.info("Skipping reverting ACL")
      return
    zkmigrator = ZkMigrator(
      zk_host=params.zk_quorum,
      java_exec=params.ambari_java_exec,
      java_home=params.ambari_java_home,
      jaas_file=params.solr_jaas_file,
      user=params.solr_user,
    )
    zkmigrator.set_acls(params.solr_znode, "world:anyone:crdwa")

  def get_log_folder(self):
    import params

    return params.solr_log_dir

  def get_user(self):
    import params

    return params.solr_user

  def get_pid_files(self):
    import status_params

    return [status_params.solr_pidfile]

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Solr Stack Upgrade pre-restart")
    import params

    env.set_params(params)

    if params.stack_version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.stack_version
    ):
      stack_select.select_packages(params.stack_version)


if __name__ == "__main__":
  Solr().execute()
