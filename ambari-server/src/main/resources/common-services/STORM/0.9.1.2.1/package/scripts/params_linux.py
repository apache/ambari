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
import re
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

import status_params

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_bare_principal import get_bare_principal
from resource_management.libraries.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", Direction.UPGRADE)
version = default("/commandParams/version", None)

storm_component_home_dir = status_params.storm_component_home_dir
conf_dir = status_params.conf_dir

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)
stack_is_hdp22_or_further = Script.is_hdp_stack_greater_or_equal("2.2")

# default hadoop params
rest_lib_dir = "/usr/lib/storm/contrib/storm-rest"
storm_bin_dir = "/usr/bin"
storm_lib_dir = "/usr/lib/storm/lib/"

# hadoop parameters for 2.2+
if stack_is_hdp22_or_further:
  rest_lib_dir = format("{storm_component_home_dir}/contrib/storm-rest")
  storm_bin_dir = format("{storm_component_home_dir}/bin")
  storm_lib_dir = format("{storm_component_home_dir}/lib")
  log4j_dir = format("{storm_component_home_dir}/log4j2")

storm_user = config['configurations']['storm-env']['storm_user']
log_dir = config['configurations']['storm-env']['storm_log_dir']
pid_dir = status_params.pid_dir
local_dir = config['configurations']['storm-site']['storm.local.dir']
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['hostLevelParams']['java_home']
jps_binary = format("{java64_home}/bin/jps")
nimbus_port = config['configurations']['storm-site']['nimbus.thrift.port']
storm_zookeeper_root_dir = default('/configurations/storm-site/storm.zookeeper.root', None)
storm_zookeeper_servers = config['configurations']['storm-site']['storm.zookeeper.servers']
storm_zookeeper_port = config['configurations']['storm-site']['storm.zookeeper.port']
storm_logs_supported = config['configurations']['storm-env']['storm_logs_supported']

# nimbus.seeds is supported in HDP 2.3.0.0 and higher
nimbus_seeds_supported = default('/configurations/storm-env/nimbus_seeds_supported', False)
nimbus_host = default('/configurations/storm-site/nimbus.host', None)
nimbus_seeds = default('/configurations/storm-site/nimbus.seeds', None)
default_topology_max_replication_wait_time_sec = default('/configurations/storm-site/topology.max.replication.wait.time.sec.default', -1)
nimbus_hosts = default("/clusterHostInfo/nimbus_hosts", [])
default_topology_min_replication_count = default('/configurations/storm-site/topology.min.replication.count.default', 1)

#Calculate topology.max.replication.wait.time.sec and topology.min.replication.count
if len(nimbus_hosts) > 1:
  # for HA Nimbus
  actual_topology_max_replication_wait_time_sec = -1
  actual_topology_min_replication_count = len(nimbus_hosts) / 2 + 1
else:
  # for non-HA Nimbus
  actual_topology_max_replication_wait_time_sec = default_topology_max_replication_wait_time_sec
  actual_topology_min_replication_count = default_topology_min_replication_count

if 'topology.max.replication.wait.time.sec.default' in config['configurations']['storm-site']:
  del config['configurations']['storm-site']['topology.max.replication.wait.time.sec.default']
if 'topology.min.replication.count.default' in config['configurations']['storm-site']:
  del config['configurations']['storm-site']['topology.min.replication.count.default']

rest_api_port = "8745"
rest_api_admin_port = "8746"
rest_api_conf_file = format("{conf_dir}/config.yaml")
storm_env_sh_template = config['configurations']['storm-env']['content']
jmxremote_port = config['configurations']['storm-env']['jmxremote_port']

if 'ganglia_server_host' in config['clusterHostInfo'] and len(config['clusterHostInfo']['ganglia_server_host'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_host'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

security_enabled = config['configurations']['cluster-env']['security_enabled']

storm_ui_host = default("/clusterHostInfo/storm_ui_server_hosts", [])

storm_user_nofile_limit = default('/configurations/storm-env/storm_user_nofile_limit', 128000)
storm_user_nproc_limit = default('/configurations/storm-env/storm_user_noproc_limit', 65536)

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  _storm_principal_name = config['configurations']['storm-env']['storm_principal_name']
  storm_jaas_principal = _storm_principal_name.replace('_HOST',_hostname_lowercase)
  storm_keytab_path = config['configurations']['storm-env']['storm_keytab']

  if stack_is_hdp22_or_further:
    storm_ui_keytab_path = config['configurations']['storm-env']['storm_ui_keytab']
    _storm_ui_jaas_principal_name = config['configurations']['storm-env']['storm_ui_principal_name']
    storm_ui_jaas_principal = _storm_ui_jaas_principal_name.replace('_HOST',_hostname_lowercase)

    storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)

    _nimbus_principal_name = config['configurations']['storm-env']['nimbus_principal_name']
    nimbus_jaas_principal = _nimbus_principal_name.replace('_HOST', _hostname_lowercase)
    nimbus_bare_jaas_principal = get_bare_principal(_nimbus_principal_name)
    nimbus_keytab_path = config['configurations']['storm-env']['nimbus_keytab']

kafka_bare_jaas_principal = None
if stack_is_hdp22_or_further:
  if security_enabled:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.secure.transport']
    # generate KafkaClient jaas config if kafka is kerberoized
    _kafka_principal_name = default("/configurations/kafka-env/kafka_principal_name", None)
    kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)

  else:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.nonsecure.transport']


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

  metric_collector_report_interval = 60
  metric_collector_app_id = "nimbus"
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
  metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
  metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")
  pass
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)
metric_collector_sink_jar = "/usr/lib/storm/lib/ambari-metrics-storm-sink*.jar"

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

#ranger storm properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_storm'

common_name_for_certificate = config['configurations']['ranger-storm-plugin-properties']['common.name.for.certificate']

storm_ui_port = config['configurations']['storm-site']['ui.port']

repo_config_username = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-storm-plugin-properties']
policy_user = config['configurations']['ranger-storm-plugin-properties']['policy_user']
storm_cluster_log4j_content = config['configurations']['storm-cluster-log4j']['content']
storm_worker_log4j_content = config['configurations']['storm-worker-log4j']['content']

# some commands may need to supply the JAAS location when running as storm
storm_jaas_file = format("{conf_dir}/storm_jaas.conf")

# For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'

if has_ranger_admin:
  enable_ranger_storm = (config['configurations']['ranger-storm-plugin-properties']['ranger-storm-plugin-enabled'].lower() == 'yes')
  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()

  if xa_audit_db_flavor == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor == 'mssql':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor == 'sqla':
    jdbc_jar_name = "sajdbc4.jar"
    jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("{storm_component_home_dir}/lib/{jdbc_jar_name}")

  storm_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'nimbus.url': 'http://' + storm_ui_host[0].lower() + ':' + str(storm_ui_port),
    'commonNameForCertificate': common_name_for_certificate
  }

  storm_ranger_plugin_repo = {
    'isActive': 'true',
    'config': json.dumps(storm_ranger_plugin_config),
    'description': 'storm repo',
    'name': repo_name,
    'repositoryType': 'storm',
    'assetType': '6'
  }

  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = config['configurations']['ranger-storm-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-storm-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-storm-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-storm-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

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
