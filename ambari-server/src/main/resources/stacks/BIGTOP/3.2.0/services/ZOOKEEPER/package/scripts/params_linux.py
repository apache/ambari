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

import json
import os

from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script

import status_params
import zookeeper_utils


config = Script.get_config()
tmp_dir = zookeeper_utils.validate_absolute_path(
  Script.get_tmp_dir(), "agent temporary directory"
)
stack_root = zookeeper_utils.validate_absolute_path(
  Script.get_stack_root(), "BIGTOP stack root"
)
stack_name = config["clusterLevelParams"]["stack_name"]
stack_version_formatted = format_stack_version(
  config["clusterLevelParams"]["stack_version"]
)
zookeeper_utils.validate_bigtop_stack(stack_name, stack_version_formatted)

version = default("/commandParams/version", None)
if version is not None:
  zookeeper_utils.validate_bigtop_stack(stack_name, format_stack_version(version))

zk_home = os.path.join(stack_root, "current", status_params.component_directory)
zk_home = zookeeper_utils.validate_absolute_path(zk_home, "ZooKeeper home")

config_dir = status_params.config_dir
zk_config_file = status_params.zk_config_file
zk_server_script = os.path.join(zk_home, "bin", "zkServer.sh")
zk_cli_script = os.path.join(zk_home, "bin", "zkCli.sh")
zk_server_jaas_file = os.path.join(config_dir, "zookeeper_jaas.conf")
zk_client_jaas_file = os.path.join(config_dir, "zookeeper_client_jaas.conf")

zk_user = status_params.zk_user
user_group = status_params.user_group
zk_pid_dir = status_params.zk_pid_dir
zk_pid_file = status_params.zk_pid_file
zk_log_dir = zookeeper_utils.validate_service_directory(
  config["configurations"]["zookeeper-env"]["zk_log_dir"],
  "zookeeper-env/zk_log_dir",
)
zk_data_dir = zookeeper_utils.validate_service_directory(
  config["configurations"]["zoo.cfg"]["dataDir"], "zoo.cfg/dataDir"
)

hostname = zookeeper_utils.validate_host(
  config["agentLevelParams"]["hostname"], "agent hostname"
)
zookeeper_hosts = tuple(
  sorted(
    {
      zookeeper_utils.validate_host(host, "ZooKeeper server host")
      for host in config["clusterHostInfo"]["zookeeper_server_hosts"]
    }
  )
)
if not zookeeper_hosts:
  raise Fail("ZooKeeper requires at least one server host")

client_port = zookeeper_utils.positive_int(
  config["configurations"]["zoo.cfg"]["clientPort"],
  "zoo.cfg/clientPort",
  maximum=65535,
)
zk_server_heap_mb = zookeeper_utils.positive_int(
  default("/configurations/zookeeper-env/zk_server_heapsize", 1024),
  "zookeeper-env/zk_server_heapsize",
  minimum=256,
  maximum=32768,
)
# Existing clusters may retain an older zookeeper-env content template.
zk_server_heapsize = f"-Xmx{zk_server_heap_mb}m"

zoo_cfg_properties_map = zookeeper_utils.sanitize_zoo_cfg(
  config["configurations"]["zoo.cfg"]
)
zk_env_sh_template = config["configurations"]["zookeeper-env"]["content"]
if not isinstance(zk_env_sh_template, str) or not zk_env_sh_template.strip():
  raise Fail("zookeeper-env/content must be a non-empty template")

security_enabled = zookeeper_utils.as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "cluster-env/security_enabled",
)
zk_principal_name = default(
  "/configurations/zookeeper-env/zookeeper_principal_name", None
)
zk_keytab_path = default(
  "/configurations/zookeeper-env/zookeeper_keytab_path", None
)
smokeuser = zookeeper_utils.validate_user(
  config["configurations"]["cluster-env"]["smokeuser"],
  "cluster-env/smokeuser",
)
smokeuser_principal = default(
  "/configurations/cluster-env/smokeuser_principal_name", None
)
smoke_user_keytab = default(
  "/configurations/cluster-env/smokeuser_keytab", None
)

if security_enabled:
  if (
    not isinstance(zk_principal_name, str)
    or not zk_principal_name
    or not zk_keytab_path
  ):
    raise Fail("ZooKeeper Kerberos principal and keytab must be configured")
  zk_principal = zookeeper_utils.validate_principal(
    zk_principal_name.replace("_HOST", hostname),
    "zookeeper-env/zookeeper_principal_name",
  )
  zk_principal_user = zookeeper_utils.validate_user(
    zk_principal.split("/", 1)[0], "ZooKeeper Kerberos service name"
  )
  zookeeper_utils.validate_absolute_path(
    zk_keytab_path, "zookeeper-env/zookeeper_keytab_path"
  )
  smokeuser_principal = zookeeper_utils.validate_principal(
    smokeuser_principal, "cluster-env/smokeuser_principal_name"
  )
  zookeeper_utils.validate_absolute_path(
    smoke_user_keytab, "cluster-env/smokeuser_keytab"
  )
else:
  zk_principal = ""
  zk_principal_user = ""

kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
java64_home = zookeeper_utils.validate_absolute_path(
  config["ambariLevelParams"]["java_home"], "ambariLevelParams/java_home"
)
java_executable = os.path.join(java64_home, "bin", "java")

zookeeper_log_max_backup_size = zookeeper_utils.positive_int(
  default("/configurations/zookeeper-log4j/zookeeper_log_max_backup_size", 10),
  "zookeeper-log4j/zookeeper_log_max_backup_size",
)
zookeeper_log_number_of_backup_files = zookeeper_utils.positive_int(
  default(
    "/configurations/zookeeper-log4j/zookeeper_log_number_of_backup_files", 10
  ),
  "zookeeper-log4j/zookeeper_log_number_of_backup_files",
  minimum=0,
)
log4j_props = default("/configurations/zookeeper-log4j/content", None)
if log4j_props is not None and not isinstance(log4j_props, str):
  raise Fail("zookeeper-log4j/content must be text")

zk_server_jvmflags = ""
zk_client_jvmflags = ""
if security_enabled:
  zk_server_jvmflags = (
    "-Djava.security.auth.login.config=" + zk_server_jaas_file
  )
  zk_client_jvmflags = (
    "-Djava.security.auth.login.config="
    + zk_client_jaas_file
    + " -Dzookeeper.sasl.client.username="
    + zk_principal_user
  )

java64_home_shell = quote_bash_args(java64_home)
zk_home_shell = quote_bash_args(zk_home)
zk_log_dir_shell = quote_bash_args(zk_log_dir)
zk_pid_file_shell = quote_bash_args(zk_pid_file)
zk_server_heap_mb_shell = quote_bash_args(str(zk_server_heap_mb))
zk_server_jvmflags_shell = quote_bash_args(zk_server_jvmflags)
zk_client_jvmflags_shell = quote_bash_args(zk_client_jvmflags)
java_executable_shell = quote_bash_args(java_executable)
zk_log_path_json = json.dumps(os.path.join(zk_log_dir, "zookeeper*.log"))
