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

import status_params
import re

from functions import (
  as_bool,
  calc_xmn_from_xms,
  ensure_unit_for_memory,
  require_external_ranger_credentials,
  strict_bool,
)

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import (
  get_stack_feature_version,
)
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  get_audit_configs,
  generate_ranger_service_config,
)
from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
is_parallel_execution_enabled = (
  int(default("/agentLevelParams/agentConfigParams/agent/parallel_execution", 0))
  == 1
)
stack_select_lock_file = format("{exec_tmp_dir}/stack_select_lock_file")

service_name = "hbase"
stack_name = status_params.stack_name
version = default("/commandParams/version", None)
repository_version = default("/repositoryFile/repoVersion", None)
component_directory = status_params.component_directory
etc_prefix_dir = "/etc/hbase"

stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted = status_params.stack_version_formatted
stack_root = status_params.stack_root

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(
  StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_audit_db = check_stack_feature(
  StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks
)

# hadoop default parameters
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
hbase_cmd = "/usr/lib/hbase/bin/hbase"
hbase_max_direct_memory_size = default(
  "configurations/hbase-env/hbase_max_direct_memory_size", None
)
hbase_home = "/usr/lib/hbase"
# hadoop parameters for stacks supporting rolling_upgrade
if stack_version_formatted and check_stack_feature(
  StackFeature.ROLLING_UPGRADE, stack_version_formatted
):
  daemon_script = format(
    "{stack_root}/current/{component_directory}/bin/hbase-daemon.sh"
  )
  hbase_cmd = format("{stack_root}/current/{component_directory}/bin/hbase")

  hbase_home = format("{stack_root}/current/{component_directory}")

hbase_conf_dir = status_params.hbase_conf_dir
limits_conf_dir = status_params.limits_conf_dir

hbase_user_nofile_limit = default(
  "/configurations/hbase-env/hbase_user_nofile_limit", "32000"
)
hbase_user_nproc_limit = default(
  "/configurations/hbase-env/hbase_user_nproc_limit", "16000"
)

hbase_excluded_hosts = default("/commandParams/excluded_hosts", "")
hbase_drain_only = as_bool(default("/commandParams/mark_draining_only", False))
hbase_included_hosts = default("/commandParams/included_hosts", "")

hbase_user = status_params.hbase_user
hbase_principal_name = config["configurations"]["hbase-env"]["hbase_principal_name"]
user_group = config["configurations"]["cluster-env"]["user_group"]
security_enabled_value = config["configurations"]["cluster-env"]["security_enabled"]
security_enabled = as_bool(security_enabled_value)

metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config["ambariLevelParams"]["java_home"]
ambari_java_home = config["ambariLevelParams"]["ambari_java_home"]
log_dir = config["configurations"]["hbase-env"]["hbase_log_dir"]
java_io_tmpdir = default("/configurations/hbase-env/hbase_java_io_tmpdir", "/tmp")
master_heapsize = ensure_unit_for_memory(
  config["configurations"]["hbase-env"]["hbase_master_heapsize"]
)

regionserver_heapsize = ensure_unit_for_memory(
  config["configurations"]["hbase-env"]["hbase_regionserver_heapsize"]
)
regionserver_xmn_max = config["configurations"]["hbase-env"][
  "hbase_regionserver_xmn_max"
]
regionserver_xmn_percent = expect(
  "/configurations/hbase-env/hbase_regionserver_xmn_ratio", float
)
regionserver_xmn_size = calc_xmn_from_xms(
  regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max
)

parallel_gc_threads = expect("/configurations/hbase-env/hbase_parallel_gc_threads", int)
if parallel_gc_threads <= 0:
  raise Fail("HBase parallel GC thread count must be a positive integer")
if hbase_max_direct_memory_size not in (None, ""):
  if not str(hbase_max_direct_memory_size).isascii() or not str(
    hbase_max_direct_memory_size
  ).isdigit() or int(hbase_max_direct_memory_size) <= 0:
    raise Fail("HBase maximum direct memory size must be a positive integer")

hbase_regionserver_shutdown_timeout = expect(
  "/configurations/hbase-env/hbase_regionserver_shutdown_timeout", int, 30
)
if hbase_regionserver_shutdown_timeout <= 0:
  raise Fail("HBase shutdown timeout must be a positive integer")
hbase_region_mover_timeout = expect(
  "/configurations/hbase-env/hbase_region_mover_timeout", int, 540
)
if hbase_region_mover_timeout <= 0:
  raise Fail("HBase RegionMover timeout must be a positive integer")

phoenix_enabled_value = default("/configurations/hbase-env/phoenix_sql_enabled", False)
phoenix_enabled = strict_bool(phoenix_enabled_value, "hbase-env/phoenix_sql_enabled")
phoenix_home = format("{stack_root}/current/phoenix-server")
phoenix_home_shell = quote_bash_args(str(phoenix_home))

hbase_thrift_port = expect(
  "/configurations/hbase-thrift-site/hbase.thrift.port", int, 9091
)
hbase_thrift_info_port = expect(
  "/configurations/hbase-thrift-site/hbase.thrift.info.port", int, 9095
)
for port_name, port in (
  ("hbase.thrift.port", hbase_thrift_port),
  ("hbase.thrift.info.port", hbase_thrift_info_port),
):
  if not 1 <= port <= 65535:
    raise Fail(f"{port_name} must be between 1 and 65535")

pid_dir = status_params.pid_dir
tmp_dir = config["configurations"]["hbase-site"]["hbase.tmp.dir"]
ioengine_param = default("/configurations/hbase-site/hbase.bucketcache.ioengine", None)

java64_home_shell = quote_bash_args(str(java64_home))
hbase_conf_dir_shell = quote_bash_args(str(hbase_conf_dir))
log_dir_shell = quote_bash_args(str(log_dir))
pid_dir_shell = quote_bash_args(str(pid_dir))
java_io_tmpdir_shell = quote_bash_args(str(java_io_tmpdir))
hbase_user_shell = quote_bash_args(str(hbase_user))

ganglia_server_hosts = default(
  "/clusterHostInfo/ganglia_server_host", []
)  # is not passed when ganglia is not present
has_ganglia_server = not len(ganglia_server_hosts) == 0
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

set_instanceId = "false"
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
has_metric_collector = not len(ams_collector_hosts) == 0
metric_collector_port = None
if has_metric_collector:
  if (
    "cluster-env" in config["configurations"]
    and "metrics_collector_external_port" in config["configurations"]["cluster-env"]
  ):
    metric_collector_port = config["configurations"]["cluster-env"][
      "metrics_collector_external_port"
    ]
  else:
    metric_collector_web_address = default(
      "/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188"
    )
    if metric_collector_web_address.find(":") != -1:
      metric_collector_port = metric_collector_web_address.split(":")[1]
    else:
      metric_collector_port = "6188"
  if (
    default(
      "/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY"
    )
    == "HTTPS_ONLY"
  ):
    metric_collector_protocol = "https"
  else:
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
metrics_report_interval = default(
  "/configurations/ams-site/timeline.metrics.sink.report.interval", 60
)
metrics_collection_period = default(
  "/configurations/ams-site/timeline.metrics.sink.collection.period", 10
)

host_in_memory_aggregation = default(
  "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True
)
host_in_memory_aggregation_port = default(
  "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888
)
is_aggregation_https_enabled = False
if (
  default(
    "/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy",
    "HTTP_ONLY",
  )
  == "HTTPS_ONLY"
):
  host_in_memory_aggregation_protocol = "https"
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = "http"

# if hbase is selected the hbase_regionserver_hosts, should not be empty, but still default just in case
if "datanode_hosts" in config["clusterHostInfo"]:
  rs_hosts = default(
    "/clusterHostInfo/hbase_regionserver_hosts", "/clusterHostInfo/datanode_hosts"
  )  # if hbase_regionserver_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default(
    "/clusterHostInfo/hbase_regionserver_hosts", "/clusterHostInfo/all_hosts"
  )

smoke_test_user = config["configurations"]["cluster-env"]["smokeuser"]
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
smokeuser_permissions = "RWXCA"
service_check_data = get_unique_id_and_date()
if security_enabled:
  zk_principal_name = default(
    "/configurations/zookeeper-env/zookeeper_principal_name",
    "zookeeper/_HOST@EXAMPLE.COM",
  )
  zk_principal_user = zk_principal_name.split("/")[0]
  if not re.fullmatch(r"[A-Za-z0-9._-]+", zk_principal_user):
    raise Fail("ZooKeeper Kerberos service name contains unsafe characters")
  zk_security_opts = format(
    "-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Dzookeeper.sasl.clientconfig=Client"
  )
  _hostname_lowercase = config["agentLevelParams"]["hostname"].lower()
  master_jaas_princ = config["configurations"]["hbase-site"][
    "hbase.master.kerberos.principal"
  ].replace("_HOST", _hostname_lowercase)
  master_keytab_path = config["configurations"]["hbase-site"][
    "hbase.master.keytab.file"
  ]
  regionserver_jaas_princ = config["configurations"]["hbase-site"][
    "hbase.regionserver.kerberos.principal"
  ].replace("_HOST", _hostname_lowercase)
  thrift_jaas_princ = default(
    "/configurations/hbase-site/hbase.thrift.kerberos.principal",
    config["configurations"]["hbase-site"][
      "hbase.master.kerberos.principal"
    ],
  ).replace("_HOST", _hostname_lowercase)
  thrift_keytab_path = default(
    "/configurations/hbase-site/hbase.thrift.keytab.file",
    master_keytab_path,
  )

regionserver_keytab_path = config["configurations"]["hbase-site"][
  "hbase.regionserver.keytab.file"
]
smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]
hbase_user_keytab = config["configurations"]["hbase-env"]["hbase_user_keytab"]
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
# log4j.properties
# HBase log4j settings
hbase_log_maxfilesize = default("configurations/hbase-log4j/hbase_log_maxfilesize", 256)
hbase_log_maxbackupindex = default(
  "configurations/hbase-log4j/hbase_log_maxbackupindex", 20
)
hbase_security_log_maxfilesize = default(
  "configurations/hbase-log4j/hbase_security_log_maxfilesize", 256
)
hbase_security_log_maxbackupindex = default(
  "configurations/hbase-log4j/hbase_security_log_maxbackupindex", 20
)

if ("hbase-log4j" in config["configurations"]) and (
  "content" in config["configurations"]["hbase-log4j"]
):
  log4j_props = config["configurations"]["hbase-log4j"]["content"]
else:
  log4j_props = None

hbase_env_sh_template = config["configurations"]["hbase-env"]["content"]

hbase_hdfs_root_dir = config["configurations"]["hbase-site"]["hbase.rootdir"]
# for create_hdfs_directory
hostname = config["agentLevelParams"]["hostname"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]

hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]

dfs_type = default("/clusterLevelParams/dfs_type", "")

import functools

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

zookeeper_znode_parent = config["configurations"]["hbase-site"][
  "zookeeper.znode.parent"
]
hbase_zookeeper_quorum = config["configurations"]["hbase-site"][
  "hbase.zookeeper.quorum"
]
hbase_zookeeper_property_clientPort = config["configurations"]["hbase-site"][
  "hbase.zookeeper.property.clientPort"
]
hbase_security_authentication = config["configurations"]["hbase-site"][
  "hbase.security.authentication"
]
hadoop_security_authentication = config["configurations"]["core-site"][
  "hadoop.security.authentication"
]

# ranger hbase plugin section start

# to get db connector jar
jdk_location = config["ambariLevelParams"]["jdk_location"]

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env introduced, using stack feature
xml_configurations_supported = check_stack_feature(
  StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks
)

# ranger hbase plugin enabled property
enable_ranger_hbase = default(
  "/configurations/ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled", "No"
)
enable_ranger_hbase = as_bool(enable_ranger_hbase)

# ranger hbase properties
if enable_ranger_hbase:
  # get ranger policy url
  policymgr_mgr_url = config["configurations"]["admin-properties"][
    "policymgr_external_url"
  ]
  if xml_configurations_supported:
    policymgr_mgr_url = config["configurations"]["ranger-hbase-security"][
      "ranger.plugin.hbase.policy.rest.url"
    ]

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith("/"):
    policymgr_mgr_url = policymgr_mgr_url.rstrip("/")

  # ranger audit db user
  xa_audit_db_user = default(
    "/configurations/admin-properties/audit_db_user", "rangerlogger"
  )

  # ranger hbase service/repository name
  repo_name = str(config["clusterName"]) + "_hbase"
  repo_name_value = config["configurations"]["ranger-hbase-security"][
    "ranger.plugin.hbase.service.name"
  ]
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  common_name_for_certificate = config["configurations"][
    "ranger-hbase-plugin-properties"
  ]["common.name.for.certificate"]
  repo_config_username = config["configurations"]["ranger-hbase-plugin-properties"][
    "REPOSITORY_CONFIG_USERNAME"
  ]
  ranger_plugin_properties = config["configurations"]["ranger-hbase-plugin-properties"]
  policy_user = config["configurations"]["ranger-hbase-plugin-properties"][
    "policy_user"
  ]
  repo_config_password = config["configurations"]["ranger-hbase-plugin-properties"][
    "REPOSITORY_CONFIG_PASSWORD"
  ]
  if not isinstance(repo_config_password, str) or not repo_config_password.strip():
    raise Fail(
      "ranger-hbase-plugin-properties/REPOSITORY_CONFIG_PASSWORD must not be "
      "empty when the Ranger HBase plugin is enabled"
    )

  # ranger-env config
  ranger_env = config["configurations"]["ranger-env"]

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_hbase:
    external_credentials = require_external_ranger_credentials(
      config["configurations"]["ranger-hbase-plugin-properties"]
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

  xa_audit_db_password = ""
  if (
    not is_empty(config["configurations"]["admin-properties"]["audit_db_password"])
    and stack_supports_ranger_audit_db
    and has_ranger_admin
  ):
    xa_audit_db_password = config["configurations"]["admin-properties"][
      "audit_db_password"
    ]

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

    downloaded_custom_connector = (
      format("{exec_tmp_dir}/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    driver_curl_source = (
      format("{jdk_location}/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    driver_curl_target = (
      format("{stack_root}/current/{component_directory}/lib/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    previous_jdbc_jar = (
      format("{stack_root}/current/{component_directory}/lib/{previous_jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    sql_connector_jar = ""

  if security_enabled:
    master_principal = config["configurations"]["hbase-site"][
      "hbase.master.kerberos.principal"
    ]

  hbase_ranger_plugin_config = {
    "username": repo_config_username,
    "password": repo_config_password,
    "hadoop.security.authentication": hadoop_security_authentication,
    "hbase.security.authentication": hbase_security_authentication,
    "hbase.zookeeper.property.clientPort": hbase_zookeeper_property_clientPort,
    "hbase.zookeeper.quorum": hbase_zookeeper_quorum,
    "zookeeper.znode.parent": zookeeper_znode_parent,
    "commonNameForCertificate": common_name_for_certificate,
    "hbase.master.kerberos.principal": master_principal if security_enabled else "",
  }

  if security_enabled:
    hbase_ranger_plugin_config["policy.download.auth.users"] = hbase_user
    hbase_ranger_plugin_config["tag.download.auth.users"] = hbase_user
    hbase_ranger_plugin_config["policy.grantrevoke.auth.users"] = hbase_user

  hbase_ranger_plugin_config["setup.additional.default.policies"] = "true"
  hbase_ranger_plugin_config["default-policy.1.name"] = (
    "Service Check User Policy for Hbase"
  )
  hbase_ranger_plugin_config["default-policy.1.resource.table"] = "ambarismoketest"
  hbase_ranger_plugin_config["default-policy.1.resource.column-family"] = "*"
  hbase_ranger_plugin_config["default-policy.1.resource.column"] = "*"
  hbase_ranger_plugin_config["default-policy.1.policyItem.1.users"] = policy_user
  hbase_ranger_plugin_config["default-policy.1.policyItem.1.accessTypes"] = (
    "read,write,create"
  )

  custom_ranger_service_config = generate_ranger_service_config(
    ranger_plugin_properties
  )
  if len(custom_ranger_service_config) > 0:
    hbase_ranger_plugin_config.update(custom_ranger_service_config)

  hbase_ranger_plugin_repo = {
    "isEnabled": "true",
    "configs": hbase_ranger_plugin_config,
    "description": "hbase repo",
    "name": repo_name,
    "type": "hbase",
  }

  ranger_hbase_principal = None
  ranger_hbase_keytab = None
  if (
    stack_supports_ranger_kerberos
    and security_enabled
    and "hbase-master" in component_directory.lower()
  ):
    ranger_hbase_principal = master_jaas_princ
    ranger_hbase_keytab = master_keytab_path
  elif (
    stack_supports_ranger_kerberos
    and security_enabled
    and "hbase-regionserver" in component_directory.lower()
  ):
    ranger_hbase_principal = regionserver_jaas_princ
    ranger_hbase_keytab = regionserver_keytab_path

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = as_bool(
      config["configurations"]["ranger-hbase-audit"][
        "xasecure.audit.destination.db"
      ]
    )

  xa_audit_hdfs_is_enabled = as_bool(
    config["configurations"]["ranger-hbase-audit"][
      "xasecure.audit.destination.hdfs"
    ]
    if xml_configurations_supported
    else False
  )
  ssl_keystore_password = (
    config["configurations"]["ranger-hbase-policymgr-ssl"][
      "xasecure.policymgr.clientssl.keystore.password"
    ]
    if xml_configurations_supported
    else None
  )
  ssl_truststore_password = (
    config["configurations"]["ranger-hbase-policymgr-ssl"][
      "xasecure.policymgr.clientssl.truststore.password"
    ]
    if xml_configurations_supported
    else None
  )
  credential_file = format("/etc/ranger/{repo_name}/cred.jceks")

  # for SQLA explicitly disable audit to DB for Ranger
  if (
    has_ranger_admin
    and stack_supports_ranger_audit_db
    and xa_audit_db_flavor.lower() == "sqla"
  ):
    xa_audit_db_is_enabled = False

# need this to capture cluster name from where ranger hbase plugin is enabled
cluster_name = config["clusterName"]

# ranger hbase plugin section end
stack_version_formatted_major = status_params.stack_version_formatted
ranger_plugin_home = format("{hbase_home}/../ranger-{service_name}-plugin")
create_hbase_home_directory = check_stack_feature(
  StackFeature.HBASE_HOME_DIRECTORY, stack_version_formatted
)
hbase_home_directory = format("/user/{hbase_user}")
