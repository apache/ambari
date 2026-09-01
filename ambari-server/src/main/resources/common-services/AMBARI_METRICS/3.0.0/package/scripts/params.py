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

import configparser
import functools
import os

from ambari_commons.ambari_metrics_helper import (
  select_metric_collector_hosts_from_hostnames,
)
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.shell import quote_bash_args
from resource_management.libraries import functions
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.functions.substitute_vars import substitute_vars
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script

import status_params
from functions import calc_xmn_from_xms
from functions import check_append_heap_property
from functions import trim_heap_property
from metrics_utils import (
  local_filesystem_path,
  parse_bool,
  parse_finite_float,
  parse_host_port,
  parse_port,
  parse_positive_int,
  validate_absolute_path,
  validate_classpath,
  validate_host,
  validate_jvm_arguments,
  validate_name,
  validate_principal,
  validate_single_line,
)

from params_linux import *
# server configurations
config = Script.get_config()
ams_user = validate_name(ams_user, "Ambari Metrics user")
zk_principal_user = validate_name(zk_principal_user, "ZooKeeper principal user")
python_binary = validate_absolute_path(
  status_params.python_binary,
  "Metrics Monitor Python interpreter",
  allowed_roots=("/usr/bin", "/usr/local/bin", "/opt"),
)


def get_combined_memory_mb(value1, value2):
  part1 = parse_positive_int(trim_heap_property(str(value1), "m"), "HBase master heap")
  part2 = parse_positive_int(
    trim_heap_property(str(value2), "m"), "HBase RegionServer heap"
  )
  return f"{part1 + part2}m"

# AMBARI_METRICS data
ams_operation_mode = config["configurations"]["ams-site"][
  "timeline.metrics.service.operation.mode"
]
if ams_operation_mode not in ("embedded", "distributed"):
  raise Fail("Metrics service operation mode must be embedded or distributed")
is_ams_distributed = ams_operation_mode == "distributed"
ams_collector_script = "/usr/sbin/ambari-metrics-collector"
ams_collector_pid_dir = validate_absolute_path(
  status_params.ams_collector_pid_dir, "Metrics Collector PID directory"
)
ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
ams_collector_list = default("/clusterHostInfo/metrics_collector_hosts", [])
embedded_mode_multiple_instances = False

if not is_ams_distributed and len(ams_collector_list) > 1:
  embedded_mode_multiple_instances = True

set_instanceId = "false"
cluster_name = validate_single_line(config["clusterName"], "Cluster name", 255)
if (
  "cluster-env" in config["configurations"]
  and "metrics_collector_external_hosts" in config["configurations"]["cluster-env"]
):
  ams_collector_hosts = config["configurations"]["cluster-env"][
    "metrics_collector_external_hosts"
  ]
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(
    default("/clusterHostInfo/metrics_collector_hosts", [])
  )

ams_collector_host_list = [
  validate_host(host, "Metrics Collector host")
  for host in ams_collector_hosts.split(",")
  if host.strip()
]
if not ams_collector_host_list:
  raise Fail("At least one Metrics Collector host is required")
ams_collector_hosts = ",".join(ams_collector_host_list)

metric_collector_host = select_metric_collector_hosts_from_hostnames(
  ams_collector_hosts
)

if (
  "cluster-env" in config["configurations"]
  and "metrics_collector_external_port" in config["configurations"]["cluster-env"]
):
  metric_collector_port = str(
    parse_port(
      config["configurations"]["cluster-env"]["metrics_collector_external_port"],
      "External Metrics Collector port",
    )
  )
else:
  metric_collector_web_address = default(
    "/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188"
  )
  _, configured_collector_port = parse_host_port(
    metric_collector_web_address, "Metrics Collector web address"
  )
  metric_collector_port = str(configured_collector_port)

failover_strategy_blacklisted_interval = parse_positive_int(
  default("/configurations/ams-env/failover_strategy_blacklisted_interval", "600"),
  "Metrics Collector failover blacklist interval",
)
failover_strategy_blacklisted_interval_seconds = failover_strategy_blacklisted_interval
failover_strategy = validate_name(
  default("/configurations/ams-site/failover.strategy", "round-robin"),
  "Metrics Collector failover strategy",
)
metric_collector_http_policy = default(
  "/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY"
)
if metric_collector_http_policy not in ("HTTP_ONLY", "HTTPS_ONLY"):
  raise Fail("Metrics Collector HTTP policy must be HTTP_ONLY or HTTPS_ONLY")
if metric_collector_http_policy == "HTTPS_ONLY":
  metric_collector_https_enabled = True
  metric_collector_protocol = "https"
else:
  metric_collector_https_enabled = False
  metric_collector_protocol = "http"
metric_truststore_path = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.location", ""
)
metric_truststore_type = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.type", ""
)
metric_truststore_password = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.password", ""
)
metric_truststore_ca_certs = "ca.pem"

agent_cache_dir = config["agentLevelParams"]["agentCacheDir"]
service_package_folder = config["serviceLevelParams"]["service_package_folder"]
stack_name = validate_name(
  default("/clusterLevelParams/stack_name", "BIGTOP"), "Stack name"
)
dashboards_dirs = []
# Stack specific
dashboards_dirs.append(
  os.path.join(
    agent_cache_dir, service_package_folder, "files", "grafana-dashboards", stack_name
  )
)
# Default
dashboards_dirs.append(
  os.path.join(
    agent_cache_dir, service_package_folder, "files", "grafana-dashboards", "default"
  )
)

# Custom services
dashboards_dirs.append(
  os.path.join(agent_cache_dir, "dashboards", "grafana-dashboards")
)


def get_grafana_dashboard_defs():
  dashboard_defs = []
  for dashboards_dir in dashboards_dirs:
    if os.path.isdir(dashboards_dir) and not os.path.islink(dashboards_dir):
      for root, dirs, files in os.walk(dashboards_dir):
        dirs[:] = sorted(
          directory
          for directory in dirs
          if not os.path.islink(os.path.join(root, directory))
        )
        for file_name in sorted(files):
          dashboard_path = os.path.join(root, file_name)
          if (
            file_name.startswith("grafana-")
            and file_name.endswith(".json")
            and os.path.isfile(dashboard_path)
            and not os.path.islink(dashboard_path)
          ):
            dashboard_defs.append(dashboard_path)
  return sorted(dashboard_defs)


# find ambari version for grafana dashboards
def get_ambari_version():
  ambari_version = None
  ambari_agent_conf = "/etc/ambari-agent/conf/ambari-agent.ini"
  ambari_agent_config = configparser.RawConfigParser()
  if os.path.isfile(ambari_agent_conf):
    try:
      ambari_agent_config.read(ambari_agent_conf)
      data_dir = ambari_agent_config.get("agent", "prefix")
      ver_file = os.path.join(data_dir, "version")
      with open(ver_file, encoding="utf-8") as version_file:
        ambari_version = version_file.read().strip()
    except (OSError, configparser.Error) as error:
      Logger.info("Unable to determine ambari version from version file.")
      Logger.debug(f"Exception: {error}")
  return ambari_version


hostname = validate_host(config["agentLevelParams"]["hostname"], "Agent hostname")

ams_collector_log_dir = validate_absolute_path(
  config["configurations"]["ams-env"]["metrics_collector_log_dir"],
  "Metrics Collector log directory",
)
ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_log_dir = validate_absolute_path(
  config["configurations"]["ams-env"]["metrics_monitor_log_dir"],
  "Metrics Monitor log directory",
)

ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf"
ams_monitor_pid_dir = validate_absolute_path(
  status_params.ams_monitor_pid_dir, "Metrics Monitor PID directory"
)
ams_monitor_script = "/usr/sbin/ambari-metrics-monitor"

ams_grafana_script = "/usr/sbin/ambari-metrics-grafana"
ams_grafana_home_dir = "/usr/lib/ambari-metrics-grafana"
ams_grafana_log_dir = validate_absolute_path(
  default(
    "/configurations/ams-grafana-env/metrics_grafana_log_dir",
    "/var/log/ambari-metrics-grafana",
  ),
  "Grafana log directory",
)
ams_grafana_pid_dir = validate_absolute_path(
  status_params.ams_grafana_pid_dir, "Grafana PID directory"
)
ams_grafana_conf_dir = "/etc/ambari-metrics-grafana/conf"
ams_grafana_data_dir = validate_absolute_path(
  default(
    "/configurations/ams-grafana-env/metrics_grafana_data_dir",
    "/var/lib/ambari-metrics-grafana",
  ),
  "Grafana data directory",
)
ams_grafana_admin_user = validate_name(
  config["configurations"]["ams-grafana-env"]["metrics_grafana_username"],
  "Grafana administrator user",
)
ams_grafana_admin_pwd = config["configurations"]["ams-grafana-env"][
  "metrics_grafana_password"
]

metrics_grafana_hosts = default("/clusterHostInfo/metrics_grafana_hosts", None)
ams_grafana_host = None
if metrics_grafana_hosts:
  ams_grafana_host = validate_host(metrics_grafana_hosts[0], "Grafana host")
ams_grafana_port = parse_port(
  default("/configurations/ams-grafana-ini/port", 3000), "Grafana port"
)
ams_grafana_protocol = default("/configurations/ams-grafana-ini/protocol", "http")
if ams_grafana_protocol not in ("http", "https"):
  raise Fail("Grafana protocol must be http or https")
ams_grafana_cert_file = validate_absolute_path(
  default(
    "/configurations/ams-grafana-ini/cert_file",
    "/etc/ambari-metrics-grafana/conf/ams-grafana.crt",
  ),
  "Grafana certificate",
)
ams_grafana_cert_key = validate_absolute_path(
  default(
    "/configurations/ams-grafana-ini/cert_key",
    "/etc/ambari-metrics-grafana/conf/ams-grafana.key",
  ),
  "Grafana certificate key",
)
ams_grafana_ca_cert = default("/configurations/ams-grafana-ini/ca_cert", None)
if ams_grafana_protocol == "https":
  ams_grafana_ca_cert = validate_absolute_path(
    ams_grafana_ca_cert, "Grafana CA certificate"
  )

grafana_connect_attempts = parse_positive_int(
  default("/configurations/ams-grafana-env/metrics_grafana_connect_attempts", 15),
  "Grafana connection attempts",
)
grafana_connect_retry_delay = parse_positive_int(
  default("/configurations/ams-grafana-env/metrics_grafana_connect_retry_delay", 20),
  "Grafana retry delay",
)
grafana_request_timeout = parse_positive_int(
  default("/configurations/ams-grafana-env/metrics_grafana_request_timeout", 10),
  "Grafana request timeout",
)

ams_hbase_home_dir = "/usr/lib/ams-hbase/"
ams_hbase_real_conf_dir = "/usr/lib/ams-hbase/conf"

ams_hbase_init_check_enabled = parse_bool(
  default("/configurations/ams-site/timeline.metrics.hbase.init.check.enabled", True),
  "HBase initialization check",
)

# hadoop params

hbase_user = status_params.hbase_user
smoke_user = validate_name(
  config["configurations"]["cluster-env"]["smokeuser"], "Smoke test user"
)
hbase_root_dir = config["configurations"]["ams-hbase-site"]["hbase.rootdir"]
hbase_pid_dir = validate_absolute_path(
  status_params.hbase_pid_dir, "AMS HBase PID directory"
)

is_hbase_distributed = parse_bool(
  config["configurations"]["ams-hbase-site"]["hbase.cluster.distributed"],
  "hbase.cluster.distributed",
)
if is_hbase_distributed != is_ams_distributed:
  raise Fail(
    "Metrics service operation mode and hbase.cluster.distributed must agree"
  )
is_local_fs_rootdir = hbase_root_dir.startswith("file:")
local_hbase_root_dir = (
  local_filesystem_path(hbase_root_dir, "HBase root directory")
  if is_local_fs_rootdir
  else None
)

# security is disabled for embedded mode, when HBase is backed by file
security_enabled = is_hbase_distributed and parse_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "Cluster security setting",
)

java_home = validate_absolute_path(
  config["ambariLevelParams"]["java_home"], "Java home"
)
# not supporting 32 bit jdk.
java64_home = java_home

metrics_collector_heapsize = default(
  "/configurations/ams-env/metrics_collector_heapsize", "512"
)
metrics_report_interval = default(
  "/configurations/ams-site/timeline.metrics.sink.report.interval", 60
)
metrics_collection_period = default(
  "/configurations/ams-site/timeline.metrics.sink.collection.period", 10
)
metrics_report_interval = parse_positive_int(
  metrics_report_interval, "Metrics report interval"
)
metrics_collection_period = parse_positive_int(
  metrics_collection_period, "Metrics collection period"
)
skip_disk_metrics_patterns = validate_single_line(
  default("/configurations/ams-env/timeline.metrics.skip.disk.metrics.patterns", "None"),
  "Skipped disk metrics patterns",
)
skip_network_interfaces_patterns = validate_single_line(
  default(
    "/configurations/ams-env/timeline.metrics.skip.network.interfaces.patterns", "None"
  ),
  "Skipped network interface patterns",
)
skip_virtual_interfaces = parse_bool(
  default("/configurations/ams-env/timeline.metrics.skip.virtual.interfaces", False),
  "Skip virtual interfaces",
)
ams_classpath_additional = validate_classpath(
  default("/configurations/ams-env/ams_classpath_additional", ""),
  "Metrics Collector additional classpath",
)

hbase_log_dir = validate_absolute_path(
  config["configurations"]["ams-hbase-env"]["hbase_log_dir"],
  "AMS HBase log directory",
)
hbase_classpath_additional = validate_classpath(
  default("/configurations/ams-hbase-env/hbase_classpath_additional", ""),
  "AMS HBase additional classpath",
)
master_heapsize = config["configurations"]["ams-hbase-env"]["hbase_master_heapsize"]
regionserver_heapsize = config["configurations"]["ams-hbase-env"][
  "hbase_regionserver_heapsize"
]

# Check if hbase java options already have appended "m". If Yes, remove the trailing m.
metrics_collector_heapsize = check_append_heap_property(
  str(metrics_collector_heapsize), "m"
)
master_heapsize = check_append_heap_property(str(master_heapsize), "m")
regionserver_heapsize = check_append_heap_property(str(regionserver_heapsize), "m")

host_in_memory_aggregation = parse_bool(
  default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", False),
  "Host in-memory aggregation",
)
host_in_memory_aggregation_port = parse_port(
  default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888),
  "Host in-memory aggregation port",
)
is_aggregation_https_enabled = False
aggregation_http_policy = default(
  "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy",
  "HTTP_ONLY",
)
if aggregation_http_policy not in ("HTTP_ONLY", "HTTPS_ONLY"):
  raise Fail("Host aggregation HTTP policy must be HTTP_ONLY or HTTPS_ONLY")
if aggregation_http_policy == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = "https"
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = "http"

if metric_collector_https_enabled or is_aggregation_https_enabled:
  metric_truststore_path = validate_absolute_path(
    metric_truststore_path, "Metrics truststore path"
  )
  if metric_truststore_type.lower() not in ("jks", "pkcs12", "p12"):
    raise Fail("Metrics truststore type must be JKS or PKCS12")
  if not metric_truststore_password:
    raise Fail("Metrics truststore password must not be empty when HTTPS is enabled")

host_in_memory_aggregation_jvm_arguments = validate_jvm_arguments(
  default(
    "/configurations/ams-env/timeline.metrics.host.inmemory.aggregation.jvm.arguments",
    "-Xmx256m -Xms128m",
  ),
  "Host in-memory aggregation JVM arguments",
)

regionserver_xmn_max = default(
  "/configurations/ams-hbase-env/hbase_regionserver_xmn_max", None
)
if regionserver_xmn_max:
  regionserver_xmn_max = int(trim_heap_property(str(regionserver_xmn_max), "m"))
  regionserver_xmn_percent = parse_finite_float(
    config["configurations"]["ams-hbase-env"]["hbase_regionserver_xmn_ratio"],
    "HBase RegionServer young generation ratio",
    maximum=1.0,
  )
  regionserver_xmn_size = calc_xmn_from_xms(
    regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max
  )
else:
  regionserver_xmn_size = config["configurations"]["ams-hbase-env"][
    "regionserver_xmn_size"
  ]

hbase_master_xmn_size = config["configurations"]["ams-hbase-env"][
  "hbase_master_xmn_size"
]
hbase_master_xmn_size = check_append_heap_property(str(hbase_master_xmn_size), "m")
regionserver_xmn_size = check_append_heap_property(str(regionserver_xmn_size), "m")

# Choose heap size for embedded mode as sum of master + regionserver
if not is_hbase_distributed:
  hbase_heapsize = get_combined_memory_mb(master_heapsize, regionserver_heapsize)
else:
  hbase_heapsize = master_heapsize

max_open_files_limit = parse_positive_int(
  default("/configurations/ams-hbase-env/max_open_files_limit", "32768"),
  "AMS HBase open file limit",
)

cluster_zookeeper_quorum_hosts = ",".join(
  config["clusterHostInfo"]["zookeeper_server_hosts"]
)
if (
  "zoo.cfg" in config["configurations"]
  and "clientPort" in config["configurations"]["zoo.cfg"]
):
  cluster_zookeeper_clientPort = config["configurations"]["zoo.cfg"]["clientPort"]
else:
  cluster_zookeeper_clientPort = "2181"

if not is_hbase_distributed:
  zookeeper_quorum_hosts = "localhost"
  zookeeper_clientPort = "61181"
else:
  zookeeper_quorum_hosts = cluster_zookeeper_quorum_hosts
  zookeeper_clientPort = cluster_zookeeper_clientPort

ams_checkpoint_dir = validate_absolute_path(
  config["configurations"]["ams-site"]["timeline.metrics.aggregator.checkpoint.dir"],
  "Metrics Collector checkpoint directory",
)
_hbase_tmp_dir = config["configurations"]["ams-hbase-site"]["hbase.tmp.dir"]
hbase_tmp_dir = validate_absolute_path(
  substitute_vars(_hbase_tmp_dir, config["configurations"]["ams-hbase-site"]),
  "AMS HBase temporary directory",
)
_zookeeper_data_dir = config["configurations"]["ams-hbase-site"][
  "hbase.zookeeper.property.dataDir"
]
zookeeper_data_dir = validate_absolute_path(
  substitute_vars(_zookeeper_data_dir, config["configurations"]["ams-hbase-site"]),
  "Embedded ZooKeeper data directory",
)
_local_dir_conf = default(
  "/configurations/ams-hbase-site/hbase.local.dir", "${hbase.tmp.dir}/local"
)
local_dir = validate_absolute_path(
  substitute_vars(_local_dir_conf, config["configurations"]["ams-hbase-site"]),
  "Embedded HBase local directory",
)

phoenix_max_global_mem_percent = default(
  "/configurations/ams-site/phoenix.query.maxGlobalMemoryPercentage", "20"
)
phoenix_client_spool_dir = default(
  "/configurations/ams-site/phoenix.spool.directory", "/tmp"
)
phoenix_server_spool_dir = default(
  "/configurations/ams-hbase-site/phoenix.spool.directory", "/tmp"
)
# Substitute vars if present
phoenix_client_spool_dir = substitute_vars(
  phoenix_client_spool_dir, config["configurations"]["ams-hbase-site"]
)
phoenix_server_spool_dir = substitute_vars(
  phoenix_server_spool_dir, config["configurations"]["ams-hbase-site"]
)
if phoenix_client_spool_dir != "/tmp":
  phoenix_client_spool_dir = validate_absolute_path(
    phoenix_client_spool_dir,
    "Phoenix client spool directory",
    allowed_roots=("/tmp", "/var/lib", "/var/tmp", "/data", "/mnt"),
  )
if phoenix_server_spool_dir != "/tmp":
  phoenix_server_spool_dir = validate_absolute_path(
    phoenix_server_spool_dir,
    "Phoenix server spool directory",
    allowed_roots=("/tmp", "/var/lib", "/var/tmp", "/data", "/mnt"),
  )

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

rs_hosts = ["localhost"]

user_group = validate_name(
  config["configurations"]["cluster-env"]["user_group"], "Service group"
)

kinit_path_local = functions.get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
monitor_kinit_cmd = ""
klist_path_local = functions.get_klist_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
klist_cmd = ""

if security_enabled:
  _hostname_lowercase = config["agentLevelParams"]["hostname"].lower()
  client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
  smoke_user_keytab = validate_absolute_path(
    config["configurations"]["cluster-env"]["smokeuser_keytab"],
    "Smoke user keytab",
  )
  smoke_user_princ = validate_principal(
    config["configurations"]["cluster-env"]["smokeuser_principal_name"],
    "Smoke user principal",
  )
  hbase_user_keytab = validate_absolute_path(
    config["configurations"]["ams-hbase-env"]["hbase_user_keytab"],
    "AMS HBase user keytab",
  )

  ams_collector_jaas_config_file = format("{hbase_conf_dir}/ams_collector_jaas.conf")
  ams_collector_keytab_path = validate_absolute_path(
    config["configurations"]["ams-hbase-security-site"]["hbase.myclient.keytab"],
    "Metrics Collector keytab",
  )
  ams_collector_jaas_princ = validate_principal(
    config["configurations"]["ams-hbase-security-site"][
      "hbase.myclient.principal"
    ].replace("_HOST", _hostname_lowercase),
    "Metrics Collector principal",
  )

  ams_zookeeper_jaas_config_file = format("{hbase_conf_dir}/ams_zookeeper_jaas.conf")
  ams_zookeeper_keytab = validate_absolute_path(
    config["configurations"]["ams-hbase-security-site"]["ams.zookeeper.keytab"],
    "AMS ZooKeeper keytab",
  )
  ams_zookeeper_principal_name = validate_principal(
    config["configurations"]["ams-hbase-security-site"][
      "ams.zookeeper.principal"
    ].replace("_HOST", _hostname_lowercase),
    "AMS ZooKeeper principal",
  )

  master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
  master_keytab_path = validate_absolute_path(
    config["configurations"]["ams-hbase-security-site"][
      "hbase.master.keytab.file"
    ],
    "AMS HBase master keytab",
  )
  master_jaas_princ = validate_principal(
    config["configurations"]["ams-hbase-security-site"][
      "hbase.master.kerberos.principal"
    ].replace("_HOST", _hostname_lowercase),
    "AMS HBase master principal",
  )

  regionserver_jaas_config_file = format(
    "{hbase_conf_dir}/hbase_regionserver_jaas.conf"
  )
  regionserver_keytab_path = validate_absolute_path(
    config["configurations"]["ams-hbase-security-site"][
      "hbase.regionserver.keytab.file"
    ],
    "AMS HBase RegionServer keytab",
  )
  regionserver_jaas_princ = validate_principal(
    config["configurations"]["ams-hbase-security-site"][
      "hbase.regionserver.kerberos.principal"
    ].replace("_HOST", _hostname_lowercase),
    "AMS HBase RegionServer principal",
  )

  # Monitor SPNEGO configs
  ams_monitor_keytab = None
  if ("ams-hbase-security-site" in config["configurations"]) and (
    "ams.monitor.keytab" in config["configurations"]["ams-hbase-security-site"]
  ):
    ams_monitor_keytab = config["configurations"]["ams-hbase-security-site"][
      "ams.monitor.keytab"
    ]

  ams_monitor_principal = None
  if ("ams-hbase-security-site" in config["configurations"]) and (
    "ams.monitor.principal" in config["configurations"]["ams-hbase-security-site"]
  ):
    ams_monitor_principal = config["configurations"]["ams-hbase-security-site"][
      "ams.monitor.principal"
    ]

  if ams_monitor_keytab and ams_monitor_principal:
    ams_monitor_keytab = validate_absolute_path(
      ams_monitor_keytab, "Metrics Monitor keytab"
    )
    monitor_credential_cache = os.path.join(
      status_params.ams_monitor_pid_dir, "ambari-metrics-monitor.ccache"
    )
    monitor_cache_name = f"FILE:{monitor_credential_cache}"
    resolved_monitor_principal = validate_principal(
      ams_monitor_principal.replace("_HOST", _hostname_lowercase),
      "Metrics Monitor principal",
    )
    monitor_kinit_cmd = " ".join(
      quote_bash_args(argument)
      for argument in (
        kinit_path_local,
        "-c",
        monitor_cache_name,
        "-kt",
        ams_monitor_keytab,
        resolved_monitor_principal,
      )
    )
    klist_cmd = " ".join(
      quote_bash_args(argument)
      for argument in (klist_path_local, "-c", monitor_cache_name)
    )

# Ambari metrics log4j settings
ams_hbase_log_maxfilesize = default(
  "configurations/ams-hbase-log4j/ams_hbase_log_maxfilesize", 256
)
ams_hbase_log_maxbackupindex = default(
  "configurations/ams-hbase-log4j/ams_hbase_log_maxbackupindex", 20
)
ams_hbase_security_log_maxfilesize = default(
  "configurations/ams-hbase-log4j/ams_hbase_security_log_maxfilesize", 256
)
ams_hbase_security_log_maxbackupindex = default(
  "configurations/ams-hbase-log4j/ams_hbase_security_log_maxbackupindex", 20
)
ams_log_max_backup_size = default(
  "configurations/ams-log4j/ams_log_max_backup_size", 80
)
ams_log_number_of_backup_files = default(
  "configurations/ams-log4j/ams_log_number_of_backup_files", 60
)

# log4j.properties
if ("ams-hbase-log4j" in config["configurations"]) and (
  "content" in config["configurations"]["ams-hbase-log4j"]
):
  hbase_log4j_props = config["configurations"]["ams-hbase-log4j"]["content"]
else:
  hbase_log4j_props = None

if ("ams-log4j" in config["configurations"]) and (
  "content" in config["configurations"]["ams-log4j"]
):
  log4j_props = config["configurations"]["ams-log4j"]["content"]
else:
  log4j_props = None

hbase_env_sh_template = config["configurations"]["ams-hbase-env"]["content"]
ams_env_sh_template = config["configurations"]["ams-env"]["content"]
ams_grafana_env_sh_template = config["configurations"]["ams-grafana-env"]["content"]
ams_grafana_ini_template = config["configurations"]["ams-grafana-ini"]["content"]

hbase_staging_dir = default(
  "/configurations/ams-hbase-site/hbase.bulkload.staging.dir", "/amshbase/staging"
)
skip_create_hbase_root_dir = parse_bool(
  default("/configurations/ams-site/timeline.metrics.skip.create.hbase.root.dir", False),
  "Skip HBase root directory creation",
)
hbase_wal_dir = default("/configurations/ams-hbase-site/hbase.wal.dir", None)
if hbase_wal_dir:
  if hbase_wal_dir.startswith("file:") or hbase_wal_dir.startswith("/"):
    hbase_wal_dir = local_filesystem_path(hbase_wal_dir, "HBase WAL directory")
  else:
    hbase_wal_dir = None

# for create_hdfs_directory
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
if security_enabled:
  hdfs_user_keytab = validate_absolute_path(hdfs_user_keytab, "HDFS user keytab")
  hdfs_user = validate_name(hdfs_user, "HDFS user")
  hdfs_principal_name = validate_principal(hdfs_principal_name, "HDFS principal")


clusterHostInfoDict = config["clusterHostInfo"]
min_hadoop_sink_version = default(
  "/configurations/ams-env/min_ambari_metrics_hadoop_sink_version", "2.7.0.0"
)

hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
dfs_type = default("/clusterLevelParams/dfs_type", "")

# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
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
  immutable_paths=get_not_managed_resources(),
  dfs_type=dfs_type,
)
