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
from xml.sax.saxutils import escape as xml_escape

from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.script.script import Script

import infra_solr_utils
import status_params


def get_port_from_url(address):
  return address.split(":")[-1] if not is_empty(address) else address


def get_name_from_principal(principal):
  if not principal:
    return principal
  return principal.split("/", 1)[0].split("@", 1)[0]


def service_user(value, name):
  user = get_name_from_principal(value)
  return infra_solr_utils.validate_user(user, name)


def shell_assignment(value):
  return quote_bash_args(str(value))


config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_name = default("/clusterLevelParams/stack_name", None)
stack_version = config["clusterLevelParams"]["stack_version"]
infra_solr_utils.validate_bigtop_stack(stack_name, stack_version)

security_enabled = status_params.security_enabled
hostname = infra_solr_utils.validate_host(
  config["agentLevelParams"]["hostname"], "Infra Solr host"
)
infra_solr_conf = "/etc/ambari-infra-solr/conf"
infra_solr_include = f"{infra_solr_conf}/infra-solr-env.sh"
infra_solr_port = status_params.infra_solr_port
infra_solr_piddir = status_params.infra_solr_piddir
infra_solr_pidfile = status_params.infra_solr_pidfile
infra_solr_datadir = status_params.infra_solr_datadir
infra_solr_user = status_params.infra_solr_user
user_group = status_params.user_group

limits_conf_dir = "/etc/security/limits.d"
infra_solr_user_nofile_limit = infra_solr_utils.bounded_int(
  default(
    "/configurations/infra-solr-env/infra_solr_user_nofile_limit", 128000
  ),
  "Infra Solr open-file limit",
  1024,
  1048576,
)
infra_solr_user_nproc_limit = infra_solr_utils.bounded_int(
  default(
    "/configurations/infra-solr-env/infra_solr_user_nproc_limit", 65536
  ),
  "Infra Solr process limit",
  1024,
  1048576,
)

java64_home = infra_solr_utils.validate_absolute_path(
  config["ambariLevelParams"]["java_home"], "Java home"
)
java_version = expect("/ambariLevelParams/java_version", int)
if java_version < 17:
  raise Fail("Infra Solr requires Java 17 or newer")
ambari_java_home = infra_solr_utils.validate_absolute_path(
  config["ambariLevelParams"]["ambari_java_home"], "Ambari Java home"
)
ambari_java_exec = f"{ambari_java_home}/bin/java"

solr_dir = "/usr/lib/ambari-infra-solr"
solr_client_dir = "/usr/lib/ambari-infra-solr-client"
solr_bindir = f"{solr_dir}/bin"
solr_executable = f"{solr_bindir}/solr"
cloud_scripts = f"{solr_dir}/server/scripts/cloud-scripts"

zookeeper_port = infra_solr_utils.bounded_int(
  default("/configurations/zoo.cfg/clientPort", 2181),
  "ZooKeeper client port",
  1,
  65535,
)
zookeeper_hosts = sorted(
  infra_solr_utils.validate_host(host, "ZooKeeper host")
  for host in config["clusterHostInfo"]["zookeeper_server_hosts"]
)
if not zookeeper_hosts:
  raise Fail("Infra Solr requires at least one ZooKeeper server")
zookeeper_quorum = ",".join(f"{host}:{zookeeper_port}" for host in zookeeper_hosts)

infra_config = config["configurations"]["infra-solr-env"]
infra_solr_hosts = tuple(
  infra_solr_utils.validate_host(host, "Infra Solr host")
  for host in config["clusterHostInfo"]["infra_solr_hosts"]
)
if not infra_solr_hosts:
  raise Fail("Infra Solr requires at least one server host")
if len(set(infra_solr_hosts)) != len(infra_solr_hosts):
  raise Fail("Infra Solr server hosts must be unique")
infra_solr_znode = infra_solr_utils.validate_znode(infra_config["infra_solr_znode"])
infra_solr_min_mem = infra_solr_utils.bounded_int(
  infra_config["infra_solr_minmem"], "Infra Solr minimum heap", 512, 32768
)
infra_solr_max_mem = infra_solr_utils.bounded_int(
  infra_config["infra_solr_maxmem"], "Infra Solr maximum heap", 512, 32768
)
if infra_solr_min_mem > infra_solr_max_mem:
  raise Fail("Infra Solr minimum heap must not exceed maximum heap")
infra_solr_java_stack_size = infra_solr_utils.bounded_int(
  infra_config["infra_solr_java_stack_size"],
  "Infra Solr Java stack size",
  1,
  128,
)
infra_solr_data_resources_dir = os.path.join(infra_solr_datadir, "resources")
infra_solr_jmx_port = infra_solr_utils.bounded_int(
  infra_config["infra_solr_jmx_port"], "Infra Solr JMX port", 1, 65535
)
infra_solr_ssl_enabled = infra_solr_utils.as_bool(
  default("/configurations/infra-solr-env/infra_solr_ssl_enabled", False),
  "Infra Solr SSL setting",
)
infra_solr_keystore_location = infra_solr_utils.validate_absolute_path(
  infra_config["infra_solr_keystore_location"], "Infra Solr key store"
)
infra_solr_keystore_password = infra_solr_utils.validate_text(
  infra_config["infra_solr_keystore_password"], "Infra Solr key store password"
)
infra_solr_keystore_type = infra_solr_utils.validate_text(
  infra_config["infra_solr_keystore_type"], "Infra Solr key store type", 32
)
infra_solr_truststore_location = infra_solr_utils.validate_absolute_path(
  infra_config["infra_solr_truststore_location"], "Infra Solr trust store"
)
infra_solr_truststore_password = infra_solr_utils.validate_text(
  infra_config["infra_solr_truststore_password"],
  "Infra Solr trust store password",
)
infra_solr_truststore_type = infra_solr_utils.validate_text(
  infra_config["infra_solr_truststore_type"], "Infra Solr trust store type", 32
)
if infra_solr_ssl_enabled and (
  not infra_solr_keystore_password or not infra_solr_truststore_password
):
  raise Fail("Infra Solr SSL requires non-empty key and trust store passwords")
infra_solr_log_dir = infra_solr_utils.validate_service_directory(
  infra_config["infra_solr_log_dir"], "Infra Solr log directory"
)
if len({infra_solr_datadir, infra_solr_piddir, infra_solr_log_dir}) != 3:
  raise Fail("Infra Solr data, PID, and log directories must be distinct")
if infra_solr_port == infra_solr_jmx_port:
  raise Fail("Infra Solr HTTP and JMX ports must be distinct")
solr_env_content = infra_config["content"]
infra_solr_extra_java_opts = default(
  "/configurations/infra-solr-env/infra_solr_extra_java_opts", ""
)
infra_solr_extra_java_opts = " ".join(
  infra_solr_utils.validate_extra_java_options(infra_solr_extra_java_opts)
)
zk_quorum = infra_solr_utils.validate_zookeeper_quorum(
  format(
    default(
      "/configurations/infra-solr-env/infra_solr_zookeeper_quorum",
      zookeeper_quorum,
    )
  )
)

security_config = config["configurations"]["infra-solr-security-json"]
infra_solr_security_manually_managed = infra_solr_utils.as_bool(
  default(
    "/configurations/infra-solr-security-json/infra_solr_security_manually_managed",
    False,
  ),
  "Infra Solr manual security setting",
)
infra_solr_security_json_content = security_config["content"]
if infra_solr_security_json_content and infra_solr_security_json_content.strip():
  try:
    custom_security = json.loads(infra_solr_security_json_content)
  except (TypeError, ValueError) as error:
    raise Fail("Custom Infra Solr security.json must be valid JSON") from error
  if not isinstance(custom_security, dict):
    raise Fail("Custom Infra Solr security.json must contain one JSON object")

default_ranger_audit_users = "nn,hbase,hive,kafka,rangerkms,yarn"
infra_solr_jaas_file = f"{infra_solr_conf}/infra_solr_jaas.conf"
if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  infra_solr_kerberos_keytab = infra_solr_utils.validate_absolute_path(
    infra_config["infra_solr_kerberos_keytab"], "Infra Solr keytab"
  )
  infra_solr_kerberos_principal = infra_solr_utils.validate_principal(
    infra_config["infra_solr_kerberos_principal"].replace("_HOST", hostname),
    "Infra Solr principal",
  )
  infra_solr_web_kerberos_keytab = infra_solr_utils.validate_absolute_path(
    infra_config["infra_solr_web_kerberos_keytab"], "Infra Solr HTTP keytab"
  )
  infra_solr_web_kerberos_principal = infra_solr_utils.validate_principal(
    infra_config["infra_solr_web_kerberos_principal"].replace("_HOST", hostname),
    "Infra Solr HTTP principal",
  )
  infra_solr_kerberos_name_rules = infra_solr_utils.validate_text(
    infra_config["infra_solr_kerberos_name_rules"],
    "Infra Solr Kerberos name rules",
  )
  infra_solr_sasl_user = service_user(
    infra_solr_kerberos_principal, "Infra Solr SASL user"
  )
  kerberos_realm = infra_solr_utils.validate_realm(
    config["configurations"]["kerberos-env"]["realm"]
  )

  zookeeper_principal = default(
    "/configurations/zookeeper-env/zookeeper_principal_name",
    "zookeeper/_HOST@EXAMPLE.COM",
  )
  external_zk_enabled = infra_solr_utils.as_bool(
    default(
      "/configurations/infra-solr-env/infra_solr_zookeeper_external_enabled",
      False,
    ),
    "External ZooKeeper setting",
  )
  if external_zk_enabled:
    zookeeper_principal = default(
      "/configurations/infra-solr-env/infra_solr_zookeeper_external_principal",
      "zookeeper/_HOST@EXAMPLE.COM",
    )
  zk_principal_user = service_user(zookeeper_principal, "ZooKeeper service user")
  zk_security_opts = (
    "-Dzookeeper.sasl.client=true "
    f"-Dzookeeper.sasl.client.username={zk_principal_user} "
    "-Dzookeeper.sasl.clientconfig=Client"
  )

  ranger_key = "xasecure.audit.jaas.Client.option.principal"
  ranger_principals = (
    default(f"/configurations/ranger-hdfs-audit/{ranger_key}", "nn"),
    default(f"/configurations/ranger-hbase-audit/{ranger_key}", "hbase"),
    default(f"/configurations/ranger-hive-audit/{ranger_key}", "hive"),
    default(f"/configurations/ranger-kafka-audit/{ranger_key}", "kafka"),
    default(f"/configurations/ranger-kms-audit/{ranger_key}", "rangerkms"),
    default(f"/configurations/ranger-yarn-audit/{ranger_key}", "yarn"),
  )
  default_ranger_audit_users = ",".join(
    service_user(principal, "Ranger audit service user")
    for principal in ranger_principals
  )

infra_solr_ranger_audit_service_users = tuple(
  infra_solr_utils.validate_user(user.strip(), "Ranger audit service user")
  for user in format(
    security_config["infra_solr_ranger_audit_service_users"]
  ).split(",")
  if user.strip()
)
infra_solr_jmx_enabled = infra_solr_utils.as_bool(
  default("/configurations/infra-solr-env/infra_solr_jmx_enabled", False),
  "Infra Solr JMX setting",
)
infra_log_maxfilesize = infra_solr_utils.bounded_int(
  default("/configurations/infra-solr-log4j/infra_log_maxfilesize", 10),
  "Infra Solr log file size",
  1,
  10240,
)
infra_log_maxbackupindex = infra_solr_utils.bounded_int(
  default("/configurations/infra-solr-log4j/infra_log_maxbackupindex", 9),
  "Infra Solr log backup count",
  1,
  1000,
)
solr_xml_content = config["configurations"]["infra-solr-xml"]["content"]
solr_log4j_content = config["configurations"]["infra-solr-log4j"]["content"]

smokeuser = infra_solr_utils.validate_user(
  config["configurations"]["cluster-env"]["smokeuser"], "Smoke user"
)
smoke_user_keytab = infra_solr_utils.validate_absolute_path(
  config["configurations"]["cluster-env"]["smokeuser_keytab"],
  "Smoke user keytab",
)
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
ranger_solr_collection_name = infra_solr_utils.validate_solr_name(
  default("/configurations/ranger-env/ranger_solr_collection_name", "ranger_audits"),
  "Ranger Solr collection",
)
ranger_admin_kerberos_service_user = service_user(
  default(
    "/configurations/ranger-admin-site/ranger.admin.kerberos.principal",
    "rangeradmin",
  ),
  "Ranger admin service user",
)
infra_solr_kerberos_service_user = service_user(
  default(
    "/configurations/infra-solr-env/infra_solr_kerberos_principal",
    "infra-solr",
  ),
  "Infra Solr service user",
)
infra_solr_role_ranger_admin = infra_solr_utils.validate_user(
  default(
    "/configurations/infra-solr-security-json/infra_solr_role_ranger_admin",
    "ranger_admin_user",
  ),
  "Ranger admin role",
)
infra_solr_role_ranger_audit = infra_solr_utils.validate_user(
  default(
    "/configurations/infra-solr-security-json/infra_solr_role_ranger_audit",
    "ranger_audit_user",
  ),
  "Ranger audit role",
)
infra_solr_role_dev = infra_solr_utils.validate_user(
  default("/configurations/infra-solr-security-json/infra_solr_role_dev", "dev"),
  "Infra Solr read role",
)
security_roles = (
  infra_solr_role_ranger_admin,
  infra_solr_role_ranger_audit,
  infra_solr_role_dev,
)
if len(set(security_roles)) != len(security_roles):
  raise Fail("Infra Solr security roles must be unique")
if len(set(infra_solr_ranger_audit_service_users)) != len(
  infra_solr_ranger_audit_service_users
):
  raise Fail("Ranger audit service users must be unique")
reserved_security_users = {
  infra_solr_kerberos_service_user,
  ranger_admin_kerberos_service_user,
}
if reserved_security_users.intersection(infra_solr_ranger_audit_service_users):
  raise Fail("Ranger audit users must not duplicate Infra Solr or Ranger Admin")

metric_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
ams_collector_hosts = ",".join(
  infra_solr_utils.validate_host(host, "Metrics collector host")
  for host in metric_hosts
)
metrics_enabled = bool(ams_collector_hosts)
if metrics_enabled:
  metrics_http_policy = config["configurations"]["ams-site"][
    "timeline.metrics.service.http.policy"
  ]
  ams_collector_protocol = (
    "https" if metrics_http_policy == "HTTPS_ONLY" else "http"
  )
  ams_collector_port = infra_solr_utils.bounded_int(
    get_port_from_url(
      config["configurations"]["ams-site"][
        "timeline.metrics.service.webapp.address"
      ]
    ),
    "Metrics collector port",
    1,
    65535,
  )
else:
  ams_collector_port = ""
  ams_collector_protocol = ""

infra_solr_java_home_assignment = shell_assignment(java64_home)
infra_solr_java_mem_assignment = shell_assignment(
  f"-Xms{infra_solr_min_mem}m -Xmx{infra_solr_max_mem}m"
)
infra_solr_java_stack_size_assignment = shell_assignment(
  f"-Xss{infra_solr_java_stack_size}m"
)
infra_solr_gc_log_opts_assignment = shell_assignment(
  f"-Xlog:gc*:file={infra_solr_log_dir}/solr_gc.log:"
  "time,uptime,level,tags:filecount=15,filesize=200M"
)
infra_solr_gc_tune_assignment = shell_assignment(
  "-XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:+ParallelRefProcEnabled"
)
infra_solr_zk_host_assignment = shell_assignment(
  f"{zk_quorum}{infra_solr_znode}"
)
infra_solr_host_assignment = shell_assignment(hostname)
infra_solr_jmx_enabled_assignment = shell_assignment(
  str(infra_solr_jmx_enabled).lower()
)
infra_solr_jmx_port_assignment = shell_assignment(infra_solr_jmx_port)
infra_solr_extra_java_opts_assignment = shell_assignment(infra_solr_extra_java_opts)
infra_solr_piddir_assignment = shell_assignment(infra_solr_piddir)
infra_solr_datadir_assignment = shell_assignment(infra_solr_datadir)
infra_solr_log4j_assignment = shell_assignment(f"{infra_solr_conf}/log4j2.xml")
infra_solr_log_dir_assignment = shell_assignment(infra_solr_log_dir)
infra_solr_port_assignment = shell_assignment(infra_solr_port)
infra_solr_keystore_location_assignment = shell_assignment(
  infra_solr_keystore_location
)
infra_solr_keystore_password_assignment = shell_assignment(
  infra_solr_keystore_password
)
infra_solr_keystore_type_assignment = shell_assignment(infra_solr_keystore_type)
infra_solr_truststore_location_assignment = shell_assignment(
  infra_solr_truststore_location
)
infra_solr_truststore_password_assignment = shell_assignment(
  infra_solr_truststore_password
)
infra_solr_truststore_type_assignment = shell_assignment(
  infra_solr_truststore_type
)
infra_solr_truststore_location_xml = xml_escape(infra_solr_truststore_location)
infra_solr_truststore_password_xml = xml_escape(infra_solr_truststore_password)
infra_solr_truststore_type_xml = xml_escape(infra_solr_truststore_type)
if security_enabled:
  infra_solr_jaas_assignment = shell_assignment(infra_solr_jaas_file)
  infra_solr_web_keytab_assignment = shell_assignment(
    infra_solr_web_kerberos_keytab
  )
  infra_solr_web_principal_assignment = shell_assignment(
    infra_solr_web_kerberos_principal
  )
  infra_solr_service_principal_assignment = shell_assignment(
    infra_solr_kerberos_principal
  )
  zk_security_opts_assignment = shell_assignment(zk_security_opts)
