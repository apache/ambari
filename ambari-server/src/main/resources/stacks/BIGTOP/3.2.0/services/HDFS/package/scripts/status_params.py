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

from ambari_commons import OSCheck

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script

import utils

config = Script.get_config()

hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
# Some datanode settings
dfs_dn_addr = default('/configurations/hdfs-site/dfs.datanode.address', None)
dfs_dn_http_addr = default('/configurations/hdfs-site/dfs.datanode.http.address', None)
dfs_dn_https_addr = default('/configurations/hdfs-site/dfs.datanode.https.address', None)
dfs_http_policy = default('/configurations/hdfs-site/dfs.http.policy', None)
dfs_dn_ipc_address = config['configurations']['hdfs-site']['dfs.datanode.ipc.address']
secure_dn_ports_are_in_use = False
hadoop_secure_dn_user = hdfs_user

security_enabled = config['configurations']['cluster-env']['security_enabled']
if not security_enabled:
  hadoop_secure_dn_user = '""'
else:
  dfs_dn_port = utils.get_port(dfs_dn_addr)
  dfs_dn_http_port = utils.get_port(dfs_dn_http_addr)
  dfs_dn_https_port = utils.get_port(dfs_dn_https_addr)
  # We try to avoid inability to start datanode as a plain user due to usage of root-owned ports
  if dfs_http_policy == "HTTPS_ONLY":
    secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_https_port)
  elif dfs_http_policy == "HTTP_AND_HTTPS":
    secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_http_port) or utils.is_secure_port(dfs_dn_https_port)
  else:   # params.dfs_http_policy == "HTTP_ONLY" or not defined:
    secure_dn_ports_are_in_use = utils.is_secure_port(dfs_dn_port) or utils.is_secure_port(dfs_dn_http_port)
  if secure_dn_ports_are_in_use:
    hadoop_secure_dn_user = hdfs_user
  else:
    hadoop_secure_dn_user = '""'

if OSCheck.is_windows_family():
  namenode_win_service_name = "namenode"
  datanode_win_service_name = "datanode"
  snamenode_win_service_name = "secondarynamenode"
  journalnode_win_service_name = "journalnode"
  zkfc_win_service_name = "zkfc"
else:
  hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']
  hadoop_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")

  root_user = 'root'
  datanode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-datanode.pid")
  datanode_secure_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-{root_user}-datanode.pid")
  if secure_dn_ports_are_in_use:
    datanode_pid_file = datanode_secure_pid_file

  namenode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-namenode.pid")
  snamenode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-secondarynamenode.pid")
  journalnode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-journalnode.pid")
  zkfc_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-zkfc.pid")
  nfsgateway_pid_file = format("{hadoop_pid_dir_prefix}/root/hadoop_privileged_nfs3.pid")

  # Security related/required params
  hostname = config['agentLevelParams']['hostname']
  hdfs_user_principal = config['configurations']['hadoop-env']['hdfs_principal_name']
  hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

  hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  tmp_dir = Script.get_tmp_dir()

stack_name = default("/clusterLevelParams/stack_name", None)