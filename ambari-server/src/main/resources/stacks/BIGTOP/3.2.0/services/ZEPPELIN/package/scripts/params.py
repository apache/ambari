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
import re
import socket
import status_params
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import (
  format_stack_version,
)
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script
from bigtop_service_contract import (
  get_livy_server_settings,
  get_spark_home,
  get_spark_thriftserver_settings,
)


def extract_spark_version(spark_home):
  try:
    with open(os.path.join(spark_home, "RELEASE"), encoding="utf-8") as release_file:
      match = re.search(r"Spark (\d+\.\d+)", release_file.readline())
      return match.group(1) if match else None
  except OSError:
    return None


# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()
# e.g. 2.3
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
# e.g. 2.3.0.0
stack_version_formatted = format_stack_version(stack_version_unformatted)
# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
# e.g. 2.3.0.0-2130
version = default("/commandParams/version", None)

security_value = str(
  config["configurations"]["cluster-env"]["security_enabled"]
).strip().lower()
if security_value not in ("true", "false"):
  raise ValueError("cluster-env/security_enabled must be true or false")
security_enabled = security_value == "true"

ui_ssl_value = str(
  config["configurations"]["zeppelin-site"]["zeppelin.ssl"]
).strip().lower()
if ui_ssl_value not in ("true", "false"):
  raise ValueError("zeppelin-site/zeppelin.ssl must be true or false")
is_ui_ssl_enabled = ui_ssl_value == "true"

# params from zeppelin-site
zeppelin_port = str(config["configurations"]["zeppelin-site"]["zeppelin.server.port"])
if is_ui_ssl_enabled:
  zeppelin_port = str(
    config["configurations"]["zeppelin-site"]["zeppelin.server.ssl.port"]
  )
zeppelin_interpreter = None
if "zeppelin.interpreter.group.order" in config["configurations"]["zeppelin-site"]:
  zeppelin_interpreter = str(
    config["configurations"]["zeppelin-site"]["zeppelin.interpreter.group.order"]
  ).split(",")

# params from zeppelin-env
zeppelin_user = config["configurations"]["zeppelin-env"]["zeppelin_user"]
zeppelin_group = config["configurations"]["zeppelin-env"]["zeppelin_group"]
zeppelin_log_dir = config["configurations"]["zeppelin-env"]["zeppelin_log_dir"]
zeppelin_pid_dir = config["configurations"]["zeppelin-env"]["zeppelin_pid_dir"]
zeppelin_pid_file = status_params.zeppelin_pid_file
zeppelin_war_tempdir = config["configurations"]["zeppelin-env"]["zeppelin_war_tempdir"]
zeppelin_notebook_dir = config["configurations"]["zeppelin-env"][
  "zeppelin_notebook_dir"
]
local_notebook_dir = "/var/lib/zeppelin/notebook"

hbase_home = config["configurations"]["zeppelin-env"]["hbase_home"]
hbase_conf_dir = config["configurations"]["zeppelin-env"]["hbase_conf_dir"]

zeppelin_conf_dir = "/etc/zeppelin/conf"
external_dependency_conf = format("{zeppelin_conf_dir}/external-dependency-conf")
zeppelin_home = "/usr/lib/zeppelin"
zeppelin_daemon = os.path.join(zeppelin_home, "bin", "zeppelin-daemon.sh")

use_current_stack_paths = stack_version_formatted and check_stack_feature(
  StackFeature.ROLLING_UPGRADE, stack_version_formatted
)
spark_home = get_spark_home(
  config["configurations"]["zeppelin-env"]["spark_home"],
  stack_root,
  use_current_stack_paths,
)

if use_current_stack_paths:
  hbase_home = format("{stack_root}/current/hbase-client")
  zeppelin_home = format("{stack_root}/current/zeppelin-server")
  local_notebook_dir = format(
    "{stack_root}/{stack_version_formatted}/{local_notebook_dir}"
  )

spark_version = None
if "spark-defaults" in config["configurations"]:
  spark_version = extract_spark_version(spark_home)

conf_stored_in_hdfs = False
if "zeppelin.config.fs.dir" in config["configurations"]["zeppelin-site"] and not config[
  "configurations"
]["zeppelin-site"]["zeppelin.config.fs.dir"].startswith("file://"):
  conf_stored_in_hdfs = True

# zeppelin-env.sh
zeppelin_env_content = config["configurations"]["zeppelin-env"]["zeppelin_env_content"]

# shiro.ini
shiro_ini_content = config["configurations"]["zeppelin-shiro-ini"]["shiro_ini_content"]

# log4j.properties
log4j_properties_content = config["configurations"]["zeppelin-log4j-properties"][
  "log4j_properties_content"
]

# detect configs
master_configs = config["clusterHostInfo"]
java64_home = config["ambariLevelParams"]["java_home"]
java64_home_shell = quote_bash_args(str(java64_home))
zeppelin_log_dir_shell = quote_bash_args(str(zeppelin_log_dir))
zeppelin_pid_dir_shell = quote_bash_args(str(zeppelin_pid_dir))
zeppelin_war_tempdir_shell = quote_bash_args(str(zeppelin_war_tempdir))
zeppelin_notebook_dir_shell = quote_bash_args(str(zeppelin_notebook_dir))
external_dependency_conf_shell = quote_bash_args(str(external_dependency_conf))
spark_home_shell = quote_bash_args(str(spark_home))
hbase_home_shell = quote_bash_args(str(hbase_home))
hbase_conf_dir_shell = quote_bash_args(str(hbase_conf_dir))
zeppelin_host = str(master_configs["zeppelin_server_hosts"][0])

# detect HS2 details, if installed

hive_server_host = None
hive_server_port = None
hive_zookeeper_quorum = None
hive_server2_support_dynamic_service_discovery = None
is_hive_installed = False
hive_zookeeper_namespace = None
hive_interactive_zookeeper_namespace = None

if (
  "hive_server_hosts" in master_configs
  and len(master_configs["hive_server_hosts"]) != 0
):
  is_hive_installed = True
  hive_server_host = str(master_configs["hive_server_hosts"][0])
  hive_server_port = str(
    config["configurations"]["hive-site"]["hive.server2.thrift.http.port"]
  )
  hive_zookeeper_quorum = config["configurations"]["hive-site"]["hive.zookeeper.quorum"]
  hive_zookeeper_namespace = config["configurations"]["hive-site"][
    "hive.server2.zookeeper.namespace"
  ]
  hive_zookeeper_namespace = default(
    "/configurations/hive-interactive-site/hive.server2.zookeeper.namespace",
    hive_zookeeper_namespace,
  )
  hive_server2_support_dynamic_service_discovery = config["configurations"][
    "hive-site"
  ]["hive.server2.support.dynamic.service.discovery"]

discovery_mode = "zooKeeper"
hive_server_interactive_hosts = None
if (
  "hive_server_interactive_hosts" in master_configs
  and len(master_configs["hive_server_interactive_hosts"]) != 0
):
  if len(master_configs["hive_server_interactive_hosts"]) > 1:
    discovery_mode = "zooKeeperHA"

  hive_server_interactive_hosts = str(
    master_configs["hive_server_interactive_hosts"][0]
  )
  hive_interactive_zookeeper_namespace = config["configurations"][
    "hive-interactive-site"
  ]["hive.server2.zookeeper.namespace"]
  hive_server_port = str(
    config["configurations"]["hive-site"]["hive.server2.thrift.http.port"]
  )
  hive_zookeeper_quorum = config["configurations"]["hive-site"]["hive.zookeeper.quorum"]
  hive_server2_support_dynamic_service_discovery = config["configurations"][
    "hive-site"
  ]["hive.server2.support.dynamic.service.discovery"]

spark_thriftserver_settings = get_spark_thriftserver_settings(
  config["configurations"], master_configs
)
spark2_transport_mode = spark_thriftserver_settings["transport_mode"]
spark2_http_path = spark_thriftserver_settings["http_path"]
spark2_ssl = spark_thriftserver_settings["ssl"]

spark2_thrift_server_hosts = spark_thriftserver_settings["host"]
spark2_hive_thrift_port = spark_thriftserver_settings["port"]
spark2_hive_principal = spark_thriftserver_settings["principal"]


# detect hbase details if installed
zookeeper_znode_parent = None
hbase_zookeeper_quorum = None
is_hbase_installed = False
if "hbase_master_hosts" in master_configs and "hbase-site" in config["configurations"]:
  is_hbase_installed = True
  zookeeper_znode_parent = config["configurations"]["hbase-site"][
    "zookeeper.znode.parent"
  ]
  hbase_zookeeper_quorum = config["configurations"]["hbase-site"][
    "hbase.zookeeper.quorum"
  ]

smoke_user = config["configurations"]["cluster-env"]["smokeuser"]
user_group = config["configurations"]["cluster-env"]["user_group"]

if security_enabled:
  zeppelin_kerberos_keytab = config["configurations"]["zeppelin-site"][
    "zeppelin.server.kerberos.keytab"
  ]
  zeppelin_kerberos_principal = config["configurations"]["zeppelin-site"][
    "zeppelin.server.kerberos.principal"
  ]

  smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]
  smokeuser_principal = config["configurations"]["cluster-env"][
    "smokeuser_principal_name"
  ]
  smokeuser_principal = smokeuser_principal.replace(
    "_HOST", socket.getfqdn().lower()
  )

interpreter_upgrade_value = str(
  default(
    "/configurations/zeppelin-site/zeppelin.interpreter.config.upgrade",
    "false",
  )
).strip().lower()
if interpreter_upgrade_value not in ("true", "false"):
  raise ValueError(
    "zeppelin-site/zeppelin.interpreter.config.upgrade must be true or false"
  )
zeppelin_interpreter_config_upgrade = interpreter_upgrade_value == "true"

exclude_interpreter_autoconfig = default(
  "/configurations/zeppelin-site/exclude.interpreter.autoconfig", None
)


hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
livy_server_settings = get_livy_server_settings(config["configurations"], master_configs)
livy2_livyserver_host = livy_server_settings["host"]
livy2_livyserver_port = livy_server_settings["port"]
livy2_livyserver_protocol = livy_server_settings["protocol"]

hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
dfs_type = default("/clusterLevelParams/dfs_type", "")

# create partial functions with common arguments for every HdfsResource call
# to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs,
  dfs_type=dfs_type,
)

mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
if "viewfs-mount-table" in config["configurations"]:
  xml_inclusion_file_name = "viewfs-mount-table.xml"
  mount_table = config["configurations"]["viewfs-mount-table"]

  if "content" in mount_table and mount_table["content"].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(
      external_dependency_conf, xml_inclusion_file_name
    )
    mount_table_content = mount_table["content"]
