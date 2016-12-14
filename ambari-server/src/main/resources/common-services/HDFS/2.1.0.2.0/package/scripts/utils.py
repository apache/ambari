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
import re
import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from resource_management.core.resources.system import Directory, File, Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core import shell
from resource_management.core.shell import as_user, as_sudo
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.namenode_ha_utils import get_namenode_states
from resource_management.libraries.functions.show_logs import show_logs
from ambari_commons.inet_utils import ensure_ssl_using_protocol
from zkfc_slave import ZkfcSlaveDefault

ensure_ssl_using_protocol(Script.get_force_https_protocol())

def safe_zkfc_op(action, env):
  """
  Idempotent operation on the zkfc process to either start or stop it.
  :param action: start or stop
  :param env: environment
  """
  Logger.info("Performing action {0} on zkfc.".format(action))
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
    Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
            user = params.hdfs_user)

  active_namenode_id = None
  standby_namenode_id = None
  active_namenodes, standby_namenodes, unknown_namenodes = get_namenode_states(params.hdfs_site, params.security_enabled, params.hdfs_user)
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
      Logger.info("NameNode HA state for {0} is unknown".format(unknown_namenode[0]))

  if params.namenode_id == active_namenode_id and params.other_namenode_id == standby_namenode_id:
    # Failover if this NameNode is active and other NameNode is up and in standby (i.e. ready to become active on failover)
    Logger.info(format("NameNode {namenode_id} is active and NameNode {other_namenode_id} is in standby"))

    failover_command = format("hdfs haadmin -ns {dfs_ha_nameservices} -failover {namenode_id} {other_namenode_id}")
    check_standby_cmd = format("hdfs haadmin -ns {dfs_ha_nameservices} -getServiceState {namenode_id} | grep standby")

    msg = "Rolling Upgrade - Initiating a ZKFC failover on active NameNode host {0}.".format(params.hostname)
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
      Execute(check_standby_cmd,
              user=params.hdfs_user,
              tries=50,
              try_sleep=6,
              logoutput=True)
  else:
    msg = "Rolling Upgrade - Skipping ZKFC failover on NameNode host {0}.".format(params.hostname)
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
      check_process = as_user(format("ls {zkfc_pid_file} > /dev/null 2>&1 && ps -p `cat {zkfc_pid_file}` > /dev/null 2>&1"), user=zkfc_user)
      code, out = shell.call(check_process)
      if code == 0:
        Logger.debug("ZKFC is running and will be killed.")
        kill_command = format("kill -15 `cat {zkfc_pid_file}`")
        Execute(kill_command,
                user=zkfc_user
        )
        File(params.zkfc_pid_file,
             action = "delete",
             )
        return True
  return False

def service(action=None, name=None, user=None, options="", create_pid_dir=False,
            create_log_dir=False):
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
  hadoop_env_exports = {
    'HADOOP_LIBEXEC_DIR': params.hadoop_libexec_dir
  }
  log_dir = format("{hdfs_log_dir_prefix}/{user}")

  # NFS GATEWAY is always started by root using jsvc due to rpcbind bugs
  # on Linux such as CentOS6.2. https://bugzilla.redhat.com/show_bug.cgi?id=731542
  if name == "nfs3" :
    pid_file = format("{pid_dir}/hadoop_privileged_nfs3.pid")
    custom_export = {
      'HADOOP_PRIVILEGED_NFS_USER': params.hdfs_user,
      'HADOOP_PRIVILEGED_NFS_PID_DIR': pid_dir,
      'HADOOP_PRIVILEGED_NFS_LOG_DIR': log_dir
    }
    hadoop_env_exports.update(custom_export)

  process_id_exists_command = as_sudo(["test", "-f", pid_file]) + " && " + as_sudo(["pgrep", "-F", pid_file])

  # on STOP directories shouldn't be created
  # since during stop still old dirs are used (which were created during previous start)
  if action != "stop":
    if name == "nfs3":
      Directory(params.hadoop_pid_dir_prefix,
                mode=0755,
                owner=params.root_user,
                group=params.root_group
      )
    else:
      Directory(params.hadoop_pid_dir_prefix,
                  mode=0755,
                  owner=params.hdfs_user,
                  group=params.user_group
      )
    if create_pid_dir:
      Directory(pid_dir,
                owner=user,
                group=params.user_group,
                create_parents = True)
    if create_log_dir:
      if name == "nfs3":
        Directory(log_dir,
                  mode=0775,
                  owner=params.root_user,
                  group=params.user_group)
      else:
        Directory(log_dir,
                  owner=user,
                  group=params.user_group,
                  create_parents = True)

  if params.security_enabled and name == "datanode":
    ## The directory where pid files are stored in the secure data environment.
    hadoop_secure_dn_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
    hadoop_secure_dn_pid_file = format("{hadoop_secure_dn_pid_dir}/hadoop_secure_dn.pid")

    # At datanode_non_root stack version and further, we may start datanode as a non-root even in secure cluster
    if not (params.stack_version_formatted and check_stack_feature(StackFeature.DATANODE_NON_ROOT, params.stack_version_formatted)) or params.secure_dn_ports_are_in_use:
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")

    if action == 'stop' and (params.stack_version_formatted and check_stack_feature(StackFeature.DATANODE_NON_ROOT, params.stack_version_formatted)) and \
      os.path.isfile(hadoop_secure_dn_pid_file):
        # We need special handling for this case to handle the situation
        # when we configure non-root secure DN and then restart it
        # to handle new configs. Otherwise we will not be able to stop
        # a running instance 
        user = "root"
        
        try:
          check_process_status(hadoop_secure_dn_pid_file)
          
          custom_export = {
            'HADOOP_SECURE_DN_USER': params.hdfs_user
          }
          hadoop_env_exports.update(custom_export)
          
        except ComponentIsNotRunning:
          pass

  hadoop_daemon = format("{hadoop_bin}/hadoop-daemon.sh")

  if user == "root":
    cmd = [hadoop_daemon, "--config", params.hadoop_conf_dir, action, name]
    if options:
      cmd += [options, ]
    daemon_cmd = as_sudo(cmd)
  else:
    cmd = format("{ulimit_cmd} {hadoop_daemon} --config {hadoop_conf_dir} {action} {name}")
    if options:
      cmd += " " + options
    daemon_cmd = as_user(cmd, user)
     
  if action == "start":
    # remove pid file from dead process
    File(pid_file, action="delete", not_if=process_id_exists_command)
    
    try:
      Execute(daemon_cmd, not_if=process_id_exists_command, environment=hadoop_env_exports)
    except:
      show_logs(log_dir, user)
      raise
  elif action == "stop":
    try:
      Execute(daemon_cmd, only_if=process_id_exists_command, environment=hadoop_env_exports)
    except:
      show_logs(log_dir, user)
      raise
    File(pid_file, action="delete")

def get_jmx_data(nn_address, modeler_type, metric, encrypted=False, security_enabled=False):
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
  Logger.info("Retrieve modeler: %s, metric: %s from JMX endpoint %s" % (modeler_type, metric, nn_address))

  if security_enabled:
    import params
    data, error_msg, time_millis = curl_krb_request(params.tmp_dir, params.smoke_user_keytab, params.smokeuser_principal, nn_address,
                            "jn_upgrade", params.kinit_path_local, False, None, params.smoke_user)
  else:
    data = urllib2.urlopen(nn_address).read()
  my_data = None
  if data:
    data_dict = json.loads(data)
    if data_dict:
      for el in data_dict['beans']:
        if el is not None and el['modelerType'] is not None and el['modelerType'].startswith(modeler_type):
          if metric in el:
            my_data = el[metric]
            if my_data:
              my_data = json.loads(str(my_data))
              break
  return my_data

def get_port(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None and len(m.groups()) >= 2:
    return int(m.group(2))
  else:
    return None


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
    nn_name_dirs = params.dfs_name_dir.split(',')
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
  if params.stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version_formatted):
    hdfs_binary = "{0}/current/{1}/bin/hdfs".format(params.stack_root, distro_component_name)

  return hdfs_binary

def get_dfsadmin_base_command(hdfs_binary, use_specific_namenode = False):
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
    dfsadmin_base_command = format("{hdfs_binary} dfsadmin -fs hdfs://{params.namenode_rpc}")
  else:
    dfsadmin_base_command = format("{hdfs_binary} dfsadmin -fs {params.namenode_address}")
  return dfsadmin_base_command
