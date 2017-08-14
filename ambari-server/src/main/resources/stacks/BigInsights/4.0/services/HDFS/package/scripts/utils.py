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
import json

from resource_management import *
from resource_management.libraries.functions.format import format
from resource_management.core.shell import call, checked_call
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.check_process_status import wait_process_stopped

from zkfc_slave import ZkfcSlave

def safe_zkfc_op(action, env):
  """
  Idempotent operation on the zkfc process to either start or stop it.
  :param action: start or stop
  :param env: environment
  """
  zkfc = None
  if action == "start":
    try:
      zkfc = ZkfcSlave()
      zkfc.status(env)
    except ComponentIsNotRunning:
      if zkfc:
        zkfc.start(env)

  if action == "stop":
    try:
      zkfc = ZkfcSlave()
      zkfc.status(env)
    except ComponentIsNotRunning:
      pass
    else:
      if zkfc:
        zkfc.stop(env)


def failover_namenode():
  """
  Failover the primary namenode by killing zkfc if it exists on this host (assuming this host is the primary).
  """
  import params
  check_service_cmd = format("hdfs haadmin -getServiceState {namenode_id}")
  code, out = call(check_service_cmd, logoutput=True, user=params.hdfs_user)

  state = "unknown"
  if code == 0 and out:
    state = "active" if "active" in out else ("standby" if "standby" in out else state)
    Logger.info("Namenode service state: %s" % state)

  if state == "active":
    Logger.info("Rolling Upgrade - Initiating namenode failover by killing zkfc on active namenode")

    # Forcefully kill ZKFC on this host to initiate a failover
    # If ZKFC is already dead, then potentially this node can still be the active one.
    was_zkfc_killed = kill_zkfc(params.hdfs_user)

    # Wait until it transitions to standby
    check_standby_cmd = format("hdfs haadmin -getServiceState {namenode_id} | grep standby")

    # process may already be down.  try one time, then proceed
    code, out = call(check_standby_cmd, user=params.hdfs_user, logoutput=True)
    Logger.info(format("Rolling Upgrade - check for standby returned {code}"))

    if code == 255 and out:
      Logger.info("Rolling Upgrade - namenode is already down")
    else:
      if was_zkfc_killed:
        # Only mandate that this be the standby namenode if ZKFC was indeed killed to initiate a failover.
        Execute(check_standby_cmd,
                user=params.hdfs_user,
                tries=50,
                try_sleep=6,
                logoutput=True)

  else:
    Logger.info("Rolling Upgrade - Host %s is the standby namenode." % str(params.hostname))


def kill_zkfc(zkfc_user):
  """
  There are two potential methods for failing over the namenode, especially during a Rolling Upgrade.
  Option 1. Kill zkfc on primary namenode provided that the secondary is up and has zkfc running on it.
  Option 2. Silent failover (not supported as of IOP 4.0.0.0)
  :param zkfc_user: User that started the ZKFC process.
  :return: Return True if ZKFC was killed, otherwise, false.
  """
  import params
  if params.dfs_ha_enabled:
    zkfc_pid_file = get_service_pid_file("zkfc", zkfc_user)
    if zkfc_pid_file:
      check_process = format("ls {zkfc_pid_file} > /dev/null 2>&1 && ps -p `cat {zkfc_pid_file}` > /dev/null 2>&1")
      code, out = call(check_process)
      if code == 0:
        Logger.debug("ZKFC is running and will be killed to initiate namenode failover.")
        kill_command = format("{check_process} && kill -9 `cat {zkfc_pid_file}` > /dev/null 2>&1")
        Execute(kill_command)
        Execute(format("rm -f {zkfc_pid_file}"))
        return True
  return False


def get_service_pid_file(name, user):
  """
  Get the pid file path that was used to start the service by the user.
  :param name: Service name
  :param user: User that started the service.
  :return: PID file path
  """
  import params
  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  return pid_file


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

  check_process = format(
    "ls {pid_file} >/dev/null 2>&1 &&"
    " ps -p `cat {pid_file}` >/dev/null 2>&1")

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
                create_parents=True)
    if create_log_dir:
      if name == "nfs3":
        Directory(log_dir,
                  mode=0775,
                  owner=params.root_user,
                  group=params.user_group)
      else:
        Directory(log_dir,
                  owner=user,
                  create_parents=True)

  if params.security_enabled and name == "datanode":
    ## The directory where pid files are stored in the secure data environment.
    hadoop_secure_dn_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
    hadoop_secure_dn_pid_file = format("{hadoop_secure_dn_pid_dir}/hadoop_secure_dn.pid")

    if params.secure_dn_ports_are_in_use:
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")

    if action == 'stop' and os.path.isfile(hadoop_secure_dn_pid_file):
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

  service_is_up = check_process if action == "start" else None
  #remove pid file from dead process
  File(pid_file,
       action="delete",
       not_if=check_process
  )
  Execute(daemon_cmd,
          not_if=service_is_up,
          environment=hadoop_env_exports
  )

  if action == "stop":
    wait_process_stopped(pid_file)
    File(pid_file,
         action="delete",
    )


def get_value_from_jmx(qry, property):
  try:
    response = urllib2.urlopen(qry)
    data = response.read()
    if data:
      data_dict = json.loads(data)
      return data_dict["beans"][0][property]
  except:
    return None

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
