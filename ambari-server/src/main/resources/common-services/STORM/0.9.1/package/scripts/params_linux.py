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
import os
import re
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

import status_params

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons import yaml_utils
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_bare_principal import get_bare_principal
from resource_management.libraries.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster
from resource_management.libraries.functions import is_empty
from ambari_commons.ambari_metrics_helper import select_metric_collector_hosts_from_hostnames
from resource_management.libraries.functions.setup_ranger_plugin_xml import get_audit_configs, generate_ranger_service_config

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = status_params.stack_root
sudo = AMBARI_SUDO_BINARY

limits_conf_dir = "/etc/security/limits.d"

# Needed since this is an Atlas Hook service.
cluster_name = config['clusterName']

stack_name = status_params.stack_name
upgrade_direction = default("/commandParams/upgrade_direction", None)
version = default("/commandParams/version", None)

agent_stack_retry_on_unavailability = config['hostLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/hostLevelParams/agent_stack_retry_count", int)

storm_component_home_dir = status_params.storm_component_home_dir
conf_dir = status_params.conf_dir

stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted = status_params.stack_version_formatted
stack_supports_ru = stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted)
stack_supports_storm_kerberos = stack_version_formatted and check_stack_feature(StackFeature.STORM_KERBEROS, stack_version_formatted)
stack_supports_storm_ams = stack_version_formatted and check_stack_feature(StackFeature.STORM_AMS, stack_version_formatted)
stack_supports_core_site_for_ranger_plugin = check_stack_feature(StackFeature.CORE_SITE_FOR_RANGER_PLUGINS_SUPPORT, stack_version_formatted)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

# default hadoop params
rest_lib_dir = "/usr/lib/storm/contrib/storm-rest"
storm_bin_dir = "/usr/bin"
storm_lib_dir = "/usr/lib/storm/lib/"

# hadoop parameters for 2.2+
if stack_supports_ru:
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
  _ambari_principal_name = default('/configurations/cluster-env/ambari_principal_name', None)
  storm_keytab_path = config['configurations']['storm-env']['storm_keytab']

  if stack_supports_storm_kerberos:
    storm_ui_keytab_path = config['configurations']['storm-env']['storm_ui_keytab']
    _storm_ui_jaas_principal_name = config['configurations']['storm-env']['storm_ui_principal_name']
    storm_ui_jaas_principal = _storm_ui_jaas_principal_name.replace('_HOST',_hostname_lowercase)
    storm_bare_jaas_principal = get_bare_principal(_storm_principal_name)
    if _ambari_principal_name:
      ambari_bare_jaas_principal = get_bare_principal(_ambari_principal_name)
    _nimbus_principal_name = config['configurations']['storm-env']['nimbus_principal_name']
    nimbus_jaas_principal = _nimbus_principal_name.replace('_HOST', _hostname_lowercase)
    nimbus_bare_jaas_principal = get_bare_principal(_nimbus_principal_name)
    nimbus_keytab_path = config['configurations']['storm-env']['nimbus_keytab']

kafka_bare_jaas_principal = None
if stack_supports_storm_kerberos:
  if security_enabled:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.secure.transport']
    # generate KafkaClient jaas config if kafka is kerberoized
    _kafka_principal_name = default("/configurations/kafka-env/kafka_principal_name", None)
    kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)
  else:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.nonsecure.transport']

set_instanceId = "false"
if 'cluster-env' in config['configurations'] and \
        'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
has_metric_collector = not len(ams_collector_hosts) == 0
metric_collector_port = None
if has_metric_collector:
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_external_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_external_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
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
metric_collector_sink_jar = "/usr/lib/storm/lib/ambari-metrics-storm-sink-with-common-*.jar"
metric_collector_legacy_sink_jar = "/usr/lib/storm/lib/ambari-metrics-storm-sink-legacy-with-common-*.jar"
host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)


# Cluster Zookeeper quorum
zookeeper_quorum = ""
if storm_zookeeper_servers:
  storm_zookeeper_servers_list = yaml_utils.get_values_from_yaml_array(storm_zookeeper_servers)
  zookeeper_quorum = (":" + storm_zookeeper_port + ",").join(storm_zookeeper_servers_list)
  zookeeper_quorum += ":" + storm_zookeeper_port

jar_jvm_opts = ''

########################################################
############# Atlas related params #####################
########################################################
#region Atlas Hooks
storm_atlas_application_properties = default('/configurations/storm-atlas-application.properties', {})
enable_atlas_hook = default('/configurations/storm-env/storm.atlas.hook', False)
atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')

if enable_atlas_hook:
  # Only append /etc/atlas/conf to classpath if on HDP 2.4.*
  if check_stack_feature(StackFeature.ATLAS_CONF_DIR_IN_PATH, stack_version_formatted):
    atlas_conf_dir = format('{stack_root}/current/atlas-server/conf')
    jar_jvm_opts += '-Datlas.conf=' + atlas_conf_dir
#endregion

storm_ui_port = config['configurations']['storm-site']['ui.port']

#Storm log4j properties
storm_a1_maxfilesize = default('/configurations/storm-cluster-log4j/storm_a1_maxfilesize', 100)
storm_a1_maxbackupindex = default('/configurations/storm-cluster-log4j/storm_a1_maxbackupindex', 9)
storm_wrkr_a1_maxfilesize = default('/configurations/storm-worker-log4j/storm_wrkr_a1_maxfilesize', 100)
storm_wrkr_a1_maxbackupindex = default('/configurations/storm-worker-log4j/storm_wrkr_a1_maxbackupindex', 9)
storm_wrkr_out_maxfilesize = default('/configurations/storm-worker-log4j/storm_wrkr_out_maxfilesize', 100)
storm_wrkr_out_maxbackupindex = default('/configurations/storm-worker-log4j/storm_wrkr_out_maxbackupindex', 4)
storm_wrkr_err_maxfilesize = default('/configurations/storm-worker-log4j/storm_wrkr_err_maxfilesize', 100)
storm_wrkr_err_maxbackupindex = default('/configurations/storm-worker-log4j/storm_wrkr_err_maxbackupindex', 4)

storm_cluster_log4j_content = config['configurations']['storm-cluster-log4j']['content']
storm_worker_log4j_content = config['configurations']['storm-worker-log4j']['content']

# some commands may need to supply the JAAS location when running as storm
storm_jaas_file = format("{conf_dir}/storm_jaas.conf")

# for curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']

# ranger storm plugin start section

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks)

# ambari-server hostname
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger storm plugin enabled property
enable_ranger_storm = default("/configurations/ranger-storm-plugin-properties/ranger-storm-plugin-enabled", "No")
enable_ranger_storm = True if enable_ranger_storm.lower() == 'yes' else False

# ranger storm properties
if enable_ranger_storm:
  # get ranger policy url
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  if xml_configurations_supported:
    policymgr_mgr_url = config['configurations']['ranger-storm-security']['ranger.plugin.storm.policy.rest.url']

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  # ranger audit db user
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')

  # ranger storm service name
  repo_name = str(config['clusterName']) + '_storm'
  repo_name_value = config['configurations']['ranger-storm-security']['ranger.plugin.storm.service.name']
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  common_name_for_certificate = config['configurations']['ranger-storm-plugin-properties']['common.name.for.certificate']
  repo_config_username = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

  # ranger-env config
  ranger_env = config['configurations']['ranger-env']

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_storm:
    external_admin_username = default('/configurations/ranger-storm-plugin-properties/external_admin_username', 'admin')
    external_admin_password = default('/configurations/ranger-storm-plugin-properties/external_admin_password', 'admin')
    external_ranger_admin_username = default('/configurations/ranger-storm-plugin-properties/external_ranger_admin_username', 'amb_ranger_admin')
    external_ranger_admin_password = default('/configurations/ranger-storm-plugin-properties/external_ranger_admin_password', 'amb_ranger_admin')
    ranger_env = {}
    ranger_env['admin_username'] = external_admin_username
    ranger_env['admin_password'] = external_admin_password
    ranger_env['ranger_admin_username'] = external_ranger_admin_username
    ranger_env['ranger_admin_password'] = external_ranger_admin_password

  ranger_plugin_properties = config['configurations']['ranger-storm-plugin-properties']
  policy_user = storm_user
  repo_config_password = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db and has_ranger_admin:
    xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']

  repo_config_password = config['configurations']['ranger-storm-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

  downloaded_custom_connector = None
  previous_jdbc_jar_name = None
  driver_curl_source = None
  driver_curl_target = None
  previous_jdbc_jar = None

  if has_ranger_admin and stack_supports_ranger_audit_db:
    xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
    jdbc_jar_name, previous_jdbc_jar_name, audit_jdbc_url, jdbc_driver = get_audit_configs(config)

    downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_source = format("{jdk_location}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_target = format("{storm_component_home_dir}/lib/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    previous_jdbc_jar = format("{storm_component_home_dir}/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    sql_connector_jar = ''

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

  custom_ranger_service_config = generate_ranger_service_config(ranger_plugin_properties)
  if len(custom_ranger_service_config) > 0:
    storm_ranger_plugin_config.update(custom_ranger_service_config)

  if stack_supports_ranger_kerberos and security_enabled:
    policy_user = format('{storm_user},{storm_bare_jaas_principal}')
    storm_ranger_plugin_config['policy.download.auth.users'] = policy_user
    storm_ranger_plugin_config['tag.download.auth.users'] = policy_user
    storm_ranger_plugin_config['ambari.service.check.user'] = policy_user

    storm_ranger_plugin_repo = {
      'isEnabled': 'true',
      'configs': storm_ranger_plugin_config,
      'description': 'storm repo',
      'name': repo_name,
      'type': 'storm'
    }

  ranger_storm_principal = None
  ranger_storm_keytab = None
  if stack_supports_ranger_kerberos and security_enabled:
    ranger_storm_principal = storm_jaas_principal
    ranger_storm_keytab = storm_keytab_path

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-storm-audit']['xasecure.audit.destination.db']

  xa_audit_hdfs_is_enabled = default('/configurations/ranger-storm-audit/xasecure.audit.destination.hdfs', False)
  ssl_keystore_password = config['configurations']['ranger-storm-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password'] if xml_configurations_supported else None
  ssl_truststore_password = config['configurations']['ranger-storm-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password'] if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

  # for SQLA explicitly disable audit to DB for Ranger
  if has_ranger_admin and stack_supports_ranger_audit_db and xa_audit_db_flavor.lower() == 'sqla':
    xa_audit_db_is_enabled = False

# ranger storm plugin end section

namenode_hosts = default("/clusterHostInfo/namenode_host", [])
has_namenode = not len(namenode_hosts) == 0

hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab'] if has_namenode else None
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
default_fs = config['configurations']['core-site']['fs.defaultFS'] if has_namenode else None
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
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
