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

def get_name_from_principal(principal):
  if not principal:  # return if empty
    return principal
  slash_split = principal.split('/')
  if len(slash_split) == 2:
    return slash_split[0]
  else:
    at_split = principal.split('@')
    return at_split[0]


# config object that holds the configurations declared in the -site.xml file
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled

credential_store_enabled = False
if 'credentialStoreEnabled' in config:
  credential_store_enabled = config['credentialStoreEnabled']

logsearch_server_conf = "/usr/lib/ambari-logsearch-portal/conf"
logsearch_server_keys_folder = logsearch_server_conf + "/keys"
logsearch_logfeeder_conf = "/usr/lib/ambari-logsearch-logfeeder/conf"
logsearch_logfeeder_keys_folder = logsearch_logfeeder_conf + "/keys"

logsearch_config_set_dir = format("{logsearch_server_conf}/solr_configsets")

# logsearch pid file
logsearch_pid_dir = status_params.logsearch_pid_dir
logsearch_pid_file = status_params.logsearch_pid_file

# logfeeder pid file
logfeeder_pid_dir = status_params.logfeeder_pid_dir
logfeeder_pid_file = status_params.logfeeder_pid_file

user_group = config['configurations']['cluster-env']['user_group']

# shared configs
java_home = config['hostLevelParams']['java_home']
ambari_java_home = default("/commandParams/ambari_java_home", None)
java64_home = ambari_java_home if ambari_java_home is not None else java_home
cluster_name = str(config['clusterName'])

configurations = config['configurations'] # need reference inside logfeeder jinja templates

# for now just pick first collector
if 'metrics_collector_hosts' in config['clusterHostInfo']:
  metrics_http_policy = config['configurations']['ams-site']['timeline.metrics.service.http.policy']
  metrics_collector_protocol = 'http'
  if metrics_http_policy == 'HTTPS_ONLY':
    metrics_collector_protocol = 'https'
    
  metrics_collector_hosts = ",".join(config['clusterHostInfo']['metrics_collector_hosts'])
  metrics_collector_port = str(get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
  metrics_collector_path = '/ws/v1/timeline/metrics'
else:
  metrics_collector_protocol = ''
  metrics_collector_hosts = ''
  metrics_collector_port = ''
  metrics_collector_path = ''

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

infra_solr_role_logsearch = default('configurations/infra-solr-security-json/infra_solr_role_logsearch', 'logsearch_user')
infra_solr_role_logfeeder = default('configurations/infra-solr-security-json/infra_solr_role_logfeeder', 'logfeeder_user')
infra_solr_role_dev = default('configurations/infra-solr-security-json/infra_solr_role_dev', 'dev')
infra_solr_role_ranger_admin = default('configurations/infra-solr-security-json/infra_solr_role_ranger_admin', 'ranger_user')

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
logsearch_log = 'logsearch.out'
logsearch_debug_enabled = str(config['configurations']['logsearch-env']["logsearch_debug_enabled"]).lower()
logsearch_debug_port = config['configurations']['logsearch-env']["logsearch_debug_port"]
logsearch_app_max_memory = config['configurations']['logsearch-env']['logsearch_app_max_memory']

logsearch_keystore_location = config['configurations']['logsearch-env']['logsearch_keystore_location']
logsearch_keystore_type = config['configurations']['logsearch-env']['logsearch_keystore_type']
logsearch_keystore_password = config['configurations']['logsearch-env']['logsearch_keystore_password']
logsearch_truststore_location = config['configurations']['logsearch-env']['logsearch_truststore_location']
logsearch_truststore_type = config['configurations']['logsearch-env']['logsearch_truststore_type']
logsearch_truststore_password = config['configurations']['logsearch-env']['logsearch_truststore_password']

logsearch_env_config = dict(config['configurations']['logsearch-env'])
logsearch_env_jceks_file = os.path.join(logsearch_server_conf, 'logsearch.jceks')

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

logsearch_protocol = config['configurations']['logsearch-properties']["logsearch.protocol"]
logsearch_http_port = config['configurations']['logsearch-properties']["logsearch.http.port"]
logsearch_https_port = config['configurations']['logsearch-properties']["logsearch.https.port"]

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

# load config values

logsearch_properties = dict(logsearch_properties.items() +\
                       dict(config['configurations']['logsearch-common-properties']).items() +\
                       dict(config['configurations']['logsearch-properties']).items())

# load derivated values

if logsearch_properties['logsearch.solr.audit.logs.use.ranger'] == 'false':
  del logsearch_properties['logsearch.ranger.audit.logs.collection.name']

del logsearch_properties['logsearch.solr.audit.logs.use.ranger']

logsearch_properties['logsearch.solr.metrics.collector.hosts'] = format(logsearch_properties['logsearch.solr.metrics.collector.hosts'])
logsearch_properties['logsearch.auth.external_auth.host_url'] = format(logsearch_properties['logsearch.auth.external_auth.host_url'])
logsearch_properties['logsearch.spnego.kerberos.host'] = logsearch_spnego_host

if not('logsearch.config.zk_connect_string' in logsearch_properties):
  logsearch_properties['logsearch.config.zk_connect_string'] = logsearch_solr_zk_quorum

if logsearch_solr_kerberos_enabled:
  logsearch_properties['logsearch.solr.kerberos.enable'] = 'true'
  logsearch_properties['logsearch.solr.jaas.file'] = logsearch_jaas_file


logsearch_solr_collection_service_logs = logsearch_properties['logsearch.solr.collection.service.logs']
logsearch_service_logs_split_interval_mins = logsearch_properties['logsearch.service.logs.split.interval.mins']
logsearch_collection_service_logs_numshards = logsearch_properties['logsearch.collection.service.logs.numshards']

logsearch_solr_collection_audit_logs = logsearch_properties['logsearch.solr.collection.audit.logs']
logsearch_audit_logs_split_interval_mins = logsearch_properties['logsearch.audit.logs.split.interval.mins']
logsearch_collection_audit_logs_numshards = logsearch_properties['logsearch.collection.audit.logs.numshards']

# check if logsearch uses ssl in any way

logsearch_use_ssl = logsearch_solr_ssl_enabled or logsearch_protocol == 'https' or ambari_server_use_ssl

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
logfeeder_log = 'logfeeder.out'
logfeeder_max_mem = config['configurations']['logfeeder-env']['logfeeder_max_mem']
solr_service_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_service_logs_enable', True)
solr_audit_logs_enable = default('/configurations/logfeeder-env/logfeeder_solr_audit_logs_enable', True)
logfeeder_env_content = config['configurations']['logfeeder-env']['content']
logfeeder_log4j_content = config['configurations']['logfeeder-log4j']['content']

logfeeder_keystore_location = config['configurations']['logfeeder-env']['logfeeder_keystore_location']
logfeeder_keystore_type = config['configurations']['logfeeder-env']['logfeeder_keystore_type']
logfeeder_keystore_password = config['configurations']['logfeeder-env']['logfeeder_keystore_password']
logfeeder_truststore_location = config['configurations']['logfeeder-env']['logfeeder_truststore_location']
logfeeder_truststore_type = config['configurations']['logfeeder-env']['logfeeder_truststore_type']
logfeeder_truststore_password = config['configurations']['logfeeder-env']['logfeeder_truststore_password']

logfeeder_env_config = dict(config['configurations']['logfeeder-env'])
logfeeder_env_jceks_file = os.path.join(logsearch_logfeeder_conf, 'logfeeder.jceks')

logfeeder_ambari_config_content = config['configurations']['logfeeder-ambari-config']['content']
logfeeder_output_config_content = config['configurations']['logfeeder-output-config']['content']

default_config_files = ','.join(['output.config.json','global.config.json'])

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

logfeeder_properties = dict(logfeeder_properties.items() +\
                       dict(config['configurations']['logsearch-common-properties']).items() +\
                       dict(config['configurations']['logfeeder-properties']).items())

# load derivated values

logfeeder_properties['cluster.name'] = cluster_name
logfeeder_properties['logfeeder.config.dir'] = logsearch_logfeeder_conf
logfeeder_properties['logfeeder.config.files'] = format(logfeeder_properties['logfeeder.config.files'])
logfeeder_properties['logfeeder.solr.zk_connect_string'] = logsearch_solr_zk_quorum + logsearch_solr_zk_znode

logfeeder_properties['logfeeder.metrics.collector.hosts'] = format(logfeeder_properties['logfeeder.metrics.collector.hosts'])
logfeeder_properties['logfeeder.metrics.collector.protocol'] = metrics_collector_protocol
logfeeder_properties['logfeeder.metrics.collector.port'] = metrics_collector_port
logfeeder_properties['logfeeder.metrics.collector.path'] = '/ws/v1/timeline/metrics'

if not('logsearch.config.zk_connect_string' in logfeeder_properties):
  logfeeder_properties['logsearch.config.zk_connect_string'] = logsearch_solr_zk_quorum

if logsearch_solr_kerberos_enabled:
  if 'logfeeder.solr.kerberos.enable' not in logfeeder_properties:
    logfeeder_properties['logfeeder.solr.kerberos.enable'] = 'true'
  if 'logfeeder.solr.jaas.file' not in logfeeder_properties:
    logfeeder_properties['logfeeder.solr.jaas.file'] = logfeeder_jaas_file

logfeeder_checkpoint_folder = logfeeder_properties['logfeeder.checkpoint.folder']

# check if logfeeder uses ssl in any way

logfeeder_use_ssl = logsearch_solr_ssl_enabled or metrics_collector_protocol == 'https'


logsearch_acls = ''
if 'infra-solr-env' in config['configurations'] and security_enabled and not logsearch_use_external_solr:
  acl_infra_solr_principal = get_name_from_principal(config['configurations']['infra-solr-env']['infra_solr_kerberos_principal'])
  acl_logsearch_principal = get_name_from_principal(config['configurations']['logsearch-env']['logsearch_kerberos_principal'])
  logsearch_acls = format('world:anyone:r,sasl:{acl_infra_solr_principal}:cdrwa,sasl:{acl_logsearch_principal}:cdrwa')
  logsearch_properties['logsearch.solr.zk.acls'] = logsearch_acls
  logsearch_properties['logsearch.solr.audit.logs.zk.acls'] = logsearch_acls
  if not('logsearch.config.zk_acls' in logsearch_properties):
    logsearch_properties['logsearch.config.zk_acls'] = logsearch_acls
  if not('logsearch.config.zk_acls' in logfeeder_properties):
    logfeeder_properties['logsearch.config.zk_acls'] = logsearch_acls

#####################################
# Smoke command
#####################################

logsearch_server_hosts = default('/clusterHostInfo/logsearch_server_hosts', None)
logsearch_server_host = ""
logsearch_ui_port =  logsearch_https_port if logsearch_protocol == 'https' else logsearch_http_port
if logsearch_server_hosts is not None and len(logsearch_server_hosts) > 0:
  logsearch_server_host = logsearch_server_hosts[0]
smoke_logsearch_cmd = format('curl -k -s -o /dev/null -w "%{{http_code}}" {logsearch_protocol}://{logsearch_server_host}:{logsearch_ui_port}/ | grep 200')
