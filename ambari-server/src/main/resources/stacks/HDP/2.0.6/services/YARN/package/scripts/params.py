#!/usr/bin/env python2.6
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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()

config_dir = "/etc/hadoop/conf"

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = config['configurations']['global']['hdfs_user']

smokeuser = config['configurations']['global']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
yarn_executor_container_group = config['configurations']['yarn-site']['yarn.nodemanager.linux-container-executor.group']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
rm_host = config['clusterHostInfo']['rm_host'][0]
rm_port = config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'].split(':')[-1]
rm_https_port = "8090"
# TODO UPGRADE default, update site during upgrade
rm_nodes_exclude_path = default("/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path","/etc/hadoop/conf/yarn.exclude")

java64_home = config['hostLevelParams']['java_home']
hadoop_ssl_enabled = default("/configurations/core-site/hadoop.ssl.enabled", False)

hadoop_libexec_dir = '/usr/lib/hadoop/libexec'
hadoop_yarn_home = '/usr/lib/hadoop-yarn'
yarn_heapsize = config['configurations']['global']['yarn_heapsize']
resourcemanager_heapsize = config['configurations']['global']['resourcemanager_heapsize']
nodemanager_heapsize = config['configurations']['global']['nodemanager_heapsize']
apptimelineserver_heapsize = default("/configurations/global/apptimelineserver_heapsize", 1024)
yarn_log_dir_prefix = config['configurations']['global']['yarn_log_dir_prefix']
yarn_pid_dir_prefix = status_params.yarn_pid_dir_prefix
mapred_pid_dir_prefix = status_params.mapred_pid_dir_prefix
mapred_log_dir_prefix = config['configurations']['global']['mapred_log_dir_prefix']

rm_webui_address = format("{rm_host}:{rm_port}")
rm_webui_https_address = format("{rm_host}:{rm_https_port}")
nm_webui_address = config['configurations']['yarn-site']['yarn.nodemanager.webapp.address']
hs_webui_address = config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address']

nm_local_dirs = config['configurations']['yarn-site']['yarn.nodemanager.local-dirs']
nm_log_dirs = config['configurations']['yarn-site']['yarn.nodemanager.log-dirs']


hadoop_mapred2_jar_location = "/usr/lib/hadoop-mapreduce"
distrAppJarName = "hadoop-yarn-applications-distributedshell-2.*.jar"
hadoopMapredExamplesJarName = "hadoop-mapreduce-examples-2.*.jar"

yarn_pid_dir = status_params.yarn_pid_dir
mapred_pid_dir = status_params.mapred_pid_dir

mapred_log_dir = format("{mapred_log_dir_prefix}/{mapred_user}")
yarn_log_dir = format("{yarn_log_dir_prefix}/{yarn_user}")
mapred_job_summary_log = format("{mapred_log_dir_prefix}/{mapred_user}/hadoop-mapreduce.jobsummary.log")
yarn_job_summary_log = format("{yarn_log_dir_prefix}/{yarn_user}/hadoop-mapreduce.jobsummary.log")

mapred_bin = "/usr/lib/hadoop-mapreduce/sbin"
yarn_bin = "/usr/lib/hadoop-yarn/sbin"

user_group = config['configurations']['global']['user_group']
limits_conf_dir = "/etc/security/limits.d"
hadoop_conf_dir = "/etc/hadoop/conf"
yarn_container_bin = "/usr/lib/hadoop-yarn/bin"

#exclude file
exclude_hosts = default("/clusterHostInfo/decom_nm_hosts", [])
exclude_file_path = config['configurations']['yarn-site']['yarn.resourcemanager.nodes.exclude-path']

hostname = config['hostname']

if security_enabled:
  nm_principal_name = config['configurations']['global']['nodemanager_principal_name']
  nodemanager_keytab = config['configurations']['global']['nodemanager_keytab']
  nodemanager_principal_name = nm_principal_name.replace('_HOST',hostname.lower())
  nm_kinit_cmd = format("{kinit_path_local} -kt {nodemanager_keytab} {nodemanager_principal_name};")
else:
  nm_kinit_cmd = ""

yarn_log_aggregation_enabled = config['configurations']['yarn-site']['yarn.log-aggregation-enable']
yarn_nm_app_log_dir =  config['configurations']['yarn-site']['yarn.nodemanager.remote-app-log-dir']
mapreduce_jobhistory_intermediate_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.intermediate-done-dir']
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapreduce.jobhistory.done-dir']

#for create_hdfs_directory
hostname = config["hostname"]
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['global']['hdfs_user_keytab']
hdfs_user = config['configurations']['global']['hdfs_user']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local
)
update_exclude_file_only = config['commandParams']['update_exclude_file_only']
