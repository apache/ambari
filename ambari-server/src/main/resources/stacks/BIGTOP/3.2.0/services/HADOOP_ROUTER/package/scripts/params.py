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
from ambari_commons import OSCheck
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version
import status_params
from resource_management.libraries.functions import stack_select, conf_select
import os

config = Script.get_config()


stack_root = Script.get_stack_root()
stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

hadoop_pid_dir_prefix = "/var/run/hadoop"
root_user="root"

hadoop_router_hosts = default("/clusterHostInfo/router_hosts", [])
router_addr = config['configurations']['hdfs-rbf-site']['dfs.federation.router.http-address']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

dfsrouter_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-{root_user}-dfsrouter.pid")
user_group = config['configurations']['cluster-env']['user_group']
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
hdfs_log_dir_prefix = "/var/log/hadoop"

if 'viewfs-mount-table' in config['configurations']:
  xml_inclusion_file_name = 'viewfs-mount-table.xml'
  mount_table = config['configurations']['viewfs-mount-table']

  if 'content' in mount_table and mount_table['content'].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(hadoop_conf_dir, xml_inclusion_file_name)
    mount_table_content = mount_table['content']

smoke_user = config['configurations']['cluster-env']['smokeuser']
tmp_dir = Script.get_tmp_dir()