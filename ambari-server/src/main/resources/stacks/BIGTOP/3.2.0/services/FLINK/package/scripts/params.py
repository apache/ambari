#!/usr/bin/env python
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
import status_params

from resource_management.libraries.functions import format
from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/clusterLevelParams/stack_name", None)
stack_root = Script.get_stack_root()

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
java_home = config['ambariLevelParams']['java_home']

# default hadoop parameters
hadoop_home = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
dfs_type = default("/clusterLevelParams/dfs_type", "")
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
default_fs = config['configurations']['core-site']['fs.defaultFS']
hdfs_site = config['configurations']['hdfs-site']
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

flink_etc_dir = "/etc/flink"
flink_config_dir = "/etc/flink/conf"
flink_dir = "/usr/lib/flink"
flink_bin_dir = "/usr/lib/flink/bin"
flink_log_dir = config['configurations']['flink-env']['flink_log_dir']
flink_pid_dir = config['configurations']['flink-env']['flink_pid_dir']

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']

flink_user = config['configurations']['flink-env']['flink_user']
user_group = config['configurations']['cluster-env']['user_group']
flink_conf_template = config['configurations']['flink-conf']['content']
flink_group = config['configurations']['flink-env']['flink_group']
flink_hdfs_user_dir = format("/user/{flink_user}")

flink_log4j_cli_properties = config['configurations']['flink-log4j-cli-properties']['content']
flink_log4j_console_properties = config['configurations']['flink-log4j-console-properties']['content']
flink_log4j_properties = config['configurations']['flink-log4j-properties']['content']
flink_log4j_session_properties = config['configurations']['flink-log4j-session-properties']['content']

jobmanager_archive_fs_dir = config['configurations']['flink-conf']['jobmanager.archive.fs.dir']
historyserver_archive_fs_dir = config['configurations']['flink-conf']['historyserver.archive.fs.dir']
historyserver_web_port = config['configurations']['flink-conf']['historyserver.web.port']
historyserver_archive_fs_refresh_interval = config['configurations']['flink-conf']['historyserver.archive.fs.refresh-interval']

flink_history_server_start = format("export HADOOP_CLASSPATH=`hadoop classpath`;{flink_dir}/bin/historyserver.sh start")
flink_history_server_stop = format("{flink_dir}/bin/historyserver.sh stop")
flink_history_server_pid_file = status_params.flink_history_server_pid_file

security_kerberos_login_principal = config['configurations']['flink-conf']['security.kerberos.login.principal']
security_kerberos_login_keytab = config['configurations']['flink-conf']['security.kerberos.login.keytab']

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