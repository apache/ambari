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
import os
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

logsearch_solr_conf = "/etc/ambari-logsearch-solr/conf"
logsearch_server_conf = "/etc/ambari-logsearch-portal/conf"
logsearch_logfeeder_conf = "/etc/ambari-logsearch-logfeeder/conf"

logsearch_config_set_dir = format("{logsearch_server_conf}/solr_configsets")

logsearch_solr_port = status_params.logsearch_solr_port
logsearch_solr_piddir = status_params.logsearch_solr_piddir
logsearch_solr_pidfile = status_params.logsearch_solr_pidfile

# logsearch pid file
logsearch_pid_dir = status_params.logsearch_pid_dir
logsearch_pid_file = status_params.logsearch_pid_file

# logfeeder pid file
logfeeder_pid_dir = status_params.logfeeder_pid_dir
logfeeder_pid_file = status_params.logfeeder_pid_file

# shared configs
java64_home = config['hostLevelParams']['java_home']
zookeeper_hosts_list = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts_list.sort()
# get comma separated list of zookeeper hosts from clusterHostInfo
zookeeper_hosts = ",".join(zookeeper_hosts_list)
cluster_name = str(config['clusterName'])

# for now just pick first collector
if 'metrics_collector_hosts' in config['clusterHostInfo']:
  metrics_collector_hosts_list = ",".join(config['clusterHostInfo']['metrics_collector_hosts'])
  metrics_collector_port = str(
    get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
  metrics_collector_hosts = format('http://{metrics_collector_hosts_list}:{metrics_collector_port}/ws/v1/timeline/metrics')
else:
  metrics_collector_hosts = ''

#####################################
# Solr configs
#####################################

# Only supporting SolrCloud mode - so hardcode those options
solr_cloudmode = 'true'
solr_dir = '/usr/lib/ambari-logsearch-solr'
solr_client_dir = '/usr/lib/ambari-logsearch-solr-client'
solr_bindir = solr_dir + '/bin'
cloud_scripts = solr_dir + '/server/scripts/cloud-scripts'

logsearch_solr_znode = config['configurations']['logsearch-solr-env']['logsearch_solr_znode']
logsearch_solr_min_mem = format(config['configurations']['logsearch-solr-env']['logsearch_solr_minmem'])
logsearch_solr_max_mem = format(config['configurations']['logsearch-solr-env']['logsearch_solr_maxmem'])
logsearch_solr_instance_count = len(config['clusterHostInfo']['logsearch_solr_hosts'])
logsearch_solr_datadir = format(config['configurations']['logsearch-solr-env']['logsearch_solr_datadir'])
logsearch_solr_data_resources_dir = os.path.join(logsearch_solr_datadir, 'resources')
logsearch_service_logs_max_retention = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_max_retention']
logsearch_service_logs_merge_factor = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_merge_factor']
logsearch_audit_logs_max_retention = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_max_retention']
logsearch_audit_logs_merge_factor = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_merge_factor']

logsearch_solr_metrics_collector_hosts = format(config['configurations']['logsearch-properties']['logsearch.solr.metrics.collector.hosts'])
logsearch_solr_jmx_port = config['configurations']['logsearch-solr-env']['logsearch_solr_jmx_port']

logsearch_service_logs_fields = config['configurations']['logsearch-properties']['logsearch.service.logs.fields']

audit_logs_collection_splits_interval_mins = config['configurations']['logsearch-properties']['logsearch.audit.logs.split.interval.mins']
service_logs_collection_splits_interval_mins = config['configurations']['logsearch-properties']['logsearch.service.logs.split.interval.mins']

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
# get comma separated list of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_hosts']):
    zookeeper_quorum += ","

if 'zoo.cfg' in config['configurations']:
  zoo_cfg_properties_map = config['configurations']['zoo.cfg']
else:
  zoo_cfg_properties_map = {}

logsearch_solr_user = config['configurations']['logsearch-solr-env']['logsearch_solr_user']
logsearch_solr_group = config['configurations']['logsearch-solr-env']['logsearch_solr_group']
logsearch_solr_log_dir = config['configurations']['logsearch-solr-env']['logsearch_solr_log_dir']
logsearch_solr_client_log_dir = config['configurations']['logsearch-solr-env']['logsearch_solr_client_log_dir']
logsearch_solr_client_log = format("{logsearch_solr_client_log_dir}/solr-client.log")
logsearch_solr_log = format("{logsearch_solr_log_dir}/solr-install.log")

solr_env_content = config['configurations']['logsearch-solr-env']['content']

solr_xml_content = config['configurations']['logsearch-solr-xml']['content']

solr_log4j_content = config['configurations']['logsearch-solr-log4j']['content']

solr_client_log4j_content = config['configurations']['logsearch-solr-client-log4j']['content']

#####################################
# Logsearch configs
#####################################
logsearch_dir = '/usr/lib/ambari-logsearch-portal'

logsearch_service_numshards_config = config['configurations']['logsearch-properties']['logsearch.collection.service.logs.numshards']
logsearch_audit_numshards_config = config['configurations']['logsearch-properties']['logsearch.collection.audit.logs.numshards']

if logsearch_service_numshards_config > 0:
  logsearch_service_logs_numshards = str(logsearch_service_numshards_config)
else:
  logsearch_service_logs_numshards = format(str(logsearch_solr_instance_count))

if logsearch_audit_numshards_config > 0:
  logsearch_audit_logs_numshards = str(logsearch_audit_numshards_config)
else:
  logsearch_audit_logs_numshards = format(str(logsearch_audit_numshards_config))

logsearch_service_logs_repfactor = str(config['configurations']['logsearch-properties']['logsearch.collection.service.logs.replication.factor'])
logsearch_audit_logs_repfactor = str(config['configurations']['logsearch-properties']['logsearch.collection.audit.logs.replication.factor'])

logsearch_solr_collection_service_logs = default('/configurations/logsearch-properties/logsearch.solr.collection.service.logs', 'hadoop_logs')
logsearch_solr_collection_audit_logs = default('/configurations/logsearch-properties/logsearch.solr.collection.audit.logs','audit_logs')
logsearch_logfeeder_log_level_include = default('/configurations/logsearch-properties/logsearch.logfeeder.include.default.level', 'fatal,error,warn')

solr_audit_logs_use_ranger = default('/configurations/logsearch-env/logsearch_solr_audit_logs_use_ranger', False)
solr_audit_logs_url = ''

if solr_audit_logs_use_ranger:
  # In Ranger, this contain the /zkNode also
  ranger_audit_solr_zookeepers = default('/configurations/ranger-admin-site/ranger.audit.solr.zookeepers', None)
  # TODO: ranger property already has zk node appended. We need to remove it.
  # For now, let's assume it is going to be URL
  solr_audit_logs_url = default('/configurations/ranger-admin-site/ranger.audit.solr.urls', solr_audit_logs_url)
else:
  solr_audit_logs_zk_node = default('/configurations/logsearch-env/logsearch_solr_audit_logs_zk_node', None)
  solr_audit_logs_zk_quorum = default('/configurations/logsearch-env/logsearch_solr_audit_logs_zk_quorum', None)

  if not (solr_audit_logs_zk_quorum):
    solr_audit_logs_zk_quorum = zookeeper_quorum
  if not (solr_audit_logs_zk_node):
    solr_audit_logs_zk_node = logsearch_solr_znode

  solr_audit_logs_zk_node = format(solr_audit_logs_zk_node)
  solr_audit_logs_zk_quorum = format(solr_audit_logs_zk_quorum)

# create custom properties - remove defaults
logsearch_custom_properties = dict(config['configurations']['logsearch-properties'])
logsearch_custom_properties.pop("logsearch.collection.numshards", None)
logsearch_custom_properties.pop("logsearch.collection.replication.factor", None)
logsearch_custom_properties.pop("logsearch.solr.collection.service.logs", None)
logsearch_custom_properties.pop("logsearch.solr.collection.audit.logs", None)
logsearch_custom_properties.pop("logsearch.service.logs.fields", None)
logsearch_custom_properties.pop("logsearch.service.logs.split.interval.mins", None)
logsearch_custom_properties.pop("logsearch.audit.logs.split.interval.mins", None)
logsearch_custom_properties.pop("logsearch.logfeeder.include.default.level", None)

# logsearch-env configs
logsearch_user = config['configurations']['logsearch-env']['logsearch_user']
logsearch_group = config['configurations']['logsearch-env']['logsearch_group']
logsearch_log_dir = config['configurations']['logsearch-env']['logsearch_log_dir']
logsearch_log = logsearch_log_dir + '/logsearch.out'
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

metrics_collector_log_dir = default('/configurations/ams-env/metrics_collector_log_dir', '/var/log')
metrics_monitor_log_dir = default('/configurations/ams-env/metrics_monitor_log_dir', '/var/log')

atlas_log_dir = default('/configurations/atlas-env/metadata_log_dir', '/var/log/atlas')
accumulo_log_dir = default('/configurations/accumulo-env/accumulo_log_dir', '/var/log/accumulo')
falcon_log_dir = default('/configurations/falcon-env/falcon_log_dir', '/var/log/falcon')
hbase_log_dir = default('/configurations/hbase-env/hbase_log_dir', '/var/log/hbase')
hdfs_log_dir_prefix = default('/configurations/hadoop-env/hdfs_log_dir_prefix', '/var/log/hadoop')
hive_log_dir = default('/configurations/hive-env/hive_log_dir', '/var/log/hive')
kafka_log_dir = default('/configurations/kafka-env/kafka_log_dir', '/var/log/kafka')
oozie_log_dir = default('/configurations/oozie-env/oozie_log_dir', '/var/log/oozie')
ranger_usersync_log_dir = default('/configurations/ranger-env/ranger_usersync_log_dir', '/var/log/ranger/usersync')
ranger_admin_log_dir = default('/configurations/ranger-env/ranger_admin_log_dir', '/var/log/ranger/admin')
ranger_kms_log_dir = default('/configurations/kms-env/kms_log_dir', '/var/log/ranger/kms')
storm_log_dir = default('/configurations/storm-env/storm_log_dir', '/var/log/storm')
yarn_log_dir_prefix = default('/configurations/yarn-env/yarn_log_dir_prefix', '/var/log/hadoop')
mapred_log_dir_prefix = default('/configurations/mapred-env/mapred_log_dir_prefix', '/var/log/hadoop')
zk_log_dir = default('/configurations/zookeeper-env/zk_log_dir', '/var/log/zookeeper')

#####################################
# Logsearch admin configs
#####################################

logsearch_admin_credential_file = 'logsearch-admin.json'
logsearch_admin_username = default('/configurations/logsearch-admin-json/logsearch_admin_username', "admin")
logsearch_admin_password = default('/configurations/logsearch-admin-json/logsearch_admin_password', "")
logsearch_admin_content = config['configurations']['logsearch-admin-json']['content']

#####################################
# Logfeeder configs
#####################################

logfeeder_dir = "/usr/lib/ambari-logsearch-logfeeder"

# logfeeder-env configs
logfeeder_user = config['configurations']['logfeeder-env']['logfeeder_user']
logfeeder_group = config['configurations']['logfeeder-env']['logfeeder_group']
logfeeder_log_dir = config['configurations']['logfeeder-env']['logfeeder_log_dir']
logfeeder_log = logfeeder_log_dir + '/logfeeder.out'
logfeeder_max_mem = config['configurations']['logfeeder-env']['logfeeder_max_mem']
solr_service_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_service_logs_enable', True)
solr_audit_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_audit_logs_enable', True)
logfeeder_env_content = config['configurations']['logfeeder-env']['content']
logfeeder_log4j_content = config['configurations']['logfeeder-log4j']['content']

logfeeder_checkpoint_folder = default('/configurations/logfeeder-env/logfeeder.checkpoint.folder',
                                      '/etc/ambari-logsearch-logfeeder/conf/checkpoints')

logfeeder_log_filter_enable = str(default('/configurations/logfeeder-properties/logfeeder.log.filter.enable', True)).lower()
logfeeder_solr_config_interval = default('/configurations/logfeeder-properties/logfeeder.solr.config.interval', 5)

logfeeder_supported_services = ['accumulo', 'ambari', 'ams', 'atlas', 'falcon', 'hbase', 'hdfs', 'hive', 'kafka',
                                'knox', 'logsearch', 'oozie', 'ranger', 'storm', 'yarn', 'zookeeper']

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