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

Ambari Agent

"""
import os

from resource_management.core import sudo
from resource_management.core.logger import Logger
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import component_version
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.version import format_stack_version, get_major_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries import functions
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions.get_architecture import get_architecture
from resource_management.libraries.functions.setup_ranger_plugin_xml import get_audit_configs, generate_ranger_service_config

import status_params
from functions import calc_heap_memory, ensure_unit_for_memory


service_name = 'yarn'
# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
MAPR_SERVER_ROLE_DIRECTORY_MAP = {
  'HISTORYSERVER' : 'hadoop-mapreduce-historyserver',
  'MAPREDUCE2_CLIENT' : 'hadoop-mapreduce-client',
}

YARN_SERVER_ROLE_DIRECTORY_MAP = {
  'APP_TIMELINE_SERVER' : 'hadoop-yarn-timelineserver',
  'NODEMANAGER' : 'hadoop-yarn-nodemanager',
  'RESOURCEMANAGER' : 'hadoop-yarn-resourcemanager',
  'YARN_CLIENT' : 'hadoop-yarn-client',
  'TIMELINE_READER' : 'hadoop-yarn-timelinereader',
  'YARN_REGISTRY_DNS' : 'hadoop-yarn-registrydns'
}

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

architecture = get_architecture()

stack_name = status_params.stack_name
stack_root = Script.get_stack_root()
tarball_map = default("/configurations/cluster-env/tarball_map", None)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted_major = format_stack_version(stack_version_unformatted)
stack_version_formatted = functions.get_stack_version('hadoop-yarn-resourcemanager')
major_stack_version = get_major_version(stack_version_formatted_major)

stack_supports_ru = check_stack_feature(StackFeature.ROLLING_UPGRADE, version_for_stack_feature_checks)
stack_supports_timeline_state_store = check_stack_feature(StackFeature.TIMELINE_STATE_STORE, version_for_stack_feature_checks)

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade.
# It cannot be used during the initial Cluser Install because the version is not yet known.
version = default("/commandParams/version", None)

def get_spark_version(service_name, component_name, yarn_version):
  """
  Attempts to calculate the correct version placeholder value for spark or spark2 based on
  what is installed in the cluster. If Spark is not installed, then this value will need to be
  that of YARN so it can still find the correct spark class.

  On cluster installs, we have not yet calcualted any versions and all known values could be None.
  This doesn't affect daemons, but it does affect client-only hosts where they will never receive
  a start command after install. Therefore, this function will attempt to use stack-select as a
  last resort to get a value value.

  ATS needs this since it relies on packages installed by Spark. Some classes, like the shuffle
  classes, are not provided by spark, but by a dependent RPM to YARN, so they do not use this
  value.
  :param service_name:  the service name (SPARK, SPARK2, etc)
  :param component_name:  the component name (SPARK_CLIENT, etc)
  :param yarn_version:  the default version of Yarn to use if no spark is installed
  :return:  a value for the version placeholder in spark classpath properties
  """
  # start off seeing if we need to populate a default value for YARN
  if yarn_version is None:
    yarn_version = component_version.get_component_repository_version(service_name = "YARN",
      component_name = "YARN_CLIENT")

  # now try to get the version of spark/spark2, defaulting to the version if YARN
  spark_classpath_version = component_version.get_component_repository_version(service_name = service_name,
    component_name = component_name, default_value = yarn_version)

  # even with the default of using YARN's version, on an install this might be None since we haven't
  # calculated the version of YARN yet - use stack_select as a last ditch effort
  if spark_classpath_version is None:
    try:
      spark_classpath_version = stack_select.get_role_component_current_stack_version()
    except:
      Logger.exception("Unable to query for the correct spark version to use when building classpaths")

  return spark_classpath_version


# these are used to render the classpath for picking up Spark classes
# in the event that spark is not installed, then we must default to the vesrion of YARN installed
# since it will still load classes from its own spark version

# No Spark services in current Mpack;
# TODO: Add Spark into stack;
#spark_version = get_spark_version("SPARK", "SPARK_CLIENT", version)
#spark2_version = get_spark_version("SPARK2", "SPARK2_CLIENT", version)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

hostname = config['agentLevelParams']['hostname']

# hadoop default parameters
hadoop_home = status_params.hadoop_home
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_hdfs_home = stack_select.get_hadoop_dir("hdfs_home")
hadoop_mapred_home = stack_select.get_hadoop_dir("mapred_home")
hadoop_yarn_home = stack_select.get_hadoop_dir("yarn_home")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
mapred_bin = format("{hadoop_mapred_home}/sbin")
yarn_bin = format("{hadoop_yarn_home}/sbin")
yarn_container_bin = format("{hadoop_yarn_home}/bin")
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")

# MapR directory root
mapred_role_root = "hadoop-mapreduce-client"
command_role = default("/role", "")
if command_role in MAPR_SERVER_ROLE_DIRECTORY_MAP:
  mapred_role_root = MAPR_SERVER_ROLE_DIRECTORY_MAP[command_role]


if stack_supports_timeline_state_store:
  # Timeline Service property that was added timeline_state_store stack feature
  ats_leveldb_state_store_dir = default('/configurations/yarn-site/yarn.timeline-service.leveldb-state-store.path', '/hadoop/yarn/timeline')

# ats 1.5 properties
entity_groupfs_active_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.active-dir']
entity_groupfs_active_dir_mode = 0o1777
entity_groupfs_store_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.done-dir']
entity_groupfs_store_dir_mode = 0o700

hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")

limits_conf_dir = "/etc/security/limits.d"
yarn_user_nofile_limit = default("/configurations/yarn-env/yarn_user_nofile_limit", "32768")
yarn_user_nproc_limit = default("/configurations/yarn-env/yarn_user_nproc_limit", "65536")

mapred_user_nofile_limit = default("/configurations/mapred-env/mapred_user_nofile_limit", "32768")
mapred_user_nproc_limit = default("/configurations/mapred-env/mapred_user_nproc_limit", "65536")

execute_path = os.environ['PATH'] + os.pathsep + hadoop_bin_dir + os.pathsep + yarn_container_bin

ulimit_cmd = "ulimit -c unlimited;"

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_tmp_dir = default("/configurations/hadoop-env/hdfs_tmp_dir", "/tmp")

smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_hdfs_user_mode = 0o770
security_enabled = config['configurations']['cluster-env']['security_enabled']
nm_security_marker_dir = "/var/lib/hadoop-yarn"
nm_security_marker = format('{nm_security_marker_dir}/nm_security_enabled')
current_nm_security_state = os.path.isfile(nm_security_marker)
toggle_nm_security = (current_nm_security_state and not security_enabled) or (not current_nm_security_state and security_enabled)
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

mapred2_service_check_test_file = format('{tmp_dir}/mapred2-service-check')

yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
yarn_nodemanager_container_executor_class =  config['configurations']['yarn-site']['yarn.nodemanager.container-executor.class']
is_linux_container_executor = (yarn_nodemanager_container_executor_class == 'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor')
container_executor_mode = 0o6050 if is_linux_container_executor else 0o2050
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
yarn_http_policy = config['configurations']['yarn-site']['yarn.http.policy']
yarn_https_on = (yarn_http_policy.upper() == 'HTTPS_ONLY')
rm_hosts = config['clusterHostInfo']['resourcemanager_hosts']
rm_host = rm_hosts[0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = default('/configurations/yarn-site/yarn.resourcemanager.webapp.https.address', ":8090").split(':')[-1]
# TODO UPGRADE default, update site during upgrade
rm_nodes_exclude_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")
rm_nodes_exclude_dir = os.path.dirname(rm_nodes_exclude_path)

java64_home = config['ambariLevelParams']['java_home']
java_exec = format("{java64_home}/bin/java")
hadoop_ssl_enabled = default("/configurations/core-site/hadoop.ssl.enabled", False)
java_version = expect("/ambariLevelParams/java_version", int)

yarn_heapsize = config['configurations']['yarn-env']['yarn_heapsize']
resourcemanager_heapsize = config['configurations']['yarn-env']['resourcemanager_heapsize']
nodemanager_heapsize = config['configurations']['yarn-env']['nodemanager_heapsize']
apptimelineserver_heapsize = default("/configurations/yarn-env/apptimelineserver_heapsize", 1024)
ats_leveldb_dir = config['configurations']['yarn-site']['yarn.timeline-service.leveldb-timeline-store.path']
ats_leveldb_lock_file = os.path.join(ats_leveldb_dir, "leveldb-timeline-store.ldb", "LOCK")
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']
yarn_pid_dir_prefix = status_params.yarn_pid_dir_prefix
mapred_pid_dir_prefix = status_params.mapred_pid_dir_prefix
mapred_log_dir_prefix = config['configurations']['mapred-env']['mapred_log_dir_prefix']
mapred_env_sh_template = config['configurations']['mapred-env']['content']
yarn_env_sh_template = config['configurations']['yarn-env']['content']
container_executor_cfg_template = config['configurations']['container-executor']['content']
yarn_nodemanager_recovery_dir = default('/configurations/yarn-site/yarn.nodemanager.recovery.dir', None)
service_check_queue_name = default('/configurations/yarn-env/service_check.queue.name', 'default')

if len(rm_hosts) > 1:
  additional_rm_host = rm_hosts[1]
  rm_webui_address = format("{rm_host}:{rm_port},{additional_rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port},{additional_rm_host}:{rm_https_port}")
else:
  rm_webui_address = format("{rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port}")

if security_enabled:
  tc_mode = 0o644
  tc_owner = "root"
else:
  tc_mode = None
  tc_owner = hdfs_user

nm_webui_address = config['configurations']['yarn-site']['yarn.nodemanager.webapp.address']
hs_webui_address = config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address']
nm_address = config['configurations']['yarn-site']['yarn.nodemanager.address']  # still contains 0.0.0.0
if hostname and nm_address and nm_address.startswith("0.0.0.0:"):
  nm_address = nm_address.replace("0.0.0.0", hostname)

# Initialize lists of work directories.
nm_local_dirs = default("/configurations/yarn-site/yarn.nodemanager.local-dirs", "")
nm_log_dirs = default("/configurations/yarn-site/yarn.nodemanager.log-dirs", "")

nm_local_dirs_list = nm_local_dirs.split(',')
nm_log_dirs_list = nm_log_dirs.split(',')

nm_log_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_log_dir_mount.hist"
nm_local_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_local_dir_mount.hist"

distrAppJarName = "hadoop-yarn-applications-distributedshell-3.*.jar"
hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-3.*.jar"

entity_file_history_directory = "/tmp/entity-file-history/active"

yarn_pid_dir = status_params.yarn_pid_dir
mapred_pid_dir = status_params.mapred_pid_dir

mapred_log_dir = format("{mapred_log_dir_prefix}/{mapred_user}")
yarn_log_dir = format("{yarn_log_dir_prefix}/{yarn_user}")
mapred_job_summary_log = format("{mapred_log_dir_prefix}/{mapred_user}/hadoop-mapreduce.jobsummary.log")
yarn_job_summary_log = format("{yarn_log_dir_prefix}/{yarn_user}/hadoop-mapreduce.jobsummary.log")

user_group = config['configurations']['cluster-env']['user_group']

#exclude file
if 'all_decommissioned_hosts' in config['commandParams']:
  exclude_hosts = config['commandParams']['all_decommissioned_hosts'].split(",")
else:
  exclude_hosts = []
exclude_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")
rm_nodes_exclude_dir = os.path.dirname(exclude_file_path)

nm_hosts = default("/clusterHostInfo/nodemanager_hosts", [])
#incude file
include_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.include-path", None)
include_hosts = None
manage_include_files = default("/configurations/yarn-site/manage.include.files", False)
if include_file_path and manage_include_files:
  rm_nodes_include_dir = os.path.dirname(include_file_path)
  include_hosts = list(set(nm_hosts) - set(exclude_hosts))

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

atsv2_host = set(default("/clusterHostInfo/timeline_reader_hosts", []))
has_atsv2 = not len(atsv2_host) == 0

registry_dns_host = set(default("/clusterHostInfo/yarn_registry_dns_hosts", []))
has_registry_dns = not len(registry_dns_host) == 0

# don't using len(nm_hosts) here, because check can take too much time on large clusters
number_of_nm = 1

hs_host = default("/clusterHostInfo/historyserver_hosts", [])
has_hs = not len(hs_host) == 0

# default kinit commands
rm_kinit_cmd = ""
yarn_timelineservice_kinit_cmd = ""
nodemanager_kinit_cmd = ""

rm_zk_address = config['configurations']['yarn-site']['yarn.resourcemanager.zk-address']
rm_zk_znode = config['configurations']['yarn-site']['yarn.resourcemanager.zk-state-store.parent-path']
rm_zk_store_class = config['configurations']['yarn-site']['yarn.resourcemanager.store.class']
stack_supports_zk_security = check_stack_feature(StackFeature.SECURE_ZOOKEEPER, version_for_stack_feature_checks)
rm_zk_failover_znode = default('/configurations/yarn-site/yarn.resourcemanager.ha.automatic-failover.zk-base-path', '/yarn-leader-election')
hadoop_registry_zk_root = default('/configurations/yarn-site/hadoop.registry.zk.root', '/registry')

if security_enabled:
  rm_principal_name = config['configurations']['yarn-site']['yarn.resourcemanager.principal']
  rm_principal_name = rm_principal_name.replace('_HOST',hostname.lower())
  rm_keytab = config['configurations']['yarn-site']['yarn.resourcemanager.keytab']
  rm_kinit_cmd = format("{kinit_path_local} -kt {rm_keytab} {rm_principal_name};")
  yarn_jaas_file = os.path.join(hadoop_conf_dir, 'yarn_jaas.conf')
  if stack_supports_zk_security:
    zk_principal_name = default("/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper/_HOST@EXAMPLE.COM")
    zk_principal_user = zk_principal_name.split('/')[0]
    rm_security_opts = format('-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Djava.security.auth.login.config={yarn_jaas_file} -Dzookeeper.sasl.clientconfig=Client')

  # YARN timeline security options
  if has_ats or has_atsv2:
    yarn_timelineservice_principal_name = config['configurations']['yarn-site']['yarn.timeline-service.principal']
    yarn_timelineservice_principal_name = yarn_timelineservice_principal_name.replace('_HOST', hostname.lower())
    yarn_timelineservice_keytab = config['configurations']['yarn-site']['yarn.timeline-service.keytab']
    yarn_timelineservice_kinit_cmd = format("{kinit_path_local} -kt {yarn_timelineservice_keytab} {yarn_timelineservice_principal_name};")
    yarn_ats_jaas_file = os.path.join(hadoop_conf_dir, 'yarn_ats_jaas.conf')

  if has_registry_dns:
    yarn_registry_dns_principal_name = config['configurations']['yarn-env']['yarn.registry-dns.principal']
    yarn_registry_dns_principal_name = yarn_registry_dns_principal_name.replace('_HOST', hostname.lower())
    yarn_registry_dns_keytab = config['configurations']['yarn-env']['yarn.registry-dns.keytab']
    yarn_registry_dns_jaas_file = os.path.join(hadoop_conf_dir, 'yarn_registry_dns_jaas.conf')

  if 'yarn.nodemanager.principal' in config['configurations']['yarn-site']:
    nodemanager_principal_name = default('/configurations/yarn-site/yarn.nodemanager.principal', None)
    if nodemanager_principal_name:
      nodemanager_principal_name = nodemanager_principal_name.replace('_HOST', hostname.lower())

    nodemanager_keytab = config['configurations']['yarn-site']['yarn.nodemanager.keytab']
    nodemanager_kinit_cmd = format("{kinit_path_local} -kt {nodemanager_keytab} {nodemanager_principal_name};")
    yarn_nm_jaas_file = os.path.join(hadoop_conf_dir, 'yarn_nm_jaas.conf')

  if has_hs:
    mapred_jhs_principal_name = config['configurations']['mapred-site']['mapreduce.jobhistory.principal']
    mapred_jhs_principal_name = mapred_jhs_principal_name.replace('_HOST', hostname.lower())
    mapred_jhs_keytab = config['configurations']['mapred-site']['mapreduce.jobhistory.keytab']
    mapred_jaas_file = os.path.join(hadoop_conf_dir, 'mapred_jaas.conf')

yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")
jhs_leveldb_state_store_dir = default('/configurations/mapred-site/mapreduce.jobhistory.recovery.store.leveldb.path', "/hadoop/mapreduce/jhs")

# Tez-related properties
tez_user = config['configurations']['tez-env']['tez_user']

# Tez jars
tez_local_api_jars = '/usr/lib/tez/tez*.jar'
tez_local_lib_jars = '/usr/lib/tez/lib/*.jar'
app_dir_files = {tez_local_api_jars:None}

# Tez libraries
tez_lib_uris = default("/configurations/tez-site/tez.lib.uris", None)

#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
is_webhdfs_enabled = hdfs_site['dfs.webhdfs.enabled']

# Path to file that contains list of HDFS resources to be skipped during processing
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

dfs_type = default("/clusterLevelParams/dfs_type", "")


import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = hdfs_resource_ignore_file,
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
update_files_only = default("/commandParams/update_files_only",False)

mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
min_user_id = config['configurations']['yarn-env']['min_user_id']

# Node labels
node_labels_dir = default("/configurations/yarn-site/yarn.node-labels.fs-store.root-dir", None)
node_label_enable = config['configurations']['yarn-site']['yarn.node-labels.enabled']

cgroups_dir = "/cgroups_test/cpu"

# hostname of the active HDFS HA Namenode (only used when HA is enabled)
dfs_ha_namenode_active = default("/configurations/cluster-env/dfs_ha_initial_namenode_active", None)
if dfs_ha_namenode_active is not None:
  namenode_hostname = dfs_ha_namenode_active
else:
  namenode_hostname = config['clusterHostInfo']['namenode_hosts'][0]

scheme = 'http' if not yarn_https_on else 'https'
yarn_rm_address = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'] if not yarn_https_on else config['configurations']['yarn-site']['yarn.resourcemanager.webapp.https.address']
rm_active_port = rm_https_port if yarn_https_on else rm_port

rm_ha_enabled = False
rm_ha_id = None
rm_ha_ids_list = []
rm_webapp_addresses_list = [yarn_rm_address]
rm_ha_ids = default("/configurations/yarn-site/yarn.resourcemanager.ha.rm-ids", None)

if rm_ha_ids:
  rm_ha_ids_list = rm_ha_ids.split(",")
  if len(rm_ha_ids_list) > 1:
    rm_ha_enabled = True

if rm_ha_enabled:
  rm_webapp_addresses_list = []
  for rm_id in rm_ha_ids_list:
    rm_webapp_address_property = format('yarn.resourcemanager.webapp.address.{rm_id}') if not yarn_https_on else format('yarn.resourcemanager.webapp.https.address.{rm_id}')
    rm_webapp_address = config['configurations']['yarn-site'][rm_webapp_address_property]
    rm_webapp_addresses_list.append(rm_webapp_address)
    rm_host_name = config['configurations']['yarn-site'][format('yarn.resourcemanager.hostname.{rm_id}')]
    if rm_host_name == hostname.lower():
      rm_ha_id = rm_id

# for curl command in ranger plugin to get db connector
jdk_location = config['ambariLevelParams']['jdk_location']

# ranger yarn plugin section start
ranger_plugin_home = format("{hadoop_home}/../ranger-{service_name}-plugin")

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks)

# ambari-server hostname
ambari_server_hostname = config['ambariLevelParams']['ambari_server_host']

# ranger yarn plugin enabled property
enable_ranger_yarn = default("/configurations/ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled", "No")
enable_ranger_yarn = True if enable_ranger_yarn.lower() == 'yes' else False

# ranger yarn-plugin supported flag, instead of using is_supported_yarn_ranger/yarn-env, using stack feature
is_supported_yarn_ranger = check_stack_feature(StackFeature.YARN_RANGER_PLUGIN_SUPPORT, version_for_stack_feature_checks)

# get ranger yarn properties if enable_ranger_yarn is True
if enable_ranger_yarn and is_supported_yarn_ranger:
  # get ranger policy url
  policymgr_mgr_url = config['configurations']['ranger-yarn-security']['ranger.plugin.yarn.policy.rest.url']

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  # ranger audit db user
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')

  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db and has_ranger_admin:
    xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']

  # ranger yarn service/repository name
  repo_name = str(config['clusterName']) + '_yarn'
  repo_name_value = config['configurations']['ranger-yarn-security']['ranger.plugin.yarn.service.name']
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  # ranger-env config
  ranger_env = config['configurations']['ranger-env']

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_yarn:
    external_admin_username = default('/configurations/ranger-yarn-plugin-properties/external_admin_username', 'admin')
    external_admin_password = default('/configurations/ranger-yarn-plugin-properties/external_admin_password', 'admin')
    external_ranger_admin_username = default('/configurations/ranger-yarn-plugin-properties/external_ranger_admin_username', 'amb_ranger_admin')
    external_ranger_admin_password = default('/configurations/ranger-yarn-plugin-properties/external_ranger_admin_password', 'amb_ranger_admin')
    ranger_env = {}
    ranger_env['admin_username'] = external_admin_username
    ranger_env['admin_password'] = external_admin_password
    ranger_env['ranger_admin_username'] = external_ranger_admin_username
    ranger_env['ranger_admin_password'] = external_ranger_admin_password

  ranger_plugin_properties = config['configurations']['ranger-yarn-plugin-properties']
  policy_user = config['configurations']['ranger-yarn-plugin-properties']['policy_user']
  yarn_rest_url = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address']

  ranger_plugin_config = {
    'username' : config['configurations']['ranger-yarn-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
    'password' : str(config['configurations']['ranger-yarn-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
    'yarn.url' : format('{scheme}://{yarn_rest_url}'),
    'commonNameForCertificate' : config['configurations']['ranger-yarn-plugin-properties']['common.name.for.certificate'],
    'hadoop.security.authentication': 'kerberos' if security_enabled else 'simple'
  }

  if security_enabled:
    ranger_plugin_config['policy.download.auth.users'] = yarn_user
    ranger_plugin_config['tag.download.auth.users'] = yarn_user

  ranger_plugin_config['setup.additional.default.policies'] = "true"
  ranger_plugin_config['default-policy.1.name'] = "Service Check User Policy for Yarn"
  ranger_plugin_config['default-policy.1.resource.queue'] = service_check_queue_name
  ranger_plugin_config['default-policy.1.policyItem.1.users'] = policy_user
  ranger_plugin_config['default-policy.1.policyItem.1.accessTypes'] = "submit-app"

  custom_ranger_service_config = generate_ranger_service_config(ranger_plugin_properties)
  if len(custom_ranger_service_config) > 0:
    ranger_plugin_config.update(custom_ranger_service_config)

  yarn_ranger_plugin_repo = {
    'isEnabled': 'true',
    'configs': ranger_plugin_config,
    'description': 'yarn repo',
    'name': repo_name,
    'repositoryType': 'yarn',
    'type': 'yarn',
    'assetType': '1'
  }

  custom_ranger_service_config = generate_ranger_service_config(ranger_plugin_properties)
  if len(custom_ranger_service_config) > 0:
    ranger_plugin_config.update(custom_ranger_service_config)

  if stack_supports_ranger_kerberos:
    ranger_plugin_config['ambari.service.check.user'] = policy_user
    ranger_plugin_config['hadoop.security.authentication'] = 'kerberos' if security_enabled else 'simple'

  if stack_supports_ranger_kerberos and security_enabled:
    ranger_plugin_config['policy.download.auth.users'] = yarn_user
    ranger_plugin_config['tag.download.auth.users'] = yarn_user

  downloaded_custom_connector = None
  previous_jdbc_jar_name = None
  driver_curl_source = None
  driver_curl_target = None
  previous_jdbc_jar = None

  if has_ranger_admin and stack_supports_ranger_audit_db:
    xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
    jdbc_jar_name, previous_jdbc_jar_name, audit_jdbc_url, jdbc_driver = get_audit_configs(config)

    downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_source = format("{jdk_location}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_target = format("{hadoop_yarn_home}/lib/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    previous_jdbc_jar = format("{hadoop_yarn_home}/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-yarn-audit']['xasecure.audit.destination.db']

  xa_audit_hdfs_is_enabled = config['configurations']['ranger-yarn-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else False
  ssl_keystore_password = config['configurations']['ranger-yarn-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password'] if xml_configurations_supported else None
  ssl_truststore_password = config['configurations']['ranger-yarn-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password'] if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

  # for SQLA explicitly disable audit to DB for Ranger
  if has_ranger_admin and stack_supports_ranger_audit_db and xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

# need this to capture cluster name from where ranger yarn plugin is enabled
cluster_name = config['clusterName']

# ranger yarn plugin end section

# container-executor properties
min_user_id = config['configurations']['container-executor']['min_user_id']
docker_module_enabled = str(config['configurations']['container-executor']['docker_module_enabled']).lower()
docker_binary = config['configurations']['container-executor']['docker_binary']
docker_allowed_capabilities = config['configurations']['yarn-site']['yarn.nodemanager.runtime.linux.docker.capabilities']
if docker_allowed_capabilities:
  docker_allowed_capabilities = ','.join(x.strip() for x in docker_allowed_capabilities.split(','))
else:
  docker_allowed_capabilities = ""
docker_allowed_devices = config['configurations']['container-executor']['docker_allowed_devices']
docker_allowed_networks = config['configurations']['yarn-site']['yarn.nodemanager.runtime.linux.docker.allowed-container-networks']
if docker_allowed_networks:
  docker_allowed_networks = ','.join(x.strip() for x in docker_allowed_networks.split(','))
else:
  docker_allowed_networks = ""
docker_allowed_ro_mounts = config['configurations']['container-executor']['docker_allowed_ro-mounts']
docker_allowed_rw_mounts = config['configurations']['container-executor']['docker_allowed_rw-mounts']
docker_privileged_containers_enabled = str(config['configurations']['container-executor']['docker_privileged-containers_enabled']).lower()
docker_trusted_registries = config['configurations']['container-executor']['docker_trusted_registries']
docker_allowed_volume_drivers = config['configurations']['container-executor']['docker_allowed_volume-drivers']

# ATSv2 integration properties started.
yarn_timelinereader_pid_file = status_params.yarn_timelinereader_pid_file

yarn_atsv2_hbase_versioned_home = format("{stack_root}/{version}/usr/lib/hbase")
yarn_hbase_bin = format("{yarn_atsv2_hbase_versioned_home}/bin")
yarn_hbase_hdfs_root_dir = config['configurations']['yarn-hbase-site']['hbase.rootdir']
cluster_zookeeper_quorum_hosts = ",".join(config['clusterHostInfo']['zookeeper_server_hosts'])
if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
  cluster_zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
else:
  cluster_zookeeper_clientPort = '2181'

zookeeper_quorum_hosts = cluster_zookeeper_quorum_hosts
zookeeper_clientPort = cluster_zookeeper_clientPort
yarn_hbase_user = status_params.yarn_hbase_user
hbase_user = config['configurations']['hbase-env']['hbase_user']
yarn_hbase_user_home = format("/user/{yarn_hbase_user}")
yarn_hbase_user_version_home = format("{yarn_hbase_user_home}/{version}")
yarn_hbase_app_hdfs_path = format("/bigtop/apps/{version}/hbase")
yarn_service_app_hdfs_path = format("/bigtop/apps/{version}/yarn")
if rm_ha_id is not None:
  yarn_hbase_app_hdfs_path = format("{yarn_hbase_app_hdfs_path}/{rm_ha_id}")
  yarn_service_app_hdfs_path = format("{yarn_service_app_hdfs_path}/{rm_ha_id}")
yarn_service_dep_source_path = format("{stack_root}/{version}/usr/lib/hadoop-yarn/lib/service-dep.tar.gz")
yarn_hbase_user_version_path = format("{yarn_hbase_user}/{version}")
yarn_hbase_user_tmp = format("{tmp_dir}/{yarn_hbase_user_version_path}")
yarn_hbase_log_dir = os.path.join(yarn_log_dir_prefix, "embedded-yarn-ats-hbase")
yarn_hbase_pid_dir_prefix = status_params.yarn_hbase_pid_dir_prefix
yarn_hbase_pid_dir = status_params.yarn_hbase_pid_dir
yarn_hbase_conf_dir = os.path.join(hadoop_conf_dir, "embedded-yarn-ats-hbase")
yarn_hbase_env_sh_template = config['configurations']['yarn-hbase-env']['content']
yarn_hbase_java_io_tmpdir = default("/configurations/yarn-hbase-env/hbase_java_io_tmpdir", "/tmp")
yarn_hbase_tmp_dir = config['configurations']['yarn-hbase-site']['hbase.tmp.dir']
yarn_hbase_local_dir = config['configurations']['yarn-hbase-site']['hbase.local.dir']
yarn_hbase_master_info_port = config['configurations']['yarn-hbase-site']['hbase.master.info.port']
yarn_hbase_regionserver_info_port = config['configurations']['yarn-hbase-site']['hbase.regionserver.info.port']

if (('yarn-hbase-log4j' in config['configurations']) and ('content' in config['configurations']['yarn-hbase-log4j'])):
  yarn_hbase_log4j_props = config['configurations']['yarn-hbase-log4j']['content']
else:
  yarn_hbase_log4j_props = None

timeline_collector = ""
yarn_timeline_service_version = config['configurations']['yarn-site']['yarn.timeline-service.version']
yarn_timeline_service_versions = config['configurations']['yarn-site']['yarn.timeline-service.versions']
yarn_timeline_service_enabled = config['configurations']['yarn-site']['yarn.timeline-service.enabled']

if yarn_timeline_service_enabled:
  if is_empty(yarn_timeline_service_versions):
    if yarn_timeline_service_version == '2.0' or yarn_timeline_service_version == '2':
      timeline_collector = "timeline_collector"
  else:
    ts_version_list = yarn_timeline_service_versions.split(',')
    for ts_version in ts_version_list:
      if '2.0' in ts_version or ts_version == '2':
        timeline_collector = "timeline_collector"
        break

coprocessor_jar_name = "hadoop-yarn-server-timelineservice-hbase-coprocessor.jar"
yarn_timeline_jar_location = format("file://{stack_root}/{version}/usr/lib/hadoop-yarn/timelineservice/{coprocessor_jar_name}")
yarn_user_hbase_permissions = "RWXCA"

yarn_hbase_kinit_cmd = ""
if security_enabled and has_atsv2:
  yarn_hbase_jaas_file = os.path.join(yarn_hbase_conf_dir, 'yarn_hbase_jaas.conf')
  yarn_hbase_master_jaas_file = os.path.join(yarn_hbase_conf_dir, 'yarn_hbase_master_jaas.conf')
  yarn_hbase_regionserver_jaas_file = os.path.join(yarn_hbase_conf_dir, 'yarn_hbase_regionserver_jaas.conf')

  yarn_hbase_master_principal_name = config['configurations']['yarn-hbase-site']['hbase.master.kerberos.principal']
  yarn_hbase_master_principal_name = yarn_hbase_master_principal_name.replace('_HOST', hostname.lower())
  yarn_hbase_master_keytab = config['configurations']['yarn-hbase-site']['hbase.master.keytab.file']

  yarn_hbase_regionserver_principal_name = config['configurations']['yarn-hbase-site']['hbase.regionserver.kerberos.principal']
  yarn_hbase_regionserver_principal_name = yarn_hbase_regionserver_principal_name.replace('_HOST', hostname.lower())
  yarn_hbase_regionserver_keytab = config['configurations']['yarn-hbase-site']['hbase.regionserver.keytab.file']

  # User master principal name as AM principal in system service. Don't replace _HOST.
  yarn_ats_hbase_principal_name = config['configurations']['yarn-hbase-site']['hbase.master.kerberos.principal']
  yarn_ats_hbase_keytab = config['configurations']['yarn-hbase-site']['hbase.master.keytab.file']
  yarn_ats_principal_name = config['configurations']['yarn-env']['yarn_ats_principal_name']
  yarn_ats_user_keytab = config['configurations']['yarn-env']['yarn_ats_user_keytab']
  yarn_hbase_kinit_cmd = format("{kinit_path_local} -kt {yarn_ats_user_keytab} {yarn_ats_principal_name};")


hbase_within_cluster = config['configurations']['yarn-hbase-env']['hbase_within_cluster']
is_hbase_installed = False
master_configs = config['clusterHostInfo']

if hbase_within_cluster:
  if 'hbase_master_hosts' in master_configs and 'hbase-site' in config['configurations']:
    is_hbase_installed = True
    zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
  else:
    zookeeper_znode_parent = "/hbase-unsecure"
  hbase_site_conf = config['configurations']['hbase-site']
  hbase_site_attributes = config['configurationAttributes']['hbase-site']
  yarn_hbase_conf_dir = "/etc/hbase/conf"
else:
  zookeeper_znode_parent = "/atsv2-hbase-unsecure"
  hbase_site_conf  = config['configurations']['yarn-hbase-site']
  hbase_site_attributes = config['configurationAttributes']['yarn-hbase-site']

yarn_hbase_grant_premissions_file = format("{yarn_hbase_conf_dir}/hbase_grant_permissions.sh")
yarn_hbase_package_preparation_file = format("{tmp_dir}/hbase_package_preparation.sh")
is_hbase_system_service_launch = config['configurations']['yarn-hbase-env']['is_hbase_system_service_launch']
use_external_hbase = config['configurations']['yarn-hbase-env']['use_external_hbase']

hbase_cmd = format("{yarn_hbase_bin}/hbase --config {yarn_hbase_conf_dir}")
class_name = format("org.apache.hadoop.yarn.server.timelineservice.storage.TimelineSchemaCreator -Dhbase.client.retries.number=35 -create -s")
yarn_hbase_table_create_cmd = format("export HBASE_CLASSPATH_PREFIX={stack_root}/{version}/usr/lib/hadoop-yarn/timelineservice/*;{yarn_hbase_kinit_cmd} {hbase_cmd} {class_name}")
yarn_hbase_table_grant_premission_cmd = format("{yarn_hbase_kinit_cmd} {hbase_cmd} shell {yarn_hbase_grant_premissions_file}")

# System service configuration as part of ATSv2.
yarn_system_service_dir = config['configurations']['yarn-site']['yarn.service.system-service.dir']
yarn_system_service_launch_mode = config['configurations']['yarn-hbase-env']['yarn_hbase_system_service_launch_mode']
yarn_hbase_service_queue_name = config['configurations']['yarn-hbase-env']['yarn_hbase_system_service_queue_name']

yarn_hbase_master_cpu = config['configurations']['yarn-hbase-env']['yarn_hbase_master_cpu']
yarn_hbase_master_memory = expect("/configurations/yarn-hbase-env/yarn_hbase_master_memory", int)
yarn_hbase_master_containers = config['configurations']['yarn-hbase-env']['yarn_hbase_master_containers']
yarn_hbase_regionserver_cpu = config['configurations']['yarn-hbase-env']['yarn_hbase_regionserver_cpu']
yarn_hbase_regionserver_memory = expect("/configurations/yarn-hbase-env/yarn_hbase_regionserver_memory", int)
yarn_hbase_regionserver_containers = config['configurations']['yarn-hbase-env']['yarn_hbase_regionserver_containers']
yarn_hbase_client_cpu = config['configurations']['yarn-hbase-env']['yarn_hbase_client_cpu']
yarn_hbase_client_memory = expect("/configurations/yarn-hbase-env/yarn_hbase_client_memory", int)
yarn_hbase_client_containers = config['configurations']['yarn-hbase-env']['yarn_hbase_client_containers']

yarn_hbase_heap_memory_factor = expect("/configurations/yarn-hbase-env/yarn_hbase_heap_memory_factor", float)
yarn_hbase_master_heapsize = ensure_unit_for_memory(calc_heap_memory(yarn_hbase_master_memory, yarn_hbase_heap_memory_factor))
yarn_hbase_regionserver_heapsize = ensure_unit_for_memory(calc_heap_memory(yarn_hbase_regionserver_memory, yarn_hbase_heap_memory_factor))

yarn_hbase_log_level = str(config['configurations']['yarn-hbase-env']['yarn_hbase_log_level']).upper()
# ATSv2 integration properties ended

gpu_module_enabled = str(config['configurations']['container-executor']['gpu_module_enabled']).lower()
cgroup_root = config['configurations']['container-executor']['cgroup_root']
yarn_hierarchy = config['configurations']['container-executor']['yarn_hierarchy']

# registry dns service
registry_dns_needs_privileged_access = status_params.registry_dns_needs_privileged_access

mount_table_content = None
if 'viewfs-mount-table' in config['configurations']:
  xml_inclusion_file_name = 'viewfs-mount-table.xml'
  mount_table = config['configurations']['viewfs-mount-table']

  if 'content' in mount_table and mount_table['content'].strip():
    mount_table_content = mount_table['content']

hbase_log_maxfilesize = default('configurations/yarn-hbase-log4j/hbase_log_maxfilesize',256)
hbase_log_maxbackupindex = default('configurations/yarn-hbase-log4j/hbase_log_maxbackupindex',20)
hbase_security_log_maxfilesize = default('configurations/yarn-hbase-log4j/hbase_security_log_maxfilesize',256)
hbase_security_log_maxbackupindex = default('configurations/yarn-hbase-log4j/hbase_security_log_maxbackupindex',20)

rm_cross_origin_enabled = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.cross-origin.enabled']

cross_origins = '*'
if rm_cross_origin_enabled:
  host_suffix = rm_host.rsplit('.', 2)[1:]
  if len(host_suffix) == 2 :
    cross_origins = 'regex:.*[.]' + '[.]'.join(host_suffix) + "(:\d*)?"

ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  if 'cluster-env' in config['configurations'] and \
          'metrics_collector_vip_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_vip_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
    if metric_collector_web_address.find(':') != -1:
      metric_collector_port = metric_collector_web_address.split(':')[1]
    else:
      metric_collector_port = '6188'
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
  metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
  metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")
  host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
  host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)

  pass
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)

host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)
is_aggregation_https_enabled = False
if default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = 'https'
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = 'http'
