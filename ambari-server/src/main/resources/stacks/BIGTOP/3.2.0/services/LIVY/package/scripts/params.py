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
import socket

import status_params

from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script


config = Script.get_config()
stack_root = Script.get_stack_root()
version = default("/commandParams/version", None)

java_home = config["ambariLevelParams"]["java_home"]
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

security_enabled = (
  str(config["configurations"]["cluster-env"]["security_enabled"])
  .strip()
  .lower()
  == "true"
)
user_group = status_params.user_group
smoke_user = config["configurations"]["cluster-env"]["smokeuser"]
smoke_user_keytab = default("/configurations/cluster-env/smokeuser_keytab", None)
smokeuser_principal = default(
  "/configurations/cluster-env/smokeuser_principal_name", None
)
if smokeuser_principal:
  smokeuser_principal = smokeuser_principal.replace(
    "_HOST", socket.getfqdn().lower()
  )

kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)

hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_principal_name = default(
  "/configurations/hadoop-env/hdfs_principal_name", None
)
hdfs_user_keytab = default("/configurations/hadoop-env/hdfs_user_keytab", None)
hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
dfs_type = default("/clusterLevelParams/dfs_type", "")
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

livy_component_directory = Script.get_component_from_role(
  {"LIVY_SERVER": "livy-server"},
  "LIVY_SERVER",
)
livy_home = format("{stack_root}/current/{livy_component_directory}")
livy_conf = format("{livy_home}/conf")
livy_server_command = format("{livy_home}/bin/livy-server")

livy_user = status_params.livy_user
livy_group = status_params.livy_group
livy_pid_dir = status_params.livy_pid_dir
livy_server_pid_file = status_params.livy_server_pid_file
livy_server_kerberos_cache_file = os.path.join(
  livy_pid_dir, ".livy-server-krb5cc"
)
livy_log_dir = config["configurations"]["livy-env"]["livy_log_dir"]
livy_hdfs_user_dir = format("/user/{livy_user}")
livy_recovery_dir = default(
  "/configurations/livy-conf/livy.server.recovery.state-store.url",
  "/livy-recovery",
)
livy_recovery_store = default(
  "/configurations/livy-conf/livy.server.recovery.state-store", "filesystem"
)

livy_env_sh = config["configurations"]["livy-env"]["content"]
livy_log4j_properties = config["configurations"]["livy-log4j-properties"][
  "content"
]
livy_spark_blacklist_properties = config["configurations"][
  "livy-spark-blacklist"
]["content"]

spark_home = config["configurations"]["livy-env"]["spark_home"]
spark_conf_dir = "/etc/spark/conf"
java_home_shell = quote_bash_args(str(java_home))
spark_home_shell = quote_bash_args(str(spark_home))
spark_conf_dir_shell = quote_bash_args(spark_conf_dir)
hadoop_conf_dir_shell = quote_bash_args(str(hadoop_conf_dir))
livy_log_dir_shell = quote_bash_args(str(livy_log_dir))
livy_pid_dir_shell = quote_bash_args(str(livy_pid_dir))
livy_user_shell = quote_bash_args(str(livy_user))

livy_livyserver_hosts = default("/clusterHostInfo/livy_server_hosts", [])
has_livyserver = bool(livy_livyserver_hosts)
livy_livyserver_port = default(
  "/configurations/livy-conf/livy.server.port", 8999
)
livy_keystore = default("/configurations/livy-conf/livy.keystore", "")
livy_http_scheme = "https" if str(livy_keystore).strip() else "http"

livy_kerberos_keytab = default(
  "/configurations/livy-conf/livy.server.launch.kerberos.keytab", None
)
livy_kerberos_principal = default(
  "/configurations/livy-conf/livy.server.launch.kerberos.principal", None
)
livy_principal = None
if livy_kerberos_principal:
  livy_principal = livy_kerberos_principal.replace(
    "_HOST", config["agentLevelParams"]["hostname"].lower()
  )

ats_hosts = default("/clusterHostInfo/app_timeline_server_hosts", [])
has_ats = bool(ats_hosts)
entity_groupfs_active_dir = default(
  "/configurations/yarn-site/"
  "yarn.timeline-service.entity-group-fs-store.active-dir",
  None,
)
entity_groupfs_store_dir = default(
  "/configurations/yarn-site/"
  "yarn.timeline-service.entity-group-fs-store.done-dir",
  None,
)

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
