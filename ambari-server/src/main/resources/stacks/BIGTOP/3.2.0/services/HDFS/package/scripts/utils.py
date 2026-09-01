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
import urllib.request, urllib.error, urllib.parse
import json

import hdfs_process

from resource_management.core.resources.system import Directory, File, Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core import shell
from resource_management.core.shell import as_user, as_sudo
from resource_management.core.source import Template
from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.namenode_ha_utils import (
  get_namenode_states,
)
from resource_management.libraries.functions.show_logs import show_logs
from ambari_commons.inet_utils import create_ssl_context
from zkfc_slave import ZkfcSlaveDefault

def safe_zkfc_op(action, env):
  """
  Idempotent operation on the zkfc process to either start or stop it.
  :param action: start or stop
  :param env: environment
  """
  Logger.info(f"Performing action {action} on zkfc.")
  zkfc = None
  if action == "start":
    try:
      ZkfcSlaveDefault.status_static(env)
    except ComponentIsNotRunning:
      ZkfcSlaveDefault.start_static(env)

  if action == "stop":
    try:
      ZkfcSlaveDefault.status_static(env)
    except ComponentIsNotRunning:
      pass
    else:
      ZkfcSlaveDefault.stop_static(env)


def initiate_safe_zkfc_failover():
  """
  If this is the active namenode, initiate a safe failover and wait for it to become the standby.

  If an error occurs, force a failover to happen by killing zkfc on this host. In this case, during the Restart,
  will also have to start ZKFC manually.
  """
  import params

  # Must kinit before running the HDFS command
  if params.security_enabled:
    Execute(
      (
        params.kinit_path_local,
        "-kt",
        params.hdfs_user_keytab,
        params.hdfs_principal_name,
      ),
      user=params.hdfs_user,
    )

  active_namenode_id = None
  standby_namenode_id = None
  active_namenodes, standby_namenodes, unknown_namenodes = get_namenode_states(
    params.hdfs_site, params.security_enabled, params.hdfs_user
  )
  if active_namenodes:
    active_namenode_id = active_namenodes[0][0]
  if standby_namenodes:
    standby_namenode_id = standby_namenodes[0][0]

  if active_namenode_id:
    Logger.info(format("Active NameNode id: {active_namenode_id}"))
  if standby_namenode_id:
    Logger.info(format("Standby NameNode id: {standby_namenode_id}"))
  if unknown_namenodes:
    for unknown_namenode in unknown_namenodes:
      Logger.info(f"NameNode HA state for {unknown_namenode[0]} is unknown")

  if (
    params.namenode_id == active_namenode_id
    and params.other_namenode_id == standby_namenode_id
  ):
    # Failover if this NameNode is active and other NameNode is up and in standby (i.e. ready to become active on failover)
    Logger.info(
      format(
        "NameNode {namenode_id} is active and NameNode {other_namenode_id} is in standby"
      )
    )

    failover_command = format(
      "hdfs haadmin -ns {dfs_ha_nameservices} -failover {namenode_id} {other_namenode_id}"
    )
    check_standby_cmd = format(
      "hdfs haadmin -ns {dfs_ha_nameservices} -getServiceState {namenode_id} | grep standby"
    )

    msg = f"Rolling Upgrade - Initiating a ZKFC failover on active NameNode host {params.hostname}."
    Logger.info(msg)
    code, out = shell.call(failover_command, user=params.hdfs_user, logoutput=True)
    Logger.info(format("Rolling Upgrade - failover command returned {code}"))
    wait_for_standby = False

    if code == 0:
      wait_for_standby = True
    else:
      # Try to kill ZKFC manually
      was_zkfc_killed = kill_zkfc(params.hdfs_user)
      code, out = shell.call(check_standby_cmd, user=params.hdfs_user, logoutput=True)
      Logger.info(format("Rolling Upgrade - check for standby returned {code}"))
      if code == 255 and out:
        Logger.info("Rolling Upgrade - NameNode is already down.")
      else:
        if was_zkfc_killed:
          # Only mandate that this be the standby namenode if ZKFC was indeed killed to initiate a failover.
          wait_for_standby = True

    if wait_for_standby:
      Logger.info("Waiting for this NameNode to become the standby one.")
      Execute(
        check_standby_cmd, user=params.hdfs_user, tries=50, try_sleep=6, logoutput=True
      )
  else:
    msg = (
      f"Rolling Upgrade - Skipping ZKFC failover on NameNode host {params.hostname}."
    )
    Logger.info(msg)


def kill_zkfc(zkfc_user):
  """
  There are two potential methods for failing over the namenode, especially during a Rolling Upgrade.
  Option 1. Kill zkfc on primary namenode provided that the secondary is up and has zkfc running on it.
  Option 2. Silent failover
  :param zkfc_user: User that started the ZKFC process.
  :return: Return True if ZKFC was killed, otherwise, false.
  """
  import params

  if params.dfs_ha_enabled:
    if params.zkfc_pid_file:
      identity = hdfs_process.recover_running_process(
        params.zkfc_pid_file, zkfc_user, "zkfc"
      )
      if identity is not None:
        Logger.debug("ZKFC is running and will be killed.")
        hdfs_process.terminate_process(identity, zkfc_user, "zkfc")
        hdfs_process.remove_pid_file_if_stopped(
          params.zkfc_pid_file, identity, zkfc_user, "zkfc"
        )
        return True
  return False


def service(
  action=None,
  name=None,
  user=None,
  options="",
  create_pid_dir=False,
  create_log_dir=False,
):
  """
  :param action: Either "start" or "stop"
  :param name: Component name, e.g., "namenode", "datanode", "secondarynamenode", "zkfc"
  :param user: User to run the command as
  :param options: Additional options to pass to command as a string
  :param create_pid_dir: Create PID directory
  :param create_log_dir: Crate log file directory
  """
  import params

  options = options if options else ""
  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  hadoop_env_exports = {"HADOOP_LIBEXEC_DIR": params.hadoop_libexec_dir}
  log_dir = format("{hdfs_log_dir_prefix}/{user}")

  # NFS GATEWAY is always started by root using jsvc due to rpcbind bugs
  # on Linux such as CentOS6.2. https://bugzilla.redhat.com/show_bug.cgi?id=731542
  if name == "nfs3":
    pid_file = format("{pid_dir}/hadoop_privileged_nfs3.pid")
    custom_export = {
      "HADOOP_PRIVILEGED_NFS_USER": params.hdfs_user,
      "HADOOP_PRIVILEGED_NFS_PID_DIR": pid_dir,
      "HADOOP_PRIVILEGED_NFS_LOG_DIR": log_dir,
    }
    hadoop_env_exports.update(custom_export)

  # on STOP directories shouldn't be created
  # since during stop still old dirs are used (which were created during previous start)
  if action != "stop":
    if name == "nfs3":
      Directory(
        params.hadoop_pid_dir_prefix,
        mode=0o755,
        owner=params.root_user,
        group=params.root_group,
      )
    else:
      Directory(
        params.hadoop_pid_dir_prefix,
        mode=0o755,
        owner=params.hdfs_user,
        group=params.user_group,
      )
    if create_pid_dir:
      Directory(pid_dir, owner=user, group=params.user_group, create_parents=True)
    if create_log_dir:
      if name == "nfs3":
        Directory(log_dir, mode=0o775, owner=params.root_user, group=params.user_group)
      else:
        Directory(log_dir, owner=user, group=params.user_group, create_parents=True)

  privileged = (
    params.security_enabled
    and name == "datanode"
    and params.secure_dn_ports_are_in_use
  )
  if params.security_enabled and name == "datanode":
    ## The directory where pid files are stored in the secure data environment.
    hadoop_secure_dn_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
    import status_params

    hadoop_secure_dn_pid_file = status_params.datanode_pid_file
    pid_file = hadoop_secure_dn_pid_file
    # At datanode_non_root stack version and further, we may start datanode as a non-root even in secure cluster
    if params.secure_dn_ports_are_in_use:
      user = "root"
    process_user = user

    if action == "stop" and os.path.isfile(hadoop_secure_dn_pid_file):
      # We need special handling for this case to handle the situation
      # when we configure non-root secure DN and then restart it
      # to handle new configs. Otherwise we will not be able to stop
      # a running instance
      user = "root"

      try:
        hdfs_process.check_component_status(
          hadoop_secure_dn_pid_file,
          process_user,
          "datanode",
          privileged=privileged,
        )

        custom_export = {"HADOOP_SECURE_DN_USER": params.hdfs_user}
        hadoop_env_exports.update(custom_export)

      except ComponentIsNotRunning:
        pass
  else:
    process_user = user

  hadoop_daemon = format("{hadoop_bin}/hadoop-daemon.sh")

  if user == "root":
    cmd = [hadoop_daemon, "--config", params.hadoop_conf_dir, action, name]
    if options:
      cmd += [
        options,
      ]
    daemon_cmd = as_sudo(cmd)
  else:
    cmd = format(
      "{ulimit_cmd} {hadoop_daemon} --config {hadoop_conf_dir} {action} {name}"
    )
    if options:
      cmd += " " + options
    daemon_cmd = as_user(cmd, user)

  if action == "start":
    running = hdfs_process.recover_running_process(
      pid_file,
      process_user,
      name,
      owner=process_user,
      group=params.user_group,
      privileged=privileged,
    )
    if running is not None:
      return

    try:
      Execute(daemon_cmd, environment=hadoop_env_exports)
      hdfs_process.wait_for_running_process(
        pid_file,
        process_user,
        name,
        owner=process_user,
        group=params.user_group,
        privileged=privileged,
      )
    except Exception:
      show_logs(log_dir, user)
      raise
  elif action == "stop":
    identity = hdfs_process.recover_running_process(
      pid_file,
      process_user,
      name,
      owner=process_user,
      group=params.user_group,
      privileged=privileged,
    )
    if identity is None:
      return

    try:
      Execute(daemon_cmd, environment=hadoop_env_exports)
    except Exception:
      show_logs(log_dir, user)
      raise

    if not hdfs_process.wait_for_process_stopped(
      identity, process_user, name, privileged=privileged
    ):
      hdfs_process.terminate_process(
        identity, process_user, name, privileged=privileged
      )
    hdfs_process.remove_pid_file_if_stopped(
      pid_file,
      identity,
      process_user,
      name,
      privileged=privileged,
    )


def get_jmx_data(
  nn_address, modeler_type, metric, encrypted=False, security_enabled=False
):
  """
  :param nn_address: Namenode Address, e.g., host:port, ** MAY ** be preceded with "http://" or "https://" already.
  If not preceded, will use the encrypted param to determine.
  :param modeler_type: Modeler type to query using startswith function
  :param metric: Metric to return
  :return: Return an object representation of the metric, or None if it does not exist
  """
  if not nn_address or not modeler_type or not metric:
    return None

  nn_address = nn_address.strip()
  if not nn_address.startswith("http"):
    nn_address = ("https://" if encrypted else "http://") + nn_address
  if not nn_address.endswith("/"):
    nn_address = nn_address + "/"

  nn_address = nn_address + "jmx"
  Logger.info(
    f"Retrieve modeler: {modeler_type}, metric: {metric} from JMX endpoint {nn_address}"
  )

  if security_enabled:
    import params

    data, error_msg, time_millis = curl_krb_request(
      params.tmp_dir,
      params.smoke_user_keytab,
      params.smokeuser_principal,
      nn_address,
      "jn_upgrade",
      params.kinit_path_local,
      False,
      None,
      params.smoke_user,
    )
  else:
    context = create_ssl_context(
      Script.get_force_https_protocol_name(), Script.get_ca_cert_file_path()
    )
    with urllib.request.urlopen(nn_address, context=context, timeout=10) as response:
      data = response.read()
  my_data = None
  if data:
    data_dict = json.loads(data)
    if data_dict:
      for el in data_dict["beans"]:
        if (
          el is not None
          and el["modelerType"] is not None
          and el["modelerType"].startswith(modeler_type)
        ):
          if metric in el:
            my_data = el[metric]
            if my_data:
              my_data = json.loads(str(my_data))
              break
  return my_data


def get_port(address):
  """
  Extracts a valid TCP port from an HTTP URL or host:port authority.
  """
  if address is None:
    return None
  if not isinstance(address, str) or not address or address != address.strip():
    raise Fail(f"Invalid HDFS network address {address!r}")

  has_scheme = "://" in address
  parsed = urllib.parse.urlparse(address if has_scheme else f"//{address}")
  if (
    (has_scheme and parsed.scheme not in ("http", "https"))
    or parsed.hostname is None
    or parsed.username is not None
    or parsed.password is not None
    or parsed.path not in ("", "/")
    or parsed.params
    or parsed.query
    or parsed.fragment
  ):
    raise Fail(f"Invalid HDFS network address {address!r}")
  try:
    port = parsed.port
  except ValueError as error:
    raise Fail(f"Invalid HDFS network address {address!r}") from error
  if port is None:
    raise Fail(f"Invalid HDFS network address {address!r}")
  return port


def is_secure_port(port):
  """
  Returns True if port is root-owned at *nix systems
  """
  if port is not None:
    return port < 1024
  else:
    return False


def is_previous_fs_image():
  """
  Return true if there's a previous folder in the HDFS namenode directories.
  """
  import params

  if params.dfs_name_dir:
    nn_name_dirs = params.dfs_name_dir.split(",")
    for nn_dir in nn_name_dirs:
      prev_dir = os.path.join(nn_dir, "previous")
      if os.path.isdir(prev_dir):
        return True
  return False


def get_hdfs_binary(distro_component_name):
  """
  Get the hdfs binary to use depending on the stack and version.
  :param distro_component_name: e.g., hadoop-hdfs-namenode, hadoop-hdfs-datanode
  :return: The hdfs binary to use
  """
  import params

  hdfs_binary = "hdfs"

  return hdfs_binary


def get_dfsadmin_base_command(hdfs_binary, use_specific_namenode=False):
  """
  Get the dfsadmin base command constructed using hdfs_binary path and passing namenode address as explicit -fs argument
  :param hdfs_binary: path to hdfs binary to use
  :param use_specific_namenode: flag if set and Namenode HA is enabled, then the dfsadmin command will use
  current namenode's address
  :return: the constructed dfsadmin base command
  """
  import params

  dfsadmin_base_command = ""
  if params.dfs_ha_enabled and use_specific_namenode:
    dfsadmin_base_command = format(
      "{hdfs_binary} dfsadmin -fs hdfs://{params.namenode_rpc}"
    )
  else:
    dfsadmin_base_command = format(
      "{hdfs_binary} dfsadmin -fs {params.namenode_address}"
    )
  return dfsadmin_base_command


def set_up_zkfc_security(params):
  """Sets up security for accessing zookeper on secure clusters"""

  if params.stack_supports_zk_security is False:
    Logger.info(
      "Skipping secure HDFS ZNode ACL setup because the stack feature is disabled."
    )
    return

  # check if the namenode is HA
  if params.dfs_ha_enabled is False:
    Logger.info(
      "Skipping secure HDFS ZNode ACL setup because NameNode HA is disabled."
    )
    return

  # check if the cluster is secure (skip otherwise)
  if params.security_enabled is False:
    Logger.info(
      "Skipping secure HDFS ZNode ACL setup because Kerberos is disabled."
    )
    return

  # process the JAAS template
  File(
    os.path.join(params.hadoop_conf_secure_dir, "hdfs_jaas.conf"),
    owner=params.hdfs_user,
    group=params.user_group,
    mode=0o644,
    content=Template("hdfs_jaas.conf.j2"),
  )
