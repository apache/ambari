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
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management import *
from resource_management.libraries.functions import conf_select, stack_select
import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

hostname = config['hostname']

host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

#hadoop params
yarn_role_root = "hadoop-yarn-client"
mapred_role_root = "hadoop-mapreduce-client"

command_role = default("/role", "")
if command_role == "APP_TIMELINE_SERVER":
  yarn_role_root = "hadoop-yarn-timelineserver"
elif command_role == "HISTORYSERVER":
  mapred_role_root = "hadoop-mapreduce-historyserver"
elif command_role == "MAPREDUCE2_CLIENT":
  mapred_role_root = "hadoop-mapreduce-client"
elif command_role == "NODEMANAGER":
  yarn_role_root = "hadoop-yarn-nodemanager"
elif command_role == "RESOURCEMANAGER":
  yarn_role_root = "hadoop-yarn-resourcemanager"
elif command_role == "YARN_CLIENT":
  yarn_role_root = "hadoop-yarn-client"

nm_hosts = default("/clusterHostInfo/nm_hosts", [])
number_of_nm = len(nm_hosts)

hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_mapred2_jar_location = format("/usr/iop/current/{mapred_role_root}")
mapred_bin = format("/usr/iop/current/{mapred_role_root}/sbin")
hadoop_yarn_home = format("/usr/iop/current/{yarn_role_root}")
yarn_bin = format("/usr/iop/current/{yarn_role_root}/sbin")
yarn_container_bin = format("/usr/iop/current/{yarn_role_root}/bin")

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
kinit_path_local = functions.get_kinit_path()
rm_hosts = config['clusterHostInfo']['rm_host']
rm_host = rm_hosts[0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = "8090"

default_exclude_path=os.path.join(hadoop_conf_dir, 'yarn.exclude')
# TODO UPGRADE default, update site during upgrade
rm_nodes_exclude_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path",default_exclude_path)

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
default_exclude_path=os.path.join(hadoop_conf_dir, 'yarn.exclude')
exclude_hosts = default("/clusterHostInfo/decom_nm_hosts", [])
exclude_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path",default_exclude_path)

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

# default kinit commands
rm_kinit_cmd = ""
yarn_timelineservice_kinit_cmd = ""
nodemanager_kinit_cmd = ""

if security_enabled:
  _rm_principal_name = config['configurations']['yarn-site']['yarn.resourcemanager.principal']
  _rm_principal_name = _rm_principal_name.replace('_HOST',hostname.lower())
  _rm_keytab = config['configurations']['yarn-site']['yarn.resourcemanager.keytab']
  rm_kinit_cmd = format("{kinit_path_local} -kt {_rm_keytab} {_rm_principal_name};")

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

else:
  rm_kinit_cmd = ""
  yarn_timelineservice_kinit_cmd = ""

yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")
jhs_leveldb_state_store_dir = default('/configurations/mapred-site/mapreduce.jobhistory.recovery.store.leveldb.path', "/hadoop/mapreduce/jhs")

#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
 )
update_exclude_file_only = default("/commandParams/update_exclude_file_only",False)

mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
min_user_id = config['configurations']['yarn-env']['min_user_id']

# Node labels
node_labels_dir = default("/configurations/yarn-site/yarn.node-labels.fs-store.root-dir", None)
