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
import status_params
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from functions import calc_xmn_from_xms, ensure_unit_for_memory

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_check import OSCheck
from ambari_commons.str_utils import cbool, cint

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_hdp_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script


# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = default("/hostLevelParams/stack_name", None)
agent_stack_retry_on_unavailability = cbool(default("/hostLevelParams/agent_stack_retry_on_unavailability", None))
agent_stack_retry_count = cint(default("/hostLevelParams/agent_stack_retry_count", None))

version = default("/commandParams/version", None)
component_directory = status_params.component_directory
etc_prefix_dir = "/etc/hbase"

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

# hadoop default parameters
hadoop_bin_dir = hdp_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
region_mover = "/usr/lib/hbase/bin/region_mover.rb"
region_drainer = "/usr/lib/hbase/bin/draining_servers.rb"
hbase_cmd = "/usr/lib/hbase/bin/hbase"
hbase_max_direct_memory_size = None

# hadoop parameters for 2.2+
if Script.is_hdp_stack_greater_or_equal("2.2"):
  daemon_script = format('/usr/hdp/current/hbase-client/bin/hbase-daemon.sh')
  region_mover = format('/usr/hdp/current/hbase-client/bin/region_mover.rb')
  region_drainer = format('/usr/hdp/current/hbase-client/bin/draining_servers.rb')
  hbase_cmd = format('/usr/hdp/current/hbase-client/bin/hbase')

  hbase_max_direct_memory_size  = default('configurations/hbase-env/hbase_max_direct_memory_size', None)

  daemon_script=format("/usr/hdp/current/{component_directory}/bin/hbase-daemon.sh")
  region_mover = format("/usr/hdp/current/{component_directory}/bin/region_mover.rb")
  region_drainer = format("/usr/hdp/current/{component_directory}/bin/draining_servers.rb")
  hbase_cmd = format("/usr/hdp/current/{component_directory}/bin/hbase")


hbase_conf_dir = status_params.hbase_conf_dir
limits_conf_dir = status_params.limits_conf_dir

hbase_user_nofile_limit = default("/configurations/hbase-env/hbase_user_nofile_limit", "32000")
hbase_user_nproc_limit = default("/configurations/hbase-env/hbase_user_nproc_limit", "16000")

# no symlink for phoenix-server at this point
phx_daemon_script = '/usr/hdp/current/phoenix-server/bin/queryserver.py'

hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = default("/commandParams/mark_draining_only",False)
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
hbase_principal_name = config['configurations']['hbase-env']['hbase_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = config['configurations']['cluster-env']['security_enabled']

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']
java_version = int(config['hostLevelParams']['java_version'])

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
java_io_tmpdir = config['configurations']['hbase-env']['hbase_java_io_tmpdir']
master_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_master_heapsize'])

regionserver_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_regionserver_heapsize'])
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = config['configurations']['hbase-env']['hbase_regionserver_xmn_ratio']
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)


phoenix_hosts = default('/clusterHostInfo/phoenix_query_server_hosts', [])
phoenix_enabled = default('/configurations/hbase-env/phoenix_sql_enabled', False)
has_phoenix = len(phoenix_hosts) > 0

if not has_phoenix and not phoenix_enabled:
  exclude_packages = ['phoenix*']
else:
  exclude_packages = []

underscored_version = stack_version_unformatted.replace('.', '_')
dashed_version = stack_version_unformatted.replace('.', '-')
if OSCheck.is_redhat_family() or OSCheck.is_suse_family():
  phoenix_package = format("phoenix_{underscored_version}_*")
elif OSCheck.is_ubuntu_family():
  phoenix_package = format("phoenix-{dashed_version}-.*")

pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
local_dir = config['configurations']['hbase-site']['hbase.local.dir']

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")
queryserver_jaas_config_file = format("{hbase_conf_dir}/hbase_queryserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
ganglia_server_host = '' if len(ganglia_server_hosts) == 0 else ganglia_server_hosts[0]

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_vip_host' in config['configurations']['cluster-env']:
    metric_collector_host = config['configurations']['cluster-env']['metrics_collector_vip_host']
  else:
    metric_collector_host = ams_collector_hosts[0]
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_vip_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_vip_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "localhost:6188")
    if metric_collector_web_address.find(':') != -1:
      metric_collector_port = metric_collector_web_address.split(':')[1]
    else:
      metric_collector_port = '6188'
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
  metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
  metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")

  pass
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)

# if hbase is selected the hbase_rs_hosts, should not be empty, but still default just in case
if 'slave_hosts' in config['clusterHostInfo']:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/slave_hosts') #if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default('/clusterHostInfo/hbase_rs_hosts', '/clusterHostInfo/all_hosts') 

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_permissions = "RWXCA"
service_check_data = get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  _queryserver_jaas_princ = config['configurations']['hbase-site']['phoenix.queryserver.kerberos.principal']
  if not is_empty(_queryserver_jaas_princ):
    queryserver_jaas_princ =_queryserver_jaas_princ.replace('_HOST',_hostname_lowercase)

master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
queryserver_keytab_path = config['configurations']['hbase-site']['phoenix.queryserver.keytab.file']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
  kinit_cmd_master = format("{kinit_path_local} -kt {master_keytab_path} {master_jaas_princ};")
  master_security_config = format("-Djava.security.auth.login.config={hbase_conf_dir}/hbase_master_jaas.conf")
else:
  kinit_cmd = ""
  kinit_cmd_master = ""
  master_security_config = ""

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



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/commandParams/dfs_type", "")

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
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
)

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0
xml_configurations_supported = config['configurations']['ranger-env']['xml_configurations_supported']
ambari_server_hostname = config['clusterHostInfo']['ambari_server_host'][0]

# ranger hbase properties
policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
sql_connector_jar = config['configurations']['admin-properties']['SQL_CONNECTOR_JAR']
xa_audit_db_name = config['configurations']['admin-properties']['audit_db_name']
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
  xa_audit_db_password = unicode(config['configurations']['admin-properties']['audit_db_password'])
  repo_config_password = unicode(config['configurations']['ranger-hbase-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'])
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

  downloaded_custom_connector = format("{exec_tmp_dir}/{jdbc_jar_name}")
  driver_curl_source = format("{jdk_location}/{jdbc_symlink_name}")
  driver_curl_target = format("/usr/hdp/current/{component_directory}/lib/{jdbc_jar_name}")

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
  xa_audit_db_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.db'] if xml_configurations_supported else None
  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else None
  ssl_keystore_password = unicode(config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']) if xml_configurations_supported else None
  ssl_truststore_password = unicode(config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']) if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks') if xml_configurations_supported else None

  #For SQLA explicitly disable audit to DB for Ranger
  if xa_audit_db_flavor == 'sqla':
    xa_audit_db_is_enabled = False
