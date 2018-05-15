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

import collections
import re
import os
import ast

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.script import Script
from resource_management.libraries.functions import default
from resource_management.libraries.functions import format
from resource_management.libraries.functions.format_jvm_option import format_jvm_option_value
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.get_architecture import get_architecture
from resource_management.libraries.functions.cluster_settings import get_cluster_setting_value
from resource_management.libraries.functions.mpack_manager_helper import get_component_conf_path, get_component_home_path
from resource_management.libraries.execution_command import execution_command
from resource_management.libraries.execution_command import module_configs
from resource_management.libraries.functions.namenode_ha_utils import get_properties_for_all_nameservices, namenode_federation_enabled

config = Script.get_config()
execution_command = Script.get_execution_command()
module_configs = Script.get_module_configs()
module_name = execution_command.get_module_name()
tmp_dir = Script.get_tmp_dir()

stack_root = Script.get_stack_root()

architecture = get_architecture()

dfs_type = execution_command.get_dfs_type()

artifact_dir = format("{tmp_dir}/AMBARI-artifacts/")
jdk_name = execution_command.get_jdk_name()
java_home = execution_command.get_java_home()
java_version = execution_command.get_java_version()
jdk_location = execution_command.get_jdk_location()

hadoop_custom_extensions_enabled = module_configs.get_property_value(module_name, 'core-site', 'hadoop.custom-extensions.enabled', False)

sudo = AMBARI_SUDO_BINARY

ambari_server_hostname = execution_command.get_ambari_server_host()

stack_version_unformatted = execution_command.get_mpack_version()
stack_version_formatted = execution_command.get_mpack_version()

upgrade_type = Script.get_upgrade_type(execution_command.get_upgrade_type())
version = execution_command.get_new_mpack_version_for_upgrade()
# Handle upgrade and downgrade
if (upgrade_type is not None) and version:
  stack_version_formatted = format_stack_version(version)
"""
??? is this the same as ambariLevelParams/java_home and ambariLevelParams/java_name ???
"""
ambari_java_home = execution_command.get_ambari_java_home()
ambari_jdk_name = execution_command.get_ambari_jdk_name()

security_enabled = get_cluster_setting_value('security_enabled')
hdfs_user = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_user')

# Some datanode settings
dfs_dn_addr = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.datanode.address')
dfs_dn_http_addr = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.datanode.http.address')
dfs_dn_https_addr = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.datanode.https.address')
dfs_http_policy = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.http.policy')
secure_dn_ports_are_in_use = False

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

# upgrades would cause these directories to have a version instead of "current"
# which would cause a lot of problems when writing out hadoop-env.sh; instead
# force the use of "current" in the hook
hdfs_user_nofile_limit = default("/configurations/hadoop-env/hdfs_user_nofile_limit", "128000")

mpack_name = execution_command.get_mpack_name()
mpack_instance_name = execution_command.get_servicegroup_name()
module_name = execution_command.get_module_name()
component_type = execution_command.get_component_type()
component_instance_name = execution_command.get_component_instance_name()

stack_name = mpack_name.lower()
component_directory = "namenode"

hadoop_dir = "/etc/hadoop"
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")
datanode_max_locked_memory = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.datanode.max.locked.memory')
is_datanode_max_locked_memory_set = not is_empty(module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.datanode.max.locked.memory'))

mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"

if not security_enabled:
  hadoop_secure_dn_user = '""'
else:
  dfs_dn_port = get_port(dfs_dn_addr)
  dfs_dn_http_port = get_port(dfs_dn_http_addr)
  dfs_dn_https_port = get_port(dfs_dn_https_addr)
  # We try to avoid inability to start datanode as a plain user due to usage of root-owned ports
  if dfs_http_policy == "HTTPS_ONLY":
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_https_port)
  elif dfs_http_policy == "HTTP_AND_HTTPS":
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port) or is_secure_port(dfs_dn_https_port)
  else:   # params.dfs_http_policy == "HTTP_ONLY" or not defined:
    secure_dn_ports_are_in_use = is_secure_port(dfs_dn_port) or is_secure_port(dfs_dn_http_port)
  if secure_dn_ports_are_in_use:
    hadoop_secure_dn_user = hdfs_user
  else:
    hadoop_secure_dn_user = '""'

#hadoop params
hdfs_log_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_log_dir_prefix')
hadoop_pid_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_pid_dir_prefix')
hadoop_root_logger = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_root_logger')

jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_heapsize')
namenode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_heapsize')
namenode_opt_newsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_newsize')
namenode_opt_maxnewsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxnewsize')
namenode_opt_permsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_permsize', '128m'), '128m')
namenode_opt_maxpermsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxpermsize', '256m'), '256m')

ttnode_heapsize = "1024m"

dtnode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'dtnode_heapsize')
nfsgateway_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'nfsgateway_heapsize')
mapred_pid_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_pid_dir_prefix', '/var/run/hadoop-mapreduce')
mapred_log_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_log_dir_prefix', '/var/log/hadoop-mapreduce')
hadoop_env_sh_template = module_configs.get_property_value(module_name, 'hadoop-env', 'content')

#users and groups
hbase_user = module_configs.get_property_value(module_name, 'hbase-env', 'hbase_user')
smoke_user =  get_cluster_setting_value('smokeuser')
gmetad_user = module_configs.get_property_value(module_name, 'ganglia-env', 'gmetad_user')
gmond_user = module_configs.get_property_value(module_name, 'ganglia-env', 'gmond_user')
tez_user = module_configs.get_property_value(module_name, 'tez-env', 'tez_user')
oozie_user = module_configs.get_property_value(module_name, 'oozie-env', 'oozie_user')
falcon_user = module_configs.get_property_value(module_name, 'falcon-env', 'falcon_user')
ranger_user = module_configs.get_property_value(module_name, 'ranger-env', 'ranger_user')
zeppelin_user = module_configs.get_property_value(module_name, 'zeppelin-env', 'zeppelin_user')
zeppelin_group = module_configs.get_property_value(module_name, 'zeppelin-env', 'zeppelin_group')

user_group = get_cluster_setting_value('user_group')

ganglia_server_hosts = execution_command.get_component_hosts('ganglia_server')
namenode_host = execution_command.get_component_hosts('namenode')
hbase_master_hosts = execution_command.get_component_hosts('hbase_master')
oozie_servers = execution_command.get_component_hosts('oozie_server')
falcon_server_hosts = execution_command.get_component_hosts('falcon_server')
ranger_admin_hosts = execution_command.get_component_hosts('ranger_admin')
zeppelin_master_hosts = execution_command.get_component_hosts('zeppelin_master')

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)


has_namenode = not len(namenode_host) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_tez = bool(module_configs.get_all_properties(module_name, 'tez-site'))
has_hbase_masters = not len(hbase_master_hosts) == 0
has_oozie_server = not len(oozie_servers) == 0
has_falcon_server_hosts = not len(falcon_server_hosts) == 0
has_ranger_admin = not len(ranger_admin_hosts) == 0
has_zeppelin_master = not len(zeppelin_master_hosts) == 0
stack_supports_zk_security = check_stack_feature(StackFeature.SECURE_ZOOKEEPER, version_for_stack_feature_checks)

hostname = config['agentLevelParams']['hostname']
hdfs_site = config['configurations']['hdfs-site']

# HDFS High Availability properties
dfs_ha_enabled = False
dfs_ha_nameservices = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.internal.nameservices')
if dfs_ha_nameservices is None:
  dfs_ha_nameservices = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.nameservices')

# on stacks without any filesystem there is no hdfs-site
dfs_ha_namenode_ids_all_ns = get_properties_for_all_nameservices(hdfs_site, 'dfs.ha.namenodes') if 'hdfs-site' in config['configurations'] else {}
dfs_ha_automatic_failover_enabled = default("/configurations/hdfs-site/dfs.ha.automatic-failover.enabled", False)

# Values for the current Host
namenode_id = None
namenode_rpc = None

dfs_ha_namemodes_ids_list = []
other_namenode_id = None

for ns, dfs_ha_namenode_ids in dfs_ha_namenode_ids_all_ns.iteritems():
  found = False
  if not is_empty(dfs_ha_namenode_ids):
    dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
    dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
    if dfs_ha_namenode_ids_array_len > 1:
      dfs_ha_enabled = True
  if dfs_ha_enabled:
    for nn_id in dfs_ha_namemodes_ids_list:
      nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{ns}.{nn_id}')]
      if hostname in nn_host:
        namenode_id = nn_id
        namenode_rpc = nn_host
        found = True
    # With HA enabled namenode_address is recomputed
    namenode_address = format('hdfs://{ns}')

    # Calculate the namenode id of the other namenode. This is needed during RU to initiate an HA failover using ZKFC.
    if namenode_id is not None and len(dfs_ha_namemodes_ids_list) == 2:
      other_namenode_id = list(set(dfs_ha_namemodes_ids_list) - set([namenode_id]))[0]

  if found:
    break

# if has_namenode or dfs_type == 'HCFS':
#     hadoop_conf_dir = get_component_conf_path(mpack_name=mpack_name, instance_name=mpack_instance_name,
#                                               module_name=HADOOP_CLIENTS_MODULE_NAME,
#                                               components_instance_type=HADOOP_CLIENT_COMPONENT_TYPE)
#     hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")

hbase_tmp_dir = "/tmp/hbase-hbase"

proxyuser_group = module_configs.get_property_value(module_name, 'hadoop-env', 'proxyuser_group', 'users')
ranger_group = module_configs.get_property_value(module_name, 'ranger-env', 'ranger_group')
dfs_cluster_administrators_group = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.cluster.administrators')

sysprep_skip_create_users_and_groups = get_cluster_setting_value('sysprep_skip_create_users_and_groups')
ignore_groupsusers_create = get_cluster_setting_value('ignore_groupsusers_create')
fetch_nonlocal_groups = get_cluster_setting_value('fetch_nonlocal_groups')

smoke_user_dirs = format("/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
if has_hbase_masters:
  hbase_user_dirs = format("/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
#repo params
repo_info = execution_command.get_repo_info()
service_repo_info = execution_command.get_service_repo_info()

user_to_groups_dict = {}

#Append new user-group mapping to the dict
try:
  user_group_map = ast.literal_eval(execution_command.get_user_groups())
  for key in user_group_map.iterkeys():
    user_to_groups_dict[key] = user_group_map[key]
except ValueError:
  print('User Group mapping (user_group) is missing in the hostLevelParams')

user_to_gid_dict = collections.defaultdict(lambda:user_group)

user_list = json.loads(execution_command.get_user_list())
group_list = json.loads(execution_command.get_group_list())
host_sys_prepped = execution_command.is_host_system_prepared()

tez_am_view_acls = module_configs.get_property_value(module_name, 'tez-site', 'tez.am.view-acls')
override_uid = get_cluster_setting_value('override_uid')

# if NN HA on secure clutser, access Zookeper securely
# if stack_supports_zk_security and dfs_ha_enabled and security_enabled:
#     hadoop_zkfc_opts=format("-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username=zookeeper -Djava.security.auth.login.config={hadoop_conf_secure_dir}/hdfs_jaas.conf -Dzookeeper.sasl.clientconfig=Client")
