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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.expect import expect
from metrics_utils import parse_positive_int

config = Script.get_config()

ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf"
ams_user = config["configurations"]["ams-env"]["ambari_metrics_user"]

ams_grafana_pid_dir = config["configurations"]["ams-grafana-env"][
  "metrics_grafana_pid_dir"
]

hadoop_bin_dir = "/usr/bin"
daemon_script = "/usr/lib/ams-hbase/bin/hbase-daemon.sh"

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hbase_conf_dir = "/etc/ams-hbase/conf"

limits_conf_dir = "/etc/security/limits.d"

dfs_type = default("/clusterLevelParams/dfs_type", "")

zk_principal_user = default(
  "/configurations/zookeeper-env/zookeeper_principal_name",
  "zookeeper/_HOST@EXAMPLE.COM",
).split("/")[0]

hbase_regionserver_shutdown_timeout = parse_positive_int(
  expect("/configurations/ams-hbase-env/hbase_regionserver_shutdown_timeout", int, 30),
  "HBase RegionServer shutdown timeout",
)

grafana_pid_file = format("{ams_grafana_pid_dir}/grafana-server.pid")
