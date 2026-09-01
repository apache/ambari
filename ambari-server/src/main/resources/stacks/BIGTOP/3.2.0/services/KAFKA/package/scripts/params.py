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
import json
import os
import re

import kafka_client
from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import (
  get_stack_feature_version,
)
from resource_management.libraries.functions.default import default
from utils import (
  as_bool,
  as_yes_no,
  get_bare_principal,
  ranger_environment,
  validate_config_segment,
)
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions.is_empty import is_empty
import status_params
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  get_audit_configs,
  generate_ranger_service_config,
)


def positive_limit(value, name):
  try:
    result = int(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{name} must be a positive integer") from error
  if result < 1:
    raise Fail(f"{name} must be a positive integer")
  return result


def jaas_escape(value):
  return json.dumps(str(value or ""), ensure_ascii=True)[1:-1]


def safe_service_directory(value, name):
  value = str(value or "")
  if (
    not os.path.isabs(value)
    or os.path.normpath(value) != value
    or value == os.path.sep
  ):
    raise Fail(f"{name} must be a safe absolute directory")
  protected_trees = ("/boot", "/dev", "/etc", "/proc", "/sys", "/usr")
  if any(value == root or value.startswith(root + os.path.sep) for root in protected_trees):
    raise Fail(f"{name} must not be inside a protected system directory")
  if value in (
    "/bin",
    "/data",
    "/home",
    "/lib",
    "/lib64",
    "/mnt",
    "/opt",
    "/run",
    "/sbin",
    "/srv",
    "/tmp",
    "/var",
    "/var/lib",
    "/var/log",
    "/var/run",
  ):
    raise Fail(f"{name} must not use a protected system directory")
  return value


# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()
retryAble = as_bool(
  default("/commandParams/command_retry_enabled", False),
  "commandParams/command_retry_enabled",
)
service_name = "kafka"
# Version being upgraded/downgraded to
version = default("/commandParams/version", None)

stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted = format_stack_version(stack_version_unformatted)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(
  StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks
)
stack_supports_ranger_audit_db = check_stack_feature(
  StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks
)
stack_supports_core_site_for_ranger_plugin = check_stack_feature(
  StackFeature.CORE_SITE_FOR_RANGER_PLUGINS_SUPPORT, version_for_stack_feature_checks
)
stack_supports_kafka_env_include_ranger_script = check_stack_feature(
  StackFeature.KAFKA_ENV_INCLUDE_RANGER_SCRIPT, version_for_stack_feature_checks
)

hostname = config["agentLevelParams"]["hostname"]

# default kafka parameters
kafka_home = "/usr/lib/kafka"
conf_dir = "/etc/kafka/conf"
limits_conf_dir = "/etc/security/limits.d"

# Used while upgrading the stack in a kerberized cluster and running kafka-acls.sh
zookeeper_connect = default("/configurations/kafka-broker/zookeeper.connect", None)

kafka_user_nofile_limit = positive_limit(
  default("/configurations/kafka-env/kafka_user_nofile_limit", 128000),
  "kafka-env/kafka_user_nofile_limit",
)
kafka_user_nproc_limit = positive_limit(
  default("/configurations/kafka-env/kafka_user_nproc_limit", 65536),
  "kafka-env/kafka_user_nproc_limit",
)

kafka_delete_topic_enable = as_bool(
  default("/configurations/kafka-broker/delete.topic.enable", True),
  "kafka-broker/delete.topic.enable",
)

# parameters for 2.2+
if stack_version_formatted and check_stack_feature(
  StackFeature.ROLLING_UPGRADE, stack_version_formatted
):
  kafka_home = os.path.join(stack_root, "current", "kafka-broker")

kafka_server_start = os.path.join(kafka_home, "bin", "kafka-server-start.sh")
kafka_topics = os.path.join(kafka_home, "bin", "kafka-topics.sh")
kafka_server_properties = os.path.join(conf_dir, "server.properties")
kafka_client_properties = os.path.join(conf_dir, "kafka-client.properties")
kafka_env_file = os.path.join(conf_dir, "kafka-env.sh")
kafka_client_jaas_file = os.path.join(conf_dir, "kafka_client_jaas.conf")

kafka_user = config["configurations"]["kafka-env"]["kafka_user"]
if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", kafka_user, re.ASCII):
  raise Fail("kafka-env/kafka_user is not a valid service user")
kafka_log_dir = safe_service_directory(
  config["configurations"]["kafka-env"]["kafka_log_dir"],
  "kafka-env/kafka_log_dir",
)
kafka_pid_dir = safe_service_directory(
  status_params.kafka_pid_dir, "kafka-env/kafka_pid_dir"
)
kafka_pid_file = kafka_pid_dir + "/kafka.pid"
user_group = config["configurations"]["cluster-env"]["user_group"]
java64_home = config["ambariLevelParams"]["java_home"]
ambari_java_home = config["ambariLevelParams"]["ambari_java_home"]
kafka_env_sh_template = config["configurations"]["kafka-env"]["content"]
kafka_jaas_conf_template = default("/configurations/kafka_jaas_conf/content", None)
kafka_client_jaas_conf_template = default(
  "/configurations/kafka_client_jaas_conf/content", None
)
kafka_hosts = sorted(config["clusterHostInfo"]["kafka_broker_hosts"])

secure_acls = as_bool(
  default("/configurations/kafka-broker/zookeeper.set.acl", False),
  "kafka-broker/zookeeper.set.acl",
)
kafka_security_migrator = os.path.join(
  kafka_home, "bin", "zookeeper-security-migration.sh"
)

all_hosts = default("/clusterHostInfo/all_hosts", [])
all_racks = default("/clusterHostInfo/all_racks", [])

# Kafka log4j
kafka_log_maxfilesize = default(
  "/configurations/kafka-log4j/kafka_log_maxfilesize", 256
)
kafka_log_maxbackupindex = default(
  "/configurations/kafka-log4j/kafka_log_maxbackupindex", 20
)
controller_log_maxfilesize = default(
  "/configurations/kafka-log4j/controller_log_maxfilesize", 256
)
controller_log_maxbackupindex = default(
  "/configurations/kafka-log4j/controller_log_maxbackupindex", 20
)

if ("kafka-log4j" in config["configurations"]) and (
  "content" in config["configurations"]["kafka-log4j"]
):
  log4j_props = config["configurations"]["kafka-log4j"]["content"]
else:
  log4j_props = None

metric_collector_port = ""
metric_collector_protocol = ""
metric_truststore_path = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.location", ""
)
metric_truststore_type = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.type", ""
)
metric_truststore_password = default(
  "/configurations/ams-ssl-client/ssl.client.truststore.password", ""
)

if (
  "cluster-env" in config["configurations"]
  and "metrics_collector_external_hosts" in config["configurations"]["cluster-env"]
):
  ams_collector_hosts = config["configurations"]["cluster-env"][
    "metrics_collector_external_hosts"
  ]
else:
  ams_collector_hosts = ",".join(
    default("/clusterHostInfo/metrics_collector_hosts", [])
  )

has_metric_collector = not len(ams_collector_hosts) == 0

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

# Security-related params
kerberos_security_enabled = as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "cluster-env/security_enabled",
)
kafka_kerberos_merge_advertised_listeners = as_bool(
  default("/configurations/kafka-env/kerberos_merge_advertised_listeners", True),
  "kafka-env/kerberos_merge_advertised_listeners",
)

kafka_broker_security = config["configurations"]["kafka-broker"]
kafka_inter_broker_protocol = kafka_client.inter_broker_protocol(
  kafka_broker_security
)
kafka_inter_broker_sasl_mechanism = str(
  kafka_broker_security.get("sasl.mechanism.inter.broker.protocol", "GSSAPI")
).upper()
kafka_sasl_enabled = kafka_inter_broker_protocol.startswith("SASL_")
kafka_kerberos_enabled = (
  kafka_sasl_enabled and kafka_inter_broker_sasl_mechanism == "GSSAPI"
)

kafka_other_sasl_enabled = (
  kafka_sasl_enabled
  and not (kerberos_security_enabled and kafka_kerberos_enabled)
  and check_stack_feature(StackFeature.KAFKA_LISTENERS, stack_version_formatted)
  and check_stack_feature(
    StackFeature.KAFKA_EXTENDED_SASL_SUPPORT,
    format_stack_version(version_for_stack_feature_checks),
  )
)

if (
  (kerberos_security_enabled or secure_acls)
  and stack_version_formatted != ""
  and "kafka_principal_name" in config["configurations"]["kafka-env"]
  and check_stack_feature(StackFeature.KAFKA_KERBEROS, stack_version_formatted)
):
  _hostname_lowercase = config["agentLevelParams"]["hostname"].lower()
  _kafka_principal_name = config["configurations"]["kafka-env"]["kafka_principal_name"]
  kafka_jaas_principal = _kafka_principal_name.replace("_HOST", _hostname_lowercase)
  kafka_keytab_path = config["configurations"]["kafka-env"]["kafka_keytab"]
  kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)
  kafka_kerberos_params = (
    "-Djava.security.auth.login.config=" + conf_dir + "/kafka_jaas.conf"
  )
elif kafka_other_sasl_enabled:
  kafka_kerberos_params = (
    "-Djava.security.auth.login.config=" + conf_dir + "/kafka_jaas.conf"
  )
  kafka_jaas_principal = None
  kafka_keytab_path = None
  kafka_bare_jaas_principal = None
else:
  kafka_kerberos_params = ""
  kafka_jaas_principal = None
  kafka_keytab_path = None
  kafka_bare_jaas_principal = None

kafka_kerberos_credentials_enabled = bool(
  kafka_keytab_path and kafka_jaas_principal and kafka_bare_jaas_principal
)
kafka_jaas_enabled = (
  kafka_kerberos_credentials_enabled
  and (kafka_kerberos_enabled or secure_acls)
) or kafka_other_sasl_enabled

kafka_keytab_path_jaas = jaas_escape(kafka_keytab_path)
kafka_jaas_principal_jaas = jaas_escape(kafka_jaas_principal)
kafka_bare_jaas_principal_jaas = jaas_escape(kafka_bare_jaas_principal)
java64_home_shell = quote_bash_args(str(java64_home))
kafka_pid_dir_shell = quote_bash_args(str(kafka_pid_dir))
kafka_log_dir_shell = quote_bash_args(str(kafka_log_dir))
conf_dir_shell = quote_bash_args(conf_dir)
kafka_opts = kafka_kerberos_params
if (
  kafka_kerberos_credentials_enabled
  and (kafka_kerberos_enabled or secure_acls)
) or kafka_other_sasl_enabled:
  kafka_opts = "-Djavax.security.auth.useSubjectCredsOnly=false " + kafka_opts
kafka_opts_shell = quote_bash_args(kafka_opts.strip())

kafka_service_check_properties = dict(config["configurations"]["kafka-broker"])
if kerberos_security_enabled and kafka_kerberos_enabled:
  for property_name in ("listeners", "advertised.listeners"):
    if property_name in kafka_service_check_properties:
      kafka_service_check_properties[property_name] = kafka_client.sasl_listeners(
        kafka_service_check_properties[property_name]
      )
  if "listener.security.protocol.map" in kafka_service_check_properties:
    kafka_service_check_properties["listener.security.protocol.map"] = (
      kafka_client.sasl_listener_protocol_map(
        kafka_service_check_properties["listener.security.protocol.map"]
      )
    )
  if "security.inter.broker.protocol" in kafka_service_check_properties:
    kafka_service_check_properties["security.inter.broker.protocol"] = (
      kafka_service_check_properties["security.inter.broker.protocol"].replace(
        "PLAINTEXTSASL", "SASL_PLAINTEXT"
      )
    )

kafka_bootstrap_servers = kafka_client.bootstrap_servers(
  kafka_service_check_properties, kafka_hosts
)
_, kafka_service_check_protocol, _ = kafka_client.select_listener(
  kafka_service_check_properties
)
kafka_service_check_sasl_mechanism = kafka_service_check_properties.get(
  "sasl.mechanism.inter.broker.protocol", "GSSAPI"
)
kafka_service_check_uses_sasl = kafka_service_check_protocol.startswith("SASL_")
kafka_service_check_uses_kerberos = (
  kafka_service_check_uses_sasl
  and kafka_service_check_sasl_mechanism.upper() == "GSSAPI"
)
kafka_service_check_timeout = 60

# for curl command in ranger plugin to get db connector
jdk_location = config["ambariLevelParams"]["jdk_location"]

# ranger kafka plugin section start
ranger_plugin_home = format("{kafka_home}/../ranger-{service_name}-plugin")
# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(
  StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks
)

# ranger kafka plugin enabled property
enable_ranger_kafka = default(
  "configurations/ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled", "No"
)
enable_ranger_kafka = as_yes_no(
  enable_ranger_kafka,
  "ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled",
)

# ranger kafka-plugin supported flag, instead of dependending on is_supported_kafka_ranger/kafka-env.xml, using stack feature
is_supported_kafka_ranger = check_stack_feature(
  StackFeature.KAFKA_RANGER_PLUGIN_SUPPORT, version_for_stack_feature_checks
)

# ranger kafka properties
if enable_ranger_kafka and is_supported_kafka_ranger:
  # get ranger policy url
  policymgr_mgr_url = config["configurations"]["ranger-kafka-security"][
    "ranger.plugin.kafka.policy.rest.url"
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

  # ranger kafka service/repository name
  repo_name = validate_config_segment(
    str(config["clusterName"]) + "_kafka", "Ranger Kafka service name"
  )
  repo_name_value = config["configurations"]["ranger-kafka-security"][
    "ranger.plugin.kafka.service.name"
  ]
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = validate_config_segment(
      repo_name_value, "ranger-kafka-security/ranger.plugin.kafka.service.name"
    )

  ranger_env = ranger_environment(config["configurations"], has_ranger_admin)

  ranger_plugin_properties = config["configurations"]["ranger-kafka-plugin-properties"]
  ranger_kafka_audit = config["configurations"]["ranger-kafka-audit"]
  ranger_kafka_audit_attrs = config["configurationAttributes"]["ranger-kafka-audit"]
  ranger_kafka_security = config["configurations"]["ranger-kafka-security"]
  ranger_kafka_security_attrs = config["configurationAttributes"][
    "ranger-kafka-security"
  ]
  ranger_kafka_policymgr_ssl = config["configurations"]["ranger-kafka-policymgr-ssl"]
  ranger_kafka_policymgr_ssl_attrs = config["configurationAttributes"][
    "ranger-kafka-policymgr-ssl"
  ]

  policy_user = config["configurations"]["ranger-kafka-plugin-properties"][
    "policy_user"
  ]

  ranger_plugin_config = {
    "username": config["configurations"]["ranger-kafka-plugin-properties"][
      "REPOSITORY_CONFIG_USERNAME"
    ],
    "password": config["configurations"]["ranger-kafka-plugin-properties"][
      "REPOSITORY_CONFIG_PASSWORD"
    ],
    "zookeeper.connect": config["configurations"]["ranger-kafka-plugin-properties"][
      "zookeeper.connect"
    ],
    "commonNameForCertificate": config["configurations"][
      "ranger-kafka-plugin-properties"
    ]["common.name.for.certificate"],
  }

  atlas_server_hosts = default("/clusterHostInfo/atlas_server_hosts", [])
  has_atlas_server = not len(atlas_server_hosts) == 0
  hive_server_hosts = default("/clusterHostInfo/hive_server_hosts", [])
  has_hive_server = not len(hive_server_hosts) == 0
  hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
  has_hbase_master = not len(hbase_master_hosts) == 0
  ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
  has_ranger_tagsync = not len(ranger_tagsync_hosts) == 0
  spark_jobhistoryserver_hosts = default(
    "/clusterHostInfo/spark_jobhistoryserver_hosts", []
  )
  has_jobhistoryserver = not len(spark_jobhistoryserver_hosts) == 0

  if has_atlas_server:
    atlas_notification_topics = default(
      "/configurations/application-properties/atlas.notification.topics",
      "ATLAS_HOOK,ATLAS_ENTITIES",
    )
    atlas_notification_topics_list = atlas_notification_topics.split(",")
    hive_user = default("/configurations/hive-env/hive_user", "hive")
    hbase_user = default("/configurations/hbase-env/hbase_user", "hbase")
    atlas_user = default("/configurations/atlas-env/metadata_user", "atlas")
    rangertagsync_user = default(
      "/configurations/ranger-tagsync-site/ranger.tagsync.dest.ranger.username",
      "rangertagsync",
    )
    spark_user = "spark_atlas"
    if len(atlas_notification_topics_list) == 2:
      atlas_hook = atlas_notification_topics_list[0]
      atlas_entity = atlas_notification_topics_list[1]
      ranger_plugin_config["setup.additional.default.policies"] = "true"
      ranger_plugin_config["default-policy.1.name"] = atlas_hook
      ranger_plugin_config["default-policy.1.resource.topic"] = atlas_hook
      hook_policy_user = []
      if has_hive_server:
        hook_policy_user.append(hive_user)
      if has_hbase_master:
        hook_policy_user.append(hbase_user)
      if has_jobhistoryserver:
        hook_policy_user.append(spark_user)
      if len(hook_policy_user) > 0:
        ranger_plugin_config["default-policy.1.policyItem.1.users"] = ",".join(
          hook_policy_user
        )
        ranger_plugin_config["default-policy.1.policyItem.1.accessTypes"] = "publish"
      ranger_plugin_config["default-policy.1.policyItem.2.users"] = atlas_user
      ranger_plugin_config["default-policy.1.policyItem.2.accessTypes"] = "consume"
      ranger_plugin_config["default-policy.2.name"] = atlas_entity
      ranger_plugin_config["default-policy.2.resource.topic"] = atlas_entity
      ranger_plugin_config["default-policy.2.policyItem.1.users"] = atlas_user
      ranger_plugin_config["default-policy.2.policyItem.1.accessTypes"] = "publish"
      if has_ranger_tagsync:
        ranger_plugin_config["default-policy.2.policyItem.2.users"] = rangertagsync_user
        ranger_plugin_config["default-policy.2.policyItem.2.accessTypes"] = "consume"

  if kerberos_security_enabled:
    ranger_plugin_config["policy.download.auth.users"] = kafka_user
    ranger_plugin_config["tag.download.auth.users"] = kafka_user

  custom_ranger_service_config = generate_ranger_service_config(
    ranger_plugin_properties
  )
  if len(custom_ranger_service_config) > 0:
    ranger_plugin_config.update(custom_ranger_service_config)

  kafka_ranger_plugin_repo = {
    "isEnabled": "true",
    "configs": ranger_plugin_config,
    "description": "kafka repo",
    "name": repo_name,
    "repositoryType": "kafka",
    "type": "kafka",
    "assetType": "1",
  }

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
      format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    )
    driver_curl_source = (
      format("{jdk_location}/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    driver_curl_target = (
      format("{kafka_home}/libs/{jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )
    previous_jdbc_jar = (
      format("{kafka_home}/libs/{previous_jdbc_jar_name}")
      if stack_supports_ranger_audit_db
      else None
    )

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = as_bool(
      config["configurations"]["ranger-kafka-audit"][
        "xasecure.audit.destination.db"
      ],
      "ranger-kafka-audit/xasecure.audit.destination.db",
    )

  xa_audit_hdfs_is_enabled = as_bool(
    default(
      "/configurations/ranger-kafka-audit/xasecure.audit.destination.hdfs", False
    ),
    "ranger-kafka-audit/xasecure.audit.destination.hdfs",
  )
  ssl_keystore_password = (
    config["configurations"]["ranger-kafka-policymgr-ssl"][
      "xasecure.policymgr.clientssl.keystore.password"
    ]
    if xml_configurations_supported
    else None
  )
  ssl_truststore_password = (
    config["configurations"]["ranger-kafka-policymgr-ssl"][
      "xasecure.policymgr.clientssl.truststore.password"
    ]
    if xml_configurations_supported
    else None
  )
  credential_file = format("/etc/ranger/{repo_name}/cred.jceks")

  stack_version = get_stack_version("kafka-broker")
  setup_ranger_env_sh_source = format(
    "{stack_root}/{stack_version}/ranger-kafka-plugin/install/conf.templates/enable/kafka-ranger-env.sh"
  )
  setup_ranger_env_sh_target = format("{conf_dir}/kafka-ranger-env.sh")

  # for SQLA explicitly disable audit to DB for Ranger
  if (
    has_ranger_admin
    and stack_supports_ranger_audit_db
    and xa_audit_db_flavor.lower() == "sqla"
  ):
    xa_audit_db_is_enabled = False

is_ranger_kms_ssl_enabled = as_bool(
  default(
    "configurations/ranger-kms-site/ranger.service.https.attrib.ssl.enabled", False
  ),
  "ranger-kms-site/ranger.service.https.attrib.ssl.enabled",
)

# ranger kafka plugin section end

namenode_hosts = default("/clusterHostInfo/namenode_hosts", [])
has_namenode = not len(namenode_hosts) == 0

hdfs_user = (
  config["configurations"]["hadoop-env"]["hdfs_user"] if has_namenode else None
)
hdfs_user_keytab = (
  config["configurations"]["hadoop-env"]["hdfs_user_keytab"] if has_namenode else None
)
hdfs_principal_name = (
  config["configurations"]["hadoop-env"]["hdfs_principal_name"]
  if has_namenode
  else None
)
hdfs_site = config["configurations"]["hdfs-site"] if has_namenode else None
default_fs = (
  config["configurations"]["core-site"]["fs.defaultFS"] if has_namenode else None
)
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
hadoop_conf_dir = conf_select.get_hadoop_conf_dir() if has_namenode else None
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
dfs_type = default("/clusterLevelParams/dfs_type", "")
ranger_kafka_plugin_impl_path = format("{kafka_home}/libs/ranger-kafka-plugin-impl")
mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
if "viewfs-mount-table" in config["configurations"]:
  xml_inclusion_file_name = "viewfs-mount-table.xml"
  mount_table = config["configurations"]["viewfs-mount-table"]

  if "content" in mount_table and mount_table["content"].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(
      conf_dir, xml_inclusion_file_name
    )
    mount_table_content = mount_table["content"]

# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled=kerberos_security_enabled,
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
