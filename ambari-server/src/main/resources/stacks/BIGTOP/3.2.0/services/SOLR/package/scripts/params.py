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
from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version

import status_params
import solr_utils


# config object that holds the configurations declared in the -site.xml file
config = Script.get_config()
stack_root = Script.get_stack_root()

# This is expected to be of the form #.#.#
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted = format_stack_version(stack_version_unformatted)

stack_version = default("/commandParams/version", None)
security_enabled = status_params.security_enabled
component_directory = status_params.component_directory

hostname = config["agentLevelParams"]["hostname"].lower()

solr_conf = "/etc/solr/conf"

solr_port = status_params.solr_port
solr_piddir = status_params.solr_piddir
solr_pidfile = status_params.solr_pidfile

user_group = solr_utils.validate_user(
  config["configurations"]["cluster-env"]["user_group"], "Hadoop group"
)

limits_conf_dir = "/etc/security/limits.d"
solr_user_nofile_limit = solr_utils.bounded_int(
  default("/configurations/solr-env/solr_user_nofile_limit", "128000"),
  "Solr open file limit",
  1024,
  1048576,
)
solr_user_nproc_limit = solr_utils.bounded_int(
  default("/configurations/solr-env/solr_user_nproc_limit", "65536"),
  "Solr process limit",
  1024,
  1048576,
)

# shared configs
java_home = config["ambariLevelParams"]["java_home"]
java_version = expect("/ambariLevelParams/java_version", int)
ambari_java_home = config['ambariLevelParams']['ambari_java_home']
ambari_java_exec = format("{ambari_java_home}/bin/java")

java64_home = java_home

#####################################
# Solr configs
#####################################

solr_dir = format("{stack_root}/current/{component_directory}")

solr_bindir = solr_dir + "/bin"

zookeeper_port = solr_utils.bounded_int(
  default("/configurations/zoo.cfg/clientPort", None),
  "ZooKeeper client port",
  1,
  65535,
)
zookeeper_quorum = solr_utils.validate_zookeeper_quorum(
  ",".join(
    f"{host}:{zookeeper_port}"
    for host in config["clusterHostInfo"]["zookeeper_server_hosts"]
  )
)

if "solr-env" in config["configurations"]:
  solr_hosts = config["clusterHostInfo"]["solr_server_hosts"]
  solr_znode = solr_utils.validate_znode(
    config["configurations"]["solr-env"]["solr_znode"]
  )
  solr_min_mem = solr_utils.bounded_int(
    config["configurations"]["solr-env"]["solr_minmem"],
    "Solr minimum heap",
    512,
    32768,
  )
  solr_max_mem = solr_utils.bounded_int(
    config["configurations"]["solr-env"]["solr_maxmem"],
    "Solr maximum heap",
    512,
    32768,
  )
  if solr_min_mem > solr_max_mem:
    raise Fail("Solr minimum heap must not exceed maximum heap")
  solr_java_stack_size = solr_utils.bounded_int(
    config["configurations"]["solr-env"]["solr_java_stack_size"],
    "Solr Java stack size",
    1,
    128,
  )
  solr_datadir = solr_utils.validate_service_directory(
    format(config["configurations"]["solr-env"]["solr_datadir"]),
    "Solr data directory",
  )
  solr_data_resources_dir = os.path.join(solr_datadir, "resources")
  solr_jmx_port = solr_utils.bounded_int(
    config["configurations"]["solr-env"]["solr_jmx_port"],
    "Solr JMX port",
    1,
    65535,
  )
  if solr_jmx_port == int(solr_port):
    raise Fail("Solr HTTP and JMX ports must be different")
  solr_ssl_enabled = solr_utils.as_bool(
    default("configurations/solr-env/solr_ssl_enabled", False),
    "Solr SSL setting",
  )
  solr_keystore_location = config["configurations"]["solr-env"][
    "solr_keystore_location"
  ]
  solr_keystore_password = config["configurations"]["solr-env"][
    "solr_keystore_password"
  ]
  solr_truststore_location = config["configurations"]["solr-env"][
    "solr_truststore_location"
  ]
  solr_truststore_password = config["configurations"]["solr-env"][
    "solr_truststore_password"
  ]
  solr_user = solr_utils.validate_user(
    config["configurations"]["solr-env"]["solr_user"], "Solr user"
  )
  solr_log_dir = solr_utils.validate_service_directory(
    config["configurations"]["solr-env"]["solr_log_dir"],
    "Solr log directory",
  )
  if len({solr_datadir, solr_log_dir, solr_piddir}) != 3:
    raise Fail("Solr data, log, and PID directories must be distinct")
  solr_env_content = config["configurations"]["solr-env"]["content"]
  solr_gc_log_opts = format(config["configurations"]["solr-env"]["solr_gc_log_opts"])
  solr_gc_tune = format(config["configurations"]["solr-env"]["solr_gc_tune"])
  solr_extra_java_opts = format(
    default("configurations/solr-env/solr_extra_java_opts", "")
  )

  zookeeper_quorum = solr_utils.validate_zookeeper_quorum(
    format(
      default("configurations/solr-env/solr_zookeeper_quorum", zookeeper_quorum)
    )
  )
  zk_quorum = zookeeper_quorum

default_ranger_audit_users = ""
solr_jaas_file = solr_conf + "/solr_jaas.conf"

if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  _hostname_lowercase = config["agentLevelParams"]["hostname"].lower()
  solr_kerberos_keytab = solr_utils.validate_absolute_path(
    config["configurations"]["solr-env"]["solr_kerberos_keytab"],
    "Solr keytab",
  )
  solr_kerberos_principal = solr_utils.validate_principal(
    config["configurations"]["solr-env"]["solr_kerberos_principal"].replace(
      "_HOST", _hostname_lowercase
    ),
    "Solr principal",
  )
  solr_web_kerberos_keytab = solr_utils.validate_absolute_path(
    config["configurations"]["solr-env"]["solr_web_kerberos_keytab"],
    "Solr HTTP keytab",
  )
  solr_web_kerberos_principal = solr_utils.validate_principal(
    config["configurations"]["solr-env"]["solr_web_kerberos_principal"].replace(
      "_HOST", _hostname_lowercase
    ),
    "Solr HTTP principal",
  )
  solr_kerberos_name_rules = config["configurations"]["solr-env"][
    "solr_kerberos_name_rules"
  ]
  kerberos_realm = solr_utils.validate_realm(
    config["configurations"]["kerberos-env"]["realm"]
  )

  zookeeper_principal_name = default(
    "/configurations/zookeeper-env/zookeeper_principal_name",
    "zookeeper/_HOST@EXAMPLE.COM",
  )
  external_zk_principal_enabled = solr_utils.as_bool(
    default(
      "/configurations/solr-env/solr_zookeeper_external_enabled", False
    ),
    "External ZooKeeper setting",
  )
  external_zk_principal_name = default(
    "/configurations/solr-env/solr_zookeeper_external_principal",
    "zookeeper/_HOST@EXAMPLE.COM",
  )
  zk_principal_name = (
    external_zk_principal_name
    if external_zk_principal_enabled
    else zookeeper_principal_name
  )
  zk_principal_user = solr_utils.service_user(
    zk_principal_name, "ZooKeeper service principal"
  )
  zk_security_opts = format(
    "-Dzookeeper.sasl.client=true "
    "-Dzookeeper.sasl.client.username={zk_principal_user} "
    "-Dzookeeper.sasl.clientconfig=Client"
  )

  ranger_audit_principal_conf_key = "xasecure.audit.jaas.Client.option.principal"
  ranger_audit_users = solr_utils.configured_principal_users(
    config["configurations"],
    (
      "ranger-hdfs-audit",
      "ranger-hbase-audit",
      "ranger-hive-audit",
      "ranger-kafka-audit",
      "ranger-kms-audit",
      "ranger-yarn-audit",
    ),
    ranger_audit_principal_conf_key,
  )
  default_ranger_audit_users = ",".join(ranger_audit_users)

security_config = config["configurations"]["solr-security-json"]
solr_ranger_audit_service_users = solr_utils.parse_users(
  format(security_config["solr_ranger_audit_service_users"]),
  "Ranger audit service users",
)
unsupported_audit_users = {"atlas", "knox", "logsearch", "nifi", "storm"}
if unsupported_audit_users.intersection(solr_ranger_audit_service_users):
  raise Fail("Ranger audit users contain services unavailable in the BIGTOP stack")
solr_security_manually_managed = solr_utils.as_bool(
  default(
    "/configurations/solr-security-json/solr_security_manually_managed",
    False,
  ),
  "Solr manual security setting",
)
solr_security_json_content = solr_utils.validate_json_object(
  security_config["content"], "Custom Solr security.json", allow_empty=True
)

solr_jmx_enabled = solr_utils.as_bool(
  default("/configurations/solr-env/solr_jmx_enabled", False),
  "Solr JMX setting",
)


def shell_assignment(value):
  return quote_bash_args(str(value))


solr_java_home_assignment = shell_assignment(java64_home)
solr_java_mem_assignment = shell_assignment(
  f"-Xms{solr_min_mem}m -Xmx{solr_max_mem}m"
)
solr_java_stack_size_assignment = shell_assignment(f"-Xss{solr_java_stack_size}m")
if java_version >= 9:
  solr_gc_log_opts_assignment = shell_assignment(
    f"-Xlog:gc*:file={solr_log_dir}/solr_gc.log:"
    "time,uptime,level,tags:filecount=15,filesize=200M"
  )
  solr_gc_tune_assignment = shell_assignment(
    "-XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:+ParallelRefProcEnabled"
  )
else:
  solr_gc_log_opts_assignment = shell_assignment(
    f"{solr_gc_log_opts} -Xloggc:{solr_log_dir}/solr_gc.log"
  )
  solr_gc_tune_assignment = shell_assignment(solr_gc_tune)

solr_zk_host_assignment = shell_assignment(f"{zookeeper_quorum}{solr_znode}")
solr_jmx_enabled_assignment = shell_assignment(str(solr_jmx_enabled).lower())
solr_jmx_port_assignment = shell_assignment(solr_jmx_port)
solr_rmi_hostname_assignment = shell_assignment(hostname)
solr_extra_java_opts_assignment = shell_assignment(solr_extra_java_opts)
solr_piddir_assignment = shell_assignment(solr_piddir)
solr_datadir_assignment = shell_assignment(solr_datadir)
solr_log4j_props_assignment = shell_assignment(f"{solr_conf}/log4j2.xml")
solr_log_dir_assignment = shell_assignment(solr_log_dir)
solr_port_assignment = shell_assignment(solr_port)

if solr_ssl_enabled:
  solr_keystore_location = solr_utils.validate_absolute_path(
    solr_keystore_location, "Solr key store"
  )
  solr_truststore_location = solr_utils.validate_absolute_path(
    solr_truststore_location, "Solr trust store"
  )
  solr_keystore_password = solr_utils.validate_secret(
    solr_keystore_password, "Solr key store password"
  )
  solr_truststore_password = solr_utils.validate_secret(
    solr_truststore_password, "Solr trust store password"
  )
  solr_keystore_location_assignment = shell_assignment(solr_keystore_location)
  solr_keystore_password_assignment = shell_assignment(solr_keystore_password)
  solr_truststore_location_assignment = shell_assignment(solr_truststore_location)
  solr_truststore_password_assignment = shell_assignment(solr_truststore_password)

if security_enabled:
  solr_jaas_file_assignment = shell_assignment(solr_jaas_file)
  solr_web_kerberos_keytab_assignment = shell_assignment(solr_web_kerberos_keytab)
  solr_web_kerberos_principal_assignment = shell_assignment(
    solr_web_kerberos_principal
  )
  solr_hdfs_kerberos_option_assignment = shell_assignment(
    f"-Dsolr.hdfs.security.kerberos.principal={solr_kerberos_principal}"
  )
  solr_kerberos_name_rules_assignment = shell_assignment(
    f"-Dsolr.kerberos.name.rules={solr_kerberos_name_rules}"
  )
  zk_security_opts_assignment = shell_assignment(zk_security_opts)

# Solr log4j
log_maxfilesize = solr_utils.bounded_int(
  default("configurations/solr-log4j/log_maxfilesize", 10),
  "Solr log file size",
  1,
  10240,
)
log_maxbackupindex = solr_utils.bounded_int(
  default("configurations/solr-log4j/log_maxbackupindex", 9),
  "Solr log backup count",
  1,
  1000,
)

solr_xml_content = default("configurations/solr-xml/content", None)
solr_log4j_content = default("configurations/solr-log4j/content", None)

smokeuser = solr_utils.validate_user(
  config["configurations"]["cluster-env"]["smokeuser"], "Smoke user"
)
smoke_user_keytab = solr_utils.validate_absolute_path(
  config["configurations"]["cluster-env"]["smokeuser_keytab"],
  "Smoke user keytab",
)
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
if security_enabled:
  smokeuser_principal = solr_utils.validate_principal(
    smokeuser_principal, "Smoke user principal"
  )

ranger_solr_collection_name = solr_utils.validate_solr_name(
  default(
    "configurations/ranger-env/ranger_solr_collection_name", "ranger_audits"
  ),
  "Ranger Solr collection",
)

ranger_admin_kerberos_service_user = solr_utils.service_user(
  default(
    "configurations/ranger-admin-site/ranger.admin.kerberos.principal", None
  ),
  "Ranger admin service principal",
  required=False,
)
solr_kerberos_service_user = solr_utils.service_user(
  default("configurations/solr-env/solr_kerberos_principal", "solr"),
  "Solr service principal",
)

solr_role_ranger_admin = default(
  "configurations/solr-security-json/solr_role_ranger_admin", "ranger_admin_user"
)
solr_role_ranger_audit = default(
  "configurations/solr-security-json/solr_role_ranger_audit", "ranger_audit_user"
)
solr_role_ranger_admin = solr_utils.validate_user(
  solr_role_ranger_admin, "Ranger admin role"
)
solr_role_ranger_audit = solr_utils.validate_user(
  solr_role_ranger_audit, "Ranger audit role"
)
solr_role_dev = solr_utils.validate_user(
  default("configurations/solr-security-json/solr_role_dev", "dev"),
  "Solr read role",
)
if len({solr_role_ranger_admin, solr_role_ranger_audit, solr_role_dev}) != 3:
  raise Fail("Solr security roles must be unique")
reserved_security_users = {solr_kerberos_service_user}
if ranger_admin_kerberos_service_user:
  reserved_security_users.add(ranger_admin_kerberos_service_user)
if reserved_security_users.intersection(solr_ranger_audit_service_users):
  raise Fail("Ranger audit users must not duplicate Solr or Ranger Admin")
