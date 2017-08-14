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
limitations under the License.

Ambari Agent

"""

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.constants import StackFeature
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management import *
import status_params

ibm_distribution_knox_dir = '/usr/iop/current/knox-server'

# server configurations
config = Script.get_config()

tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)
retryAble = default("/commandParams/command_retry_enabled", False)

upgrade_direction = default("/commandParams/upgrade_direction", None)
version = default("/commandParams/version", None)

# This is the version whose state is CURRENT. During an RU, this is the source version.
# DO NOT format it since we need the build number too.
upgrade_from_version = upgrade_summary.get_source_version()

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

knox_bin = ibm_distribution_knox_dir + '/bin/gateway.sh'
ldap_bin = ibm_distribution_knox_dir + '/bin/ldap.sh'
knox_client_bin = ibm_distribution_knox_dir + '/bin/knoxcli.sh'

namenode_hosts = default("/clusterHostInfo/namenode_host", None)
if type(namenode_hosts) is list:
  namenode_host = namenode_hosts[0]
else:
  namenode_host = namenode_hosts

has_namenode = not namenode_host == None
namenode_http_port = "50070"
namenode_https_port = "50470"
namenode_rpc_port = "8020"

hdfs_scheme='http'



if has_namenode:
  if 'dfs.namenode.http-address' in config['configurations']['hdfs-site']:
    namenode_http_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])

  if 'dfs.namenode.https-address' in config['configurations']['hdfs-site']:
    namenode_https_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.https-address'])

  if 'dfs.namenode.rpc-address' in config['configurations']['hdfs-site']:
    namenode_rpc_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.rpc-address'])



#
# Obtain HDFS HA info if any
#

HAServers=""
dfs_http_policy = default('/configurations/hdfs-site/dfs.http.policy', None)

hdfs_https_one = False
hdfs_scheme = 'http'
if dfs_http_policy  !=  None :
   hdfs_https_on = (dfs_http_policy.upper() == 'HTTPS_ONLY')
   hdfs_scheme = 'http' if not hdfs_https_on else 'https'
   hdfs_port = str(namenode_http_port)  if not hdfs_https_on else str(namenode_https_port)
   namenode_http_port = hdfs_port


if type(namenode_hosts) is list and len(namenode_hosts) > 1:
   HAServers += "HDFSUI,"+ hdfs_scheme  + "://" + namenode_hosts[0]+ ":" + hdfs_port + "," + hdfs_scheme  + "://"  +  namenode_hosts[1] + ":" + hdfs_port
   HAServers += " WEBHDFS," + hdfs_scheme  + "://"+ namenode_hosts[0]+ ":" + hdfs_port  + "/webhdfs"+ "," + hdfs_scheme  + "://" +   namenode_hosts[1] + ":" + hdfs_port + "/webhdfs"
   HAServers += " NAMENODE,hdfs://"+ namenode_hosts[0]+ ":" + str(namenode_rpc_port) + "," + "hdfs://" +  namenode_hosts[1] + ":" + str(namenode_rpc_port)

#
# Yarn
#

rm_hosts = default("/clusterHostInfo/rm_host", None)


yarn_http_policy = default('/configurations/yarn-site/yarn.http.policy', None )
yarn_https_on = False
yarn_scheme = 'http'
if yarn_http_policy !=  None :
   yarn_https_on = ( yarn_http_policy.upper() == 'HTTPS_ONLY')
   yarn_scheme = 'http' if not yarn_https_on else 'https'


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


#
# Obtain RM HA info if any
#
  if rm_hosts != None and type(rm_hosts): 
     if 'yarn.resourcemanager.webapp.address' in config['configurations']['yarn-site']:
        rm_http_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'])
        
     if 'yarn.resourcemanager.webapp.https.address' in config['configurations']['yarn-site']:
        rm_https_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.https.address'])
        rm_port = rm_http_port if not yarn_https_on else rm_https_port

if rm_hosts != None and type(rm_hosts) and len(rm_hosts) > 1:
   HAServers += " YARNUI,"+yarn_scheme+ "://" +  rm_hosts[0]+ ":" + rm_port + "," + yarn_scheme + "://" + rm_hosts[1]+":"+rm_port
   HAServers += " RESOURCEMANAGER,"+yarn_scheme+ "://" +  rm_hosts[0]+ ":" + rm_port + "," + yarn_scheme + "://" + rm_hosts[1]+":"+rm_port


#
# Hive 
#
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

#
# HBase
#

hbase_master_ui_port = default('/configurations/hbase-site/hbase.master.info.port', "16010");
hbase_master_port = default('/configurations/hbase-site/hbase.rest.port', "8080")
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", None)
if type(hbase_master_hosts) is list:
  hbase_master_host = hbase_master_hosts[0]
else:
  hbase_master_host = hbase_master_hosts
hbase_master_scheme = 'http'

#
# Obtain HBASE master info if any
#
if type(hbase_master_hosts) is list and len(hbase_master_hosts)>1:
   hbaseui_hosts=''
   webhbase_hosts=''
   for index in range(len(hbase_master_hosts)):
      hbaseui_hosts  += "http://"+ hbase_master_hosts[index]+ ":" + str(hbase_master_ui_port) +"," 
      webhbase_hosts += "http://"+ hbase_master_hosts[index]+ ":" + str(hbase_master_port)  +","
   HAServers +=  " HBASEUI,"+hbaseui_hosts
   HAServers +=  " WEBHBASE,"+webhbase_hosts


#
# Oozie
#

oozie_https_port = None
oozie_server_port = "11000"
oozie_server_hosts = default("/clusterHostInfo/oozie_server", None)
if type(oozie_server_hosts) is list:
  oozie_server_host = oozie_server_hosts[0]
else:
  oozie_server_host = oozie_server_hosts

oozie_scheme = 'http'
has_oozie = not oozie_server_host == None
oozie_server_port = "11000"

if has_oozie:
    if 'oozie.base.url' in config['configurations']['oozie-site']:
        oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
        oozie_https_port = default("/configurations/oozie-site/oozie.https.port", None)

if oozie_https_port is not None:
   oozie_scheme = 'https'
   oozie_server_port = oozie_https_port

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

# Knox managed properties
knox_managed_pid_symlink= "/usr/iop/current/knox-server/pids"
knox_managed_logs_symlink = ibm_distribution_knox_dir + "/logs"
#
#Hbase master port
#
hbase_master_ui_port = default('/configurations/hbase-site/hbase.master.info.port', "16010");

#Spark 
spark_historyserver_hosts = default("/clusterHostInfo/spark_jobhistoryserver_hosts", None) 
if type(spark_historyserver_hosts) is list: 
  spark_historyserver_host = spark_historyserver_hosts[0] 
else: 
  spark_historyserver_host = spark_historyserver_hosts 

spark_historyserver_ui_port = default("/configurations/spark-defaults/spark.history.ui.port", "18080") 
spark_scheme = 'http'
#
# Solr
#
solr_server_hosts  = default("/clusterHostInfo/solr_hosts", None)
if type(solr_server_hosts ) is list:
  solr_host = solr_server_hosts[0]
else:
  solr_host = solr_server_hosts
solr_port=default("/configuration/solr/solr-env/solr_port","8983")
solr_scheme='http'
# JobHistory mapreduce 
mr_historyserver_address = default("/configurations/mapred-site/mapreduce.jobhistory.webapp.address", None) 
mr_scheme='http'

# Yarn nodemanager
nodeui_port = "8042"
nm_hosts = default("/clusterHostInfo/nm_hosts", "localhost")
nodeui_scheme= 'http'
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
if spark_thriftserver_hosts != None and type(spark_thriftserver_hosts) is list:
  spark_thriftserver_host = spark_thriftserver_hosts[0]
else:
  spark_thriftserver_host = spark_thriftserver_hosts

# server configurations
knox_conf_dir = ibm_distribution_knox_dir + '/conf'
knox_data_dir = ibm_distribution_knox_dir +  '/data'
knox_logs_dir = default("/configurations/knox-env/knox_logs_dir", "/var/log/knox")
knox_pid_dir = status_params.knox_pid_dir
knox_user = default("/configurations/knox-env/knox_user", "knox")
knox_group = default("/configurations/knox-env/knox_group", "knox")
mode = 0644
knox_pid_file = status_params.knox_pid_file
ldap_pid_file = status_params.ldap_pid_file
knox_master_secret = config['configurations']['knox-env']['knox_master_secret']
knox_master_secret_path = ibm_distribution_knox_dir + '/data/security/master'
knox_cert_store_path = ibm_distribution_knox_dir + '/data/security/keystores/gateway.jks'
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
xa_audit_db_name = default('/configurations/admin-properties/audit_db_name', 'ranger_audit')
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
  xa_audit_db_password = ''
  if not is_empty(config['configurations']['admin-properties']['audit_db_password']) and stack_supports_ranger_audit_db:
    xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-knox-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
  xa_audit_db_flavor = (config['configurations']['admin-properties']['DB_FLAVOR']).lower()
  previous_jdbc_jar_name= None

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

  downloaded_custom_connector = format("{tmp_dir}/{jdbc_jar_name}")

  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("/usr/iop/current/knox-server/ext/{jdbc_jar_name}")
  previous_jdbc_jar = format("{stack_root}/current/knox-server/ext/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None

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
  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.db']
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
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
hadoop_conf_dir = conf_select.get_hadoop_conf_dir() if has_namenode else None

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
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
