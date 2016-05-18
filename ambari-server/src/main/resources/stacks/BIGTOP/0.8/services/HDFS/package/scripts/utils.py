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

from resource_management import *
import re


def service(action=None, name=None, user=None, create_pid_dir=False,
            create_log_dir=False):
  import params

  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  log_dir = format("{hdfs_log_dir_prefix}/{user}")
  check_process = format(
    "ls {pid_file} >/dev/null 2>&1 &&"
    " ps -p `cat {pid_file}` >/dev/null 2>&1")

  if create_pid_dir:
    Directory(pid_dir,
              owner=user,
              create_parents = True)
  if create_log_dir:
    Directory(log_dir,
              owner=user,
              create_parents = True)

  hadoop_env_exports = {
    'HADOOP_LIBEXEC_DIR': params.hadoop_libexec_dir
  }

  if params.security_enabled and name == "datanode":
    dfs_dn_port = get_port(params.dfs_dn_addr)
    dfs_dn_http_port = get_port(params.dfs_dn_http_addr)
    dfs_dn_https_port = get_port(params.dfs_dn_https_addr)

    # We try to avoid inability to start datanode as a plain user due to usage of root-owned ports
    if params.dfs_http_policy == "HTTPS_ONLY":
      secure_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_https_port)
    elif params.dfs_http_policy == "HTTP_AND_HTTPS":
      secure_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port) or is_secure_port(dfs_dn_https_port)
    else:   # params.dfs_http_policy == "HTTP_ONLY" or not defined:
      secure_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port)

    # Calculate HADOOP_SECURE_DN_* env vars, but not append them yet
    # These variables should not be set when starting secure datanode as a non-root
    ## On secure datanodes, user to run the datanode as after dropping privileges
    hadoop_secure_dn_user = params.hdfs_user
    ## Where log files are stored in the secure data environment.
    hadoop_secure_dn_log_dir = format("{hdfs_log_dir_prefix}/{hadoop_secure_dn_user}")
    ## The directory where pid files are stored in the secure data environment.
    hadoop_secure_dn_pid_dir = format("{hadoop_pid_dir_prefix}/{hadoop_secure_dn_user}")
    hadoop_secure_dn_exports = {
      'HADOOP_SECURE_DN_USER' : hadoop_secure_dn_user,
      'HADOOP_SECURE_DN_LOG_DIR' : hadoop_secure_dn_log_dir,
      'HADOOP_SECURE_DN_PID_DIR' : hadoop_secure_dn_pid_dir
    }
    hadoop_secure_dn_pid_file = format("{hadoop_secure_dn_pid_dir}/hadoop_secure_dn.pid")

    # At Champlain stack and further, we may start datanode as a non-root even in secure cluster
    if not params.stack_is_champlain_or_further or secure_ports_are_in_use:
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")
      if params.stack_is_champlain_or_further:
        hadoop_env_exports.update(hadoop_secure_dn_exports)

    if action == 'stop' and params.stack_is_champlain_or_further and \
      os.path.isfile(hadoop_secure_dn_pid_file):
        # We need special handling for this case to handle the situation
        # when we configure non-root secure DN and then restart it
        # to handle new configs. Otherwise we will not be able to stop
        # a running instance
        user = "root"
        try:
          with open(hadoop_secure_dn_pid_file, 'r') as f:
            pid = f.read()
          os.kill(int(pid), 0)
          hadoop_env_exports.update(hadoop_secure_dn_exports)
        except IOError:
          pass  # Can not open pid file
        except ValueError:
          pass  # Pid file content is invalid
        except OSError:
          pass  # Process is not running


  hadoop_env_exports_str = ''
  for exp in hadoop_env_exports.items():
    hadoop_env_exports_str += "export {0}={1} && ".format(exp[0], exp[1])

  hadoop_daemon = format(
    "{hadoop_env_exports_str}"
    "{hadoop_bin}/hadoop-daemon.sh")
  cmd = format("{hadoop_daemon} --config {hadoop_conf_dir}")

  daemon_cmd = format("{ulimit_cmd} su -s /bin/bash - {user} -c '{cmd} {action} {name}'")

  service_is_up = check_process if action == "start" else None
  #remove pid file from dead process
  File(pid_file,
       action="delete",
       not_if=check_process,
  )
  Execute(daemon_cmd,
          not_if=service_is_up
  )
  if action == "stop":
    File(pid_file,
         action="delete",
    )

def get_port(address):
  """
  Extracts port from the address like 0.0.0.0:1019
  """
  if address is None:
    return None
  m = re.search(r'(?:http(?:s)?://)?([\w\d.]*):(\d{1,5})', address)
  if m is not None:
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
