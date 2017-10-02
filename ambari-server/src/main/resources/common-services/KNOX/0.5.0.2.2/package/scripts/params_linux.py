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

Ambari Agent

"""
import status_params

from resource_management.core.logger import Logger

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from status_params import *
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions.setup_ranger_plugin_xml import get_audit_configs, generate_ranger_service_config

# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()

tmp_dir = Script.get_tmp_dir()
stack_name = status_params.stack_name
upgrade_direction = default("/commandParams/upgrade_direction", None)
version = default("/commandParams/version", None)
# E.g., 2.3.2.0
version_formatted = format_stack_version(version)

# E.g., 2.3
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)
stack_supports_core_site_for_ranger_plugin = check_stack_feature(StackFeature.CORE_SITE_FOR_RANGER_PLUGINS_SUPPORT, version_for_stack_feature_checks)

# This is the version whose state is CURRENT. During an RU, this is the source version.
# DO NOT format it since we need the build number too.
upgrade_from_version = upgrade_summary.get_source_version()

# server configurations
# Default value used in HDP 2.3.0.0 and earlier.
knox_data_dir = '/var/lib/knox/data'

# Important, it has to be strictly greater than 2.3.0.0!!!
Logger.info(format("Stack version to use is {version_formatted}"))
if version_formatted and check_stack_feature(StackFeature.KNOX_VERSIONED_DATA_DIR, version_formatted):
  # This is the current version. In the case of a Rolling Upgrade, it will be the newer version.
  # In the case of a Downgrade, it will be the version downgrading to.
  # This is always going to be a symlink to /var/lib/knox/data_${version}
  knox_data_dir = format('{stack_root}/{version}/knox/data')
  Logger.info(format("Detected stack with version {version}, will use knox_data_dir = {knox_data_dir}"))


knox_master_secret_path = format('{knox_data_dir}/security/master')
knox_cert_store_path = format('{knox_data_dir}/security/keystores/gateway.jks')
knox_user = default("/configurations/knox-env/knox_user", "knox")

# server configurations
knox_data_dir = '/var/lib/knox/data'
knox_logs_dir = '/var/log/knox'

# default parameters
knox_bin = '/usr/bin/gateway'
knox_conf_dir = '/etc/knox/conf'
ldap_bin = '/usr/lib/knox/bin/ldap.sh'
knox_client_bin = '/usr/lib/knox/bin/knoxcli.sh'

# HDP 2.2+ parameters
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  knox_bin = format('{stack_root}/current/knox-server/bin/gateway.sh')
  knox_conf_dir = format('{stack_root}/current/knox-server/conf')
  ldap_bin = format('{stack_root}/current/knox-server/bin/ldap.sh')
  knox_client_bin = format('{stack_root}/current/knox-server/bin/knoxcli.sh')
  knox_master_secret_path = format('{stack_root}/current/knox-server/data/security/master')
  knox_cert_store_path = format('{stack_root}/current/knox-server/data/security/keystores/gateway.jks')
  knox_data_dir = format('{stack_root}/current/knox-server/data/')

knox_group = default("/configurations/knox-env/knox_group", "knox")
mode = 0644

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

dfs_ha_enabled = False
dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.internal.nameservices', None)
if dfs_ha_nameservices is None:
  dfs_ha_nameservices = default('/configurations/hdfs-site/dfs.nameservices', None)
dfs_ha_namenode_ids = default(format("/configurations/hdfs-site/dfs.ha.namenodes.{dfs_ha_nameservices}"), None)

namenode_rpc = None

if dfs_ha_namenode_ids:
  dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
  dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
  if dfs_ha_namenode_ids_array_len > 1:
    dfs_ha_enabled = True
if dfs_ha_enabled:
  for nn_id in dfs_ha_namemodes_ids_list:
    nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}')]
    if hostname.lower() in nn_host.lower():
      namenode_id = nn_id
      namenode_rpc = nn_host
    # With HA enabled namenode_address is recomputed
  namenode_address = format('hdfs://{dfs_ha_nameservices}')

namenode_port_map = {}
if dfs_ha_enabled:
    for nn_id in dfs_ha_namemodes_ids_list:
        nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.http-address.{dfs_ha_nameservices}.{nn_id}')]
        nn_host_parts = nn_host.split(':')
        namenode_port_map[nn_host_parts[0]] = nn_host_parts[1]


namenode_hosts = default("/clusterHostInfo/namenode_host", None)
if type(namenode_hosts) is list:
  namenode_host = namenode_hosts[0]
else:
  namenode_host = namenode_hosts

has_namenode = not namenode_host == None
namenode_http_port = "50070"
namenode_https_port = "50470"
namenode_rpc_port = "8020"

if has_namenode:
  if 'dfs.namenode.http-address' in config['configurations']['hdfs-site']:
    namenode_http_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])
  if 'dfs.namenode.https-address' in config['configurations']['hdfs-site']:
    namenode_https_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.https-address'])
  if dfs_ha_enabled and namenode_rpc:
    namenode_rpc_port = get_port_from_url(namenode_rpc)
  else:
    if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
      namenode_rpc_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.rpc-address'])

dfs_http_policy = default('/configurations/hdfs-site/dfs.http.policy', None)

hdfs_https_on = False
hdfs_scheme = 'http'
if dfs_http_policy  !=  None :
   hdfs_https_on = (dfs_http_policy.upper() == 'HTTPS_ONLY')
   hdfs_scheme = 'http' if not hdfs_https_on else 'https'
   hdfs_port = str(namenode_http_port)  if not hdfs_https_on else str(namenode_https_port)
   namenode_http_port = hdfs_port

webhdfs_service_urls = ""

def buildUrlElement(protocol, hdfs_host, port, servicePath) :
  openTag = "<url>"
  closeTag = "</url>"
  proto = protocol + "://"
  newLine = "\n"
  if hdfs_host is None or port is None:
      return ""
  else:
    return openTag + proto + hdfs_host + ":" + port + servicePath + closeTag + newLine

namenode_host_keys = namenode_port_map.keys();
if len(namenode_host_keys) > 0:
    for host in namenode_host_keys:
      webhdfs_service_urls += buildUrlElement("http", host, namenode_port_map[host], "/webhdfs")
else:
  webhdfs_service_urls = buildUrlElement("http", namenode_host, namenode_http_port, "/webhdfs")


yarn_http_policy = default('/configurations/yarn-site/yarn.http.policy', None )
yarn_https_on = False
yarn_scheme = 'http'
if yarn_http_policy !=  None :
   yarn_https_on = ( yarn_http_policy.upper() == 'HTTPS_ONLY')
   yarn_scheme = 'http' if not yarn_https_on else 'https'
   
rm_hosts = default("/clusterHostInfo/rm_host", None)
if type(rm_hosts) is list:
  rm_host = rm_hosts[0]
else:
  rm_host = rm_hosts
has_rm = not rm_host == None

jt_rpc_port = "8050"
rm_port = "8080"

if has_rm:
  if 'yarn.resourcemanager.address' in config['configurations']['yarn-site']:
    jt_rpc_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.address'])

  if 'yarn.resourcemanager.webapp.address' in config['configurations']['yarn-site']:
    rm_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'])

hive_http_port = default('/configurations/hive-site/hive.server2.thrift.http.port', "10001")
hive_http_path = default('/configurations/hive-site/hive.server2.thrift.http.path', "cliservice")
hive_server_hosts = default("/clusterHostInfo/hive_server_host", None)
if type(hive_server_hosts) is list:
  hive_server_host = hive_server_hosts[0]
else:
  hive_server_host = hive_server_hosts

templeton_port = default('/configurations/webhcat-site/templeton.port', "50111")
webhcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", None)
if type(webhcat_server_hosts) is list:
  webhcat_server_host = webhcat_server_hosts[0]
else:
  webhcat_server_host = webhcat_server_hosts

hive_scheme = 'http'
webhcat_scheme = 'http'

hbase_master_scheme = 'http'
hbase_master_ui_port = default('/configurations/hbase-site/hbase.master.info.port', "16010");

hbase_master_port = default('/configurations/hbase-site/hbase.rest.port', "8080")
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", None)
if type(hbase_master_hosts) is list:
  hbase_master_host = hbase_master_hosts[0]
else:
  hbase_master_host = hbase_master_hosts

#
# Oozie
#
oozie_https_port = None
oozie_scheme = 'http'
oozie_server_port = "11000"
oozie_server_hosts = default("/clusterHostInfo/oozie_server", None)

if type(oozie_server_hosts) is list:
  oozie_server_host = oozie_server_hosts[0]
else:
  oozie_server_host = oozie_server_hosts

has_oozie = not oozie_server_host == None

if has_oozie:
  oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
  oozie_https_port = default("/configurations/oozie-site/oozie.https.port", None)

if oozie_https_port is not None:
  oozie_scheme = 'https'
  oozie_server_port = oozie_https_port

#
# Falcon
#
falcon_server_hosts = default("/clusterHostInfo/falcon_server_hosts", None)
if type(falcon_server_hosts) is list:
  falcon_server_host = falcon_server_hosts[0]
else:
  falcon_server_host = falcon_server_hosts

falcon_scheme = 'http'
has_falcon = not falcon_server_host == None
falcon_server_port = "15000"

if has_falcon:
  falcon_server_port = config['configurations']['falcon-env']['falcon_port']

#
# Solr
#
solr_scheme='http'
solr_server_hosts  = default("/clusterHostInfo/solr_hosts", None)
if type(solr_server_hosts ) is list:
  solr_host = solr_server_hosts[0]
else:
  solr_host = solr_server_hosts
solr_port=default("/configuration/solr/solr-env/solr_port","8983")

#
# Spark
# 
spark_scheme = 'http'
spark_historyserver_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", None)
if type(spark_historyserver_hosts) is list:
  spark_historyserver_host = spark_historyserver_hosts[0]
else: 
  spark_historyserver_host = spark_historyserver_hosts
spark_historyserver_ui_port = default("/configurations/spark-defaults/spark.history.ui.port", "18080")


#
# JobHistory mapreduce
#
mr_scheme='http'
mr_historyserver_address = default("/configurations/mapred-site/mapreduce.jobhistory.webapp.address", None) 

#
# Yarn nodemanager
#
nodeui_scheme= 'http'
nodeui_port = "8042"
nm_hosts = default("/clusterHostInfo/nm_hosts", None)
if type(nm_hosts) is list:
  nm_host = nm_hosts[0]
else:
  nm_host = nm_hosts

has_yarn = default("/configurations/yarn-site", None )
if has_yarn and 'yarn.nodemanager.webapp.address' in config['configurations']['yarn-site']:
  nodeui_port = get_port_from_url(config['configurations']['yarn-site']['yarn.nodemanager.webapp.address'])


#
# Spark Thrift UI
#
spark_thriftserver_scheme = 'http'
spark_thriftserver_ui_port = 4039
spark_thriftserver_hosts = default("/clusterHostInfo/spark_thriftserver_hosts", None)
if type(spark_thriftserver_hosts) is list:
  spark_thriftserver_host = spark_thriftserver_hosts[0]
else:
  spark_thriftserver_host = spark_thriftserver_hosts

# Knox managed properties
knox_managed_pid_symlink= format('{stack_root}/current/knox-server/pids')

#knox log4j
knox_gateway_log_maxfilesize = default('/configurations/gateway-log4j/knox_gateway_log_maxfilesize',256)
knox_gateway_log_maxbackupindex = default('/configurations/gateway-log4j/knox_gateway_log_maxbackupindex',20)
knox_ldap_log_maxfilesize = default('/configurations/ldap-log4j/knox_ldap_log_maxfilesize',256)
knox_ldap_log_maxbackupindex = default('/configurations/ldap-log4j/knox_ldap_log_maxbackupindex',20)

# server configurations
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
knox_host_name = config['clusterHostInfo']['knox_gateway_hosts'][0]
knox_host_name_in_cluster = config['hostname']
knox_host_port = config['configurations']['gateway-site']['gateway.port']
topology_template = config['configurations']['topology']['content']
admin_topology_template = default('/configurations/admin-topology/content', None)
knoxsso_topology_template = config['configurations']['knoxsso-topology']['content']
gateway_log4j = config['configurations']['gateway-log4j']['content']
ldap_log4j = config['configurations']['ldap-log4j']['content']
users_ldif = config['configurations']['users-ldif']['content']
java_home = config['hostLevelParams']['java_home']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
if security_enabled:
  knox_keytab_path = config['configurations']['knox-env']['knox_keytab_path']
  _hostname_lowercase = config['hostname'].lower()
  knox_principal_name = config['configurations']['knox-env']['knox_principal_name'].replace('_HOST',_hostname_lowercase)

# for curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']

# ranger knox plugin start section

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks)

# ambari-server hostname
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger knox plugin enabled property
enable_ranger_knox = default("/configurations/ranger-knox-plugin-properties/ranger-knox-plugin-enabled", "No")
enable_ranger_knox = True if enable_ranger_knox.lower() == 'yes' else False

# get ranger knox properties if enable_ranger_knox is True
if enable_ranger_knox:
  # get ranger policy url
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  if xml_configurations_supported:
    policymgr_mgr_url = config['configurations']['ranger-knox-security']['ranger.plugin.knox.policy.rest.url']

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  # ranger audit db user
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')

  # ranger knox service/repositry name
  repo_name = str(config['clusterName']) + '_knox'
  repo_name_value = config['configurations']['ranger-knox-security']['ranger.plugin.knox.service.name']
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  knox_home = config['configurations']['ranger-knox-plugin-properties']['KNOX_HOME']
  common_name_for_certificate = config['configurations']['ranger-knox-plugin-properties']['common.name.for.certificate']
  repo_config_username = config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

  # ranger-env config
  ranger_env = config['configurations']['ranger-env']

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_knox:
    external_admin_username = default('/configurations/ranger-knox-plugin-properties/external_admin_username', 'admin')
    external_admin_password = default('/configurations/ranger-knox-plugin-properties/external_admin_password', 'admin')
    external_ranger_admin_username = default('/configurations/ranger-knox-plugin-properties/external_ranger_admin_username', 'amb_ranger_admin')
    external_ranger_admin_password = default('/configurations/ranger-knox-plugin-properties/external_ranger_admin_password', 'amb_ranger_admin')
    ranger_env = {}
    ranger_env['admin_username'] = external_admin_username
    ranger_env['admin_password'] = external_admin_password
    ranger_env['ranger_admin_username'] = external_ranger_admin_username
    ranger_env['ranger_admin_password'] = external_ranger_admin_password

  ranger_plugin_properties = config['configurations']['ranger-knox-plugin-properties']
  policy_user = config['configurations']['ranger-knox-plugin-properties']['policy_user']
  repo_config_password = config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db and has_ranger_admin:
    xa_audit_db_password = config['configurations']['admin-properties']['audit_db_password']

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
    driver_curl_target = format("{stack_root}/current/knox-server/ext/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    previous_jdbc_jar = format("{stack_root}/current/knox-server/ext/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    sql_connector_jar = ''

  knox_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'knox.url': format("https://{knox_host_name}:{knox_host_port}/gateway/admin/api/v1/topologies"),
    'commonNameForCertificate': common_name_for_certificate
  }

  knox_ranger_plugin_repo = {
    'isActive': 'true',
    'config': json.dumps(knox_ranger_plugin_config),
    'description': 'knox repo',
    'name': repo_name,
    'repositoryType': 'knox',
    'assetType': '5',
  }

  custom_ranger_service_config = generate_ranger_service_config(ranger_plugin_properties)
  if len(custom_ranger_service_config) > 0:
    knox_ranger_plugin_config.update(custom_ranger_service_config)

  if stack_supports_ranger_kerberos and security_enabled:
    knox_ranger_plugin_config['policy.download.auth.users'] = knox_user
    knox_ranger_plugin_config['tag.download.auth.users'] = knox_user

  if stack_supports_ranger_kerberos:
    knox_ranger_plugin_config['ambari.service.check.user'] = policy_user

    knox_ranger_plugin_repo = {
      'isEnabled': 'true',
      'configs': knox_ranger_plugin_config,
      'description': 'knox repo',
      'name': repo_name,
      'type': 'knox'
    }

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-knox-audit']['xasecure.audit.destination.db']

  xa_audit_hdfs_is_enabled = config['configurations']['ranger-knox-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else False
  ssl_keystore_password = config['configurations']['ranger-knox-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password'] if xml_configurations_supported else None
  ssl_truststore_password = config['configurations']['ranger-knox-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password'] if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

  # for SQLA explicitly disable audit to DB for Ranger
  if has_ranger_admin and stack_supports_ranger_audit_db and xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

# need this to capture cluster name from where ranger knox plugin is enabled
cluster_name = config['clusterName']

# ranger knox plugin end section

hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab'] if has_namenode else None
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
default_fs = config['configurations']['core-site']['fs.defaultFS'] if has_namenode else None
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
hadoop_conf_dir = conf_select.get_hadoop_conf_dir() if has_namenode else None

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

druid_coordinator_urls = ""
if "druid-coordinator" in config['configurations']:
  port = config['configurations']['druid-coordinator']['druid.port']
  for host in config['clusterHostInfo']['druid_coordinator_hosts']:
    druid_coordinator_urls += buildUrlElement("http", host, port, "")

druid_overlord_urls = ""
if "druid-overlord" in config['configurations']:
  port = config['configurations']['druid-overlord']['druid.port']
  for host in config['clusterHostInfo']['druid_overlord_hosts']:
    druid_overlord_urls += buildUrlElement("http", host, port, "")

druid_broker_urls = ""
if "druid-broker" in config['configurations']:
  port = config['configurations']['druid-broker']['druid.port']
  for host in config['clusterHostInfo']['druid_broker_hosts']:
    druid_broker_urls += buildUrlElement("http", host, port, "")

druid_router_urls = ""
if "druid-router" in config['configurations']:
  port = config['configurations']['druid-router']['druid.port']
  for host in config['clusterHostInfo']['druid_router_hosts']:
    druid_router_urls += buildUrlElement("http", host, port, "")

zeppelin_ui_urls = ""
zeppelin_ws_urls = ""
websocket_support = "false"
if "zeppelin-config" in config['configurations']:
  port = config['configurations']['zeppelin-config']['zeppelin.server.port']
  protocol = "https" if config['configurations']['zeppelin-config']['zeppelin.ssl'] else "http"
  host = config['clusterHostInfo']['zeppelin_master_hosts'][0]
  zeppelin_ui_urls += buildUrlElement(protocol, host, port, "")
  zeppelin_ws_urls += buildUrlElement("ws", host, port, "/ws")
  websocket_support = "true"
