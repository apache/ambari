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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions import stack_select
from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.get_stack_version import get_stack_version

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
stack_root = Script.get_stack_root()
stack_name = default("/hostLevelParams/stack_name", None)
retryAble = default("/commandParams/command_retry_enabled", False)
version = default("/commandParams/version", None)

upgrade_direction = default("/commandParams/upgrade_direction", None)
stack_version = default("/commandParams/version", None)
sudo = AMBARI_SUDO_BINARY
security_enabled = status_params.security_enabled
fs_root = config['configurations']['core-site']['fs.defaultFS']
solr_conf = "/etc/solr/conf"

solr_port = status_params.solr_port
solr_piddir = status_params.solr_piddir
solr_pidfile = status_params.solr_pidfile

user_group = config['configurations']['cluster-env']['user_group']
fetch_nonlocal_groups = config['configurations']['cluster-env']["fetch_nonlocal_groups"]

# shared configs
java64_home = config['hostLevelParams']['java_home']
zookeeper_hosts_list = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts_list.sort()
# get comma separated list of zookeeper hosts from clusterHostInfo
zookeeper_hosts = ",".join(zookeeper_hosts_list)

#####################################
# Solr configs
#####################################

# Only supporting SolrCloud mode - so hardcode those options
solr_cloudmode = 'true'
solr_dir = '/usr/iop/current/solr-server'
solr_client_dir = '/usr/iop/current/solr-client'
solr_bindir = solr_dir + '/bin'
cloud_scripts = solr_dir + '/server/scripts/cloud-scripts'

if "solr-env" in config['configurations']:
  solr_hosts = config['clusterHostInfo']['solr_hosts']
  solr_znode = default('/configurations/solr-env/solr_znode', '/solr')
  solr_min_mem = default('/configurations/solr-env/solr_minmem', 1024)
  solr_max_mem = default('/configurations/solr-env/solr_maxmem', 2048)
  solr_instance_count = len(config['clusterHostInfo']['solr_hosts'])
  solr_datadir = default('/configurations/solr-env/solr_datadir', '/opt/solr/data')
  solr_data_resources_dir = os.path.join(solr_datadir, 'resources')
  solr_jmx_port = default('/configurations/solr-env/solr_jmx_port', 18983)
  solr_ssl_enabled = default('configurations/solr-env/solr_ssl_enabled', False)
  solr_keystore_location = config['configurations']['solr-env']['solr_keystore_location']
  solr_keystore_password = config['configurations']['solr-env']['solr_keystore_password']
  solr_keystore_type = config['configurations']['solr-env']['solr_keystore_type']
  solr_truststore_location = config['configurations']['solr-env']['solr_truststore_location']
  solr_truststore_password = config['configurations']['solr-env']['solr_truststore_password']
  solr_truststore_type = config['configurations']['solr-env']['solr_truststore_type']
  solr_user = config['configurations']['solr-env']['solr_user']
  solr_log_dir = config['configurations']['solr-env']['solr_log_dir']
  solr_log = format("{solr_log_dir}/solr-install.log")
  solr_env_content = config['configurations']['solr-env']['content']
  solr_hdfs_home_dir = config['configurations']['solr-env']['solr_hdfs_home_dir']

if upgrade_direction is not None and upgrade_direction == Direction.UPGRADE:
  old_lib_dir=default("/configurations/solr-env/solr_lib_dir", None)

zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)
# get comma separated list of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in config['clusterHostInfo']['zookeeper_hosts']:
  zookeeper_quorum += host + ":" + str(zookeeper_port)
  index += 1
  if index < len(config['clusterHostInfo']['zookeeper_hosts']):
    zookeeper_quorum += ","

solr_jaas_file = None

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  solr_jaas_file = solr_conf + '/solr_jaas.conf'
  solr_kerberos_keytab = default('/configurations/solr-env/solr_kerberos_keytab', None)
  if not solr_kerberos_keytab: #Maybe against older configurations during a downgrade operation. Look for the old property
    solr_keytab=config['configurations']['solr-site']['solr.hdfs.security.kerberos.keytabfile']
    solr_kerberos_keytab = solr_keytab

  solr_kerberos_principal = default('/configurations/solr-env/solr_kerberos_principal', None)
  if solr_kerberos_principal:
    solr_kerberos_principal = solr_kerberos_principal.replace('_HOST',_hostname_lowercase)
  else: #Maybe against older configurations during a downgrade operation. Look for the old property
    solr_site = dict(config['configurations']['solr-site'])
    solr_principal = solr_site['solr.hdfs.security.kerberos.principal']
    solr_principal = solr_principal.replace('_HOST', _hostname_lowercase)
    solr_site['solr.hdfs.security.kerberos.principal']=solr_principal
    solr_kerberos_principal = solr_principal

  solr_web_kerberos_keytab = config['configurations']['solr-env']['solr_web_kerberos_keytab']
  solr_web_kerberos_principal = default('/configurations/solr-env/solr_web_kerberos_principal', None)
  if solr_web_kerberos_principal:
    solr_web_kerberos_principal = solr_web_kerberos_principal.replace('_HOST',_hostname_lowercase)
  solr_kerberos_name_rules = config['configurations']['solr-env']['solr_kerberos_name_rules']

solr_xml_content = default('configurations/solr-xml/content', None)
solr_log4j_content = default('configurations/solr-log4j/content', None)

solr_client_custom_log4j = "solr-client-log4j" in config['configurations']

restart_during_downgrade = False
upgrade_direction = default("/commandParams/upgrade_direction", None)
restart_during_downgrade = (upgrade_direction == Direction.DOWNGRADE)

# ***********************  RANGER PLUGIN CHANGES ***********************
# ranger host
# **********************************************************************

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

ranger_admin_log_dir = default("/configurations/ranger-env/ranger_admin_log_dir","/var/log/ranger/admin")

#need to set the defaut to false to satisfy downgrade from 4.2,5 to 4.2 or 4.1
is_supported_solr_ranger = default('/configurations/solr-env/is_supported_solr_ranger', False)

#ranger solr properties
if has_ranger_admin and is_supported_solr_ranger:

  enable_ranger_solr = config['configurations']['ranger-solr-plugin-properties']['ranger-solr-plugin-enabled']
  enable_ranger_solr = not is_empty(enable_ranger_solr) and enable_ranger_solr.lower() == 'yes'
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  if 'admin-properties' in config['configurations'] and 'policymgr_external_url' in config['configurations']['admin-properties'] and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')
  xa_audit_db_flavor = config['configurations']['admin-properties']['DB_FLAVOR']
  xa_audit_db_flavor = xa_audit_db_flavor.lower() if xa_audit_db_flavor else None
  xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audits')
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')
  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
    xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  xa_db_host = config['configurations']['admin-properties']['db_host']
  repo_name = str(config['clusterName']) + '_solr'

  ranger_env = config['configurations']['ranger-env']
  ranger_plugin_properties = config['configurations']['ranger-solr-plugin-properties']

  ranger_solr_audit = config['configurations']['ranger-solr-audit']
  ranger_solr_audit_attrs = config['configuration_attributes']['ranger-solr-audit']
  ranger_solr_security = config['configurations']['ranger-solr-security']
  ranger_solr_security_attrs = config['configuration_attributes']['ranger-solr-security']
  ranger_solr_policymgr_ssl = config['configurations']['ranger-solr-policymgr-ssl']
  ranger_solr_policymgr_ssl_attrs = config['configuration_attributes']['ranger-solr-policymgr-ssl']

  policy_user = config['configurations']['ranger-solr-plugin-properties']['policy_user']

  ranger_plugin_config = {
    'username' : config['configurations']['ranger-solr-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
    'password' : unicode(config['configurations']['ranger-solr-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
    'solr.url' : config['configurations']['ranger-solr-plugin-properties']['solr.url'],
    'commonNameForCertificate' : config['configurations']['ranger-solr-plugin-properties']['common.name.for.certificate']
  }

  solr_ranger_plugin_repo = {
    'isEnabled': 'true',
    'configs': ranger_plugin_config,
    'description': 'solr repo',
    'name': repo_name,
    'repositoryType': 'solr',
    'type': 'solr',
    'assetType': '1'
  }

  if stack_supports_ranger_kerberos and security_enabled:
    ranger_plugin_config['policy.download.auth.users'] = solr_user
    ranger_plugin_config['tag.download.auth.users'] = solr_user
    ranger_plugin_config['ambari.service.check.user'] = policy_user

  #For curl command in ranger plugin to get db connector
  jdk_location = config['hostLevelParams']['jdk_location']
  java_share_dir = '/usr/share/java'
  previous_jdbc_jar_name = None

  if stack_supports_ranger_audit_db:
    if xa_audit_db_flavor and xa_audit_db_flavor == 'mysql':
      jdbc_jar_name = default("/hostLevelParams/custom_mysql_jdbc_name", None)
      previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "com.mysql.jdbc.Driver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'oracle':
      jdbc_jar_name = default("/hostLevelParams/custom_oracle_jdbc_name", None)
      previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
      colon_count = xa_db_host.count(':')
      if colon_count == 2 or colon_count == 0:
        audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
      else:
        audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
      jdbc_driver = "oracle.jdbc.OracleDriver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'postgres':
      jdbc_jar_name = default("/hostLevelParams/custom_postgres_jdbc_name", None)
      previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
      audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
      jdbc_driver = "org.postgresql.Driver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'mssql':
      jdbc_jar_name = default("/hostLevelParams/custom_mssql_jdbc_name", None)
      previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
      jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    elif xa_audit_db_flavor and xa_audit_db_flavor == 'sqla':
      jdbc_jar_name = default("/hostLevelParams/custom_sqlanywhere_jdbc_name", None)
      previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
      audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
      jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  driver_curl_source = format("{jdk_location}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  driver_curl_target = format("{solr_home}/libs/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  previous_jdbc_jar = format("{solr_home}/libs/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None

  xa_audit_db_is_enabled = False
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-solr-audit']['xasecure.audit.destination.db']
  xa_audit_hdfs_is_enabled = default('/configurations/ranger-solr-audit/xasecure.audit.destination.hdfs', False)
  ssl_keystore_password = unicode(config['configurations']['ranger-solr-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-solr-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  stack_version = get_stack_version('solr-server')
  setup_ranger_env_sh_source = format('{stack_root}/{stack_version}/ranger-solr-plugin/install/conf.templates/enable/solr-ranger-env.sh')
  setup_ranger_env_sh_target = format("{solr_conf}/solr-ranger-env.sh")

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

  namenode_hosts = default("/clusterHostInfo/namenode_host", [])
  has_namenode = not len(namenode_hosts) == 0


# *********************** end RANGER PLUGIN CHANGES ****************
smokeuser = config['configurations']['cluster-env']['smokeuser']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = status_params.kinit_path_local

if 'ranger-env' in config['configurations']:
  stack_root = Script.get_stack_root()
  ranger_home = format('{stack_root}/current/ranger-admin')
  audit_solr_enabled = default('/configurations/ranger-env/xasecure.audit.destination.solr', False)
  ranger_solr_config_set = config['configurations']['ranger-env']['ranger_solr_config_set']
  ranger_solr_collection_name = config['configurations']['ranger-env']['ranger_solr_collection_name']
  ranger_solr_shards = config['configurations']['ranger-env']['ranger_solr_shards']
  replication_factor = config['configurations']['ranger-env']['ranger_solr_replication_factor']
  ranger_solr_conf = format('{solr_dir}/server/solr/configsets/ranger_audit_configs/conf')
  is_solrCloud_enabled = default('/configurations/ranger-env/is_solrCloud_enabled', False)
  is_external_solrCloud_enabled = default('/configurations/ranger-env/is_external_solrCloud_enabled', False)
  stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)


import functools
#create partial functions with common arguments for every HdfsDirectory call
#to create hdfs directory we need to call params.HdfsDirectory in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs
)
