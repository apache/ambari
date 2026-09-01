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

Ambari Agent

"""

import json
import os
import re

from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import (
  get_stack_feature_version,
)
from resource_management.libraries.functions.version import (
  get_current_component_version,
)
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  get_audit_configs,
  generate_ranger_service_config,
)

import status_params
from functions import (
  build_topology_mappings,
  calc_heap_memory,
  escape_java_quoted_string,
  escape_java_properties_value,
  ensure_unit_for_memory,
  format_zookeeper_quorum,
  local_file_uri,
  normalize_network_hosts,
  normalize_ipv4_addresses,
  parse_address_port,
  parse_boolean,
  parse_fraction,
  parse_nonnegative_int,
  parse_port,
  parse_network_host_csv,
  parse_positive_int,
  parse_yes_no,
  require_external_ranger_credentials,
  require_bigtop_component_version,
  resolve_local_rm_ha_id,
  select_rm_webapp_address,
  timeline_service_v2_enabled,
  validate_absolute_path,
  validate_bigtop_stack,
  validate_config_segment,
  validate_hbase_backend_mode,
  validate_jar_file_name,
  validate_rm_ha_ids,
  validate_single_line_value,
  validate_rack_paths,
  validate_unix_name,
  yarn_artifact_paths,
)


service_name = "yarn"
# server configurations
config = Script.get_config()
tmp_dir = validate_absolute_path(Script.get_tmp_dir(), "agent temporary directory")

stack_name = config["clusterLevelParams"]["stack_name"]
stack_version = config["clusterLevelParams"]["stack_version"]
validate_bigtop_stack(stack_name, stack_version)
stack_root = validate_absolute_path(Script.get_stack_root(), "BIGTOP stack root")

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_timeline_state_store = check_stack_feature(
  StackFeature.TIMELINE_STATE_STORE, version_for_stack_feature_checks
)

# Resolve install commands from repositoryFile and ordinary commands from the
# selected component symlink when commandParams does not carry a version.
version = get_current_component_version()
if version is not None:
  validate_bigtop_stack(stack_name, version)

stack_supports_ranger_kerberos = check_stack_feature(
  StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_audit_db = check_stack_feature(
  StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks
)

hostname = normalize_network_hosts(
  (config["agentLevelParams"]["hostname"],), "agentLevelParams/hostname"
)[0]

# hadoop default parameters
hadoop_home = status_params.hadoop_home
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")
hadoop_hdfs_home = stack_select.get_hadoop_dir("hdfs_home")
hadoop_mapred_home = stack_select.get_hadoop_dir("mapred_home")
hadoop_yarn_home = stack_select.get_hadoop_dir("yarn_home")
hadoop_bin = stack_select.get_hadoop_dir("sbin")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_lib_home = stack_select.get_hadoop_dir("lib")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
mapred_container_bin = format("{hadoop_mapred_home}/bin")
yarn_container_bin = format("{hadoop_yarn_home}/bin")
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")
hadoop_yarn_home_shell = quote_bash_args(str(hadoop_yarn_home))
hadoop_libexec_dir_shell = quote_bash_args(str(hadoop_libexec_dir))
hadoop_java_io_tmpdir_shell = quote_bash_args(str(hadoop_java_io_tmpdir))

if stack_supports_timeline_state_store:
  # Timeline Service property that was added timeline_state_store stack feature
  ats_leveldb_state_store_dir = default(
    "/configurations/yarn-site/yarn.timeline-service.leveldb-state-store.path",
    "/hadoop/yarn/timeline",
  )

# ats 1.5 properties
entity_groupfs_active_dir = config["configurations"]["yarn-site"][
  "yarn.timeline-service.entity-group-fs-store.active-dir"
]
entity_groupfs_active_dir_mode = 0o1777
entity_groupfs_store_dir = config["configurations"]["yarn-site"][
  "yarn.timeline-service.entity-group-fs-store.done-dir"
]
entity_groupfs_store_dir_mode = 0o700

hadoop_conf_secure_dir = os.path.join(hadoop_conf_dir, "secure")

limits_conf_dir = "/etc/security/limits.d"
yarn_user_nofile_limit = parse_positive_int(
  default("/configurations/yarn-env/yarn_user_nofile_limit", "32768"),
  "yarn-env/yarn_user_nofile_limit",
)
yarn_user_nproc_limit = parse_positive_int(
  default("/configurations/yarn-env/yarn_user_nproc_limit", "65536"),
  "yarn-env/yarn_user_nproc_limit",
)

mapred_user_nofile_limit = parse_positive_int(
  default("/configurations/mapred-env/mapred_user_nofile_limit", "32768"),
  "mapred-env/mapred_user_nofile_limit",
)
mapred_user_nproc_limit = parse_positive_int(
  default("/configurations/mapred-env/mapred_user_nproc_limit", "65536"),
  "mapred-env/mapred_user_nproc_limit",
)

execute_path = (
  os.environ["PATH"] + os.pathsep + hadoop_bin_dir + os.pathsep + yarn_container_bin
)

mapred_user = status_params.mapred_user
yarn_user = status_params.yarn_user
hdfs_user = validate_unix_name(
  config["configurations"]["hadoop-env"]["hdfs_user"], "hadoop-env/hdfs_user"
)
hdfs_tmp_dir = default("/configurations/hadoop-env/hdfs_tmp_dir", "/tmp")

smokeuser = validate_unix_name(
  config["configurations"]["cluster-env"]["smokeuser"],
  "cluster-env/smokeuser",
)
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
smoke_hdfs_user_mode = 0o770
security_enabled = parse_boolean(
  config["configurations"]["cluster-env"]["security_enabled"]
)
nm_security_marker_dir = "/var/lib/ambari-agent/data/yarn"
nm_security_marker = os.path.join(nm_security_marker_dir, "nm_security_enabled")
legacy_nm_security_marker = "/var/lib/hadoop-yarn/nm_security_enabled"
smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]

yarn_executor_container_group = validate_unix_name(
  config["configurations"]["yarn-site"][
    "yarn.nodemanager.linux-container-executor.group"
  ],
  "yarn-site/yarn.nodemanager.linux-container-executor.group",
)
yarn_nodemanager_container_executor_class = config["configurations"]["yarn-site"][
  "yarn.nodemanager.container-executor.class"
]
is_linux_container_executor = (
  yarn_nodemanager_container_executor_class
  == "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor"
)
container_executor_mode = 0o6050 if is_linux_container_executor else 0o2050
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
yarn_http_policy = str(
  config["configurations"]["yarn-site"]["yarn.http.policy"]
).strip().upper()
if yarn_http_policy not in ("HTTP_ONLY", "HTTPS_ONLY"):
  raise Fail("yarn-site/yarn.http.policy must be HTTP_ONLY or HTTPS_ONLY")
yarn_https_on = yarn_http_policy == "HTTPS_ONLY"
rm_hosts = normalize_network_hosts(
  config["clusterHostInfo"].get("resourcemanager_hosts"),
  "clusterHostInfo/resourcemanager_hosts",
)
rm_host = rm_hosts[0]
java64_home = config["ambariLevelParams"]["java_home"]
java_exec = format("{java64_home}/bin/java")

ambari_java_home = config['ambariLevelParams']['ambari_java_home']
ambari_java_exec = format("{ambari_java_home}/bin/java")

hadoop_ssl_enabled = parse_boolean(
  default("/configurations/core-site/hadoop.ssl.enabled", False)
)

yarn_heapsize = config["configurations"]["yarn-env"]["yarn_heapsize"]
resourcemanager_heapsize = config["configurations"]["yarn-env"][
  "resourcemanager_heapsize"
]
nodemanager_heapsize = config["configurations"]["yarn-env"]["nodemanager_heapsize"]
apptimelineserver_heapsize = default(
  "/configurations/yarn-env/apptimelineserver_heapsize", 1024
)
ats_leveldb_dir = config["configurations"]["yarn-site"][
  "yarn.timeline-service.leveldb-timeline-store.path"
]
yarn_log_dir_prefix = config["configurations"]["yarn-env"]["yarn_log_dir_prefix"]
yarn_pid_dir_prefix = status_params.yarn_pid_dir_prefix
yarn_log_dir_prefix_shell = quote_bash_args(str(yarn_log_dir_prefix))
yarn_pid_dir_prefix_shell = quote_bash_args(str(yarn_pid_dir_prefix))
java64_home_shell = quote_bash_args(str(java64_home))
yarn_heapsize_shell = quote_bash_args(str(yarn_heapsize))
resourcemanager_heapsize_shell = quote_bash_args(str(resourcemanager_heapsize))
nodemanager_heapsize_shell = quote_bash_args(str(nodemanager_heapsize))
apptimelineserver_heapsize_shell = quote_bash_args(
  str(apptimelineserver_heapsize)
)
yarn_user_shell = quote_bash_args(str(yarn_user))
mapred_pid_dir_prefix = status_params.mapred_pid_dir_prefix
mapred_log_dir_prefix = config["configurations"]["mapred-env"]["mapred_log_dir_prefix"]
mapred_log_dir_prefix_shell = quote_bash_args(str(mapred_log_dir_prefix))
mapred_env_sh_template = config["configurations"]["mapred-env"]["content"]
yarn_env_sh_template = config["configurations"]["yarn-env"]["content"]
container_executor_cfg_template = config["configurations"]["container-executor"][
  "content"
]
yarn_nodemanager_recovery_dir = default(
  "/configurations/yarn-site/yarn.nodemanager.recovery.dir", None
)
service_check_queue_name = default(
  "/configurations/yarn-env/service_check.queue.name", "default"
)

nm_address = config["configurations"]["yarn-site"][
  "yarn.nodemanager.address"
]  # still contains 0.0.0.0
if hostname and nm_address and nm_address.startswith("0.0.0.0:"):
  nm_address = nm_address.replace("0.0.0.0", hostname)

# Initialize lists of work directories.
nm_local_dirs = default("/configurations/yarn-site/yarn.nodemanager.local-dirs", "")
nm_log_dirs = default("/configurations/yarn-site/yarn.nodemanager.log-dirs", "")

nm_local_dirs_list = [path.strip() for path in nm_local_dirs.split(",") if path.strip()]
nm_log_dirs_list = [path.strip() for path in nm_log_dirs.split(",") if path.strip()]
if not nm_local_dirs_list or not nm_log_dirs_list:
  raise Fail("NodeManager local and log directory lists must not be empty")
nm_local_dirs_list = [
  validate_absolute_path(path, "yarn.nodemanager.local-dirs")
  for path in nm_local_dirs_list
]
nm_log_dirs_list = [
  validate_absolute_path(path, "yarn.nodemanager.log-dirs")
  for path in nm_log_dirs_list
]
nm_local_dirs = ",".join(nm_local_dirs_list)
nm_log_dirs = ",".join(nm_log_dirs_list)

nm_log_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_log_dir_mount.hist"
nm_local_dir_to_mount_file = "/var/lib/ambari-agent/data/yarn/yarn_local_dir_mount.hist"

yarn_pid_dir = status_params.yarn_pid_dir
mapred_pid_dir = status_params.mapred_pid_dir

mapred_log_dir = format("{mapred_log_dir_prefix}/{mapred_user}")
yarn_log_dir = format("{yarn_log_dir_prefix}/{yarn_user}")

user_group = status_params.user_group

# topology files
net_topology_mapping_data_file_path = os.path.join(hadoop_conf_dir, "topology_mappings.data")

# hosts
all_hosts = normalize_network_hosts(
  default("/clusterHostInfo/all_hosts", []),
  "clusterHostInfo/all_hosts",
  require_hosts=False,
)
all_racks = validate_rack_paths(
  default("/clusterHostInfo/all_racks", []),
  "clusterHostInfo/all_racks",
  require_racks=False,
)
all_ipv4_ips = normalize_ipv4_addresses(
  default("/clusterHostInfo/all_ipv4_ips", []),
  "clusterHostInfo/all_ipv4_ips",
  require_addresses=False,
)
slave_hosts = normalize_network_hosts(
  default("/clusterHostInfo/datanode_hosts", []),
  "clusterHostInfo/datanode_hosts",
  require_hosts=False,
)

# exclude file
exclude_hosts = parse_network_host_csv(
  config["commandParams"].get("all_decommissioned_hosts"),
  "commandParams/all_decommissioned_hosts",
)
exclude_file_path = default(
  "/configurations/yarn-site/yarn.resourcemanager.nodes.exclude-path",
  "/etc/hadoop/conf/yarn.exclude",
)
nm_hosts = normalize_network_hosts(
  default("/clusterHostInfo/nodemanager_hosts", []),
  "clusterHostInfo/nodemanager_hosts",
  require_hosts=False,
)
topology_mappings = build_topology_mappings(
  all_hosts,
  all_ipv4_ips,
  all_racks,
  tuple(dict.fromkeys(slave_hosts + nm_hosts)),
)
# include file
include_file_path = default(
  "/configurations/yarn-site/yarn.resourcemanager.nodes.include-path", None
)
include_hosts = None
manage_include_files = parse_boolean(
  default("/configurations/yarn-site/manage.include.files", False)
)
if include_file_path and manage_include_files:
  excluded_host_set = set(exclude_hosts)
  include_hosts = tuple(host for host in nm_hosts if host not in excluded_host_set)

ats_host = set(default("/clusterHostInfo/app_timeline_server_hosts", []))
has_ats = not len(ats_host) == 0

atsv2_host = set(default("/clusterHostInfo/timeline_reader_hosts", []))
has_atsv2 = not len(atsv2_host) == 0
yarn_timeline_service_version = config["configurations"]["yarn-site"][
  "yarn.timeline-service.version"
]
yarn_timeline_service_versions = config["configurations"]["yarn-site"][
  "yarn.timeline-service.versions"
]
yarn_timeline_service_enabled = parse_boolean(
  config["configurations"]["yarn-site"]["yarn.timeline-service.enabled"]
)
has_timeline_service_v2 = timeline_service_v2_enabled(
  yarn_timeline_service_enabled,
  yarn_timeline_service_version,
  yarn_timeline_service_versions,
)

registry_dns_host = set(default("/clusterHostInfo/yarn_registry_dns_hosts", []))
has_registry_dns = not len(registry_dns_host) == 0

# don't using len(nm_hosts) here, because check can take too much time on large clusters
number_of_nm = 1

hs_host = default("/clusterHostInfo/historyserver_hosts", [])
has_hs = not len(hs_host) == 0

nodemanager_principal_name = None
nodemanager_keytab = None

rm_zk_address = config["configurations"]["yarn-site"]["hadoop.zk.address"]
rm_zk_znode = config["configurations"]["yarn-site"][
  "yarn.resourcemanager.zk-state-store.parent-path"
]
rm_zk_store_class = config["configurations"]["yarn-site"][
  "yarn.resourcemanager.store.class"
]
stack_supports_zk_security = check_stack_feature(
  StackFeature.SECURE_ZOOKEEPER, version_for_stack_feature_checks
)
rm_zk_failover_znode = default(
  "/configurations/yarn-site/yarn.resourcemanager.ha.automatic-failover.zk-base-path",
  "/yarn-leader-election",
)
hadoop_registry_zk_root = default(
  "/configurations/yarn-site/hadoop.registry.zk.root", "/registry"
)

if security_enabled:
  rm_principal_name = config["configurations"]["yarn-site"][
    "yarn.resourcemanager.principal"
  ]
  rm_principal_name = rm_principal_name.replace("_HOST", hostname.lower())
  rm_keytab = config["configurations"]["yarn-site"]["yarn.resourcemanager.keytab"]
  rm_principal_name_jaas = escape_java_quoted_string(
    rm_principal_name, "yarn-site/yarn.resourcemanager.principal"
  )
  rm_keytab_jaas = escape_java_quoted_string(
    rm_keytab, "yarn-site/yarn.resourcemanager.keytab"
  )
  yarn_jaas_file = os.path.join(hadoop_conf_dir, "yarn_jaas.conf")
  if stack_supports_zk_security:
    zk_principal_name = default(
      "/configurations/zookeeper-env/zookeeper_principal_name",
      "zookeeper/_HOST@EXAMPLE.COM",
    )
    zk_principal_user = zk_principal_name.split("/")[0]
    rm_security_opts = format(
      "-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Djava.security.auth.login.config={yarn_jaas_file} -Dzookeeper.sasl.clientconfig=Client"
    )
    rm_security_opts_shell = quote_bash_args(str(rm_security_opts))

  # YARN timeline security options
  if has_ats or has_atsv2:
    yarn_timelineservice_principal_name = config["configurations"]["yarn-site"][
      "yarn.timeline-service.principal"
    ]
    yarn_timelineservice_principal_name = yarn_timelineservice_principal_name.replace(
      "_HOST", hostname.lower()
    )
    yarn_timelineservice_keytab = config["configurations"]["yarn-site"][
      "yarn.timeline-service.keytab"
    ]
    yarn_timelineservice_principal_name_jaas = escape_java_quoted_string(
      yarn_timelineservice_principal_name,
      "yarn-site/yarn.timeline-service.principal",
    )
    yarn_timelineservice_keytab_jaas = escape_java_quoted_string(
      yarn_timelineservice_keytab, "yarn-site/yarn.timeline-service.keytab"
    )
    yarn_ats_jaas_file = os.path.join(hadoop_conf_dir, "yarn_ats_jaas.conf")
    yarn_ats_jaas_option_shell = quote_bash_args(
      f"-Djava.security.auth.login.config={yarn_ats_jaas_file}"
    )

  if has_registry_dns:
    yarn_registry_dns_principal_name = config["configurations"]["yarn-env"][
      "yarn.registry-dns.principal"
    ]
    yarn_registry_dns_principal_name = yarn_registry_dns_principal_name.replace(
      "_HOST", hostname.lower()
    )
    yarn_registry_dns_keytab = config["configurations"]["yarn-env"][
      "yarn.registry-dns.keytab"
    ]
    yarn_registry_dns_principal_name_jaas = escape_java_quoted_string(
      yarn_registry_dns_principal_name,
      "yarn-env/yarn.registry-dns.principal",
    )
    yarn_registry_dns_keytab_jaas = escape_java_quoted_string(
      yarn_registry_dns_keytab, "yarn-env/yarn.registry-dns.keytab"
    )
    yarn_registry_dns_jaas_file = os.path.join(
      hadoop_conf_dir, "yarn_registry_dns_jaas.conf"
    )
    yarn_registry_dns_jaas_option_shell = quote_bash_args(
      f"-Djava.security.auth.login.config={yarn_registry_dns_jaas_file}"
    )

  nodemanager_principal_name = config["configurations"]["yarn-site"][
    "yarn.nodemanager.principal"
  ].replace("_HOST", hostname.lower())
  nodemanager_keytab = config["configurations"]["yarn-site"][
    "yarn.nodemanager.keytab"
  ]
  nodemanager_principal_name_jaas = escape_java_quoted_string(
    nodemanager_principal_name, "yarn-site/yarn.nodemanager.principal"
  )
  nodemanager_keytab_jaas = escape_java_quoted_string(
    nodemanager_keytab, "yarn-site/yarn.nodemanager.keytab"
  )
  yarn_nm_jaas_file = os.path.join(hadoop_conf_dir, "yarn_nm_jaas.conf")
  yarn_nm_jaas_option_shell = quote_bash_args(
    f"-Djava.security.auth.login.config={yarn_nm_jaas_file} "
    "-Dsun.security.krb5.rcache=none"
  )

  if has_hs:
    mapred_jhs_principal_name = config["configurations"]["mapred-site"][
      "mapreduce.jobhistory.principal"
    ]
    mapred_jhs_principal_name = mapred_jhs_principal_name.replace(
      "_HOST", hostname.lower()
    )
    mapred_jhs_keytab = config["configurations"]["mapred-site"][
      "mapreduce.jobhistory.keytab"
    ]
    mapred_jhs_principal_name_jaas = escape_java_quoted_string(
      mapred_jhs_principal_name,
      "mapred-site/mapreduce.jobhistory.principal",
    )
    mapred_jhs_keytab_jaas = escape_java_quoted_string(
      mapred_jhs_keytab, "mapred-site/mapreduce.jobhistory.keytab"
    )
    mapred_jaas_file = os.path.join(hadoop_conf_dir, "mapred_jaas.conf")

  yarn_jaas_option_shell = quote_bash_args(
    f"-Djava.security.auth.login.config={yarn_jaas_file}"
  )

yarn_log_aggregation_enabled = parse_boolean(
  config["configurations"]["yarn-site"]["yarn.log-aggregation-enable"]
)
yarn_nm_app_log_dir = config["configurations"]["yarn-site"][
  "yarn.nodemanager.remote-app-log-dir"
]
mapreduce_jobhistory_intermediate_done_dir = config["configurations"]["mapred-site"][
  "mapreduce.jobhistory.intermediate-done-dir"
]
mapreduce_jobhistory_done_dir = config["configurations"]["mapred-site"][
  "mapreduce.jobhistory.done-dir"
]
jobhistory_heapsize = default("/configurations/mapred-env/jobhistory_heapsize", "900")
jobhistory_heapsize_shell = quote_bash_args(str(jobhistory_heapsize))
jhs_leveldb_state_store_dir = default(
  "/configurations/mapred-site/mapreduce.jobhistory.recovery.store.leveldb.path",
  "/hadoop/mapreduce/jhs",
)

# for create_hdfs_directory
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
is_webhdfs_enabled = parse_boolean(hdfs_site["dfs.webhdfs.enabled"])

# Path to file that contains list of HDFS resources to be skipped during processing
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

dfs_type = default("/clusterLevelParams/dfs_type", "")


import functools

# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
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
update_files_only = parse_boolean(default("/commandParams/update_files_only", False))

# Node labels
node_labels_dir = default(
  "/configurations/yarn-site/yarn.node-labels.fs-store.root-dir", None
)
node_label_enable = parse_boolean(
  config["configurations"]["yarn-site"]["yarn.node-labels.enabled"]
)

scheme = "http" if not yarn_https_on else "https"
yarn_rm_address = (
  config["configurations"]["yarn-site"]["yarn.resourcemanager.webapp.address"]
  if not yarn_https_on
  else config["configurations"]["yarn-site"][
    "yarn.resourcemanager.webapp.https.address"
  ]
)
rm_ha_id = None
rm_webapp_addresses = {None: yarn_rm_address}
rm_ha_enabled = parse_boolean(
  default("/configurations/yarn-site/yarn.resourcemanager.ha.enabled", False)
)
rm_ha_ids_list = list(
  validate_rm_ha_ids(
    rm_ha_enabled,
    default("/configurations/yarn-site/yarn.resourcemanager.ha.rm-ids", None),
  )
)

if rm_ha_enabled:
  rm_webapp_addresses = {}
  rm_hostnames = {}
  for rm_id in rm_ha_ids_list:
    rm_webapp_address_property = (
      format("yarn.resourcemanager.webapp.address.{rm_id}")
      if not yarn_https_on
      else format("yarn.resourcemanager.webapp.https.address.{rm_id}")
    )
    rm_webapp_address = config["configurations"]["yarn-site"].get(
      rm_webapp_address_property
    )
    parse_address_port(rm_webapp_address, rm_webapp_address_property)
    rm_webapp_addresses[rm_id] = rm_webapp_address
    rm_hostname_property = format("yarn.resourcemanager.hostname.{rm_id}")
    rm_hostnames[rm_id] = config["configurations"]["yarn-site"].get(
      rm_hostname_property
    )
  rm_ha_id = resolve_local_rm_ha_id(
    rm_hostnames, hostname, require_match=False
  )
yarn_rm_address = select_rm_webapp_address(rm_webapp_addresses, rm_ha_id)

# for curl command in ranger plugin to get db connector
jdk_location = config["ambariLevelParams"]["jdk_location"]

# ranger yarn plugin section start
ranger_plugin_home = format("{hadoop_home}/../ranger-{service_name}-plugin")

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(
  StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks
)

# ambari-server hostname
ambari_server_hostname = config["ambariLevelParams"]["ambari_server_host"]

# ranger yarn plugin enabled property
enable_ranger_yarn = default(
  "/configurations/ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled", "No"
)
enable_ranger_yarn = parse_yes_no(
  enable_ranger_yarn,
  "ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled",
)

# ranger yarn-plugin supported flag, instead of using is_supported_yarn_ranger/yarn-env, using stack feature
is_supported_yarn_ranger = check_stack_feature(
  StackFeature.YARN_RANGER_PLUGIN_SUPPORT, version_for_stack_feature_checks
)

# get ranger yarn properties if enable_ranger_yarn is True
if enable_ranger_yarn and is_supported_yarn_ranger:
  # get ranger policy url
  policymgr_mgr_url = config["configurations"]["ranger-yarn-security"][
    "ranger.plugin.yarn.policy.rest.url"
  ]

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith("/"):
    policymgr_mgr_url = policymgr_mgr_url.rstrip("/")

  # ranger audit db user
  xa_audit_db_user = default(
    "/configurations/admin-properties/audit_db_user", "rangerlogger"
  )

  xa_audit_db_password = ""
  if (
    not is_empty(config["configurations"]["admin-properties"]["audit_db_password"])
    and stack_supports_ranger_audit_db
    and has_ranger_admin
  ):
    xa_audit_db_password = config["configurations"]["admin-properties"][
      "audit_db_password"
    ]

  # ranger yarn service/repository name
  repo_name = validate_config_segment(
    str(config["clusterName"]) + "_yarn", "Ranger YARN service name"
  )
  repo_name_value = config["configurations"]["ranger-yarn-security"][
    "ranger.plugin.yarn.service.name"
  ]
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = validate_config_segment(
      repo_name_value, "ranger-yarn-security/ranger.plugin.yarn.service.name"
    )

  # ranger-env config
  ranger_env = config["configurations"]["ranger-env"]

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_yarn:
    external_credentials = require_external_ranger_credentials(
      config["configurations"]["ranger-yarn-plugin-properties"]
    )
    ranger_env = {
      "admin_username": external_credentials["external_admin_username"],
      "admin_password": external_credentials["external_admin_password"],
      "ranger_admin_username": external_credentials[
        "external_ranger_admin_username"
      ],
      "ranger_admin_password": external_credentials[
        "external_ranger_admin_password"
      ],
    }

  ranger_plugin_properties = config["configurations"]["ranger-yarn-plugin-properties"]
  policy_user = config["configurations"]["ranger-yarn-plugin-properties"]["policy_user"]
  yarn_rest_url = yarn_rm_address

  ranger_plugin_config = {
    "username": config["configurations"]["ranger-yarn-plugin-properties"][
      "REPOSITORY_CONFIG_USERNAME"
    ],
    "password": str(
      config["configurations"]["ranger-yarn-plugin-properties"][
        "REPOSITORY_CONFIG_PASSWORD"
      ]
    ),
    "yarn.url": format("{scheme}://{yarn_rest_url}"),
    "commonNameForCertificate": config["configurations"][
      "ranger-yarn-plugin-properties"
    ]["common.name.for.certificate"],
    "hadoop.security.authentication": "kerberos" if security_enabled else "simple",
  }

  if security_enabled:
    ranger_plugin_config["policy.download.auth.users"] = yarn_user
    ranger_plugin_config["tag.download.auth.users"] = yarn_user

  ranger_plugin_config["setup.additional.default.policies"] = "true"
  ranger_plugin_config["default-policy.1.name"] = "Service Check User Policy for Yarn"
  ranger_plugin_config["default-policy.1.resource.queue"] = service_check_queue_name
  ranger_plugin_config["default-policy.1.policyItem.1.users"] = policy_user
  ranger_plugin_config["default-policy.1.policyItem.1.accessTypes"] = "submit-app"

  custom_ranger_service_config = generate_ranger_service_config(
    ranger_plugin_properties
  )
  if len(custom_ranger_service_config) > 0:
    ranger_plugin_config.update(custom_ranger_service_config)

  yarn_ranger_plugin_repo = {
    "isEnabled": "true",
    "configs": ranger_plugin_config,
    "description": "yarn repo",
    "name": repo_name,
    "repositoryType": "yarn",
    "type": "yarn",
    "assetType": "1",
  }

  if stack_supports_ranger_kerberos:
    ranger_plugin_config["ambari.service.check.user"] = policy_user
    ranger_plugin_config["hadoop.security.authentication"] = (
      "kerberos" if security_enabled else "simple"
    )

  if stack_supports_ranger_kerberos and security_enabled:
    ranger_plugin_config["policy.download.auth.users"] = yarn_user
    ranger_plugin_config["tag.download.auth.users"] = yarn_user

  downloaded_custom_connector = None
  previous_jdbc_jar_name = None
  driver_curl_source = None
  driver_curl_target = None
  previous_jdbc_jar = None

  if has_ranger_admin and stack_supports_ranger_audit_db:
    xa_audit_db_flavor = config["configurations"]["admin-properties"]["DB_FLAVOR"]
    jdbc_jar_name, previous_jdbc_jar_name, audit_jdbc_url, jdbc_driver = (
      get_audit_configs(config)
    )
    jdbc_jar_name = validate_jar_file_name(
      jdbc_jar_name, "Ranger YARN audit JDBC JAR"
    )
    if previous_jdbc_jar_name:
      previous_jdbc_jar_name = validate_jar_file_name(
        previous_jdbc_jar_name, "previous Ranger YARN audit JDBC JAR"
      )

    downloaded_custom_connector = (
      format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    )
    driver_curl_source = (
      format("{jdk_location}/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    driver_curl_target = (
      format("{hadoop_yarn_home}/lib/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    previous_jdbc_jar = (
      format("{hadoop_yarn_home}/lib/{previous_jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = parse_boolean(
      config["configurations"]["ranger-yarn-audit"][
        "xasecure.audit.destination.db"
      ]
    )

  xa_audit_hdfs_is_enabled = (
    parse_boolean(
      config["configurations"]["ranger-yarn-audit"][
        "xasecure.audit.destination.hdfs"
      ]
    )
    if xml_configurations_supported
    else False
  )
  ssl_keystore_password = (
    config["configurations"]["ranger-yarn-policymgr-ssl"][
      "xasecure.policymgr.clientssl.keystore.password"
    ]
    if xml_configurations_supported
    else None
  )
  ssl_truststore_password = (
    config["configurations"]["ranger-yarn-policymgr-ssl"][
      "xasecure.policymgr.clientssl.truststore.password"
    ]
    if xml_configurations_supported
    else None
  )
  credential_file = format("/etc/ranger/{repo_name}/cred.jceks")

  # for SQLA explicitly disable audit to DB for Ranger
  if (
    has_ranger_admin and stack_supports_ranger_audit_db and xa_audit_db_flavor == "sqla"
  ):
    xa_audit_db_is_enabled = False

# need this to capture cluster name from where ranger yarn plugin is enabled
cluster_name = config["clusterName"]

# ranger yarn plugin end section

# container-executor properties
container_executor_config = config["configurations"]["container-executor"]
min_user_id = parse_nonnegative_int(
  container_executor_config["min_user_id"], "container-executor/min_user_id"
)
docker_module_enabled = (
  "true"
  if parse_boolean(container_executor_config["docker_module_enabled"])
  else "false"
)
docker_binary = validate_single_line_value(
  container_executor_config["docker_binary"], "container-executor/docker_binary"
)
if docker_binary:
  docker_binary = validate_absolute_path(
    docker_binary, "container-executor/docker_binary"
  )
elif docker_module_enabled == "true":
  raise Fail("container-executor/docker_binary is required when Docker is enabled")
docker_allowed_capabilities = validate_single_line_value(
  config["configurations"]["yarn-site"][
    "yarn.nodemanager.runtime.linux.docker.capabilities"
  ],
  "yarn-site/yarn.nodemanager.runtime.linux.docker.capabilities",
)
if docker_allowed_capabilities:
  docker_allowed_capabilities = ",".join(
    x.strip() for x in docker_allowed_capabilities.split(",")
  )
else:
  docker_allowed_capabilities = ""
docker_allowed_devices = validate_single_line_value(
  container_executor_config["docker_allowed_devices"],
  "container-executor/docker_allowed_devices",
)
docker_allowed_networks = validate_single_line_value(
  config["configurations"]["yarn-site"][
    "yarn.nodemanager.runtime.linux.docker.allowed-container-networks"
  ],
  "yarn-site/yarn.nodemanager.runtime.linux.docker.allowed-container-networks",
)
if docker_allowed_networks:
  docker_allowed_networks = ",".join(
    x.strip() for x in docker_allowed_networks.split(",")
  )
else:
  docker_allowed_networks = ""
docker_allowed_ro_mounts = validate_single_line_value(
  container_executor_config["docker_allowed_ro-mounts"],
  "container-executor/docker_allowed_ro-mounts",
)
docker_allowed_rw_mounts = validate_single_line_value(
  container_executor_config["docker_allowed_rw-mounts"],
  "container-executor/docker_allowed_rw-mounts",
)
docker_privileged_containers_enabled = (
  "true"
  if parse_boolean(
    container_executor_config["docker_privileged-containers_enabled"]
  )
  else "false"
)
docker_trusted_registries = validate_single_line_value(
  container_executor_config["docker_trusted_registries"],
  "container-executor/docker_trusted_registries",
)
docker_allowed_volume_drivers = validate_single_line_value(
  container_executor_config["docker_allowed_volume-drivers"],
  "container-executor/docker_allowed_volume-drivers",
)
gpu_module_enabled = (
  "true"
  if parse_boolean(container_executor_config["gpu_module_enabled"])
  else "false"
)

# ATSv2 integration properties started.
yarn_timelinereader_pid_file = status_params.yarn_timelinereader_pid_file

yarn_atsv2_hbase_versioned_home = None
yarn_hbase_bin = None
yarn_hbase_hdfs_root_dir = config["configurations"]["yarn-hbase-site"]["hbase.rootdir"]
cluster_zookeeper_quorum_hosts = format_zookeeper_quorum(
  config["clusterHostInfo"].get("zookeeper_server_hosts"),
  "clusterHostInfo/zookeeper_server_hosts",
)
if (
  "zoo.cfg" in config["configurations"]
  and "clientPort" in config["configurations"]["zoo.cfg"]
):
  cluster_zookeeper_clientPort = parse_port(
    config["configurations"]["zoo.cfg"]["clientPort"],
    "zoo.cfg/clientPort",
  )
else:
  cluster_zookeeper_clientPort = 2181
zookeeper_quorum_hosts = cluster_zookeeper_quorum_hosts
zookeeper_clientPort = str(cluster_zookeeper_clientPort)
yarn_hbase_user = status_params.yarn_hbase_user
yarn_hbase_user_home = format("/user/{yarn_hbase_user}")
yarn_hbase_conf_dir = os.path.join(hadoop_conf_dir, "embedded-yarn-ats-hbase")
hbase_within_cluster = parse_boolean(
  config["configurations"]["yarn-hbase-env"]["hbase_within_cluster"]
)
is_hbase_system_service_launch = parse_boolean(
  config["configurations"]["yarn-hbase-env"]["is_hbase_system_service_launch"]
)
use_external_hbase = parse_boolean(
  config["configurations"]["yarn-hbase-env"]["use_external_hbase"]
)
validate_hbase_backend_mode(
  hbase_within_cluster,
  is_hbase_system_service_launch,
  use_external_hbase,
  config["clusterHostInfo"].get("hbase_master_hosts"),
  "hbase-site" in config["configurations"],
)
if is_hbase_system_service_launch and not has_timeline_service_v2:
  raise Fail("YARN embedded HBase system-service mode requires Timeline Service v2")
if has_timeline_service_v2 and not has_atsv2:
  raise Fail("YARN Timeline Service v2 requires at least one TIMELINE_READER host")
atsv2_backend_enabled = has_timeline_service_v2 and has_atsv2

yarn_hbase_user_version_home = None
yarn_service_app_hdfs_path = None
yarn_hbase_app_hdfs_path = None
yarn_hbase_user_tmp = None
if atsv2_backend_enabled:
  version = require_bigtop_component_version(version, "YARN Timeline Service v2")
  yarn_atsv2_hbase_versioned_home = format(
    "{stack_root}/{version}/usr/lib/hbase"
  )
  yarn_hbase_bin = format("{yarn_atsv2_hbase_versioned_home}/bin")
  yarn_hbase_user_version_home = format("{yarn_hbase_user_home}/{version}")
  yarn_service_app_hdfs_path, yarn_hbase_app_hdfs_path = yarn_artifact_paths(
    version, rm_ha_id
  )
  yarn_hbase_user_tmp = format("/var/lib/ambari-agent/yarn-ats-hbase/{version}")
yarn_hbase_log_dir = os.path.join(yarn_log_dir_prefix, "embedded-yarn-ats-hbase")
yarn_hbase_log_dir_shell = quote_bash_args(yarn_hbase_log_dir)
yarn_hbase_pid_dir_prefix = status_params.yarn_hbase_pid_dir_prefix
yarn_hbase_pid_dir = status_params.yarn_hbase_pid_dir
yarn_hbase_pid_dir_shell = quote_bash_args(yarn_hbase_pid_dir)
if hbase_within_cluster:
  yarn_hbase_conf_dir = "/etc/hbase/conf"
yarn_hbase_conf_dir_shell = quote_bash_args(yarn_hbase_conf_dir)
yarn_hbase_env_sh_template = config["configurations"]["yarn-hbase-env"]["content"]
yarn_hbase_java_io_tmpdir = validate_absolute_path(
  default("/configurations/yarn-hbase-env/hbase_java_io_tmpdir", "/tmp"),
  "yarn-hbase-env/hbase_java_io_tmpdir",
)
yarn_hbase_java_io_tmpdir_shell = quote_bash_args(str(yarn_hbase_java_io_tmpdir))
yarn_hbase_tmp_dir = config["configurations"]["yarn-hbase-site"]["hbase.tmp.dir"]
yarn_hbase_local_dir = config["configurations"]["yarn-hbase-site"]["hbase.local.dir"]
yarn_hbase_master_info_port = parse_port(
  config["configurations"]["yarn-hbase-site"]["hbase.master.info.port"],
  "yarn-hbase-site/hbase.master.info.port",
)
yarn_hbase_regionserver_info_port = parse_port(
  config["configurations"]["yarn-hbase-site"]["hbase.regionserver.info.port"],
  "yarn-hbase-site/hbase.regionserver.info.port",
)

if ("yarn-hbase-log4j" in config["configurations"]) and (
  "content" in config["configurations"]["yarn-hbase-log4j"]
):
  yarn_hbase_log4j_props = config["configurations"]["yarn-hbase-log4j"]["content"]
else:
  yarn_hbase_log4j_props = None

timeline_collector = ""
if has_timeline_service_v2:
  timeline_collector = "timeline_collector"

coprocessor_jar_name = "hadoop-yarn-server-timelineservice-hbase-coprocessor.jar"
yarn_timeline_jar_location = None
if atsv2_backend_enabled:
  yarn_timeline_jar_location = format(
    "file://{stack_root}/{version}/usr/lib/hadoop-yarn/"
    "timelineservice/{coprocessor_jar_name}"
  )
yarn_user_hbase_permissions = "RWXCA"

yarn_hbase_kinit_cmd = ""
if security_enabled and atsv2_backend_enabled:
  yarn_hbase_jaas_file = os.path.join(yarn_hbase_conf_dir, "yarn_hbase_jaas.conf")
  yarn_hbase_master_jaas_file = os.path.join(
    yarn_hbase_conf_dir, "yarn_hbase_master_jaas.conf"
  )
  yarn_hbase_regionserver_jaas_file = os.path.join(
    yarn_hbase_conf_dir, "yarn_hbase_regionserver_jaas.conf"
  )
  yarn_hbase_master_jaas_file_shell = quote_bash_args(yarn_hbase_master_jaas_file)
  yarn_hbase_regionserver_jaas_file_shell = quote_bash_args(
    yarn_hbase_regionserver_jaas_file
  )

  yarn_hbase_master_principal_name = config["configurations"]["yarn-hbase-site"][
    "hbase.master.kerberos.principal"
  ]
  yarn_hbase_master_principal_name = yarn_hbase_master_principal_name.replace(
    "_HOST", hostname.lower()
  )
  yarn_hbase_master_keytab = config["configurations"]["yarn-hbase-site"][
    "hbase.master.keytab.file"
  ]
  yarn_hbase_master_principal_name_jaas = escape_java_quoted_string(
    yarn_hbase_master_principal_name,
    "yarn-hbase-site/hbase.master.kerberos.principal",
  )
  yarn_hbase_master_keytab_jaas = escape_java_quoted_string(
    yarn_hbase_master_keytab, "yarn-hbase-site/hbase.master.keytab.file"
  )

  yarn_hbase_regionserver_principal_name = config["configurations"]["yarn-hbase-site"][
    "hbase.regionserver.kerberos.principal"
  ]
  yarn_hbase_regionserver_principal_name = (
    yarn_hbase_regionserver_principal_name.replace("_HOST", hostname.lower())
  )
  yarn_hbase_regionserver_keytab = config["configurations"]["yarn-hbase-site"][
    "hbase.regionserver.keytab.file"
  ]
  yarn_hbase_regionserver_principal_name_jaas = escape_java_quoted_string(
    yarn_hbase_regionserver_principal_name,
    "yarn-hbase-site/hbase.regionserver.kerberos.principal",
  )
  yarn_hbase_regionserver_keytab_jaas = escape_java_quoted_string(
    yarn_hbase_regionserver_keytab,
    "yarn-hbase-site/hbase.regionserver.keytab.file",
  )

  # User master principal name as AM principal in system service. Don't replace _HOST.
  yarn_ats_hbase_principal_name = config["configurations"]["yarn-hbase-site"][
    "hbase.master.kerberos.principal"
  ]
  yarn_ats_hbase_keytab = config["configurations"]["yarn-hbase-site"][
    "hbase.master.keytab.file"
  ]
  yarn_ats_principal_name = config["configurations"]["yarn-env"][
    "yarn_ats_principal_name"
  ]
  yarn_ats_user_keytab = config["configurations"]["yarn-env"]["yarn_ats_user_keytab"]
  yarn_hbase_kinit_cmd = (
    f'{quote_bash_args(kinit_path_local)} -c "$KRB5CCNAME" -kt '
    f"{quote_bash_args(yarn_ats_user_keytab)} "
    f"{quote_bash_args(yarn_ats_principal_name)}"
  )


if hbase_within_cluster:
  zookeeper_znode_parent = config["configurations"]["hbase-site"][
    "zookeeper.znode.parent"
  ]
  hbase_site_conf = config["configurations"]["hbase-site"]
  hbase_site_attributes = config["configurationAttributes"].get("hbase-site", {})
else:
  zookeeper_znode_parent = "/atsv2-hbase-unsecure"
  hbase_site_conf = config["configurations"]["yarn-hbase-site"]
  hbase_site_attributes = config["configurationAttributes"]["yarn-hbase-site"]

yarn_hbase_grant_permissions_file = format(
  "{yarn_hbase_conf_dir}/hbase_grant_permissions.rb"
)
yarn_hbase_executable = (
  os.path.join(yarn_hbase_bin, "hbase") if yarn_hbase_bin else None
)
yarn_hbase_schema_creator_args = (
  "org.apache.hadoop.yarn.server.timelineservice.storage.TimelineSchemaCreator",
  "-Dhbase.client.retries.number=35",
  "-create",
  "-s",
)
yarn_hbase_classpath_prefix = None
if atsv2_backend_enabled:
  yarn_hbase_classpath_prefix = (
    f"{yarn_atsv2_hbase_versioned_home}/lib/*:"
    f"{stack_root}/{version}/usr/lib/hadoop-yarn/timelineservice/*"
  )
# System service configuration as part of ATSv2.
yarn_system_service_dir = config["configurations"]["yarn-site"][
  "yarn.service.system-service.dir"
]
yarn_system_service_launch_mode = config["configurations"]["yarn-hbase-env"][
  "yarn_hbase_system_service_launch_mode"
]
yarn_hbase_service_queue_name = config["configurations"]["yarn-hbase-env"][
  "yarn_hbase_system_service_queue_name"
]

yarn_hbase_master_cpu = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_master_cpu", int),
  "yarn-hbase-env/yarn_hbase_master_cpu",
)
yarn_hbase_master_memory = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_master_memory", int),
  "yarn-hbase-env/yarn_hbase_master_memory",
)
yarn_hbase_master_containers = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_master_containers", int),
  "yarn-hbase-env/yarn_hbase_master_containers",
)
yarn_hbase_regionserver_cpu = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_regionserver_cpu", int),
  "yarn-hbase-env/yarn_hbase_regionserver_cpu",
)
yarn_hbase_regionserver_memory = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_regionserver_memory", int),
  "yarn-hbase-env/yarn_hbase_regionserver_memory",
)
yarn_hbase_regionserver_containers = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_regionserver_containers", int),
  "yarn-hbase-env/yarn_hbase_regionserver_containers",
)
yarn_hbase_client_cpu = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_client_cpu", int),
  "yarn-hbase-env/yarn_hbase_client_cpu",
)
yarn_hbase_client_memory = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_client_memory", int),
  "yarn-hbase-env/yarn_hbase_client_memory",
)
yarn_hbase_client_containers = parse_positive_int(
  expect("/configurations/yarn-hbase-env/yarn_hbase_client_containers", int),
  "yarn-hbase-env/yarn_hbase_client_containers",
)

yarn_hbase_heap_memory_factor = parse_fraction(
  expect("/configurations/yarn-hbase-env/yarn_hbase_heap_memory_factor", float),
  "yarn-hbase-env/yarn_hbase_heap_memory_factor",
)
yarn_hbase_master_heapsize = ensure_unit_for_memory(
  calc_heap_memory(yarn_hbase_master_memory, yarn_hbase_heap_memory_factor)
)
yarn_hbase_regionserver_heapsize = ensure_unit_for_memory(
  calc_heap_memory(yarn_hbase_regionserver_memory, yarn_hbase_heap_memory_factor)
)
yarn_hbase_master_heapsize_shell = quote_bash_args(yarn_hbase_master_heapsize)
yarn_hbase_regionserver_heapsize_shell = quote_bash_args(
  yarn_hbase_regionserver_heapsize
)

yarn_hbase_log_level = str(
  config["configurations"]["yarn-hbase-env"]["yarn_hbase_log_level"]
).upper()

yarn_hbase_service_queue_json = json.dumps(str(yarn_hbase_service_queue_name))
yarn_hbase_archive_id_json = None
yarn_service_framework_path = None
yarn_service_framework_path_json = None
yarn_hbase_log4j_source_json = None
yarn_hbase_site_source_json = None
yarn_hbase_policy_source_json = None
yarn_hbase_grant_source_json = None
yarn_hbase_core_site_source_json = None
yarn_hbase_metrics_source_json = None
if atsv2_backend_enabled:
  yarn_hbase_archive_id_json = json.dumps(
    f"{yarn_hbase_app_hdfs_path}/hbase.tar.gz"
  )
  yarn_service_framework_path = (
    f"{yarn_service_app_hdfs_path}/service-dep.tar.gz"
  )
  yarn_service_framework_path_json = json.dumps(yarn_service_framework_path)
  yarn_hbase_log4j_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/log4j.properties"
  )
  yarn_hbase_site_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/hbase-site.xml"
  )
  yarn_hbase_policy_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/hbase-policy.xml"
  )
  yarn_hbase_grant_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/hbase_grant_permissions.rb"
  )
  yarn_hbase_core_site_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/core-site.xml"
  )
  yarn_hbase_metrics_source_json = json.dumps(
    f"{yarn_hbase_user_version_home}/hadoop-metrics2-hbase.properties"
  )
java64_home_json = json.dumps(str(java64_home))
yarn_hbase_root_logger_json = json.dumps(f"{yarn_hbase_log_level},RFA")
yarn_hbase_system_service_opts_json = json.dumps(
  "-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:-ResizePLAB "
  "-XX:ErrorFile=${HBASE_LOG_DIR}/hs_err_pid%p.log "
  f"-Djava.io.tmpdir={yarn_hbase_java_io_tmpdir}"
)
yarn_hbase_master_url_json = json.dumps(
  f"http://${{THIS_HOST}}:{yarn_hbase_master_info_port}/master-status"
)
yarn_hbase_regionserver_url_json = json.dumps(
  f"http://${{THIS_HOST}}:{yarn_hbase_regionserver_info_port}/rs-status"
)
yarn_hbase_master_memory_json = json.dumps(str(yarn_hbase_master_memory))
yarn_hbase_regionserver_memory_json = json.dumps(
  str(yarn_hbase_regionserver_memory)
)
yarn_hbase_client_memory_json = json.dumps(str(yarn_hbase_client_memory))
yarn_hbase_master_opts = (
  f"-Xms{yarn_hbase_master_heapsize} -Xmx{yarn_hbase_master_heapsize}"
)
yarn_hbase_regionserver_opts = (
  f"-Xms{yarn_hbase_regionserver_heapsize} "
  f"-Xmx{yarn_hbase_regionserver_heapsize}"
)
if security_enabled and atsv2_backend_enabled:
  yarn_hbase_master_opts += (
    f" -Djava.security.auth.login.config={yarn_hbase_master_jaas_file}"
  )
  yarn_hbase_regionserver_opts += (
    f" -Djava.security.auth.login.config={yarn_hbase_regionserver_jaas_file}"
  )
yarn_hbase_master_opts_json = json.dumps(yarn_hbase_master_opts)
yarn_hbase_regionserver_opts_json = json.dumps(yarn_hbase_regionserver_opts)

schema_creator_command = " ".join(
  quote_bash_args(argument) for argument in yarn_hbase_schema_creator_args
)
launch_command = "sleep 10 && "
if security_enabled and atsv2_backend_enabled:
  launch_command += (
    "export KRB5CCNAME=FILE:$PWD/krb5cc-hbaseclient && "
    "trap 'rm -f -- \"$PWD/krb5cc-hbaseclient\"' EXIT && "
  )
launch_command += (
  "export HBASE_CLASSPATH_PREFIX=$HADOOP_HOME/share/hadoop/yarn/"
  "timelineservice/* && "
)
if security_enabled and atsv2_backend_enabled:
  launch_command += f"{yarn_hbase_kinit_cmd} && "
launch_command += f'"$HBASE_HOME/bin/hbase" {schema_creator_command}'
if security_enabled and atsv2_backend_enabled:
  launch_command += (
    f" && {yarn_hbase_kinit_cmd} && "
    f'"$HBASE_HOME/bin/hbase" shell '
    '"$PWD/conf/hbase_grant_permissions.rb" && '
    'rm -f -- "$PWD/krb5cc-hbaseclient" && trap - EXIT'
  )
launch_command += " && exec sleep infinity"
yarn_hbase_client_launch_command_json = json.dumps(launch_command)
if security_enabled and atsv2_backend_enabled:
  yarn_ats_hbase_principal_json = json.dumps(str(yarn_ats_hbase_principal_name))
  yarn_ats_hbase_keytab_uri_json = json.dumps(
    local_file_uri(
      yarn_ats_hbase_keytab, "yarn-hbase-site/hbase.master.keytab.file"
    )
  )
# ATSv2 integration properties ended

cgroup_root = validate_single_line_value(
  container_executor_config["cgroup_root"], "container-executor/cgroup_root"
)
if cgroup_root:
  cgroup_root = validate_absolute_path(
    cgroup_root, "container-executor/cgroup_root"
  )
yarn_hierarchy = validate_single_line_value(
  container_executor_config["yarn_hierarchy"],
  "container-executor/yarn_hierarchy",
)
if yarn_hierarchy and re.fullmatch(r"[A-Za-z0-9_.-]+", yarn_hierarchy) is None:
  raise Fail("container-executor/yarn_hierarchy must be a safe hierarchy name")

# registry dns service
registry_dns_needs_privileged_access = (
  status_params.registry_dns_needs_privileged_access
)

mount_table_content = None
if "viewfs-mount-table" in config["configurations"]:
  xml_inclusion_file_name = "viewfs-mount-table.xml"
  mount_table = config["configurations"]["viewfs-mount-table"]

  if "content" in mount_table and mount_table["content"].strip():
    mount_table_content = mount_table["content"]

hbase_log_maxfilesize = default(
  "/configurations/yarn-hbase-log4j/hbase_log_maxfilesize", 256
)
hbase_log_maxbackupindex = default(
  "/configurations/yarn-hbase-log4j/hbase_log_maxbackupindex", 20
)
hbase_security_log_maxfilesize = default(
  "/configurations/yarn-hbase-log4j/hbase_security_log_maxfilesize", 256
)
hbase_security_log_maxbackupindex = default(
  "/configurations/yarn-hbase-log4j/hbase_security_log_maxbackupindex", 20
)

rm_cross_origin_enabled = parse_boolean(
  config["configurations"]["yarn-site"][
    "yarn.resourcemanager.webapp.cross-origin.enabled"
  ]
)

cross_origins = "*"
if rm_cross_origin_enabled:
  if ":" not in rm_host and not re.fullmatch(r"[0-9.]+", rm_host):
    host_suffix = rm_host.rsplit(".", 2)[1:]
    if len(host_suffix) == 2:
      cross_origins = "regex:.*[.]" + "[.]".join(host_suffix) + r"(:\d*)?"

ams_collector_host_list = default("/clusterHostInfo/metrics_collector_hosts", [])
ams_collector_hosts = ",".join(
  normalize_network_hosts(
    ams_collector_host_list,
    "clusterHostInfo/metrics_collector_hosts",
    require_hosts=False,
  )
)
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  if (
    "cluster-env" in config["configurations"]
    and "metrics_collector_vip_port" in config["configurations"]["cluster-env"]
  ):
    metric_collector_port = str(
      parse_port(
        config["configurations"]["cluster-env"]["metrics_collector_vip_port"],
        "cluster-env/metrics_collector_vip_port",
      )
    )
  else:
    metric_collector_web_address = default(
      "/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188"
    )
    metric_collector_port = str(
      parse_address_port(
        metric_collector_web_address,
        "ams-site/timeline.metrics.service.webapp.address",
      )
    )
  metric_http_policy = str(
    default(
      "/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY"
    )
  ).strip().upper()
  if metric_http_policy == "HTTPS_ONLY":
    metric_collector_protocol = "https"
  elif metric_http_policy == "HTTP_ONLY":
    metric_collector_protocol = "http"
  else:
    raise Fail(
      "ams-site/timeline.metrics.service.http.policy must be HTTP_ONLY or HTTPS_ONLY"
    )
  metric_truststore_path = default(
    "/configurations/ams-ssl-client/ssl.client.truststore.location", ""
  )
  metric_truststore_type = default(
    "/configurations/ams-ssl-client/ssl.client.truststore.type", ""
  )
  metric_truststore_password = default(
    "/configurations/ams-ssl-client/ssl.client.truststore.password", ""
  )
  metric_truststore_path = escape_java_properties_value(
    metric_truststore_path,
    "ams-ssl-client/ssl.client.truststore.location",
  )
  metric_truststore_type = escape_java_properties_value(
    metric_truststore_type,
    "ams-ssl-client/ssl.client.truststore.type",
  )
  metric_truststore_password = escape_java_properties_value(
    metric_truststore_password,
    "ams-ssl-client/ssl.client.truststore.password",
  )
metrics_report_interval = parse_positive_int(
  default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60),
  "ams-site/timeline.metrics.sink.report.interval",
)
metrics_collection_period = parse_positive_int(
  default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10),
  "ams-site/timeline.metrics.sink.collection.period",
)

host_in_memory_aggregation = parse_boolean(
  default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
)
host_in_memory_aggregation_port = parse_port(
  default(
    "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888
  ),
  "ams-site/timeline.metrics.host.inmemory.aggregation.port",
)
is_aggregation_https_enabled = False
aggregation_http_policy = str(
  default(
    "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy",
    "HTTP_ONLY",
  )
).strip().upper()
if aggregation_http_policy == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = "https"
  is_aggregation_https_enabled = True
elif aggregation_http_policy == "HTTP_ONLY":
  host_in_memory_aggregation_protocol = "http"
else:
  raise Fail(
    "ams-site/timeline.metrics.host.inmemory.aggregation.http.policy must be "
    "HTTP_ONLY or HTTPS_ONLY"
  )
