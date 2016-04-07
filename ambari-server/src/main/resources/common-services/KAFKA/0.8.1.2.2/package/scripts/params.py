#!/usr/bin/env python
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
from resource_management.libraries.functions import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from utils import get_bare_principal
from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.libraries.functions.is_empty import is_empty
import status_params
from resource_management.core.logger import Logger
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_name = default("/hostLevelParams/stack_name", None)
retryAble = default("/commandParams/command_retry_enabled", False)

# Version being upgraded/downgraded to
version = default("/commandParams/version", None)

# Version that is CURRENT.
current_version = default("/hostLevelParams/current_version", None)

host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
upgrade_direction = default("/commandParams/upgrade_direction", None)

# When downgrading the 'version' and 'current_version' are both pointing to the downgrade-target version
# downgrade_from_version provides the source-version the downgrade is happening from
downgrade_from_version = default("/commandParams/downgrade_from_version", None)

hostname = config['hostname']

# default kafka parameters
kafka_home = '/usr/lib/kafka/'
kafka_bin = kafka_home+'/bin/kafka'
conf_dir = "/etc/kafka/conf"
limits_conf_dir = "/etc/security/limits.d"

# Used while upgrading the stack in a kerberized cluster and running kafka-acls.sh
zookeeper_connect = default("/configurations/kafka-broker/zookeeper.connect", None)

kafka_user_nofile_limit = config['configurations']['kafka-env']['kafka_user_nofile_limit']
kafka_user_nproc_limit = config['configurations']['kafka-env']['kafka_user_nproc_limit']

# parameters for 2.2+
if Script.is_hdp_stack_greater_or_equal("2.2"):
  kafka_home = '/usr/hdp/current/kafka-broker/'
  kafka_bin = kafka_home+'bin/kafka'
  conf_dir = "/usr/hdp/current/kafka-broker/config"


kafka_user = config['configurations']['kafka-env']['kafka_user']
kafka_log_dir = config['configurations']['kafka-env']['kafka_log_dir']
kafka_pid_dir = status_params.kafka_pid_dir
kafka_pid_file = kafka_pid_dir+"/kafka.pid"
# This is hardcoded on the kafka bash process lifecycle on which we have no control over
kafka_managed_pid_dir = "/var/run/kafka"
kafka_managed_log_dir = "/var/log/kafka"
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['hostLevelParams']['java_home']
kafka_env_sh_template = config['configurations']['kafka-env']['content']
kafka_hosts = config['clusterHostInfo']['kafka_broker_hosts']
kafka_hosts.sort()

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()

if (('kafka-log4j' in config['configurations']) and ('content' in config['configurations']['kafka-log4j'])):
    log4j_props = config['configurations']['kafka-log4j']['content']
else:
    log4j_props = None

if 'ganglia_server_host' in config['clusterHostInfo'] and \
    len(config['clusterHostInfo']['ganglia_server_host'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_host'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

metric_collector_host = ""
metric_collector_port = ""
metric_collector_protocol = ""
metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0

if has_metric_collector:
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_vip_host' in config['configurations']['cluster-env']:
    metric_collector_host = config['configurations']['cluster-env']['metrics_collector_vip_host']
  else:
    metric_collector_host = ams_collector_hosts[0]
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_vip_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_vip_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "localhost:6188")
    if metric_collector_web_address.find(':') != -1:
      metric_collector_port = metric_collector_web_address.split(':')[1]
    else:
      metric_collector_port = '6188'
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  pass
# Security-related params
security_enabled = config['configurations']['cluster-env']['security_enabled']
kafka_kerberos_enabled = (('security.inter.broker.protocol' in config['configurations']['kafka-broker']) and
                         ((config['configurations']['kafka-broker']['security.inter.broker.protocol'] == "PLAINTEXTSASL") or
                          (config['configurations']['kafka-broker']['security.inter.broker.protocol'] == "SASL_PLAINTEXT")))


if security_enabled and hdp_stack_version != "" and 'kafka_principal_name' in config['configurations']['kafka-env'] and compare_versions(hdp_stack_version, '2.3') >= 0:
    _hostname_lowercase = config['hostname'].lower()
    _kafka_principal_name = config['configurations']['kafka-env']['kafka_principal_name']
    kafka_jaas_principal = _kafka_principal_name.replace('_HOST',_hostname_lowercase)
    kafka_keytab_path = config['configurations']['kafka-env']['kafka_keytab']
    kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)
    kafka_kerberos_params = "-Djava.security.auth.login.config="+ conf_dir +"/kafka_jaas.conf"
else:
    kafka_kerberos_params = ''

# ***********************  RANGER PLUGIN CHANGES ***********************
# ranger host
# **********************************************************************
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

ranger_admin_log_dir = default("/configurations/ranger-env/ranger_admin_log_dir","/var/log/ranger/admin")
is_supported_kafka_ranger = config['configurations']['kafka-env']['is_supported_kafka_ranger']

#ranger kafka properties
if has_ranger_admin and is_supported_kafka_ranger:

  enable_ranger_kafka = config['configurations']['ranger-kafka-plugin-properties']['ranger-kafka-plugin-enabled']
  enable_ranger_kafka = not is_empty(enable_ranger_kafka) and enable_ranger_kafka.lower() == 'yes'
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
  xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
  xa_audit_db_flavor = xa_audit_db_flavor.lower() if xa_audit_db_flavor else None
  xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
  xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  xa_db_host = config['configurations']['admin-properties']['db_host']
  repo_name = str(config['clusterName']) + '_kafka'

  ranger_env = config['configurations']['ranger-env']
  ranger_plugin_properties = config['configurations']['ranger-kafka-plugin-properties']

  ranger_kafka_audit = config['configurations']['ranger-kafka-audit']
  ranger_kafka_audit_attrs = config['configuration_attributes']['ranger-kafka-audit']
  ranger_kafka_security = config['configurations']['ranger-kafka-security']
  ranger_kafka_security_attrs = config['configuration_attributes']['ranger-kafka-security']
  ranger_kafka_policymgr_ssl = config['configurations']['ranger-kafka-policymgr-ssl']
  ranger_kafka_policymgr_ssl_attrs = config['configuration_attributes']['ranger-kafka-policymgr-ssl']

  policy_user = config['configurations']['ranger-kafka-plugin-properties']['policy_user']

  ranger_plugin_config = {
    'username' : config['configurations']['ranger-kafka-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
    'password' : unicode(config['configurations']['ranger-kafka-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
    'zookeeper.connect' : config['configurations']['ranger-kafka-plugin-properties']['zookeeper.connect'],
    'commonNameForCertificate' : config['configurations']['ranger-kafka-plugin-properties']['common.name.for.certificate']
  }

  kafka_ranger_plugin_repo = {
    'isEnabled': 'true',
    'configs': ranger_plugin_config,
    'description': 'kafka repo',
    'name': repo_name,
    'repositoryType': 'kafka',
    'type': 'kafka',
    'assetType': '1'
  }
  #For curl command in ranger plugin to get db connector
  jdk_location = config['hostLevelParams']['jdk_location']
  java_share_dir = '/usr/share/java'
  if xa_audit_db_flavor and xa_audit_db_flavor == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'mssql':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor and xa_audit_db_flavor == 'sqla':
    jdbc_jar_name = "sajdbc4.jar"
    jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("{kafka_home}libs/{jdbc_jar_name}")

  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = config['configurations']['ranger-kafka-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-kafka-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-kafka-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-kafka-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  hdp_version = get_hdp_version('kafka-broker')
  setup_ranger_env_sh_source = format('/usr/hdp/{hdp_version}/ranger-kafka-plugin/install/conf.templates/enable/kafka-ranger-env.sh')
  setup_ranger_env_sh_target = format("{conf_dir}/kafka-ranger-env.sh")

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

namenode_hosts = default("/clusterHostInfo/namenode_host", [])
has_namenode = not len(namenode_hosts) == 0

hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab'] if has_namenode else None
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
default_fs = config['configurations']['core-site']['fs.defaultFS'] if has_namenode else None
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin") if has_namenode else None
hadoop_conf_dir = conf_select.get_hadoop_conf_dir() if has_namenode else None
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources()
)
