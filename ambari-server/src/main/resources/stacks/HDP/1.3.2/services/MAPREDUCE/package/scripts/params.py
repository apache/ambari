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

conf_dir = "/etc/hadoop/conf"

mapred_user = status_params.mapred_user
pid_dir_prefix = status_params.pid_dir_prefix
mapred_pid_dir = status_params.mapred_pid_dir

historyserver_pid_file = status_params.historyserver_pid_file
jobtracker_pid_file = status_params.jobtracker_pid_file
tasktracker_pid_file = status_params.tasktracker_pid_file

hadoop_libexec_dir = '/usr/lib/hadoop/libexec'
hadoop_bin = "/usr/lib/hadoop/bin"
user_group = config['configurations']['global']['user_group']
hdfs_log_dir_prefix = config['configurations']['global']['hdfs_log_dir_prefix']
mapred_log_dir_prefix = default("mapred_log_dir_prefix",hdfs_log_dir_prefix)
mapred_local_dir = config['configurations']['mapred-site']['mapred.local.dir']
update_exclude_file_only = config['commandParams']['update_exclude_file_only']

hadoop_jar_location = "/usr/lib/hadoop/"
smokeuser = config['configurations']['global']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

#exclude file
mr_exclude_hosts = default("/clusterHostInfo/decom_tt_hosts", [])
exclude_file_path = config['configurations']['mapred-site']['mapred.hosts.exclude']
mapred_hosts_file_path = config['configurations']['mapred-site']['mapred.hosts']

#hdfs directories
mapreduce_jobhistory_intermediate_done_dir = default('/configurations/mapred-site/mapreduce.jobhistory.intermediate-done-dir', '/mr-history/tmp')
mapreduce_jobhistory_done_dir = config['configurations']['mapred-site']['mapred.job.tracker.history.completed.location']
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