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

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.core.system import System
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import default, format
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.cluster_settings import get_cluster_setting_value
from resource_management.libraries.execution_command import execution_command
from resource_management.libraries.execution_command import module_configs

config = Script.get_config()
execution_command = Script.get_execution_command()
module_configs = Script.get_module_configs()
module_name = execution_command.get_module_name()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_version_unformatted = execution_command.get_mpack_version()
agent_stack_retry_on_unavailability = execution_command.check_agent_stack_want_retry_on_unavailability()
agent_stack_retry_count = execution_command.get_agent_stack_retry_count()
stack_version_formatted = format_stack_version(stack_version_unformatted)

#users and groups
hbase_user = module_configs.get_property_value(module_name, 'hbase-env', 'hbase_user')
smoke_user = get_cluster_setting_value('smokeuser')
gmetad_user = module_configs.get_property_value(module_name, 'ganglia-env', 'gmetad_user')
gmond_user = module_configs.get_property_value(module_name, 'ganglia-env', 'gmond_user')
tez_user = module_configs.get_property_value(module_name, 'tez-env', 'tez_user')

user_group = get_cluster_setting_value('user_group')
proxyuser_group = module_configs.get_property_value(module_name, 'hadoop-env', 'proxyuser_group', 'users')

hdfs_log_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_log_dir_prefix')

# repo templates
repo_rhel_suse =  get_cluster_setting_value('repo_suse_rhel_template')
repo_ubuntu =  get_cluster_setting_value('repo_ubuntu_template')

#hosts
hostname = execution_command.get_host_name()
ambari_server_hostname = execution_command.get_ambari_server_host()
rm_host = execution_command.get_component_hosts('resourcemanager')
slave_hosts = execution_command.get_component_hosts('datanode')
oozie_servers = execution_command.get_component_hosts('oozie_server')
hcat_server_hosts = execution_command.get_component_hosts('webhcat_server')
hive_server_host =  execution_command.get_component_hosts('hive_server')
hbase_master_hosts = execution_command.get_component_hosts('hbase_master')
hs_host = execution_command.get_component_hosts('historyserver')
jtnode_host = execution_command.get_component_hosts('jtnode')
namenode_host = execution_command.get_component_hosts('namenode')
zk_hosts = execution_command.get_component_hosts('zookeeper_server')
ganglia_server_hosts = execution_command.get_component_hosts('ganglia_server')
storm_server_hosts = execution_command.get_component_hosts('nimbus')
falcon_host = execution_command.get_component_hosts('falcon_server')

has_sqoop_client = bool(module_configs.get_all_properties(module_name, 'sqoop-env'))
has_namenode = not len(namenode_host) == 0
has_hs = not len(hs_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
has_oozie_server = not len(oozie_servers)  == 0
has_hcat_server_host = not len(hcat_server_hosts) == 0
has_hive_server_host = not len(hive_server_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_zk_host = not len(zk_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_storm_server = not len(storm_server_hosts) == 0
has_falcon_server = not len(falcon_host) == 0
has_tez = bool(module_configs.get_all_properties(module_name, 'tez-site'))

is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

hbase_tmp_dir = "/tmp/hbase-hbase"

#security params
security_enabled = get_cluster_setting_value('security_enabled')

#java params
java_home = execution_command.get_java_home()
artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jdk_name = execution_command.get_jdk_name() # None when jdk is already installed by user
jce_policy_zip = execution_command.get_jce_name() # None when jdk is already installed by user
jce_location = execution_command.get_jdk_location()
jdk_location = execution_command.get_jdk_location()
ignore_groupsusers_create = get_cluster_setting_value('ignore_groupsusers_create')
host_sys_prepped = execution_command.is_host_system_prepared()

smoke_user_dirs = format("/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
if has_hbase_masters:
  hbase_user_dirs = format("/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
#repo params
repo_info = execution_command.get_repo_info()
service_repo_info = execution_command.get_service_repo_info()

repo_file = execution_command.get_repository_file()
