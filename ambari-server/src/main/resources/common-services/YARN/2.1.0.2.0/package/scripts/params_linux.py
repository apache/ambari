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

from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_directory import HdfsDirectory

import status_params

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
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

stack_name = default("/hostLevelParams/stack_name", None)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

hostname = config['hostname']

# hadoop default parameters
hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
hadoop_bin = "/usr/lib/hadoop/sbin"
hadoop_bin_dir = "/usr/bin"
hadoop_conf_dir = "/etc/hadoop/conf"
hadoop_yarn_home = '/usr/lib/hadoop-yarn'
hadoop_mapred2_jar_location = "/usr/lib/hadoop-mapreduce"
mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
yarn_bin = "/usr/lib/hadoop-yarn/sbin"
yarn_container_bin = "/usr/lib/hadoop-yarn/bin"

# hadoop parameters for 2.2+
if Script.is_hdp_stack_greater_or_equal("2.2"):

  # MapR directory root
  mapred_role_root = "hadoop-mapreduce-client"
  command_role = default("/role", "")
  if command_role in MAPR_SERVER_ROLE_DIRECTORY_MAP:
    mapred_role_root = MAPR_SERVER_ROLE_DIRECTORY_MAP[command_role]

  # YARN directory root
  yarn_role_root = "hadoop-yarn-client"
  if command_role in YARN_SERVER_ROLE_DIRECTORY_MAP:
    yarn_role_root = YARN_SERVER_ROLE_DIRECTORY_MAP[command_role]

  hadoop_libexec_dir = "/usr/hdp/current/hadoop-client/libexec"
  hadoop_bin = "/usr/hdp/current/hadoop-client/sbin"
  hadoop_bin_dir = "/usr/hdp/current/hadoop-client/bin"

  hadoop_mapred2_jar_location = format("/usr/hdp/current/{mapred_role_root}")
  mapred_bin = format("/usr/hdp/current/{mapred_role_root}/sbin")

  hadoop_yarn_home = format("/usr/hdp/current/{yarn_role_root}")
  yarn_bin = format("/usr/hdp/current/{yarn_role_root}/sbin")
  yarn_container_bin = format("/usr/hdp/current/{yarn_role_root}/bin")

  # the configuration direction for HDFS/YARN/MapR is the hadoop config
  # directory, which is symlinked by hadoop-client only
  hadoop_conf_dir = "/usr/hdp/current/hadoop-client/conf"


limits_conf_dir = "/etc/security/limits.d"
execute_path = os.environ['PATH'] + os.pathsep + hadoop_bin_dir + os.pathsep + yarn_container_bin

ulimit_cmd = "ulimit -c unlimited;"

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']

smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
rm_hosts = config['clusterHostInfo']['rm_host']
rm_host = rm_hosts[0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = "8090"
# TODO UPGRADE default, update site during upgrade
rm_nodes_exclude_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")

java64_home = config['hostLevelParams']['java_home']
hadoop_ssl_enabled = default("/configurations/core-site/hadoop.ssl.enabled", False)

yarn_heapsize = config['configurations']['yarn-env']['yarn_heapsize']
resourcemanager_heapsize = config['configurations']['yarn-env']['resourcemanager_heapsize']
nodemanager_heapsize = config['configurations']['yarn-env']['nodemanager_heapsize']
apptimelineserver_heapsize = default("/configurations/yarn-env/apptimelineserver_heapsize", 1024)
ats_leveldb_dir = config['configurations']['yarn-site']['yarn.timeline-service.leveldb-timeline-store.path']
yarn_log_dir_prefix = config['configurations']['yarn-env']['yarn_log_dir_prefix']
yarn_pid_dir_prefix = status_params.yarn_pid_dir_prefix
mapred_pid_dir_prefix = status_params.mapred_pid_dir_prefix
mapred_log_dir_prefix = config['configurations']['mapred-env']['mapred_log_dir_prefix']
mapred_env_sh_template = config['configurations']['mapred-env']['content']
yarn_env_sh_template = config['configurations']['yarn-env']['content']

if len(rm_hosts) > 1:
  additional_rm_host = rm_hosts[1]
  rm_webui_address = format("{rm_host}:{rm_port},{additional_rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port},{additional_rm_host}:{rm_https_port}")
else:
  rm_webui_address = format("{rm_host}:{rm_port}")
  rm_webui_https_address = format("{rm_host}:{rm_https_port}")

nm_webui_address = config['configurations']['yarn-site']['yarn.nodemanager.webapp.address']
hs_webui_address = config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address']
nm_address = config['configurations']['yarn-site']['yarn.nodemanager.address']  # still contains 0.0.0.0
if hostname and nm_address and nm_address.startswith("0.0.0.0:"):
  nm_address = nm_address.replace("0.0.0.0", hostname)

nm_local_dirs = config['configurations']['yarn-site']['yarn.nodemanager.local-dirs']
nm_log_dirs = config['configurations']['yarn-site']['yarn.nodemanager.log-dirs']

distrAppJarName = "hadoop-yarn-applications-distributedshell-2.*.jar"
hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-2.*.jar"

yarn_pid_dir = status_params.yarn_pid_dir
mapred_pid_dir = status_params.mapred_pid_dir

mapred_log_dir = format("{mapred_log_dir_prefix}/{mapred_user}")
yarn_log_dir = format("{yarn_log_dir_prefix}/{yarn_user}")
mapred_job_summary_log = format("{mapred_log_dir_prefix}/{mapred_user}/hadoop-mapreduce.jobsummary.log")
yarn_job_summary_log = format("{yarn_log_dir_prefix}/{yarn_user}/hadoop-mapreduce.jobsummary.log")

user_group = config['configurations']['cluster-env']['user_group']

#exclude file
exclude_hosts = default("/clusterHostInfo/decom_nm_hosts", [])
exclude_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

nm_hosts = default("/clusterHostInfo/nm_hosts", [])
number_of_nm = len(nm_hosts)

# default kinit commands
rm_kinit_cmd = ""
yarn_timelineservice_kinit_cmd = ""
nodemanager_kinit_cmd = ""

if security_enabled:
  _rm_principal_name = config['configurations']['yarn-site']['yarn.resourcemanager.principal']
  _rm_principal_name = _rm_principal_name.replace('_HOST',hostname.lower())
  _rm_keytab = config['configurations']['yarn-site']['yarn.resourcemanager.keytab']
  rm_kinit_cmd = format("{kinit_path_local} -kt {_rm_keytab} {_rm_principal_name};")

  # YARN timeline security options are only available in HDP Champlain
  if has_ats:
    _yarn_timelineservice_principal_name = config['configurations']['yarn-site']['yarn.timeline-service.principal']
    _yarn_timelineservice_principal_name = _yarn_timelineservice_principal_name.replace('_HOST', hostname.lower())
    _yarn_timelineservice_keytab = config['configurations']['yarn-site']['yarn.timeline-service.keytab']
    yarn_timelineservice_kinit_cmd = format("{kinit_path_local} -kt {_yarn_timelineservice_keytab} {_yarn_timelineservice_principal_name};")

  if 'yarn.nodemanager.principal' in config['configurations']['yarn-site']:
    _nodemanager_principal_name = default('/configurations/yarn-site/yarn.nodemanager.principal', None)
    if _nodemanager_principal_name:
      _nodemanager_principal_name = _nodemanager_principal_name.replace('_HOST', hostname.lower())

    _nodemanager_keytab = config['configurations']['yarn-site']['yarn.nodemanager.keytab']
    nodemanager_kinit_cmd = format("{kinit_path_local} -kt {_nodemanager_keytab} {_nodemanager_principal_name};")


yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")

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
import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  bin_dir = hadoop_bin_dir
)
update_exclude_file_only = default("/commandParams/update_exclude_file_only",False)

mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
min_user_id = config['configurations']['yarn-env']['min_user_id']

# Node labels
node_labels_dir = default("/configurations/yarn-site/yarn.node-labels.fs-store.root-dir", None)
node_label_enable = config['configurations']['yarn-site']['yarn.node-labels.enabled']

cgroups_dir = "/cgroups_test/cpu"