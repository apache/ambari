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
import status_params
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from functions import calc_xmn_from_xms, ensure_unit_for_memory

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.os_check import OSCheck
from ambari_commons.str_utils import string_set_intersection

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.expect import expect
from ambari_commons.ambari_metrics_helper import select_metric_collector_hosts_from_hostnames
from resource_management.libraries.functions.setup_ranger_plugin_xml import get_audit_configs, generate_ranger_service_config

# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY

stack_name = status_params.stack_name
agent_stack_retry_on_unavailability = config['ambariLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/ambariLevelParams/agent_stack_retry_count", int)
version = default("/commandParams/version", None)
component_directory = status_params.component_directory
etc_prefix_dir = "/etc/hbase"

stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted = status_params.stack_version_formatted
stack_root = status_params.stack_root

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_audit_db = check_stack_feature(StackFeature.RANGER_AUDIT_DB_SUPPORT, version_for_stack_feature_checks)

# hadoop default parameters
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
region_mover = "/usr/lib/hbase/bin/region_mover.rb"
region_drainer = "/usr/lib/hbase/bin/draining_servers.rb"
hbase_cmd = "/usr/lib/hbase/bin/hbase"
hbase_max_direct_memory_size = None

# hadoop parameters for stacks supporting rolling_upgrade
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  daemon_script = format('{stack_root}/current/hbase-client/bin/hbase-daemon.sh')
  region_mover = format('{stack_root}/current/hbase-client/bin/region_mover.rb')
  region_drainer = format('{stack_root}/current/hbase-client/bin/draining_servers.rb')
  hbase_cmd = format('{stack_root}/current/hbase-client/bin/hbase')

  hbase_max_direct_memory_size  = default('configurations/hbase-env/hbase_max_direct_memory_size', None)

  daemon_script=format("{stack_root}/current/{component_directory}/bin/hbase-daemon.sh")
  region_mover = format("{stack_root}/current/{component_directory}/bin/region_mover.rb")
  region_drainer = format("{stack_root}/current/{component_directory}/bin/draining_servers.rb")
  hbase_cmd = format("{stack_root}/current/{component_directory}/bin/hbase")


hbase_conf_dir = status_params.hbase_conf_dir
limits_conf_dir = status_params.limits_conf_dir

hbase_user_nofile_limit = default("/configurations/hbase-env/hbase_user_nofile_limit", "32000")
hbase_user_nproc_limit = default("/configurations/hbase-env/hbase_user_nproc_limit", "16000")

# no symlink for phoenix-server at this point
phx_daemon_script = format('{stack_root}/current/phoenix-server/bin/queryserver.py')

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
java64_home = config['ambariLevelParams']['java_home']
java_version = expect("/ambariLevelParams/java_version", int)

log_dir = config['configurations']['hbase-env']['hbase_log_dir']
java_io_tmpdir = default("/configurations/hbase-env/hbase_java_io_tmpdir", "/tmp")
master_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_master_heapsize'])

regionserver_heapsize = ensure_unit_for_memory(config['configurations']['hbase-env']['hbase_regionserver_heapsize'])
regionserver_xmn_max = config['configurations']['hbase-env']['hbase_regionserver_xmn_max']
regionserver_xmn_percent = expect("/configurations/hbase-env/hbase_regionserver_xmn_ratio", float)
regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)

parallel_gc_threads = expect("/configurations/hbase-env/hbase_parallel_gc_threads", int)

hbase_regionserver_shutdown_timeout = expect('/configurations/hbase-env/hbase_regionserver_shutdown_timeout', int, 30)

phoenix_hosts = default('/clusterHostInfo/phoenix_query_server_hosts', [])
phoenix_enabled = default('/configurations/hbase-env/phoenix_sql_enabled', False)
has_phoenix = len(phoenix_hosts) > 0

underscored_version = stack_version_unformatted.replace('.', '_')
dashed_version = stack_version_unformatted.replace('.', '-')
# if OSCheck.is_redhat_family() or OSCheck.is_suse_family():
#   phoenix_package = format("phoenix_{underscored_version}_*")
# elif OSCheck.is_ubuntu_family():
#   phoenix_package = format("phoenix-{dashed_version}-.*")
phoenix_package = "phoenix"

pid_dir = status_params.pid_dir
tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
local_dir = config['configurations']['hbase-site']['hbase.local.dir']
ioengine_param = default('/configurations/hbase-site/hbase.bucketcache.ioengine', None)

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")
queryserver_jaas_config_file = format("{hbase_conf_dir}/hbase_queryserver_jaas.conf")

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', []) # is not passed when ganglia is not present
has_ganglia_server = not len(ganglia_server_hosts) == 0
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

set_instanceId = "false"
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
has_metric_collector = not len(ams_collector_hosts) == 0
metric_collector_port = None
if has_metric_collector:
  if 'cluster-env' in config['configurations'] and \
      'metrics_collector_external_port' in config['configurations']['cluster-env']:
    metric_collector_port = config['configurations']['cluster-env']['metrics_collector_external_port']
  else:
    metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
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

host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)
is_aggregation_https_enabled = False
if default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = 'https'
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = 'http'

# if hbase is selected the hbase_regionserver_hosts, should not be empty, but still default just in case
if 'datanode_hosts' in config['clusterHostInfo']:
  rs_hosts = default('/clusterHostInfo/hbase_regionserver_hosts', '/clusterHostInfo/datanode_hosts') #if hbase_regionserver_hosts not given it is assumed that region servers on same nodes as slaves
else:
  rs_hosts = default('/clusterHostInfo/hbase_regionserver_hosts', '/clusterHostInfo/all_hosts') 

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_permissions = "RWXCA"
service_check_data = get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]

if security_enabled:
  zk_principal_name = default("/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper/_HOST@EXAMPLE.COM")
  zk_principal_user = zk_principal_name.split('/')[0]
  zk_security_opts = format('-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={zk_principal_user} -Dzookeeper.sasl.clientconfig=Client')
  _hostname_lowercase = config['agentLevelParams']['hostname'].lower()
  master_jaas_princ = config['configurations']['hbase-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  master_keytab_path = config['configurations']['hbase-site']['hbase.master.keytab.file']
  regionserver_jaas_princ = config['configurations']['hbase-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)
  _queryserver_jaas_princ = config['configurations']['hbase-site']['phoenix.queryserver.kerberos.principal']
  if not is_empty(_queryserver_jaas_princ):
    queryserver_jaas_princ =_queryserver_jaas_princ.replace('_HOST',_hostname_lowercase)

regionserver_keytab_path = config['configurations']['hbase-site']['hbase.regionserver.keytab.file']
queryserver_keytab_path = config['configurations']['hbase-site']['phoenix.queryserver.keytab.file']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hbase_user_keytab = config['configurations']['hbase-env']['hbase_user_keytab']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
if security_enabled:
  kinit_cmd = format("{kinit_path_local} -kt {hbase_user_keytab} {hbase_principal_name};")
  kinit_cmd_master = format("{kinit_path_local} -kt {master_keytab_path} {master_jaas_princ};")
  master_security_config = format("-Djava.security.auth.login.config={hbase_conf_dir}/hbase_master_jaas.conf")
  hbase_decommission_auth_config = "--auth-as-server"
else:
  kinit_cmd = ""
  kinit_cmd_master = ""
  master_security_config = ""
  hbase_decommission_auth_config = ""

#log4j.properties
# HBase log4j settings
hbase_log_maxfilesize = default('configurations/hbase-log4j/hbase_log_maxfilesize',256)
hbase_log_maxbackupindex = default('configurations/hbase-log4j/hbase_log_maxbackupindex',20)
hbase_security_log_maxfilesize = default('configurations/hbase-log4j/hbase_security_log_maxfilesize',256)
hbase_security_log_maxbackupindex = default('configurations/hbase-log4j/hbase_security_log_maxbackupindex',20)

if (('hbase-log4j' in config['configurations']) and ('content' in config['configurations']['hbase-log4j'])):
  log4j_props = config['configurations']['hbase-log4j']['content']
else:
  log4j_props = None
  
hbase_env_sh_template = config['configurations']['hbase-env']['content']

hbase_hdfs_root_dir = config['configurations']['hbase-site']['hbase.rootdir']
hbase_staging_dir = "/apps/hbase/staging"
#for create_hdfs_directory
hostname = config['agentLevelParams']['hostname']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/clusterLevelParams/dfs_type", "")

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

zookeeper_znode_parent = config['configurations']['hbase-site']['zookeeper.znode.parent']
hbase_zookeeper_quorum = config['configurations']['hbase-site']['hbase.zookeeper.quorum']
hbase_zookeeper_property_clientPort = config['configurations']['hbase-site']['hbase.zookeeper.property.clientPort']
hbase_security_authentication = config['configurations']['hbase-site']['hbase.security.authentication']
hadoop_security_authentication = config['configurations']['core-site']['hadoop.security.authentication']

# ranger hbase plugin section start

# to get db connector jar
jdk_location = config['ambariLevelParams']['jdk_location']

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env introduced, using stack feature
xml_configurations_supported = check_stack_feature(StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks)

# ranger hbase plugin enabled property
enable_ranger_hbase = default("/configurations/ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled", "No")
enable_ranger_hbase = True if enable_ranger_hbase.lower() == 'yes' else False

# ranger hbase properties
if enable_ranger_hbase:
  # get ranger policy url
  policymgr_mgr_url = config['configurations']['admin-properties']['policymgr_external_url']
  if xml_configurations_supported:
    policymgr_mgr_url = config['configurations']['ranger-hbase-security']['ranger.plugin.hbase.policy.rest.url']

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  # ranger audit db user
  xa_audit_db_user = default('/configurations/admin-properties/audit_db_user', 'rangerlogger')

  # ranger hbase service/repository name
  repo_name = str(config['clusterName']) + '_hbase'
  repo_name_value = config['configurations']['ranger-hbase-security']['ranger.plugin.hbase.service.name']
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  common_name_for_certificate = config['configurations']['ranger-hbase-plugin-properties']['common.name.for.certificate']
  repo_config_username = config['configurations']['ranger-hbase-plugin-properties']['REPOSITORY_CONFIG_USERNAME']
  ranger_plugin_properties = config['configurations']['ranger-hbase-plugin-properties']
  policy_user = config['configurations']['ranger-hbase-plugin-properties']['policy_user']
  repo_config_password = config['configurations']['ranger-hbase-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']

  # ranger-env config
  ranger_env = config['configurations']['ranger-env']

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_hbase:
    external_admin_username = default('/configurations/ranger-hbase-plugin-properties/external_admin_username', 'admin')
    external_admin_password = default('/configurations/ranger-hbase-plugin-properties/external_admin_password', 'admin')
    external_ranger_admin_username = default('/configurations/ranger-hbase-plugin-properties/external_ranger_admin_username', 'amb_ranger_admin')
    external_ranger_admin_password = default('/configurations/ranger-hbase-plugin-properties/external_ranger_admin_password', 'amb_ranger_admin')
    ranger_env = {}
    ranger_env['admin_username'] = external_admin_username
    ranger_env['admin_password'] = external_admin_password
    ranger_env['ranger_admin_username'] = external_ranger_admin_username
    ranger_env['ranger_admin_password'] = external_ranger_admin_password

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

    downloaded_custom_connector = format("{exec_tmp_dir}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_source = format("{jdk_location}/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    driver_curl_target = format("{stack_root}/current/{component_directory}/lib/{jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    previous_jdbc_jar = format("{stack_root}/current/{component_directory}/lib/{previous_jdbc_jar_name}") if stack_supports_ranger_audit_db else None
    sql_connector_jar = ''

  if security_enabled:
    master_principal = config['configurations']['hbase-site']['hbase.master.kerberos.principal']

  hbase_ranger_plugin_config = {
    'username': repo_config_username,
    'password': repo_config_password,
    'hadoop.security.authentication': hadoop_security_authentication,
    'hbase.security.authentication': hbase_security_authentication,
    'hbase.zookeeper.property.clientPort': hbase_zookeeper_property_clientPort,
    'hbase.zookeeper.quorum': hbase_zookeeper_quorum,
    'zookeeper.znode.parent': zookeeper_znode_parent,
    'commonNameForCertificate': common_name_for_certificate,
    'hbase.master.kerberos.principal': master_principal if security_enabled else ''
  }

  if security_enabled:
    hbase_ranger_plugin_config['policy.download.auth.users'] = hbase_user
    hbase_ranger_plugin_config['tag.download.auth.users'] = hbase_user
    hbase_ranger_plugin_config['policy.grantrevoke.auth.users'] = hbase_user

  hbase_ranger_plugin_config['setup.additional.default.policies'] = "true"
  hbase_ranger_plugin_config['default-policy.1.name'] = "Service Check User Policy for Hbase"
  hbase_ranger_plugin_config['default-policy.1.resource.table'] = "ambarismoketest"
  hbase_ranger_plugin_config['default-policy.1.resource.column-family'] = "*"
  hbase_ranger_plugin_config['default-policy.1.resource.column'] = "*"
  hbase_ranger_plugin_config['default-policy.1.policyItem.1.users'] = policy_user
  hbase_ranger_plugin_config['default-policy.1.policyItem.1.accessTypes'] = "read,write,create"

  custom_ranger_service_config = generate_ranger_service_config(ranger_plugin_properties)
  if len(custom_ranger_service_config) > 0:
    hbase_ranger_plugin_config.update(custom_ranger_service_config)

  hbase_ranger_plugin_repo = {
    'isEnabled': 'true',
    'configs': hbase_ranger_plugin_config,
    'description': 'hbase repo',
    'name': repo_name,
    'type': 'hbase'
  }

  ranger_hbase_principal = None
  ranger_hbase_keytab = None
  if stack_supports_ranger_kerberos and security_enabled and 'hbase-master' in component_directory.lower():
    ranger_hbase_principal = master_jaas_princ
    ranger_hbase_keytab = master_keytab_path
  elif stack_supports_ranger_kerberos and security_enabled and 'hbase-regionserver' in component_directory.lower():
    ranger_hbase_principal = regionserver_jaas_princ
    ranger_hbase_keytab = regionserver_keytab_path

  xa_audit_db_is_enabled = False
  if xml_configurations_supported and stack_supports_ranger_audit_db:
    xa_audit_db_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.db']

  xa_audit_hdfs_is_enabled = config['configurations']['ranger-hbase-audit']['xasecure.audit.destination.hdfs'] if xml_configurations_supported else False
  ssl_keystore_password = config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password'] if xml_configurations_supported else None
  ssl_truststore_password = config['configurations']['ranger-hbase-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password'] if xml_configurations_supported else None
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks')

  # for SQLA explicitly disable audit to DB for Ranger
  if has_ranger_admin and stack_supports_ranger_audit_db and xa_audit_db_flavor.lower() == 'sqla':
    xa_audit_db_is_enabled = False

# need this to capture cluster name from where ranger hbase plugin is enabled
cluster_name = config['clusterName']

# ranger hbase plugin section end

create_hbase_home_directory = check_stack_feature(StackFeature.HBASE_HOME_DIRECTORY, stack_version_formatted)
hbase_home_directory = format("/user/{hbase_user}")

atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
has_atlas = len(atlas_hosts) > 0

metadata_user = default('/configurations/atlas-env/metadata_user', None)
atlas_graph_storage_hostname = default('/configurations/application-properties/atlas.graph.storage.hostname', None)
atlas_graph_storage_hbase_table = default('/configurations/application-properties/atlas.graph.storage.hbase.table', None)
atlas_audit_hbase_tablename = default('/configurations/application-properties/atlas.audit.hbase.tablename', None)

if has_atlas:
  zk_hosts_matches = string_set_intersection(atlas_graph_storage_hostname, hbase_zookeeper_quorum)
  atlas_with_managed_hbase = len(zk_hosts_matches) > 0
else:
  atlas_with_managed_hbase = False

# Hbase Atlas hook configurations
atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')
enable_hbase_atlas_hook = default('/configurations/hbase-env/hbase.atlas.hook', False)
hbase_atlas_hook_properties = default('/configurations/hbase-atlas-application-properties', {})

mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
if 'viewfs-mount-table' in config['configurations']:
  xml_inclusion_file_name = 'viewfs-mount-table.xml'
  mount_table = config['configurations']['viewfs-mount-table']

  if 'content' in mount_table and mount_table['content'].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(hbase_conf_dir, xml_inclusion_file_name)
    mount_table_content = mount_table['content']
