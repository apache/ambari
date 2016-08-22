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
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.script.script import Script
import status_params


def get_port_from_url(address):
  if not is_empty(address):
    return address.split(':')[-1]
  else:
    return address


# config object that holds the configurations declared in the -site.xml file
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

stack_version = default("/commandParams/version", None)
sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled

logsearch_server_conf = "/etc/ambari-logsearch-portal/conf"
logsearch_logfeeder_conf = "/etc/ambari-logsearch-logfeeder/conf"

logsearch_config_set_dir = format("{logsearch_server_conf}/solr_configsets")

# logsearch pid file
logsearch_pid_dir = status_params.logsearch_pid_dir
logsearch_pid_file = status_params.logsearch_pid_file

# logfeeder pid file
logfeeder_pid_dir = status_params.logfeeder_pid_dir
logfeeder_pid_file = status_params.logfeeder_pid_file

user_group = config['configurations']['cluster-env']['user_group']
fetch_nonlocal_groups = config['configurations']['cluster-env']["fetch_nonlocal_groups"]

# shared configs
java64_home = config['hostLevelParams']['java_home']
zookeeper_hosts_list = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts_list.sort()
# get comma separated list of zookeeper hosts from clusterHostInfo
zookeeper_hosts = ",".join(zookeeper_hosts_list)
cluster_name = str(config['clusterName'])
availableServices = config['availableServices']

# for now just pick first collector
if 'metrics_collector_hosts' in config['clusterHostInfo']:
  metrics_collector_hosts_list = ",".join(config['clusterHostInfo']['metrics_collector_hosts'])
  metrics_collector_port = str(
    get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
  metrics_collector_hosts = format('http://{metrics_collector_hosts_list}:{metrics_collector_port}/ws/v1/timeline/metrics')
else:
  metrics_collector_hosts = ''

logsearch_solr_metrics_collector_hosts = format(config['configurations']['logsearch-properties']['logsearch.solr.metrics.collector.hosts'])

# Infra Solr configs
infra_solr_znode = default('/configurations/infra-solr-env/infra_solr_znode', '/infra-solr')
infra_solr_instance_count = len(config['clusterHostInfo']['infra_solr_hosts'])
infra_solr_ssl_enabled = default('configurations/infra-solr-env/infra_solr_ssl_enabled', False)
infra_solr_jmx_port = config['configurations']['infra-solr-env']['infra_solr_jmx_port']

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_hosts']):
    zookeeper_quorum += ","


if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  _hostname_lowercase = config['hostname'].lower()
  logsearch_jaas_file = logsearch_server_conf + '/logsearch_jaas.conf'
  logfeeder_jaas_file = logsearch_logfeeder_conf + '/logfeeder_jaas.conf'
  logsearch_kerberos_keytab = config['configurations']['logsearch-env']['logsearch_kerberos_keytab']
  logsearch_kerberos_principal = config['configurations']['logsearch-env']['logsearch_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  logfeeder_kerberos_keytab = config['configurations']['logfeeder-env']['logfeeder_kerberos_keytab']
  logfeeder_kerberos_principal = config['configurations']['logfeeder-env']['logfeeder_kerberos_principal'].replace('_HOST',_hostname_lowercase)

#####################################
# Logsearch configs
#####################################
logsearch_dir = '/usr/lib/ambari-logsearch-portal'

logsearch_collection_service_logs_numshards_config = config['configurations']['logsearch-properties']['logsearch.collection.service.logs.numshards']
logsearch_collection_audit_logs_numshards_config = config['configurations']['logsearch-properties']['logsearch.collection.audit.logs.numshards']

if logsearch_collection_service_logs_numshards_config > 0:
  logsearch_collection_service_logs_numshards = str(logsearch_collection_service_logs_numshards_config)
else:
  logsearch_collection_service_logs_numshards = format(str(infra_solr_instance_count))

if logsearch_collection_audit_logs_numshards_config > 0:
  logsearch_collection_audit_logs_numshards = str(logsearch_collection_audit_logs_numshards_config)
else:
  logsearch_collection_audit_logs_numshards = format(str(infra_solr_instance_count))

logsearch_collection_service_logs_replication_factor = str(config['configurations']['logsearch-properties']['logsearch.collection.service.logs.replication.factor'])
logsearch_collection_audit_logs_replication_factor = str(config['configurations']['logsearch-properties']['logsearch.collection.audit.logs.replication.factor'])

logsearch_solr_collection_service_logs = default('/configurations/logsearch-properties/logsearch.solr.collection.service.logs', 'hadoop_logs')
logsearch_solr_collection_audit_logs = default('/configurations/logsearch-properties/logsearch.solr.collection.audit.logs','audit_logs')

logsearch_service_logs_max_retention = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_max_retention']
logsearch_service_logs_merge_factor = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_merge_factor']
logsearch_service_logs_fields = config['configurations']['logsearch-properties']['logsearch.service.logs.fields']
logsearch_service_logs_split_interval_mins = config['configurations']['logsearch-properties']['logsearch.service.logs.split.interval.mins']

logsearch_audit_logs_max_retention = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_max_retention']
logsearch_audit_logs_merge_factor = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_merge_factor']
logsearch_audit_logs_split_interval_mins = config['configurations']['logsearch-properties']['logsearch.audit.logs.split.interval.mins']

logsearch_logfeeder_include_default_level = default('/configurations/logsearch-properties/logsearch.logfeeder.include.default.level', 'fatal,error,warn')

logsearch_solr_audit_logs_zk_node = default('/configurations/logsearch-env/logsearch_solr_audit_logs_zk_node', zookeeper_quorum)
logsearch_solr_audit_logs_zk_quorum = default('/configurations/logsearch-env/logsearch_solr_audit_logs_zk_quorum', infra_solr_znode)
logsearch_solr_audit_logs_zk_node = format(logsearch_solr_audit_logs_zk_node)
logsearch_solr_audit_logs_zk_quorum = format(logsearch_solr_audit_logs_zk_quorum)

# create custom properties - remove defaults
logsearch_custom_properties = dict(config['configurations']['logsearch-properties'])
logsearch_custom_properties.pop("logsearch.service.logs.fields", None)
logsearch_custom_properties.pop("logsearch.audit.logs.split.interval.mins", None)
logsearch_custom_properties.pop("logsearch.collection.service.logs.replication.factor", None)
logsearch_custom_properties.pop("logsearch.solr.collection.service.logs", None)
logsearch_custom_properties.pop("logsearch.solr.metrics.collector.hosts", None)
logsearch_custom_properties.pop("logsearch.solr.collection.audit.logs", None)
logsearch_custom_properties.pop("logsearch.logfeeder.include.default.level", None)
logsearch_custom_properties.pop("logsearch.collection.audit.logs.replication.factor", None)
logsearch_custom_properties.pop("logsearch.collection.service.logs.numshards", None)
logsearch_custom_properties.pop("logsearch.service.logs.split.interval.mins", None)
logsearch_custom_properties.pop("logsearch.collection.audit.logs.numshards", None)
logsearch_custom_properties.pop("logsearch.external.auth.enabled", None)
logsearch_custom_properties.pop("logsearch.external.auth.host_url", None)
logsearch_custom_properties.pop("logsearch.external.auth.login_url", None)

# logsearch-env configs
logsearch_user = config['configurations']['logsearch-env']['logsearch_user']
logsearch_log_dir = config['configurations']['logsearch-env']['logsearch_log_dir']
logsearch_log = logsearch_log_dir + '/logsearch.out'
logsearch_ui_protocol = config['configurations']['logsearch-env']["logsearch_ui_protocol"]
logsearch_ui_port = config['configurations']['logsearch-env']["logsearch_ui_port"]
logsearch_debug_enabled = str(config['configurations']['logsearch-env']["logsearch_debug_enabled"]).lower()
logsearch_debug_port = config['configurations']['logsearch-env']["logsearch_debug_port"]
logsearch_app_max_memory = config['configurations']['logsearch-env']['logsearch_app_max_memory']

# store the log file for the service from the 'solr.log' property of the 'logsearch-env.xml' file
logsearch_env_content = config['configurations']['logsearch-env']['content']
logsearch_service_logs_solrconfig_content = config['configurations']['logsearch-service_logs-solrconfig']['content']
logsearch_audit_logs_solrconfig_content = config['configurations']['logsearch-audit_logs-solrconfig']['content']
logsearch_app_log4j_content = config['configurations']['logsearch-log4j']['content']

# Log dirs
ambari_server_log_dir = '/var/log/ambari-server'
ambari_agent_log_dir = '/var/log/ambari-agent'
knox_log_dir = '/var/log/knox'
hst_log_dir = '/var/log/hst'
hst_activity_log_dir = '/var/log/smartsense-activity'

metrics_collector_log_dir = default('/configurations/ams-env/metrics_collector_log_dir', '/var/log/ambari-metrics-collector')
metrics_monitor_log_dir = default('/configurations/ams-env/metrics_monitor_log_dir', '/var/log/ambari-metrics-monitor')
metrics_grafana_log_dir = default('/configurations/ams-grafana-env/metrics_grafana_log_dir', '/var/log/ambari-metrics-grafana')

atlas_log_dir = default('/configurations/atlas-env/metadata_log_dir', '/var/log/atlas')
accumulo_log_dir = default('/configurations/accumulo-env/accumulo_log_dir', '/var/log/accumulo')
falcon_log_dir = default('/configurations/falcon-env/falcon_log_dir', '/var/log/falcon')
flume_log_dir = default('/configurations/flume-env/flume_log_dir', '/var/log/flume')
hbase_log_dir = default('/configurations/hbase-env/hbase_log_dir', '/var/log/hbase')
hdfs_log_dir_prefix = default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')
hive_log_dir = default('/configurations/hive-env/hive_log_dir', '/var/log/hive')
hcat_log_dir = default('configurations/hive-env/hcat_log_dir', '/var/log/webhcat')
infra_solr_log_dir = default('configurations/infra-solr-env/infra_solr_log_dir', '/var/log/ambari-infra-solr')
kafka_log_dir = default('/configurations/kafka-env/kafka_log_dir', '/var/log/kafka')
nifi_log_dir = default('/configurations/nifi-env/nifi_node_log_dir', '/var/log/nifi')
oozie_log_dir = default('/configurations/oozie-env/oozie_log_dir', '/var/log/oozie')
ranger_usersync_log_dir = default('/configurations/ranger-env/ranger_usersync_log_dir', '/var/log/ranger/usersync')
ranger_admin_log_dir = default('/configurations/ranger-env/ranger_admin_log_dir', '/var/log/ranger/admin')
ranger_kms_log_dir = default('/configurations/kms-env/kms_log_dir', '/var/log/ranger/kms')
storm_log_dir = default('/configurations/storm-env/storm_log_dir', '/var/log/storm')
yarn_log_dir_prefix = default('/configurations/yarn-env/yarn_log_dir_prefix', '/var/log/hadoop')
mapred_log_dir_prefix = default('/configurations/mapred-env/mapred_log_dir_prefix', '/var/log/hadoop')
zeppelin_log_dir = default('/configurations/zeppelin-env/zeppelin_log_dir', '/var/log/zeppelin')
zk_log_dir = default('/configurations/zookeeper-env/zk_log_dir', '/var/log/zookeeper')
spark_log_dir = default('/configurations/spark-env/spark_log_dir', '/var/log/spark')
livy_log_dir = default('/configurations/livy-env/livy_log_dir', '/var/log/livy')
spark2_log_dir = default('/configurations/spark2-env/spark_log_dir', '/var/log/spark2')

hdfs_user = default('configurations/hadoop-env/hdfs_user', 'hdfs')
mapred_user =  default('configurations/mapred-env/mapred_user', 'mapred')
yarn_user =  default('configurations/yarn-env/yarn_user', 'yarn')

#####################################
# Logsearch auth configs
#####################################

logsearch_admin_credential_file = 'logsearch-admin.json'
logsearch_admin_username = default('/configurations/logsearch-admin-json/logsearch_admin_username', "admin")
logsearch_admin_password = default('/configurations/logsearch-admin-json/logsearch_admin_password', "")
logsearch_admin_content = config['configurations']['logsearch-admin-json']['content']

# for now just pick first collector
if 'ambari_server_host' in config['clusterHostInfo']:
  ambari_server_host = config['clusterHostInfo']['ambari_server_host'][0]
  ambari_server_port = config['clusterHostInfo']['ambari_server_port'][0]
  ambari_server_use_ssl = config['clusterHostInfo']['ambari_server_use_ssl'][0] == 'true'
  
  ambari_server_protocol = 'https' if ambari_server_use_ssl else 'http'

  ambari_server_auth_host_url = format('{ambari_server_protocol}://{ambari_server_host}:{ambari_server_port}')
else:
  ambari_server_auth_host_url = ''

logsearch_auth_external_enabled = str(config['configurations']['logsearch-properties']['logsearch.external.auth.enabled']).lower()
logsearch_auth_external_host_url = format(config['configurations']['logsearch-properties']['logsearch.external.auth.host_url'])
logsearch_auth_external_login_url = config['configurations']['logsearch-properties']['logsearch.external.auth.login_url']

#####################################
# Logfeeder configs
#####################################

logfeeder_dir = "/usr/lib/ambari-logsearch-logfeeder"

# logfeeder-env configs
logfeeder_log_dir = config['configurations']['logfeeder-env']['logfeeder_log_dir']
logfeeder_log = logfeeder_log_dir + '/logfeeder.out'
logfeeder_max_mem = config['configurations']['logfeeder-env']['logfeeder_max_mem']
solr_service_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_service_logs_enable', True)
solr_audit_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_audit_logs_enable', True)
logfeeder_env_content = config['configurations']['logfeeder-env']['content']
logfeeder_log4j_content = config['configurations']['logfeeder-log4j']['content']

logsearch_keystore_location = config['configurations']['logsearch-env']['logsearch_keystore_location']
logsearch_keystore_password = config['configurations']['logsearch-env']['logsearch_keystore_password']
logsearch_keystore_type = config['configurations']['logsearch-env']['logsearch_keystore_type']
logsearch_truststore_location = config['configurations']['logsearch-env']['logsearch_truststore_location']
logsearch_truststore_password = config['configurations']['logsearch-env']['logsearch_truststore_password']
logsearch_truststore_type = config['configurations']['logsearch-env']['logsearch_truststore_type']
logfeeder_keystore_location = config['configurations']['logfeeder-env']['logfeeder_keystore_location']
logfeeder_keystore_password = config['configurations']['logfeeder-env']['logfeeder_keystore_password']
logfeeder_keystore_type = config['configurations']['logfeeder-env']['logfeeder_keystore_type']
logfeeder_truststore_location = config['configurations']['logfeeder-env']['logfeeder_truststore_location']
logfeeder_truststore_password = config['configurations']['logfeeder-env']['logfeeder_truststore_password']
logfeeder_truststore_type = config['configurations']['logfeeder-env']['logfeeder_truststore_type']

logfeeder_checkpoint_folder = default('/configurations/logfeeder-env/logfeeder.checkpoint.folder',
                                      '/etc/ambari-logsearch-logfeeder/conf/checkpoints')

logfeeder_log_filter_enable = str(default('/configurations/logfeeder-properties/logfeeder.log.filter.enable', True)).lower()
logfeeder_solr_config_interval = default('/configurations/logfeeder-properties/logfeeder.solr.config.interval', 5)

logfeeder_supported_services = ['accumulo', 'ambari', 'ams', 'atlas', 'falcon', 'flume', 'hbase', 'hdfs', 'hive', 'hst', 'infra', 'kafka',
                                'knox', 'logsearch', 'nifi', 'oozie', 'ranger', 'spark', 'spark2', 'storm', 'yarn', 'zeppelin', 'zookeeper']

logfeeder_config_file_names = ['global.config.json', 'output.config.json'] + ['input.config-%s.json' % (tag) for tag in
                                                                              logfeeder_supported_services]

default_config_files = ','.join(logfeeder_config_file_names)

logfeeder_config_files = format(config['configurations']['logfeeder-properties']['logfeeder.config.files'])
logfeeder_metrics_collector_hosts = format(config['configurations']['logfeeder-properties']['logfeeder.metrics.collector.hosts'])

logfeeder_custom_properties = dict(config['configurations']['logfeeder-properties'])
logfeeder_custom_properties.pop('logfeeder.config.files', None)
logfeeder_custom_properties.pop('logfeeder.checkpoint.folder', None)
logfeeder_custom_properties.pop('logfeeder.metrics.collector.hosts', None)
logfeeder_custom_properties.pop('logfeeder.log.filter.enable', None)
logfeeder_custom_properties.pop('logfeeder.solr.config.interval', None)

logsearch_server_hosts = config['clusterHostInfo']['logsearch_server_hosts']
logsearch_server_host = ""
if logsearch_server_hosts is not None and len(logsearch_server_hosts) > 0:
  logsearch_server_host = logsearch_server_hosts[0]
smoke_logsearch_cmd = format('curl -k -s -o /dev/null -w "%{{http_code}}" {logsearch_ui_protocol}://{logsearch_server_host}:{logsearch_ui_port}/login.html | grep 200')
