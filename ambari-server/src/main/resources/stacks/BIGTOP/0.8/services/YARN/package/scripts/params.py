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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
  hadoop_libexec_dir = "/usr/bigtop/current/hadoop-client/libexec"
  hadoop_bin = "/usr/bigtop/current/hadoop-client/sbin"
  hadoop_bin_dir = "/usr/bigtop/current/hadoop-client/bin"
  hadoop_yarn_home = '/usr/bigtop/current/hadoop-yarn-client'
  hadoop_mapred2_jar_location = '/usr/bigtop/current/hadoop-mapreduce-client'
  mapred_bin = '/usr/bigtop/current/hadoop-mapreduce-client/sbin'
  yarn_bin = '/usr/bigtop/current/hadoop-yarn-client/sbin'
  yarn_container_bin = '/usr/bigtop/current/hadoop-yarn-client/bin'
else:
  hadoop_libexec_dir = "/usr/lib/hadoop/libexec"
  hadoop_bin = "/usr/lib/hadoop/sbin"
  hadoop_bin_dir = "/usr/bin"
  hadoop_yarn_home = '/usr/lib/hadoop-yarn'
  hadoop_mapred2_jar_location = "/usr/lib/hadoop-mapreduce"
  mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
  yarn_bin = "/usr/lib/hadoop-yarn/sbin"
  yarn_container_bin = "/usr/lib/hadoop-yarn/bin"

hadoop_conf_dir = "/etc/hadoop/conf"
limits_conf_dir = "/etc/security/limits.d"
execute_path = os.environ['PATH'] + os.pathsep + hadoop_bin_dir + os.pathsep + yarn_container_bin

ulimit_cmd = "ulimit -c unlimited;"

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']

smokeuser = config['configurations']['cluster-env']['smokeuser']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
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

nm_hosts = default("/clusterHostInfo/nm_hosts", [])
#incude file
include_file_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.include-path", None)
include_hosts = None
manage_include_files = default("/configurations/yarn-site/manage.include.files", False)
if include_file_path and manage_include_files:
  include_hosts = list(set(nm_hosts) - set(exclude_hosts))

hostname = config['hostname']

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

if security_enabled:
  _rm_principal_name = config['configurations']['yarn-site']['yarn.resourcemanager.principal']
  _rm_keytab = config['configurations']['yarn-site']['yarn.resourcemanager.keytab']
  _rm_principal_name = _rm_principal_name.replace('_HOST',hostname.lower())
  
  rm_kinit_cmd = format("{kinit_path_local} -kt {_rm_keytab} {_rm_principal_name};")

  # YARN timeline security options are only available in HDP Champlain
  if has_ats:
    _yarn_timelineservice_principal_name = config['configurations']['yarn-site']['yarn.timeline-service.principal']
    _yarn_timelineservice_principal_name = _yarn_timelineservice_principal_name.replace('_HOST', hostname.lower())
    _yarn_timelineservice_keytab = config['configurations']['yarn-site']['yarn.timeline-service.keytab']
    yarn_timelineservice_kinit_cmd = format("{kinit_path_local} -kt {_yarn_timelineservice_keytab} {_yarn_timelineservice_principal_name};")
else:
  rm_kinit_cmd = ""
  yarn_timelineservice_kinit_cmd = ""

yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")

#for create_hdfs_directory
hostname = config["hostname"]
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
update_files_only = default("/commandParams/update_files_only",False)

mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#taskcontroller.cfg

mapred_local_dir = "/tmp/hadoop-mapred/mapred/local"
hdfs_log_dir_prefix = config['configurations']['hadoop-env']['hdfs_log_dir_prefix']
min_user_id = config['configurations']['yarn-env']['min_user_id']

