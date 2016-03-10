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
from resource_management.core.logger import Logger

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from status_params import *
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources

# server configurations
config = Script.get_config()

tmp_dir = Script.get_tmp_dir()
stack_name = default("/hostLevelParams/stack_name", None)
upgrade_direction = default("/commandParams/upgrade_direction", None)
version = default("/commandParams/version", None)
if version is None:
  version = get_hdp_version('knox-server')
# E.g., 2.3.2.0
version_formatted = format_hdp_stack_version(version)

# E.g., 2.3
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

# This is the version whose state is CURRENT. During an RU, this is the source version.
# DO NOT format it since we need the build number too.
upgrade_from_version = default("/hostLevelParams/current_version", None)

# server configurations
# Default value used in HDP 2.3.0.0 and earlier.

knox_data_dir = '/var/lib/knox/data'

# Important, it has to be strictly greater than 2.3.0.0!!!
if stack_name and stack_name.upper() == "HDP":
  Logger.info(format("HDP version to use is {version_formatted}"))
  if Script.is_hdp_stack_greater(version_formatted, "2.3.0.0"):
    # This is the current version. In the case of a Rolling Upgrade, it will be the newer version.
    # In the case of a Downgrade, it will be the version downgrading to.
    # This is always going to be a symlink to /var/lib/knox/data_${version}
    knox_data_dir = format('/usr/hdp/{version}/knox/data')
    Logger.info(format("Detected HDP with stack version {version}, will use knox_data_dir = {knox_data_dir}"))


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
if Script.is_hdp_stack_greater_or_equal("2.2"):
  knox_bin = '/usr/hdp/current/knox-server/bin/gateway.sh'
  knox_conf_dir = '/usr/hdp/current/knox-server/conf'
  ldap_bin = '/usr/hdp/current/knox-server/bin/ldap.sh'
  knox_client_bin = '/usr/hdp/current/knox-server/bin/knoxcli.sh'

  knox_master_secret_path = '/usr/hdp/current/knox-server/data/security/master'
  knox_cert_store_path = '/usr/hdp/current/knox-server/data/security/keystores/gateway.jks'
  knox_data_dir = '/usr/hdp/current/knox-server/data/'

knox_group = default("/configurations/knox-env/knox_group", "knox")
mode = 0644

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

dfs_ha_enabled = False
dfs_ha_nameservices = default("/configurations/hdfs-site/dfs.nameservices", None)
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
    if hostname in nn_host:
      namenode_id = nn_id
      namenode_rpc = nn_host
    # With HA enabled namenode_address is recomputed
  namenode_address = format('hdfs://{dfs_ha_nameservices}')


namenode_hosts = default("/clusterHostInfo/namenode_host", None)
if type(namenode_hosts) is list:
  namenode_host = namenode_hosts[0]
else:
  namenode_host = namenode_hosts

has_namenode = not namenode_host == None
namenode_http_port = "50070"
namenode_rpc_port = "8020"

if has_namenode:
  if 'dfs.namenode.http-address' in config['configurations']['hdfs-site']:
    namenode_http_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])
  if dfs_ha_enabled and namenode_rpc:
    namenode_rpc_port = get_port_from_url(namenode_rpc)
  else:
    if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
      namenode_rpc_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.rpc-address'])

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

hbase_master_port = default('/configurations/hbase-site/hbase.rest.port', "8080")
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", None)
if type(hbase_master_hosts) is list:
  hbase_master_host = hbase_master_hosts[0]
else:
  hbase_master_host = hbase_master_hosts

oozie_server_hosts = default("/clusterHostInfo/oozie_server", None)
if type(oozie_server_hosts) is list:
  oozie_server_host = oozie_server_hosts[0]
else:
  oozie_server_host = oozie_server_hosts

has_oozie = not oozie_server_host == None
oozie_server_port = "11000"

if has_oozie:
  oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])

# Knox managed properties
knox_managed_pid_symlink= "/usr/hdp/current/knox-server/pids"

# server configurations
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
knox_host_name = config['clusterHostInfo']['knox_gateway_hosts'][0]
knox_host_name_in_cluster = config['hostname']
knox_host_port = config['configurations']['gateway-site']['gateway.port']
topology_template = config['configurations']['topology']['content']
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

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']

ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger knox properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_knox'

knox_home = config['configurations']['ranger-knox-plugin-properties']['KNOX_HOME']
common_name_for_certificate = config['configurations']['ranger-knox-plugin-properties']['common.name.for.certificate']

repo_config_username = config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-knox-plugin-properties']
policy_user = config['configurations']['ranger-knox-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
if has_ranger_admin:
  enable_ranger_knox = (config['configurations']['ranger-knox-plugin-properties']['ranger-knox-plugin-enabled'].lower() == 'yes')
  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
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
  driver_curl_target = format("/usr/hdp/current/knox-server/ext/{jdbc_jar_name}")

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
  
  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = config['configurations']['ranger-knox-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-knox-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-knox-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-knox-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab'] if has_namenode else None
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
default_fs = config['configurations']['core-site']['fs.defaultFS'] if has_namenode else None
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin") if has_namenode else None
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