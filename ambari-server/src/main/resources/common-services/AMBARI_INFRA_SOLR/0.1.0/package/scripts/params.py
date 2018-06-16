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

stack_version = default("/commandParams/version", None)
sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled

hostname = config['agentLevelParams']['hostname'].lower()

infra_solr_conf = "/etc/ambari-infra-solr/conf"

infra_solr_port = status_params.infra_solr_port
infra_solr_piddir = status_params.infra_solr_piddir
infra_solr_pidfile = status_params.infra_solr_pidfile
prev_infra_solr_pidfile = status_params.prev_infra_solr_pidfile

user_group = config['configurations']['cluster-env']['user_group']
fetch_nonlocal_groups = config['configurations']['cluster-env']["fetch_nonlocal_groups"]

limits_conf_dir = "/etc/security/limits.d"
infra_solr_user_nofile_limit = default("/configurations/infra-solr-env/infra_solr_user_nofile_limit", "128000")
infra_solr_user_nproc_limit = default("/configurations/infra-solr-env/infra_solr_user_nproc_limit", "65536")

# shared configs
java_home = config['ambariLevelParams']['java_home']
ambari_java_home = default("/ambariLevelParams/ambari_java_home", None)
java64_home = ambari_java_home if ambari_java_home is not None else java_home
java_exec = format("{java64_home}/bin/java")
zookeeper_hosts_list = config['clusterHostInfo']['zookeeper_server_hosts']
zookeeper_hosts_list.sort()
# get comma separated list of zookeeper hosts from clusterHostInfo
zookeeper_hosts = ",".join(zookeeper_hosts_list)

#####################################
# Solr configs
#####################################

# Only supporting SolrCloud mode - so hardcode those options
solr_cloudmode = 'true'
solr_dir = '/usr/lib/ambari-infra-solr'
solr_client_dir = '/usr/lib/ambari-infra-solr-client'
solr_bindir = solr_dir + '/bin'
cloud_scripts = solr_dir + '/server/scripts/cloud-scripts'

logsearch_hosts = default("/clusterHostInfo/logsearch_server_hosts", [])
has_logsearch = len(logsearch_hosts) > 0

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
# get comma separated list of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_server_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_server_hosts']):
    zookeeper_quorum += ","

if "infra-solr-env" in config['configurations']:
  infra_solr_hosts = config['clusterHostInfo']['infra_solr_hosts']
  infra_solr_znode = config['configurations']['infra-solr-env']['infra_solr_znode']
  infra_solr_min_mem = format(config['configurations']['infra-solr-env']['infra_solr_minmem'])
  infra_solr_max_mem = format(config['configurations']['infra-solr-env']['infra_solr_maxmem'])
  infra_solr_java_stack_size = format(config['configurations']['infra-solr-env']['infra_solr_java_stack_size'])
  infra_solr_instance_count = len(config['clusterHostInfo']['infra_solr_hosts'])
  infra_solr_datadir = format(config['configurations']['infra-solr-env']['infra_solr_datadir'])
  infra_solr_data_resources_dir = os.path.join(infra_solr_datadir, 'resources')
  infra_solr_jmx_port = config['configurations']['infra-solr-env']['infra_solr_jmx_port']
  infra_solr_ssl_enabled = default('configurations/infra-solr-env/infra_solr_ssl_enabled', False)
  infra_solr_keystore_location = config['configurations']['infra-solr-env']['infra_solr_keystore_location']
  infra_solr_keystore_password = config['configurations']['infra-solr-env']['infra_solr_keystore_password']
  infra_solr_keystore_type = config['configurations']['infra-solr-env']['infra_solr_keystore_type']
  infra_solr_truststore_location = config['configurations']['infra-solr-env']['infra_solr_truststore_location']
  infra_solr_truststore_password = config['configurations']['infra-solr-env']['infra_solr_truststore_password']
  infra_solr_truststore_type = config['configurations']['infra-solr-env']['infra_solr_truststore_type']
  infra_solr_user = config['configurations']['infra-solr-env']['infra_solr_user']
  infra_solr_log_dir = config['configurations']['infra-solr-env']['infra_solr_log_dir']
  infra_solr_log = format("{infra_solr_log_dir}/solr-install.log")
  solr_env_content = config['configurations']['infra-solr-env']['content']
  infra_solr_gc_log_opts = format(config['configurations']['infra-solr-env']['infra_solr_gc_log_opts'])
  infra_solr_gc_tune = format(config['configurations']['infra-solr-env']['infra_solr_gc_tune'])
  infra_solr_extra_java_opts = format(default('configurations/infra-solr-env/infra_solr_extra_java_opts', ""))

  zk_quorum = format(default('configurations/infra-solr-env/infra_solr_zookeeper_quorum', zookeeper_quorum))

if 'infra-solr-security-json' in config['configurations']:
  infra_solr_security_manually_managed = default("/configurations/infra-solr-security-json/infra_solr_security_manually_managed", False)

default_ranger_audit_users = 'nn,hbase,hive,knox,kafka,kms,storm,yarn,nifi'

if security_enabled:
  kinit_path_local = status_params.kinit_path_local
  _hostname_lowercase = config['agentLevelParams']['hostname'].lower()
  infra_solr_jaas_file = infra_solr_conf + '/infra_solr_jaas.conf'
  infra_solr_kerberos_keytab = config['configurations']['infra-solr-env']['infra_solr_kerberos_keytab']
  infra_solr_kerberos_principal = config['configurations']['infra-solr-env']['infra_solr_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  infra_solr_web_kerberos_keytab = config['configurations']['infra-solr-env']['infra_solr_web_kerberos_keytab']
  infra_solr_web_kerberos_principal = config['configurations']['infra-solr-env']['infra_solr_web_kerberos_principal'].replace('_HOST',_hostname_lowercase)
  infra_solr_kerberos_name_rules = config['configurations']['infra-solr-env']['infra_solr_kerberos_name_rules']
  infra_solr_sasl_user = get_name_from_principal(infra_solr_kerberos_principal)
  kerberos_realm = config['configurations']['kerberos-env']['realm']

  zookeeper_principal_name = default("/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper/_HOST@EXAMPLE.COM")
  external_zk_principal_enabled = default("/configurations/infra-solr-env/infra_solr_zookeeper_external_enabled", False)
  external_zk_principal_name = default("/configurations/infra-solr-env/infra_solr_zookeeper_external_principal", "zookeeper/_HOST@EXAMPLE.COM")
  zk_principal_name = external_zk_principal_name if external_zk_principal_enabled else zookeeper_principal_name
  zk_principal_user = zk_principal_name.split('/')[0]
  zk_security_opts = format('-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Dzookeeper.sasl.clientconfig=Client')

  ranger_audit_principal_conf_key = "xasecure.audit.jaas.Client.option.principal"
  ranger_audit_principals = []
  ranger_audit_principals.append(default('configurations/ranger-hdfs-audit/' + ranger_audit_principal_conf_key, 'nn'))
  ranger_audit_principals.append(default('configurations/ranger-hbase-audit/' + ranger_audit_principal_conf_key, 'hbase'))
  ranger_audit_principals.append(default('configurations/ranger-hive-audit/' + ranger_audit_principal_conf_key, 'hive'))
  ranger_audit_principals.append(default('configurations/ranger-knox-audit/' + ranger_audit_principal_conf_key, 'knox'))
  ranger_audit_principals.append(default('configurations/ranger-kafka-audit/' + ranger_audit_principal_conf_key, 'kafka'))
  ranger_audit_principals.append(default('configurations/ranger-kms-audit/' + ranger_audit_principal_conf_key, 'rangerkms'))
  ranger_audit_principals.append(default('configurations/ranger-storm-audit/' + ranger_audit_principal_conf_key, 'storm'))
  ranger_audit_principals.append(default('configurations/ranger-yarn-audit/' + ranger_audit_principal_conf_key, 'yarn'))
  ranger_audit_principals.append(default('configurations/ranger-nifi-audit/' + ranger_audit_principal_conf_key, 'nifi'))
  ranger_audit_names_from_principals = [ get_name_from_principal(x) for x in ranger_audit_principals ]
  default_ranger_audit_users = ','.join(ranger_audit_names_from_principals)

  infra_solr_logsearch_service_users = []
  logsearch_kerberos_service_user = get_name_from_principal(default('/configurations/logsearch-env/logsearch_kerberos_principal', 'logsearch'))
  infra_solr_logsearch_service_users.append(logsearch_kerberos_service_user)
  logsearch_kerberos_service_users_str = str(default('/configurations/logsearch-env/logsearch_kerberos_service_users', ''))
  if logsearch_kerberos_service_users_str and logsearch_kerberos_service_users_str.strip():
    logsearch_kerberos_service_users = logsearch_kerberos_service_users_str.split(',')
    infra_solr_logsearch_service_users.extend(logsearch_kerberos_service_users)

infra_solr_ranger_audit_service_users = format(config['configurations']['infra-solr-security-json']['infra_solr_ranger_audit_service_users']).split(',')
infra_solr_security_json_content = config['configurations']['infra-solr-security-json']['content']

infra_solr_jmx_enabled = str(default('/configurations/infra-solr-env/infra_solr_jmx_enabled', False)).lower()

#Solr log4j
infra_log_maxfilesize = default('configurations/infra-solr-log4j/infra_log_maxfilesize',10)
infra_log_maxbackupindex = default('configurations/infra-solr-log4j/infra_log_maxbackupindex',9)

solr_xml_content = default('configurations/infra-solr-xml/content', None)
solr_log4j_content = default('configurations/infra-solr-log4j/content', None)

smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

ranger_solr_collection_name = default('configurations/ranger-env/ranger_solr_collection_name', 'ranger_audits')
logsearch_service_logs_collection = default('configurations/logsearch-properties/logsearch.solr.collection.service.logs', 'hadoop_logs')
logsearch_audit_logs_collection = default('configurations/logsearch-properties/logsearch.solr.collection.audit.logs', 'audit_logs')

ranger_admin_kerberos_service_user = get_name_from_principal(default('configurations/ranger-admin-site/ranger.admin.kerberos.principal', 'rangeradmin'))
atlas_kerberos_service_user = get_name_from_principal(default('configurations/application-properties/atlas.authentication.principal', 'atlas'))
logfeeder_kerberos_service_user = get_name_from_principal(default('configurations/logfeeder-env/logfeeder_kerberos_principal', 'logfeeder'))
infra_solr_kerberos_service_user = get_name_from_principal(default('configurations/infra-solr-env/infra_solr_kerberos_principal', 'infra-solr'))

infra_solr_role_ranger_admin = default('configurations/infra-solr-security-json/infra_solr_role_ranger_admin', 'ranger_user')
infra_solr_role_ranger_audit = default('configurations/infra-solr-security-json/infra_solr_role_ranger_audit', 'ranger_audit_user')
infra_solr_role_atlas = default('configurations/infra-solr-security-json/infra_solr_role_atlas', 'atlas_user')
infra_solr_role_logsearch = default('configurations/infra-solr-security-json/infra_solr_role_logsearch', 'logsearch_user')
infra_solr_role_logfeeder = default('configurations/infra-solr-security-json/infra_solr_role_logfeeder', 'logfeeder_user')
infra_solr_role_dev = default('configurations/infra-solr-security-json/infra_solr_role_dev', 'dev')

ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
metrics_enabled = ams_collector_hosts != ''
if metrics_enabled:
  metrics_http_policy = config['configurations']['ams-site']['timeline.metrics.service.http.policy']
  ams_collector_protocol = 'http'
  if metrics_http_policy == 'HTTPS_ONLY':
    ams_collector_protocol = 'https'
  ams_collector_port = str(get_port_from_url(config['configurations']['ams-site']['timeline.metrics.service.webapp.address']))
else:
  ams_collector_port = ''
  ams_collector_protocol = ''
