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

import os


def get_spark_home(configured_home, stack_root, use_current):
  if use_current:
    return os.path.join(stack_root, "current", "spark-client")
  return configured_home


def get_spark_thriftserver_settings(configurations, cluster_host_info):
  spark_config = configurations.get("spark-hive-site-override", {})
  hosts = cluster_host_info.get("spark_thriftserver_hosts", [])
  transport_mode = spark_config.get("hive.server2.transport.mode", "binary")
  port_property = (
    "hive.server2.thrift.http.port"
    if str(transport_mode).lower() == "http"
    else "hive.server2.thrift.port"
  )
  port = spark_config.get(port_property)
  port_available = port is not None and bool(str(port).strip())
  ssl_enabled = str(spark_config.get("hive.server2.use.SSL", False)).lower() == "true"

  return {
    "host": str(hosts[0]) if hosts and port_available else None,
    "port": str(port) if port_available else None,
    "principal": spark_config.get("hive.server2.authentication.kerberos.principal"),
    "transport_mode": transport_mode,
    "http_path": spark_config.get("hive.server2.http.endpoint"),
    "ssl": ssl_enabled,
  }


def get_livy_server_settings(configurations, cluster_host_info):
  livy_config = configurations.get("livy-conf", {})
  hosts = cluster_host_info.get("livy_server_hosts", [])
  livy_available = "livy-conf" in configurations and bool(hosts)

  return {
    "host": str(hosts[0]) if livy_available else None,
    "port": str(livy_config.get("livy.server.port", 8999)),
    "protocol": "https" if str(livy_config.get("livy.keystore", "")).strip() else "http",
  }
