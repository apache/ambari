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

from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format

from metrics_utils import parse_bool, validate_absolute_path, validate_name
from params_linux import *

hbase_pid_dir = validate_absolute_path(
  config["configurations"]["ams-hbase-env"]["hbase_pid_dir"],
  "AMS HBase PID directory",
)
hbase_user = ams_user
user_group = validate_name(
  config["configurations"]["cluster-env"]["user_group"], "Service group"
)
ams_collector_pid_dir = validate_absolute_path(
  config["configurations"]["ams-env"]["metrics_collector_pid_dir"],
  "Metrics Collector PID directory",
)
ams_monitor_pid_dir = validate_absolute_path(
  config["configurations"]["ams-env"]["metrics_monitor_pid_dir"],
  "Metrics Monitor PID directory",
)
ams_grafana_pid_dir = validate_absolute_path(
  config["configurations"]["ams-grafana-env"]["metrics_grafana_pid_dir"],
  "Grafana PID directory",
)

monitor_pid_file = format("{ams_monitor_pid_dir}/ambari-metrics-monitor.pid")
grafana_pid_file = format("{ams_grafana_pid_dir}/grafana-server.pid")

is_hbase_distributed = parse_bool(
  config["configurations"]["ams-hbase-site"]["hbase.cluster.distributed"],
  "hbase.cluster.distributed",
)
security_enabled = parse_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "Cluster security setting",
)
ams_hbase_conf_dir = hbase_conf_dir
python_binary = validate_absolute_path(
  default("/configurations/ams-env/metrics_monitor_python_binary", "/usr/bin/python3.9"),
  "Metrics Monitor Python interpreter",
  allowed_roots=("/usr/bin", "/usr/local/bin", "/opt"),
)
