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
from resource_management.libraries import functions
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions.get_architecture import get_architecture
from resource_management.libraries.functions.setup_ranger_plugin_xml import get_audit_configs, generate_ranger_service_config

import status_params

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
  'YARN_CLIENT' : 'hadoop-yarn-client'
}

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

architecture = get_architecture()

stack_name = status_params.stack_name
stack_root = Script.get_stack_root()
tarball_map = default("/configurations/cluster-env/tarball_map", None)

config_path = stack_select.get_hadoop_dir("conf")
config_dir = os.path.realpath(config_path)

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
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_yarn_home = '/usr/lib/hadoop-yarn'
hadoop_mapred2_jar_location = "/usr/lib/hadoop-mapreduce"
mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
yarn_bin = "/usr/lib/hadoop-yarn/sbin"
yarn_container_bin = "/usr/lib/hadoop-yarn/bin"
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")

# hadoop parameters stack supporting rolling_uprade
if stack_supports_ru:
  # MapR directory root
  mapred_role_root = "hadoop-mapreduce-client"
  command_role = default("/role", "")
  if command_role in MAPR_SERVER_ROLE_DIRECTORY_MAP:
    mapred_role_root = MAPR_SERVER_ROLE_DIRECTORY_MAP[command_role]

  # YARN directory root
  yarn_role_root = "hadoop-yarn-client"
  if command_role in YARN_SERVER_ROLE_DIRECTORY_MAP:
    yarn_role_root = YARN_SERVER_ROLE_DIRECTORY_MAP[command_role]

  # defaults set to current based on role
  hadoop_mapr_home = format("{stack_root}/current/{mapred_role_root}")
  hadoop_yarn_home = format("{stack_root}/current/{yarn_role_root}")

  # try to render the specific version
  version = component_version.get_component_repository_version()
  if version is None:
    version = default("/commandParams/version", None)


  if version is not None:
    hadoop_mapr_versioned_home = format("{stack_root}/{version}/hadoop-mapreduce")
    hadoop_yarn_versioned_home = format("{stack_root}/{version}/hadoop-yarn")

    if sudo.path_isdir(hadoop_mapr_versioned_home):
      hadoop_mapr_home = hadoop_mapr_versioned_home

    if sudo.path_isdir(hadoop_yarn_versioned_home):
      hadoop_yarn_home = hadoop_yarn_versioned_home


  hadoop_mapred2_jar_location = hadoop_mapr_home
  mapred_bin = format("{hadoop_mapr_home}/sbin")

  yarn_bin = format("{hadoop_yarn_home}/sbin")
  yarn_container_bin = format("{hadoop_yarn_home}/bin")


if stack_supports_timeline_state_store:
  # Timeline Service property that was added timeline_state_store stack feature
  ats_leveldb_state_store_dir = default('/configurations/yarn-site/yarn.timeline-service.leveldb-state-store.path', '/hadoop/yarn/timeline')

# ats 1.5 properties
entity_groupfs_active_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.active-dir']
entity_groupfs_active_dir_mode = 01777
entity_groupfs_store_dir = config['configurations']['yarn-site']['yarn.timeline-service.entity-group-fs-store.done-dir']
entity_groupfs_store_dir_mode = 0700

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
smoke_hdfs_user_mode = 0770
security_enabled = config['configurations']['cluster-env']['security_enabled']
nm_security_marker_dir = "/var/lib/hadoop-yarn"
nm_security_marker = format('{nm_security_marker_dir}/nm_security_enabled')
current_nm_security_state = os.path.isfile(nm_security_marker)
toggle_nm_security = (current_nm_security_state and not security_enabled) or (not current_nm_security_state and security_enabled)
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
yarn_nodemanager_container_executor_class =  config['configurations']['yarn-site']['yarn.nodemanager.container-executor.class']
is_linux_container_executor = (yarn_nodemanager_container_executor_class == 'org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor')
container_executor_mode = 06050 if is_linux_container_executor else 02050
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
  tc_mode = 0644
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
  yarn_jaas_file = os.path.join(config_dir, 'yarn_jaas.conf')
  if stack_supports_zk_security:
    zk_principal_name = default("/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper/_HOST@EXAMPLE.COM")
    zk_principal_user = zk_principal_name.split('/')[0]
    rm_security_opts = format('-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Djava.security.auth.login.config={yarn_jaas_file} -Dzookeeper.sasl.clientconfig=Client')

  # YARN timeline security options
  if has_ats:
    yarn_timelineservice_principal_name = config['configurations']['yarn-site']['yarn.timeline-service.principal']
    yarn_timelineservice_principal_name = yarn_timelineservice_principal_name.replace('_HOST', hostname.lower())
    yarn_timelineservice_keytab = config['configurations']['yarn-site']['yarn.timeline-service.keytab']
    yarn_timelineservice_kinit_cmd = format("{kinit_path_local} -kt {yarn_timelineservice_keytab} {yarn_timelineservice_principal_name};")
    yarn_ats_jaas_file = os.path.join(config_dir, 'yarn_ats_jaas.conf')

  if 'yarn.nodemanager.principal' in config['configurations']['yarn-site']:
    nodemanager_principal_name = default('/configurations/yarn-site/yarn.nodemanager.principal', None)
    if nodemanager_principal_name:
      nodemanager_principal_name = nodemanager_principal_name.replace('_HOST', hostname.lower())

    nodemanager_keytab = config['configurations']['yarn-site']['yarn.nodemanager.keytab']
    nodemanager_kinit_cmd = format("{kinit_path_local} -kt {nodemanager_keytab} {nodemanager_principal_name};")
    yarn_nm_jaas_file = os.path.join(config_dir, 'yarn_nm_jaas.conf')

  if has_hs:
    mapred_jhs_principal_name = config['configurations']['mapred-site']['mapreduce.jobhistory.principal']
    mapred_jhs_principal_name = mapred_jhs_principal_name.replace('_HOST', hostname.lower())
    mapred_jhs_keytab = config['configurations']['mapred-site']['mapreduce.jobhistory.keytab']
    mapred_jaas_file = os.path.join(config_dir, 'mapred_jaas.conf')

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

# for curl command in ranger plugin to get db connector
jdk_location = config['ambariLevelParams']['jdk_location']

# ranger yarn plugin section start

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
    'password' : unicode(config['configurations']['ranger-yarn-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
    'yarn.url' : format('{scheme}://{yarn_rest_url}'),
    'commonNameForCertificate' : config['configurations']['ranger-yarn-plugin-properties']['common.name.for.certificate']
  }

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
