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
"""

import functools
import os

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import conf_select, get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.script.script import Script

import flink_utils
import status_params


SERVER_ROLE_DIRECTORY_MAP = {
  "FLINK_HISTORYSERVER": "flink-historyserver",
  "FLINK_CLIENT": "flink-client",
}

config = Script.get_config()
tmp_dir = flink_utils.validate_service_directory(Script.get_tmp_dir(), "Ambari temp directory")
stack_name = default("/clusterLevelParams/stack_name", None)
stack_version = flink_utils.validate_bigtop_stack(
  stack_name, config["clusterLevelParams"]["stack_version"]
)
version = default("/commandParams/version", None)
if version is not None:
  flink_utils.validate_bigtop_stack(stack_name, version)

component_directory = Script.get_component_from_role(
  SERVER_ROLE_DIRECTORY_MAP, "FLINK_CLIENT"
)
stack_root = flink_utils.validate_absolute_path(Script.get_stack_root(), "stack root")
flink_dir = flink_utils.validate_absolute_path(
  os.path.join(stack_root, "current", component_directory), "Flink home"
)
flink_etc_dir = "/etc/flink"
flink_config_dir = "/etc/flink/conf"
flink_config_file = f"{flink_config_dir}/flink-conf.yaml"
flink_cli_log_dir = "/var/log/flink-cli"

java_home = flink_utils.validate_absolute_path(
  config["ambariLevelParams"]["java_home"], "Java home"
)
hadoop_bin_dir = flink_utils.validate_absolute_path(
  stack_select.get_hadoop_dir("bin"), "Hadoop binary directory"
)
hadoop_conf_dir = flink_utils.validate_absolute_path(
  conf_select.get_hadoop_conf_dir(), "Hadoop configuration directory"
)
hadoop_executable = os.path.join(hadoop_bin_dir, "hadoop")
dfs_type = default("/clusterLevelParams/dfs_type", "")
hdfs_user = flink_utils.validate_user(
  config["configurations"]["hadoop-env"]["hdfs_user"], "HDFS user"
)
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
hdfs_site = config["configurations"]["hdfs-site"]
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

flink_user = flink_utils.validate_user(
  config["configurations"]["flink-env"]["flink_user"], "Flink user"
)
flink_group = flink_utils.validate_user(
  config["configurations"]["flink-env"]["flink_group"], "Flink group"
)
user_group = flink_utils.validate_user(
  config["configurations"]["cluster-env"]["user_group"], "cluster user group"
)
smokeuser = flink_utils.validate_user(
  config["configurations"]["cluster-env"]["smokeuser"], "smoke-test user"
)
flink_log_dir = flink_utils.validate_service_directory(
  config["configurations"]["flink-env"]["flink_log_dir"], "Flink log directory"
)
flink_pid_dir = status_params.flink_pid_dir
flink_history_server_pid_file = status_params.flink_history_server_pid_file
flink_hdfs_user_dir = f"/user/{flink_user}"

security_enabled = flink_utils.as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "cluster-env/security_enabled",
)
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]
security_kerberos_login_principal = config["configurations"]["flink-conf"][
  "security.kerberos.login.principal"
]
security_kerberos_login_keytab = config["configurations"]["flink-conf"][
  "security.kerberos.login.keytab"
]
if security_enabled:
  flink_utils.validate_absolute_path(kinit_path_local, "kinit executable")
  flink_utils.validate_absolute_path(hdfs_user_keytab, "HDFS keytab")
  flink_utils.validate_absolute_path(smoke_user_keytab, "smoke-test keytab")
  flink_utils.validate_principal(smokeuser_principal, "smoke-test principal")
  flink_utils.validate_principal(
    security_kerberos_login_principal, "Flink service principal"
  )
  flink_utils.validate_absolute_path(
    security_kerberos_login_keytab, "Flink service keytab"
  )

flink_conf_template = config["configurations"]["flink-conf"]["content"]
if not isinstance(flink_conf_template, str) or not flink_conf_template.strip():
  raise Fail("flink-conf/content must be a non-empty template")
flink_log4j_cli_properties = config["configurations"]["flink-log4j-cli-properties"]["content"]
flink_log4j_console_properties = config["configurations"][
  "flink-log4j-console-properties"
]["content"]
flink_log4j_properties = config["configurations"]["flink-log4j-properties"]["content"]
flink_log4j_session_properties = config["configurations"][
  "flink-log4j-session-properties"
]["content"]

jobmanager_archive_fs_dir = flink_utils.validate_hdfs_uri(
  config["configurations"]["flink-conf"]["jobmanager.archive.fs.dir"],
  "flink-conf/jobmanager.archive.fs.dir",
)
historyserver_archive_fs_dir = flink_utils.validate_hdfs_uri_list(
  config["configurations"]["flink-conf"]["historyserver.archive.fs.dir"],
  "flink-conf/historyserver.archive.fs.dir",
)
historyserver_web_port = flink_utils.bounded_int(
  config["configurations"]["flink-conf"]["historyserver.web.port"],
  "flink-conf/historyserver.web.port",
  1,
  65535,
)
historyserver_archive_fs_refresh_interval = flink_utils.bounded_int(
  config["configurations"]["flink-conf"][
    "historyserver.archive.fs.refresh-interval"
  ],
  "flink-conf/historyserver.archive.fs.refresh-interval",
  1,
  2147483647,
)

java_home_yaml = flink_utils.yaml_string(java_home, "Java home")
hadoop_conf_dir_yaml = flink_utils.yaml_string(hadoop_conf_dir, "Hadoop configuration directory")
flink_pid_dir_yaml = flink_utils.yaml_string(flink_pid_dir, "Flink PID directory")
flink_log_dir_yaml = flink_utils.yaml_string(flink_log_dir, "Flink log directory")
security_kerberos_login_principal_yaml = flink_utils.yaml_string(
  security_kerberos_login_principal, "Flink service principal"
)
security_kerberos_login_keytab_yaml = flink_utils.yaml_string(
  security_kerberos_login_keytab, "Flink service keytab"
)
jobmanager_archive_fs_dir_yaml = flink_utils.yaml_string(
  jobmanager_archive_fs_dir, "JobManager archive directory"
)
historyserver_archive_fs_dir_yaml = flink_utils.yaml_string(
  historyserver_archive_fs_dir, "History Server archive directories"
)

historyserver_script = os.path.join(flink_dir, "bin", "historyserver.sh")
flink_cli = os.path.join(flink_dir, "bin", "flink")
wordcount_jar = os.path.join(flink_dir, "examples", "batch", "WordCount.jar")

HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file=hdfs_resource_ignore_file,
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs,
  immutable_paths=get_not_managed_resources(),
  dfs_type=dfs_type,
)
