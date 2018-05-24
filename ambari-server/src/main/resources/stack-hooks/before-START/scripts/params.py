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

from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.format_jvm_option import format_jvm_option_value
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version, get_major_version
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import StackFeature
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.cluster_settings import get_cluster_setting_value

config = Script.get_config()
execution_command = Script.get_execution_command()
module_configs = Script.get_module_configs()
module_name = execution_command.get_module_name()
tmp_dir = Script.get_tmp_dir()
artifact_dir = tmp_dir + "/AMBARI-artifacts"

version_for_stack_feature_checks = get_stack_feature_version(config)
stack_supports_hadoop_custom_extensions = check_stack_feature(StackFeature.HADOOP_CUSTOM_EXTENSIONS, version_for_stack_feature_checks)

sudo = AMBARI_SUDO_BINARY

# Global flag enabling or disabling the sysprep feature
host_sys_prepped = execution_command.is_host_system_prepared()

# Whether to skip copying fast-hdfs-resource.jar to /var/lib/ambari-agent/lib/
# This is required if tarballs are going to be copied to HDFS, so set to False
sysprep_skip_copy_fast_jar_hdfs = host_sys_prepped and get_cluster_setting_value('sysprep_skip_copy_fast_jar_hdfs')

# Whether to skip setting up the unlimited key JCE policy
sysprep_skip_setup_jce = host_sys_prepped and get_cluster_setting_value('sysprep_skip_setup_jce')

stack_version_unformatted = execution_command.get_mpack_version()
stack_version_formatted = format_stack_version(stack_version_unformatted)
major_stack_version = get_major_version(stack_version_formatted)

dfs_type = execution_command.get_dfs_type()
hadoop_conf_dir = "/etc/hadoop/conf"
component_list = execution_command.get_local_components()

hdfs_tmp_dir = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_tmp_dir', '/tmp')

hadoop_metrics2_properties_content = module_configs.get_property_value(module_name, 'hadoop-metrics2.properties', 'content')

hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")
hadoop_bin = stack_select.get_hadoop_dir("sbin")

mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"
hadoop_home = stack_select.get_hadoop_dir("home")
create_lib_snappy_symlinks = False

current_service = module_name

#security params
security_enabled = get_cluster_setting_value('security_enabled')

ambari_server_resources_url = execution_command.get_jdk_location()
if ambari_server_resources_url and ambari_server_resources_url.endswith('/'):
  ambari_server_resources_url = ambari_server_resources_url[:-1]

# Unlimited key JCE policy params
jce_policy_zip = execution_command.get_jce_name() # None when jdk is already installed by user
unlimited_key_jce_required = execution_command.check_unlimited_key_jce_required()
jdk_name = execution_command.get_jdk_name()
java_home = execution_command.get_java_home()
java_exec = "{0}/bin/java".format(java_home) if java_home is not None else "/bin/java"

#users and groups
has_hadoop_env = bool(module_configs.get_all_properties(module_name, "hadoop-env"))
mapred_user = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_user')
hdfs_user = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_user')
yarn_user = module_configs.get_property_value(module_name, 'yarn-env', 'yarn_user')

user_group = get_cluster_setting_value('user_group')

#hosts
hostname = execution_command.get_host_name()
ambari_server_hostname = execution_command.get_ambari_server_host()
rm_host = execution_command.get_component_hosts('resourcemanager')
slave_hosts = execution_command.get_component_hosts('datanode')
oozie_servers = execution_command.get_component_hosts('oozie_server')
hcat_server_hosts = execution_command.get_component_hosts('webhcat_server')
hive_server_host =  execution_command.get_component_hosts('hive_server')
hs_host = execution_command.get_component_hosts('historyserver')
namenode_host = execution_command.get_component_hosts('namenode')
zk_hosts = execution_command.get_component_hosts('zookeeper_server')
ganglia_server_hosts = execution_command.get_component_hosts('ganglia_server')
cluster_name = execution_command.get_cluster_name()
set_instanceId = "false"
ams_collector_hosts = module_configs.get_property_value(module_name, 'cluster-env', 'metrics_collector_external_hosts')
if ams_collector_hosts:
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(execution_command.get_component_hosts('metrics_collector'))

has_namenode = not len(namenode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
has_oozie_server = not len(oozie_servers) == 0
has_hcat_server_host = not len(hcat_server_hosts) == 0
has_hive_server_host = not len(hive_server_host) == 0
has_zk_host = not len(zk_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_metric_collector = not len(ams_collector_hosts) == 0

is_namenode_master = hostname in namenode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_slave = hostname in slave_hosts

if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

metric_collector_port = module_configs.get_property_value(module_name, 'cluster-env', 'metrics_collector_external_port')
if has_metric_collector:
  if not metric_collector_port:
    metric_collector_web_address = module_configs.get_property_value(module_name, 'ams-env', 'timeline.metrics.service.webapp.address', '0.0.0.0:6188')
    if metric_collector_web_address.find(':') != -1:
      metric_collector_port = metric_collector_web_address.split(':')[1]
    else:
      metric_collector_port = '6188'
  if module_configs.get_property_value(module_name, 'ams-env', 'timeline.metrics.service.http.policy', 'HTTP_ONLY') == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= module_configs.get_property_value(module_name, 'ams-ssl-client', 'ams-ssl-client/ssl.client.truststore.location', '')
  metric_truststore_type= module_configs.get_property_value(module_name, 'ams-ssl-client', 'ams-ssl-client/ssl.client.truststore.type', '')
  metric_truststore_password= module_configs.get_property_value(module_name, 'ams-ssl-client', 'ssl.client.truststore.password', '')

  pass
metrics_report_interval = module_configs.get_property_value(module_name, 'ams-site', 'timeline.metrics.sink.report.interval', 60)
metrics_collection_period = module_configs.get_property_value(module_name, 'ams-site', 'timeline.metrics.sink.collection.period', 10)

host_in_memory_aggregation = module_configs.get_property_value(module_name, 'ams-site', 'timeline.metrics.host.inmemory.aggregation', True)
host_in_memory_aggregation_port = module_configs.get_property_value(module_name, 'ams-site', 'timeline.metrics.host.inmemory.aggregation.port', 61888)

# Cluster Zookeeper quorum
zookeeper_quorum = None
if has_zk_host:
  if not zookeeper_quorum:
    zookeeper_clientPort = '2181'
  zookeeper_quorum = (':' + zookeeper_clientPort + ',').join(execution_command.get_component_hosts('zookeeper_server'))
  # last port config
  zookeeper_quorum += ':' + zookeeper_clientPort

#hadoop params

if has_namenode or dfs_type == 'HCFS':
  hadoop_tmp_dir = format("/tmp/hadoop-{hdfs_user}")
  hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
  task_log4j_properties_location = os.path.join(hadoop_conf_dir, "task-log4j.properties")

hadoop_pid_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_pid_dir_prefix')
hdfs_log_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_log_dir_prefix')
#db params
oracle_driver_symlink_url = format("{ambari_server_resources_url}/oracle-jdbc-driver.jar")
mysql_driver_symlink_url = format("{ambari_server_resources_url}/mysql-jdbc-driver.jar")

if has_namenode:
  rca_enabled = module_configs.get_property_value(module_name, 'hadoop-env', 'rca_enabled', False)
else:
  rca_enabled = False
rca_disabled_prefix = "###"
if rca_enabled == True:
  rca_prefix = ""
else:
  rca_prefix = rca_disabled_prefix

#hadoop-env.sh

jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_heapsize')
namenode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_heapsize')
namenode_opt_newsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_newsize')
namenode_opt_maxnewsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxnewsize')
namenode_opt_permsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_permsize', '128m'), '128m')
namenode_opt_maxpermsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxpermsize', '256m'), '256m')

ttnode_heapsize = "1024m"

dtnode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'dtnode_heapsize')
mapred_pid_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_pid_dir_prefix', '/var/run/hadoop-mapreduce')
mapred_log_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_log_dir_prefix', '/var/log/hadoop-mapreduce')

#log4j.properties

yarn_log_dir_prefix = module_configs.get_property_value(module_name, 'yarn-env', 'yarn_log_dir_prefix', '/var/log/hadoop-yarn')

dfs_hosts = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.hosts')

# Hdfs log4j settings
hadoop_log_max_backup_size = module_configs.get_property_value(module_name, 'hdfs-log4j', 'hadoop_log_max_backup_size', 256)
hadoop_log_number_of_backup_files = module_configs.get_property_value(module_name, 'hdfs-log4j', 'hadoop_log_number_of_backup_files', 10)
hadoop_security_log_max_backup_size = module_configs.get_property_value(module_name, 'hdfs-log4j', 'hadoop_security_log_max_backup_size', 256)
hadoop_security_log_number_of_backup_files = module_configs.get_property_value(module_name, 'hdfs-log4j', 'hadoop_security_log_number_of_backup_files', 20)

# Yarn log4j settings
yarn_rm_summary_log_max_backup_size = module_configs.get_property_value(module_name, 'yarn-log4j', 'yarn_rm_summary_log_max_backup_size', 256)
yarn_rm_summary_log_number_of_backup_files = module_configs.get_property_value(module_name, 'yarn-log4j', 'yarn_rm_summary_log_number_of_backup_files', 20)

#log4j.properties
log4j_props = module_configs.get_property_value(module_name, 'hdfs-log4j', 'content')
if log4j_props and module_configs.get_property_value(module_name, 'yarn-log4j', 'content'):
  log4j_props += module_configs.get_property_value(module_name, 'yarn-log4j', 'content')

refresh_topology = execution_command.need_refresh_topology()

ambari_java_home = execution_command.get_ambari_java_home()
ambari_jdk_name = execution_command.get_ambari_jdk_name()
ambari_jce_name = execution_command.get_ambari_jce_name()

ambari_libs_dir = "/var/lib/ambari-agent/lib"
is_webhdfs_enabled = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.webhdfs.enabled')
default_fs = module_configs.get_property_value(module_name, 'core-site', 'fs.defaultFS')

#host info
all_hosts = execution_command.get_all_hosts()
all_racks = execution_command.get_all_racks()
all_ipv4_ips = execution_command.get_all_ipv4_ips()
slave_hosts = execution_command.get_component_hosts('datanode')

#topology files
net_topology_script_file_path = "/etc/hadoop/conf/topology_script.py"
net_topology_script_dir = os.path.dirname(net_topology_script_file_path)
net_topology_mapping_data_file_name = 'topology_mappings.data'
net_topology_mapping_data_file_path = os.path.join(net_topology_script_dir, net_topology_mapping_data_file_name)

#Added logic to create /tmp and /user directory for HCFS stack.
has_core_site = bool(module_configs.get_all_properties(module_name, "core-site"))
hdfs_user_keytab = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_user_keytab')
kinit_path_local = get_kinit_path()
stack_version_unformatted = execution_command.get_mpack_version()
stack_version_formatted = format_stack_version(stack_version_unformatted)
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hdfs_principal_name = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_principal_name')
hdfs_site = module_configs.get_all_properties(module_name, 'hdfs-site')
smoke_user = get_cluster_setting_value('smokeuser')
smoke_hdfs_user_dir = format("/user/{smoke_user}")
smoke_hdfs_user_mode = 0770


##### Namenode RPC ports - metrics config section start #####

# Figure out the rpc ports for current namenode
nn_rpc_client_port = None
nn_rpc_dn_port = None
nn_rpc_healthcheck_port = None

namenode_id = None
namenode_rpc = None

dfs_ha_enabled = False
dfs_ha_nameservices = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.internal.nameservices')
if dfs_ha_nameservices is None:
  dfs_ha_nameservices = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.nameservices')
dfs_ha_namenode_ids = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.ha.namenodes.{dfs_ha_nameservices}')

dfs_ha_namemodes_ids_list = []
other_namenode_id = None

if dfs_ha_namenode_ids:
 dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
 dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
 if dfs_ha_namenode_ids_array_len > 1:
   dfs_ha_enabled = True

if dfs_ha_enabled:
 for nn_id in dfs_ha_namemodes_ids_list:
   nn_host = module_configs.get_property_value(module_name, 'hdfs-site', format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}'))
   if hostname.lower() in nn_host.lower():
     namenode_id = nn_id
     namenode_rpc = nn_host
   pass
 pass
else:
  namenode_rpc = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.namenode.rpc-address', default_fs)

# if HDFS is not installed in the cluster, then don't try to access namenode_rpc
if has_namenode and namenode_rpc and module_configs.get_all_properties(module_name, 'core-site'):
  port_str = namenode_rpc.split(':')[-1].strip()
  try:
    nn_rpc_client_port = int(port_str)
  except ValueError:
    nn_rpc_client_port = None

if dfs_ha_enabled:
 dfs_service_rpc_address = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.namenode.servicerpc-address.{dfs_ha_nameservices}.{namenode_id}')
 dfs_lifeline_rpc_address = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.namenode.lifeline.rpc-address.{dfs_ha_nameservices}.{namenode_id}')
else:
 dfs_service_rpc_address = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.namenode.servicerpc-address')
 dfs_lifeline_rpc_address = module_configs.get_property_value(module_name, 'hdfs-site', 'dfs.namenode.lifeline.rpc-address')

if dfs_service_rpc_address:
 nn_rpc_dn_port = dfs_service_rpc_address.split(':')[1].strip()

if dfs_lifeline_rpc_address:
 nn_rpc_healthcheck_port = dfs_lifeline_rpc_address.split(':')[1].strip()

is_nn_client_port_configured = False if nn_rpc_client_port is None else True
is_nn_dn_port_configured = False if nn_rpc_dn_port is None else True
is_nn_healthcheck_port_configured = False if nn_rpc_healthcheck_port is None else True

##### end #####

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
)
