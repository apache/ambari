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

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.resources.hdfs_resource import (
  HdfsResource as HdfsResourceBase,
)
from resource_management.libraries.script.script import Script

import spark_utils
import status_params


config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
configurations = config["configurations"]

stack_name = config["clusterLevelParams"]["stack_name"]
stack_version = config["clusterLevelParams"]["stack_version"]
spark_utils.validate_bigtop_stack(stack_name, stack_version)
version = default("/commandParams/version", None)

spark_home = "/usr/lib/spark"
spark_conf_dir = "/etc/spark/conf"
spark_lib_dir = "/var/lib/spark"
spark_class = os.path.join(spark_home, "bin", "spark-class")
spark_submit = os.path.join(spark_home, "bin", "spark-submit")
spark_beeline = os.path.join(spark_home, "bin", "beeline")
spark_defaults_file = os.path.join(spark_conf_dir, "spark-defaults.conf")
hadoop_home = "/usr/lib/hadoop"
hadoop_bin_dir = os.path.join(hadoop_home, "bin")
hadoop_conf_dir = "/etc/hadoop/conf"
hive_home = "/usr/lib/hive"

spark_user = status_params.spark_user
spark_group = status_params.spark_group
user_group = status_params.user_group
spark_pid_dir = status_params.spark_pid_dir
spark_history_server_pid_file = status_params.spark_history_server_pid_file
spark_thrift_server_pid_file = status_params.spark_thrift_server_pid_file
spark_log_dir = configurations["spark-env"]["spark_log_dir"]
spark_daemon_memory = spark_utils.bounded_int(
  configurations["spark-env"]["spark_daemon_memory"],
  "Spark daemon memory",
  256,
  1048576,
)
spark_utils.validate_service_directory(spark_log_dir, "Spark log directory")
spark_history_store_path = default(
  "/configurations/spark-defaults/spark.history.store.path",
  "/var/lib/spark/shs_db",
)
spark_utils.validate_service_directory(spark_history_store_path, "Spark history store directory")
spark_hdfs_user_dir = f"/user/{spark_user}"

java_home = config["ambariLevelParams"]["java_home"]
spark_utils.validate_absolute_path(java_home, "Java home")
security_enabled = spark_utils.as_bool(
  configurations["cluster-env"]["security_enabled"],
  "cluster security setting",
)
fqdn = spark_utils.validate_host(socket.getfqdn(), "local host")
spnego_principal = None
spnego_keytab = None
if security_enabled:
  spnego_principal = configurations["spark-defaults"][
    "history.server.spnego.kerberos.principal"
  ].replace("_HOST", fqdn)
  spnego_keytab = configurations["spark-defaults"]["history.server.spnego.keytab.file"]
  spark_utils.validate_principal(spnego_principal, "Spark History Server SPNEGO principal")
  spark_utils.validate_absolute_path(spnego_keytab, "Spark History Server SPNEGO keytab")

spark_history_dir = spark_utils.validate_hdfs_uri(
  configurations["spark-defaults"]["spark.history.fs.logDirectory"],
  "Spark history directory",
)
spark_history_ui_port = spark_utils.bounded_int(
  configurations["spark-defaults"]["spark.history.ui.port"],
  "Spark History Server UI port",
  1,
  65535,
)
spark_history_scheme = (
  "https"
  if spark_utils.as_bool(
    default(
      "/configurations/spark-defaults/spark.ssl.historyServer.enabled",
      False,
    ),
    "Spark History Server SSL setting",
  )
  else "http"
)
history_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", [])
has_history_server = bool(history_hosts)
spark_history_server_host = spark_utils.validate_host(
  history_hosts[0] if history_hosts else "localhost",
  "Spark History Server host",
)
spark_history_kerberos_principal = configurations["spark-defaults"].get(
  "spark.history.kerberos.principal"
)
spark_history_kerberos_keytab = configurations["spark-defaults"].get(
  "spark.history.kerberos.keytab"
)
spark_thrift_kerberos_principal = configurations["spark-defaults"].get("spark.kerberos.principal")
spark_thrift_kerberos_keytab = configurations["spark-defaults"].get("spark.kerberos.keytab")
if security_enabled:
  spark_history_kerberos_principal = spark_history_kerberos_principal.replace("_HOST", fqdn)
  spark_utils.validate_principal(spark_history_kerberos_principal, "Spark History Server principal")
  spark_utils.validate_absolute_path(spark_history_kerberos_keytab, "Spark History Server keytab")
  if spark_thrift_kerberos_principal and spark_thrift_kerberos_principal != "none":
    spark_thrift_kerberos_principal = spark_thrift_kerberos_principal.replace("_HOST", fqdn)
    spark_utils.validate_principal(
      spark_thrift_kerberos_principal,
      "Spark Thrift Server application principal",
    )
    spark_utils.validate_absolute_path(
      spark_thrift_kerberos_keytab,
      "Spark Thrift Server application keytab",
    )

spark_env_sh = configurations["spark-env"]["content"]
spark_log4j2_properties = configurations["spark-log4j-properties"]["content"]
spark_metrics_properties = configurations["spark-metrics-properties"]["content"]
spark_thrift_fairscheduler_content = configurations[
  "spark-thrift-fairscheduler"
]["fairscheduler_content"]
spark_thrift_cmd_opts = spark_utils.validate_thrift_options(
  spark_utils.parse_command_options(
    configurations["spark-env"].get("spark_thrift_cmd_opts", ""),
    "Spark Thrift Server options",
  )
)

spark_defaults = configurations["spark-defaults"]
spark_warehouse_dir = default("/configurations/hive-site/hive.metastore.warehouse.dir", None)
is_hive_installed = "hive-site" in configurations
spark_hive_properties = {}
if is_hive_installed:
  for name in (
    "hive.metastore.uris",
    "hive.metastore.client.socket.timeout",
    "hive.metastore.sasl.enabled",
    "hive.metastore.kerberos.keytab.file",
    "hive.metastore.kerberos.principal",
  ):
    if name in configurations["hive-site"]:
      spark_hive_properties[name] = configurations["hive-site"][name]
  spark_hive_properties.update(configurations["spark-hive-site-override"])

spark_transport_mode = configurations["spark-hive-site-override"][
  "hive.server2.transport.mode"
].strip().lower()
if spark_transport_mode not in ("binary", "http"):
  raise Fail("Spark Thrift Server transport mode must be binary or http")
port_property = (
  "hive.server2.thrift.port"
  if spark_transport_mode == "binary"
  else "hive.server2.thrift.http.port"
)
spark_thrift_port = spark_utils.bounded_int(
  configurations["spark-hive-site-override"][port_property],
  "Spark Thrift Server port",
  1,
  65535,
)
spark_thrift_endpoint = configurations["spark-hive-site-override"].get(
  "hive.server2.thrift.http.path", "cliservice"
)
if (
  not spark_thrift_endpoint
  or "/" in spark_thrift_endpoint
  or any(character.isspace() for character in spark_thrift_endpoint)
):
  raise Fail("Spark Thrift Server HTTP endpoint is invalid")
spark_thrift_ssl_enabled = spark_utils.as_bool(
  configurations["spark-hive-site-override"].get(
    "hive.server2.use.SSL", False
  ),
  "Spark Thrift Server SSL setting",
)
spark_thriftserver_hosts = [
  spark_utils.validate_host(host, "Spark Thrift Server host")
  for host in default("/clusterHostInfo/spark_thriftserver_hosts", [])
]
has_spark_thriftserver = bool(spark_thriftserver_hosts)

kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
smoke_user = configurations["cluster-env"]["smokeuser"]
spark_utils.validate_user(smoke_user, "Spark smoke user")
smoke_user_keytab = configurations["cluster-env"].get("smokeuser_keytab")
smokeuser_principal = configurations["cluster-env"].get("smokeuser_principal_name")
hive_kerberos_keytab = configurations["spark-hive-site-override"].get(
  "hive.server2.authentication.kerberos.keytab"
)
default_hive_kerberos_principal = configurations[
  "spark-hive-site-override"
].get("hive.server2.authentication.kerberos.principal")
if security_enabled:
  spark_utils.validate_keytab(smoke_user_keytab, "Spark smoke user keytab")
  spark_utils.validate_principal(smokeuser_principal, "Spark smoke user principal")
  if default_hive_kerberos_principal:
    spark_utils.validate_principal(
      default_hive_kerberos_principal.replace(
        "_HOST", spark_history_server_host
      ),
      "Spark Thrift Server principal",
    )

hdfs_user = configurations["hadoop-env"]["hdfs_user"]
hdfs_user_keytab = configurations["hadoop-env"]["hdfs_user_keytab"]
hdfs_principal_name = configurations["hadoop-env"]["hdfs_principal_name"]
hdfs_site = configurations["hdfs-site"]
default_fs = configurations["core-site"]["fs.defaultFS"]
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"
dfs_type = default("/clusterLevelParams/dfs_type", "")

HdfsResource = functools.partial(
  HdfsResourceBase,
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
