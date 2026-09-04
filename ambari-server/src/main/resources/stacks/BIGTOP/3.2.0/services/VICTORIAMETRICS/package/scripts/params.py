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
from urllib.parse import quote

from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script


config = Script.get_config()


def get_property(config_type, name, fallback):
  return default("/configurations/{0}/{1}".format(config_type, name), fallback)


def as_bool(value):
  return str(value).lower() == "true"


def get_hosts(component):
  key = component.lower() + "_hosts"
  return sorted(set(config.get("clusterHostInfo", {}).get(key, [])))


role = default("/role", "VICTORIAMETRICS_SERVER")
supported_roles = {
  "VICTORIAMETRICS_SERVER",
  "VMSTORAGE",
  "VMINSERT",
  "VMSELECT",
  "VMAGENT",
  "VMAUTH",
}
if role not in supported_roles:
  role = "VICTORIAMETRICS_SERVER"

hostname = config["agentLevelParams"]["hostname"]
cluster_name = config["clusterName"]
ambari_server_host = config["ambariLevelParams"]["ambari_server_host"]

victoriametrics_user = get_property(
  "victoriametrics-env", "victoriametrics_user", "ambari-metrics"
)
victoriametrics_group = get_property(
  "victoriametrics-env", "victoriametrics_group", "ambari-metrics"
)
victoriametrics_binary_dir = get_property(
  "victoriametrics-env",
  "victoriametrics_binary_dir",
  "/usr/lib/ambari-metrics/providers/victoriametrics/bin",
)
victoriametrics_pid_dir = get_property(
  "victoriametrics-env",
  "victoriametrics_pid_dir",
  "/var/run/ambari-metrics/victoriametrics",
)
victoriametrics_log_dir = get_property(
  "victoriametrics-env",
  "victoriametrics_log_dir",
  "/var/log/ambari-metrics/victoriametrics",
)
victoriametrics_config_dir = get_property(
  "victoriametrics-env",
  "victoriametrics_config_dir",
  "/etc/ambari-metrics/victoriametrics",
)

deployment_mode = get_property("victoriametrics", "deployment_mode", "single")
retention_period = get_property("victoriametrics", "retention_period", "12")
replication_factor = int(get_property("victoriametrics", "replication_factor", 1))
dedup_min_scrape_interval = get_property(
  "victoriametrics", "dedup_min_scrape_interval", "1ms"
)
tenant_id = get_property("victoriametrics", "tenant_id", "0")

server_http_port = int(get_property("victoriametrics", "server_http_port", 8428))
server_data_dir = get_property(
  "victoriametrics",
  "server_data_dir",
  "/var/lib/ambari-metrics/victoriametrics/server-data",
)
vmstorage_http_port = int(
  get_property("victoriametrics", "vmstorage_http_port", 8482)
)
vmstorage_vminsert_port = int(
  get_property("victoriametrics", "vmstorage_vminsert_port", 8400)
)
vmstorage_vmselect_port = int(
  get_property("victoriametrics", "vmstorage_vmselect_port", 8401)
)
vmstorage_data_dir = get_property(
  "victoriametrics",
  "vmstorage_data_dir",
  "/var/lib/ambari-metrics/victoriametrics/storage-data",
)
vminsert_http_port = int(get_property("victoriametrics", "vminsert_http_port", 8480))
vmselect_http_port = int(get_property("victoriametrics", "vmselect_http_port", 8481))
vmselect_cache_dir = get_property(
  "victoriametrics",
  "vmselect_cache_dir",
  "/var/lib/ambari-metrics/victoriametrics/select-cache",
)

vmagent_http_port = int(
  get_property("victoriametrics-scrape", "vmagent_http_port", 8429)
)
vmagent_data_dir = get_property(
  "victoriametrics-scrape",
  "vmagent_data_dir",
  "/var/lib/ambari-metrics/victoriametrics/vmagent-data",
)
scrape_interval = get_property("victoriametrics-scrape", "scrape_interval", "30s")
scrape_timeout = get_property("victoriametrics-scrape", "scrape_timeout", "10s")
http_sd_refresh_interval = get_property(
  "victoriametrics-scrape", "http_sd_refresh_interval", "30s"
)
vmagent_replication_factor = int(
  get_property("victoriametrics-scrape", "vmagent_replication_factor", 1)
)
deduplication_enabled = (
  replication_factor > 1 or vmagent_replication_factor > 1
)
effective_dedup_min_scrape_interval = (
  scrape_interval
  if vmagent_replication_factor > 1
  else dedup_min_scrape_interval
)
ambari_sd_protocol = get_property(
  "victoriametrics-scrape", "ambari_sd_protocol", "http"
)
ambari_sd_port = int(get_property("victoriametrics-scrape", "ambari_sd_port", 8080))
managed_discovery_identity = as_bool(
  get_property("victoriametrics-scrape", "managed_discovery_identity", "true")
)
ambari_sd_username = get_property(
  "victoriametrics-scrape", "ambari_sd_username", "prometheus-scraper"
)
ambari_sd_password = get_property(
  "victoriametrics-scrape", "ambari_sd_password", ""
)
ambari_sd_ca_file = get_property(
  "victoriametrics-scrape", "ambari_sd_ca_file", ""
)
ambari_sd_tls_insecure_skip_verify = as_bool(
  get_property(
    "victoriametrics-scrape", "ambari_sd_tls_insecure_skip_verify", "false"
  )
)
remote_write_url_override = get_property(
  "victoriametrics-scrape", "remote_write_url", ""
)
remote_write_max_disk_usage = get_property(
  "victoriametrics-scrape", "remote_write_max_disk_usage", "10GB"
)

vmauth_http_port = int(
  get_property("victoriametrics-auth", "vmauth_http_port", 8427)
)
require_authentication = as_bool(
  get_property("victoriametrics-auth", "require_authentication", "false")
)
api_username = get_property("victoriametrics-auth", "api_username", "")
api_password = get_property("victoriametrics-auth", "api_password", "")

server_hosts = get_hosts("victoriametrics_server")
vmstorage_hosts = get_hosts("vmstorage")
vminsert_hosts = get_hosts("vminsert")
vmselect_hosts = get_hosts("vmselect")
vmagent_hosts = get_hosts("vmagent")
vmauth_hosts = get_hosts("vmauth")

promscrape_config_file = os.path.join(
  victoriametrics_config_dir, "promscrape.yml"
)
ambari_sd_password_file = os.path.join(
  victoriametrics_config_dir, "ambari-sd.password"
)
remote_write_password_file = os.path.join(
  victoriametrics_config_dir, "remote-write.password"
)
vmauth_config_file = os.path.join(victoriametrics_config_dir, "vmauth.yml")

ambari_sd_url = "{0}://{1}:{2}/api/v1/clusters/{3}/prometheus_targets".format(
  ambari_sd_protocol,
  ambari_server_host,
  ambari_sd_port,
  quote(cluster_name, safe=""),
)

vmauth_target_host = None
if vmauth_hosts:
  vmauth_target_host = hostname if hostname in vmauth_hosts else vmauth_hosts[0]

if remote_write_url_override:
  remote_write_url = remote_write_url_override
elif vmauth_target_host:
  remote_write_url = "http://{0}:{1}/api/v1/write".format(
    vmauth_target_host, vmauth_http_port
  )
elif deployment_mode == "single" and server_hosts:
  remote_write_url = "http://{0}:{1}/api/v1/write".format(
    server_hosts[0], server_http_port
  )
elif vminsert_hosts:
  remote_write_url = "http://{0}:{1}/insert/{2}/prometheus/api/v1/write".format(
    vminsert_hosts[0], vminsert_http_port, tenant_id
  )
else:
  remote_write_url = ""
remote_write_uses_vmauth = bool(vmauth_target_host) and not remote_write_url_override

vmagent_members_count = max(len(vmagent_hosts), 1)
vmagent_member_num = (
  vmagent_hosts.index(hostname) if hostname in vmagent_hosts else 0
)

binary_names = {
  "VICTORIAMETRICS_SERVER": "victoria-metrics-prod",
  "VMSTORAGE": "vmstorage-prod",
  "VMINSERT": "vminsert-prod",
  "VMSELECT": "vmselect-prod",
  "VMAGENT": "vmagent-prod",
  "VMAUTH": "vmauth-prod",
}
component_names = {
  "VICTORIAMETRICS_SERVER": "server",
  "VMSTORAGE": "vmstorage",
  "VMINSERT": "vminsert",
  "VMSELECT": "vmselect",
  "VMAGENT": "vmagent",
  "VMAUTH": "vmauth",
}
http_ports = {
  "VICTORIAMETRICS_SERVER": server_http_port,
  "VMSTORAGE": vmstorage_http_port,
  "VMINSERT": vminsert_http_port,
  "VMSELECT": vmselect_http_port,
  "VMAGENT": vmagent_http_port,
  "VMAUTH": vmauth_http_port,
}
extra_args = {
  component_role: get_property(
    "victoriametrics-env", component_names[component_role] + "_extra_args", ""
  )
  for component_role in supported_roles
}

component_name = component_names[role]
binary_path = os.path.join(victoriametrics_binary_dir, binary_names[role])
http_port = http_ports[role]
pid_file = os.path.join(victoriametrics_pid_dir, component_name + ".pid")
log_file = os.path.join(victoriametrics_log_dir, component_name + ".log")
component_extra_args = extra_args[role]

component_data_dirs = {
  "VICTORIAMETRICS_SERVER": [server_data_dir],
  "VMSTORAGE": [vmstorage_data_dir],
  "VMSELECT": [vmselect_cache_dir],
  "VMAGENT": [vmagent_data_dir],
}
data_dirs = component_data_dirs.get(role, [])

# JSON string literals are valid YAML scalars and safely quote generated values.
yaml_cluster_name = json.dumps(cluster_name)
yaml_ambari_sd_url = json.dumps(ambari_sd_url)
yaml_ambari_sd_username = json.dumps(ambari_sd_username)
yaml_ambari_sd_ca_file = json.dumps(ambari_sd_ca_file)
yaml_api_username = json.dumps(api_username)
yaml_api_password = json.dumps(api_password)
single_backend_urls = [
  json.dumps("http://{0}:{1}".format(host, server_http_port))
  for host in server_hosts
]
insert_backend_urls = [
  json.dumps(
    "http://{0}:{1}/insert/{2}/prometheus".format(
      host, vminsert_http_port, tenant_id
    )
  )
  for host in vminsert_hosts
]
select_backend_urls = [
  json.dumps(
    "http://{0}:{1}/select/{2}/prometheus".format(
      host, vmselect_http_port, tenant_id
    )
  )
  for host in vmselect_hosts
]
select_ui_backend_urls = [
  json.dumps(
    "http://{0}:{1}/select/{2}".format(host, vmselect_http_port, tenant_id)
  )
  for host in vmselect_hosts
]
vmagent_backend_urls = [
  json.dumps("http://{0}:{1}".format(host, vmagent_http_port))
  for host in vmagent_hosts
]
