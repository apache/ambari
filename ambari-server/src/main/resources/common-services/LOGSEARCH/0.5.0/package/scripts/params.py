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
from logsearch_config_aggregator import get_logfeeder_metadata, get_logsearch_metadata, get_logsearch_meta_configs
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

sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled

logsearch_server_conf = "/etc/ambari-logsearch-portal/conf"
logsearch_server_keys_folder = logsearch_server_conf + "/keys"
logsearch_logfeeder_conf = "/etc/ambari-logsearch-logfeeder/conf"

logsearch_config_set_dir = format("{logsearch_server_conf}/solr_configsets")

# logsearch pid file
logsearch_pid_dir = status_params.logsearch_pid_dir
logsearch_pid_file = status_params.logsearch_pid_file

# logfeeder pid file
logfeeder_pid_dir = status_params.logfeeder_pid_dir
logfeeder_pid_file = status_params.logfeeder_pid_file

user_group = config['configurations']['cluster-env']['user_group']

# shared configs
java64_home = config['hostLevelParams']['java_home']
cluster_name = str(config['clusterName'])

configurations = config['configurations'] # need reference inside logfeeder jinja templates
logserch_meta_configs = get_logsearch_meta_configs(configurations)
logsearch_metadata = get_logsearch_metadata(logserch_meta_configs)
logfeeder_metadata = get_logfeeder_metadata(logserch_meta_configs)

# for now just pick first collector
if 'metrics_collector_hosts' in config['clusterHostInfo']:
  metrics_collector_hosts_list = ",".join(config['clusterHostInfo']['metrics_collector_hosts'])
  metrics_collector_port = str(
    get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
  metrics_collector_hosts = format('http://{metrics_collector_hosts_list}:{metrics_collector_port}/ws/v1/timeline/metrics')
else:
  metrics_collector_hosts = ''

#####################################
# Infra Solr configs
#####################################
infra_solr_znode = '/infra-solr'
infra_solr_ssl_enabled = False
infra_solr_jmx_port = ''

if 'infra-solr-env' in config['configurations']:
  infra_solr_znode = default('/configurations/infra-solr-env/infra_solr_znode', '/infra-solr')
  infra_solr_ssl_enabled = default('configurations/infra-solr-env/infra_solr_ssl_enabled', False)
  infra_solr_jmx_port = config['configurations']['infra-solr-env']['infra_solr_jmx_port']

_hostname_lowercase = config['hostname'].lower()
if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  logsearch_jaas_file = logsearch_server_conf + '/logsearch_jaas.conf'
  logfeeder_jaas_file = logsearch_logfeeder_conf + '/logfeeder_jaas.conf'
  use_external_solr_with_kerberos = default('configurations/logsearch-common-env/logsearch_external_solr_kerberos_enabled', False)
  if use_external_solr_with_kerberos:
    logsearch_kerberos_keytab = config['configurations']['logsearch-env']['logsearch_external_solr_kerberos_keytab']
    logsearch_kerberos_principal = config['configurations']['logsearch-env']['logsearch_external_solr_kerberos_principal'].replace('_HOST',_hostname_lowercase)
    logfeeder_kerberos_keytab = config['configurations']['logfeeder-env']['logfeeder_external_solr_kerberos_keytab']
    logfeeder_kerberos_principal = config['configurations']['logfeeder-env']['logfeeder_external_solr_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  else:
    logsearch_kerberos_keytab = config['configurations']['logsearch-env']['logsearch_kerberos_keytab']
    logsearch_kerberos_principal = config['configurations']['logsearch-env']['logsearch_kerberos_principal'].replace('_HOST',_hostname_lowercase)
    logfeeder_kerberos_keytab = config['configurations']['logfeeder-env']['logfeeder_kerberos_keytab']
    logfeeder_kerberos_principal = config['configurations']['logfeeder-env']['logfeeder_kerberos_principal'].replace('_HOST',_hostname_lowercase)

logsearch_spnego_host = config['configurations']['logsearch-properties']['logsearch.spnego.kerberos.host'].replace('_HOST', _hostname_lowercase)

#####################################
# Logsearch configs
#####################################
logsearch_dir = '/usr/lib/ambari-logsearch-portal'

logsearch_service_logs_max_retention = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_max_retention']
logsearch_service_logs_merge_factor = config['configurations']['logsearch-service_logs-solrconfig']['logsearch_service_logs_merge_factor']

logsearch_audit_logs_max_retention = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_max_retention']
logsearch_audit_logs_merge_factor = config['configurations']['logsearch-audit_logs-solrconfig']['logsearch_audit_logs_merge_factor']

logsearch_use_external_solr = default('/configurations/logsearch-common-env/logsearch_use_external_solr', False)

if logsearch_use_external_solr:
  logsearch_solr_zk_znode = config['configurations']['logsearch-common-env']['logsearch_external_solr_zk_znode']
  logsearch_solr_zk_quorum = config['configurations']['logsearch-common-env']['logsearch_external_solr_zk_quorum']
  logsearch_solr_ssl_enabled = default('configurations/logsearch-common-env/logsearch_external_solr_ssl_enabled', False)
  logsearch_solr_kerberos_enabled = security_enabled and use_external_solr_with_kerberos
else:
  logsearch_solr_zk_znode = infra_solr_znode

  logsearch_solr_zk_quorum = ""
  zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
  if 'zookeeper_hosts' in config['clusterHostInfo']:
    for host in config['clusterHostInfo']['zookeeper_hosts']:
      if logsearch_solr_zk_quorum:
        logsearch_solr_zk_quorum += ','
      logsearch_solr_zk_quorum += host + ":" + str(zookeeper_port)
  
  logsearch_solr_ssl_enabled = infra_solr_ssl_enabled
  logsearch_solr_kerberos_enabled = security_enabled

zookeeper_quorum = logsearch_solr_zk_quorum



# logsearch-env configs
logsearch_user = config['configurations']['logsearch-env']['logsearch_user']
logsearch_log_dir = config['configurations']['logsearch-env']['logsearch_log_dir']
logsearch_log = logsearch_log_dir + '/logsearch.out'
logsearch_ui_protocol = config['configurations']['logsearch-env']["logsearch_ui_protocol"]
logsearch_ui_port = config['configurations']['logsearch-env']["logsearch_ui_port"]
logsearch_debug_enabled = str(config['configurations']['logsearch-env']["logsearch_debug_enabled"]).lower()
logsearch_debug_port = config['configurations']['logsearch-env']["logsearch_debug_port"]
logsearch_app_max_memory = config['configurations']['logsearch-env']['logsearch_app_max_memory']

#Logsearch log4j properties
logsearch_log_maxfilesize = default('/configurations/logsearch-log4j/logsearch_log_maxfilesize',10)
logsearch_log_maxbackupindex = default('/configurations/logsearch-log4j/logsearch_log_maxbackupindex',10)
logsearch_json_log_maxfilesize = default('/configurations/logsearch-log4j/logsearch_json_log_maxfilesize',10)
logsearch_json_log_maxbackupindex = default('/configurations/logsearch-log4j/logsearch_json_log_maxbackupindex',10)
logsearch_audit_log_maxfilesize = default('/configurations/logsearch-log4j/logsearch_audit_log_maxfilesize',10)
logsearch_audit_log_maxbackupindex =default('/configurations/logsearch-log4j/logsearch_audit_log_maxbackupindex',10)
logsearch_perf_log_maxfilesize =default('/configurations/logsearch-log4j/logsearch_perf_log_maxfilesize',10)
logsearch_perf_log_maxbackupindex =default('/configurations/logsearch-log4j/logsearch_perf_log_maxbackupindex',10)

# store the log file for the service from the 'solr.log' property of the 'logsearch-env.xml' file
logsearch_env_content = config['configurations']['logsearch-env']['content']
logsearch_service_logs_solrconfig_content = config['configurations']['logsearch-service_logs-solrconfig']['content']
logsearch_audit_logs_solrconfig_content = config['configurations']['logsearch-audit_logs-solrconfig']['content']
logsearch_app_log4j_content = config['configurations']['logsearch-log4j']['content']

# Log dirs
ambari_server_log_dir = '/var/log/ambari-server'
ambari_agent_log_dir = '/var/log/ambari-agent'
hst_log_dir = '/var/log/hst'
hst_activity_log_dir = '/var/log/smartsense-activity'
nifi_log_dir = default('/configurations/nifi-env/nifi_node_log_dir', '/var/log/nifi')

# System logs
logfeeder_system_messages_content = config['configurations']['logfeeder-system_log-env']['logfeeder_system_messages_content']
logfeeder_secure_log_content = config['configurations']['logfeeder-system_log-env']['logfeeder_secure_log_content']
logfeeder_system_log_enabled = default('/configurations/logfeeder-system_log-env/logfeeder_system_log_enabled', False)

# Logsearch auth configs

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

# Logsearch propreties

logsearch_properties = {}

# default values

logsearch_properties['logsearch.solr.zk_connect_string'] = logsearch_solr_zk_quorum + logsearch_solr_zk_znode
logsearch_properties['logsearch.solr.audit.logs.zk_connect_string'] = logsearch_solr_zk_quorum + logsearch_solr_zk_znode

logsearch_properties['logsearch.solr.collection.history'] = 'history'
logsearch_properties['logsearch.solr.history.config.name'] = 'history'
logsearch_properties['logsearch.collection.history.replication.factor'] = '1'

logsearch_properties['logsearch.solr.jmx.port'] = infra_solr_jmx_port

logsearch_properties['logsearch.login.credentials.file'] = logsearch_admin_credential_file
logsearch_properties['logsearch.auth.file.enabled'] = 'true'
logsearch_properties['logsearch.auth.ldap.enabled'] = 'false'
logsearch_properties['logsearch.auth.simple.enabled'] = 'false'

logsearch_properties['logsearch.protocol'] = logsearch_ui_protocol

# load config values

logsearch_properties = dict(logsearch_properties.items() + dict(config['configurations']['logsearch-properties']).items())

# load derivated values

if logsearch_properties['logsearch.solr.audit.logs.use.ranger'] == 'false':
  del logsearch_properties['logsearch.ranger.audit.logs.collection.name']

del logsearch_properties['logsearch.solr.audit.logs.use.ranger']

logsearch_properties['logsearch.solr.metrics.collector.hosts'] = format(logsearch_properties['logsearch.solr.metrics.collector.hosts'])
logsearch_properties['logsearch.auth.external_auth.host_url'] = format(logsearch_properties['logsearch.auth.external_auth.host_url'])
logsearch_properties['logsearch.spnego.kerberos.host'] = logsearch_spnego_host

if logsearch_solr_kerberos_enabled:
  logsearch_properties['logsearch.solr.kerberos.enable'] = 'true'
  logsearch_properties['logsearch.solr.jaas.file'] = logsearch_jaas_file


logsearch_solr_collection_service_logs = logsearch_properties['logsearch.solr.collection.service.logs']
logsearch_service_logs_split_interval_mins = logsearch_properties['logsearch.service.logs.split.interval.mins']
logsearch_collection_service_logs_numshards = logsearch_properties['logsearch.collection.service.logs.numshards']

logsearch_solr_collection_audit_logs = logsearch_properties['logsearch.solr.collection.audit.logs']
logsearch_audit_logs_split_interval_mins = logsearch_properties['logsearch.audit.logs.split.interval.mins']
logsearch_collection_audit_logs_numshards = logsearch_properties['logsearch.collection.audit.logs.numshards']

#####################################
# Logfeeder configs
#####################################

logfeeder_dir = "/usr/lib/ambari-logsearch-logfeeder"

# logfeeder-log4j
logfeeder_log_maxfilesize = default('/configurations/logfeeder-log4j/logfeeder_log_maxfilesize',10)
logfeeder_log_maxbackupindex =  default('/configurations/logfeeder-log4j/logfeeder_log_maxbackupindex',10)
logfeeder_json_log_maxfilesize = default('/configurations/logfeeder-log4j/logfeeder_json_log_maxfilesize',10)
logfeeder_json_log_maxbackupindex = default('/configurations/logfeeder-log4j/logfeeder_json_log_maxbackupindex',10)

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

logfeeder_ambari_config_content = config['configurations']['logfeeder-ambari-config']['content']
logfeeder_output_config_content = config['configurations']['logfeeder-output-config']['content']

logfeeder_default_services = ['logsearch']
logfeeder_default_config_file_names = ['global.config.json'] + ['input.config-%s.json' % (tag) for tag in logfeeder_default_services]
logfeeder_custom_config_file_names = ['input.config-%s.json' % (tag.replace('-logsearch-conf', ''))
                                      for tag, content in logfeeder_metadata.iteritems() if any(logfeeder_metadata)]

if logfeeder_system_log_enabled:
  default_config_files = ','.join(['output.json','input.config-ambari.json'] + logfeeder_default_config_file_names + logfeeder_custom_config_file_names
                                  + ['input.config-system_messages.json', 'input.config-secure_log.json'])
else:
  default_config_files = ','.join(['output.config.json','input.config-ambari.json'] + logfeeder_default_config_file_names + logfeeder_custom_config_file_names)


logfeeder_grok_patterns = config['configurations']['logfeeder-grok']['default_grok_patterns']
if config['configurations']['logfeeder-grok']['custom_grok_patterns'].strip():
  logfeeder_grok_patterns = \
    logfeeder_grok_patterns + '\n' + \
    '\n' + \
    '########################\n' +\
    '# Custom Grok Patterns #\n' +\
    '########################\n' +\
    '\n' + \
    config['configurations']['logfeeder-grok']['custom_grok_patterns']

# logfeeder properties

# load default values

logfeeder_properties = {}

logfeeder_properties['logfeeder.solr.core.config.name'] = 'history'

# load config values

logfeeder_properties = dict(logfeeder_properties.items() + dict(config['configurations']['logfeeder-properties']).items())

# load derivated values

logfeeder_properties['logfeeder.metrics.collector.hosts'] = format(logfeeder_properties['logfeeder.metrics.collector.hosts'])
logfeeder_properties['logfeeder.config.files'] = format(logfeeder_properties['logfeeder.config.files'])
logfeeder_properties['logfeeder.solr.zk_connect_string'] = logsearch_solr_zk_quorum + logsearch_solr_zk_znode

if logsearch_solr_kerberos_enabled:
  if 'logfeeder.solr.kerberos.enable' not in logfeeder_properties:
    logfeeder_properties['logfeeder.solr.kerberos.enable'] = 'true'
  if 'logfeeder.solr.jaas.file' not in logfeeder_properties:
    logfeeder_properties['logfeeder.solr.jaas.file'] = logfeeder_jaas_file

logfeeder_checkpoint_folder = logfeeder_properties['logfeeder.checkpoint.folder']

#####################################
# Smoke command
#####################################

logsearch_server_hosts = config['clusterHostInfo']['logsearch_server_hosts']
logsearch_server_host = ""
if logsearch_server_hosts is not None and len(logsearch_server_hosts) > 0:
  logsearch_server_host = logsearch_server_hosts[0]
smoke_logsearch_cmd = format('curl -k -s -o /dev/null -w "%{{http_code}}" {logsearch_ui_protocol}://{logsearch_server_host}:{logsearch_ui_port}/login.html | grep 200')
