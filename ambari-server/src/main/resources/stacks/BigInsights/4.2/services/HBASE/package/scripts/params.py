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
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from ambari_commons.constants import AMBARI_SUDO_BINARY
from functions import calc_xmn_from_xms, ensure_unit_for_memory
import status_params
from resource_management.libraries.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries import functions
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select

def treat_value_as_mb(value1):
  value = str(value1)
  try:
    part = int(value.strip()[:-1]) if value.lower().strip()[-1:] == 'm' else int(value)
    return str(part) + 'm'
  except:
    return None

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)
retryAble = default("/commandParams/command_retry_enabled", False)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)
stack_root = status_params.stack_root

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

component_directory = status_params.component_directory

#hadoop params
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
daemon_script = format('/usr/iop/current/{component_directory}/bin/hbase-daemon.sh')
region_mover = format('/usr/iop/current/{component_directory}/bin/region_mover.rb')
region_drainer = format('/usr/iop/current/{component_directory}/bin/draining_servers.rb')
hbase_cmd = format('/usr/iop/current/{component_directory}/bin/hbase')

limits_conf_dir = "/etc/security/limits.d"
hbase_conf_dir = status_params.hbase_conf_dir
hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = default("/commandParams/mark_draining_only",False)
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = config['configurations']['cluster-env']['security_enabled']

hbase_hdfs_user_dir = format("/user/{hbase_user}")
hbase_hdfs_user_mode = 0755

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']

# no symlink for phoenix-server at this point
phx_daemon_script = '/usr/iop/current/phoenix-server/bin/queryserver.py'

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
master_heapsize_cfg = config['configurations']['hbase-env']['hbase_master_heapsize']
master_heapsize = treat_value_as_mb(master_heapsize_cfg)

hbase_javaopts_properties = config['configurations']['hbase-javaopts-properties']['content']

hbase_javaopts_properties = str(hbase_javaopts_properties)	
if hbase_javaopts_properties.find('-Diop.version') == -1:
  iop_full_version = format_stack_version(version)
  hbase_javaopts_properties = hbase_javaopts_properties+ ' -Diop.version=' + str(iop_full_version)

regionserver_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_regionserver_heapsize'])
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = expect("/configurations/hbase-env/hbase_regionserver_xmn_ratio", float) #AMBARI-15614
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)

phoenix_hosts = default('/clusterHostInfo/phoenix_query_server_hosts', [])
phoenix_enabled = default('/configurations/hbase-env/phoenix_sql_enabled', False)
has_phoenix = len(phoenix_hosts) > 0

if not has_phoenix and not phoenix_enabled:
  exclude_packages = ['phoenix*']
else:
  exclude_packages = []


pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
local_dir = config['configurations']['hbase-site']['hbase.local.dir']

# TODO UPGRADE default, update site during upgrade
#_local_dir_conf = default('/configurations/hbase-site/hbase.local.dir', "${hbase.tmp.dir}/local")
#local_dir = substitute_vars(_local_dir_conf, config['configurations']['hbase-site'])

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")
queryserver_jaas_config_file = format("{hbase_conf_dir}/hbase_queryserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
ganglia_server_host = '' if len(ganglia_server_hosts) == 0 else ganglia_server_hosts[0]

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]
  pass

# if hbase is selected the hbase_rs_hosts, should not be empty, but still default just in case
if 'slave_hosts' in config['clusterHostInfo']:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/slave_hosts') #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/all_hosts') 

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  rest_server_jaas_princ = config['configurations']['hbase-site']['hbase.rest.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  rest_server_spnego_jaas_princ = config['configurations']['hbase-site']['hbase.rest.authentication.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  if has_phoenix:  
    queryserver_jaas_princ = config['configurations']['hbase-site']['phoenix.queryserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)


master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
rest_server_keytab_path = config['configurations']['hbase-site']['hbase.rest.keytab.file']
rest_server_spnego_keytab_path = config['configurations']['hbase-site']['hbase.rest.authentication.kerberos.keytab']
queryserver_keytab_path = config['configurations']['hbase-site']['phoenix.queryserver.keytab.file']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = functions.get_kinit_path()
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
  kinit_cmd_decommission = format("{kinit_path_local} -kt {master_keytab_path} {master_jaas_princ};")

else:
  kinit_cmd = ""
  kinit_cmd_decommission = ""

#log4j.properties
if (('hbase-log4j' in config['configurations']) and ('content' in config['configurations']['hbase-log4j'])):
  log4j_props = config['configurations']['hbase-log4j']['content']
else:
  log4j_props = None
  
hbase_env_sh_template = config['configurations']['hbase-env']['content']

hbase_hdfs_root_dir = config['configurations']['hbase-site']['hbase.rootdir']
hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path()
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

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

if stack_version != "" and compare_versions(stack_version, '4.0') >= 0:
  command_role = default("/role", "")
  if command_role == "HBASE_MASTER" or command_role == "HBASE_REGIONSERVER" or command_role == "HBASE_REST_SERVER":
    if command_role == "HBASE_MASTER":
      role_root = "master"
    elif command_role == "HBASE_REGIONSERVER":
      role_root = "regionserver"
    elif command_role == "HBASE_REST_SERVER":
      role_root = "restserver"

    daemon_script=format("/usr/iop/current/hbase-{role_root}/bin/hbase-daemon.sh")
    region_mover = format("/usr/iop/current/hbase-{role_root}/bin/region_mover.rb")
    region_drainer = format("/usr/iop/current/hbase-{role_root}/bin/draining_servers.rb")
    hbase_cmd = format("/usr/iop/current/hbase-{role_root}/bin/hbase")

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger hbase properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audit')
xa_audit_db_user = config['configurations']['admin-properties']['audit_db_user']
xa_db_host = config['configurations']['admin-properties']['db_host']
repo_name = str(config['clusterName']) + '_hbase'

common_name_for_certificate = config['configurations']['ranger-hbase-plugin-properties']['common.name.for.certificate']

zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
hbase_zookeeper_quorum = config['configurations']['hbase-site']['hbase.zookeeper.quorum']
hbase_zookeeper_property_clientPort = config['configurations']['hbase-site']['hbase.zookeeper.property.clientPort']
hbase_security_authentication = config['configurations']['hbase-site']['hbase.security.authentication']
hadoop_security_authentication = config['configurations']['core-site']['hadoop.security.authentication']

repo_config_username = config['configurations']['ranger-hbase-plugin-properties']['REPOSITORY_CONFIG_USERNAME']

ranger_env = config['configurations']['ranger-env']
ranger_plugin_properties = config['configurations']['ranger-hbase-plugin-properties']
policy_user = config['configurations']['ranger-hbase-plugin-properties']['policy_user']

#For curl command in ranger plugin to get db connector
jdk_location = config['hostLevelParams']['jdk_location']
java_share_dir = '/usr/share/java'
enable_ranger_hbase = False
if has_ranger_admin:
  enable_ranger_hbase = (config['configurations']['ranger-hbase-plugin-properties']['ranger-hbase-plugin-enabled'].lower() == 'yes')
  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
    xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-hbase-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
  previous_jdbc_jar_name = None

  if xa_audit_db_flavor == 'mysql':
    jdbc_symlink_name = "mysql-jdbc-driver.jar"
    jdbc_jar_name = "mysql-connector-java.jar"
    previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mysql_jdbc_name", None)
    audit_jdbc_url = format('jdbc:mysql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "com.mysql.jdbc.Driver"
  elif xa_audit_db_flavor == 'oracle':
    jdbc_jar_name = "ojdbc6.jar"
    jdbc_symlink_name = "oracle-jdbc-driver.jar"
    previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_oracle_jdbc_name", None)
    colon_count = xa_db_host.count(':')
    if colon_count == 2 or colon_count == 0:
      audit_jdbc_url = format('jdbc:oracle:thin:@{xa_db_host}')
    else:
      audit_jdbc_url = format('jdbc:oracle:thin:@//{xa_db_host}')
    jdbc_driver = "oracle.jdbc.OracleDriver"
  elif xa_audit_db_flavor == 'postgres':
    jdbc_jar_name = "postgresql.jar"
    jdbc_symlink_name = "postgres-jdbc-driver.jar"
    previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_postgres_jdbc_name", None)
    audit_jdbc_url = format('jdbc:postgresql://{xa_db_host}/{xa_audit_db_name}')
    jdbc_driver = "org.postgresql.Driver"
  elif xa_audit_db_flavor == 'mssql':
    jdbc_jar_name = "sqljdbc4.jar"
    jdbc_symlink_name = "mssql-jdbc-driver.jar"
    previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_mssql_jdbc_name", None)
    audit_jdbc_url = format('jdbc:sqlserver://{xa_db_host};databaseName={xa_audit_db_name}')
    jdbc_driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
  elif xa_audit_db_flavor == 'sqla':
    jdbc_jar_name = "sajdbc4.jar"
    jdbc_symlink_name = "sqlanywhere-jdbc-driver.tar.gz"
    previous_jdbc_jar_name = default("/hostLevelParams/previous_custom_sqlanywhere_jdbc_name", None)
    audit_jdbc_url = format('jdbc:sqlanywhere:database={xa_audit_db_name};host={xa_db_host}')
    jdbc_driver = "sap.jdbc4.sqlanywhere.IDriver"

  downloaded_custom_connector = format("{exec_tmp_dir}/{jdbc_jar_name}")
  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("/usr/iop/current/{component_directory}/lib/{jdbc_jar_name}")
  previous_jdbc_jar = format("{stack_root}/current/{component_directory}/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
  
  hbase_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'hadoop.security.authentication': hadoop_security_authentication,
    'hbase.security.authentication': hbase_security_authentication,
    'hbase.zookeeper.property.clientPort': hbase_zookeeper_property_clientPort,
    'hbase.zookeeper.quorum': hbase_zookeeper_quorum,
    'zookeeper.znode.parent': zookeeper_znode_parent,
    'commonNameForCertificate': common_name_for_certificate,
    'hbase.master.kerberos.principal': master_jaas_princ if security_enabled else ''
  }

  hbase_ranger_plugin_repo = {
    'isActive': 'true',
    'config': json.dumps(hbase_ranger_plugin_config),
    'description': 'hbase repo',
    'name': repo_name,
    'repositoryType': 'hbase',
    'assetType': '2'
  }

  ranger_audit_solr_urls = config['configurations']['ranger-admin-site']['ranger.audit.solr.urls']
  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.db']
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False

# Used to dynamically set the hbase-site props that are referenced during Kerberization
if security_enabled:
  if not enable_ranger_hbase: # Default props, no ranger plugin
    hbase_coprocessor_master_classes = "org.apache.hadoop.hbase.security.access.AccessController"
    hbase_coprocessor_regionserver_classes = "org.apache.hadoop.hbase.security.access.AccessController"
    hbase_coprocessor_region_classes = "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.hadoop.hbase.security.access.AccessController"
  elif xml_configurations_supported: # HDP stack 2.3+ ranger plugin enabled
    hbase_coprocessor_master_classes = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor "
    hbase_coprocessor_regionserver_classes = "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
    hbase_coprocessor_region_classes = "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor"
  else: # HDP Stack 2.2 and less / ranger plugin enabled
    hbase_coprocessor_master_classes = "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"
    hbase_coprocessor_regionserver_classes = "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"
    hbase_coprocessor_region_classes = "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"

