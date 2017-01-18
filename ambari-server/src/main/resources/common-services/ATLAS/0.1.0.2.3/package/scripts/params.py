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
import sys

from ambari_commons import OSCheck
from resource_management import get_bare_principal
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

# Local Imports
from status_params import *
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.expect import expect


def configs_for_ha(atlas_hosts, metadata_port, is_atlas_ha_enabled):
  """
  Return a dictionary of additional configs to merge if Atlas HA is enabled.
  :param atlas_hosts: List of hostnames that contain Atlas
  :param metadata_port: Port number
  :param is_atlas_ha_enabled: None, True, or False
  :return: Dictionary with additional configs to merge to application-properties if HA is enabled.
  """
  additional_props = {}
  if atlas_hosts is None or len(atlas_hosts) == 0 or metadata_port is None:
    return additional_props

  # Sort to guarantee each host sees the same values, assuming restarted at the same time.
  atlas_hosts = sorted(atlas_hosts)

  # E.g., id1,id2,id3,...,idn
  _server_id_list = ["id" + str(i) for i in range(1, len(atlas_hosts) + 1)]
  atlas_server_ids = ",".join(_server_id_list)
  additional_props["atlas.server.ids"] = atlas_server_ids

  i = 0
  for curr_hostname in atlas_hosts:
    id = _server_id_list[i]
    prop_name = "atlas.server.address." + id
    prop_value = curr_hostname + ":" + metadata_port
    additional_props[prop_name] = prop_value
    i += 1

  # This may override the existing property
  if i == 1 or (i > 1 and is_atlas_ha_enabled is False):
    additional_props["atlas.server.ha.enabled"] = "false"
  elif i > 1:
    additional_props["atlas.server.ha.enabled"] = "true"

  return additional_props
  
# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()

# Needed since this is an Atlas Hook service.
cluster_name = config['clusterName']

java_version = expect("/hostLevelParams/java_version", int)

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  _atlas_principal_name = config['configurations']['application-properties']['atlas.authentication.principal']
  atlas_jaas_principal = _atlas_principal_name.replace('_HOST',_hostname_lowercase)
  atlas_keytab_path = config['configurations']['application-properties']['atlas.authentication.keytab']

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

# stack version
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

metadata_home = os.environ['METADATA_HOME_DIR'] if 'METADATA_HOME_DIR' in os.environ else format('{stack_root}/current/atlas-server')
metadata_bin = format("{metadata_home}/bin")

python_binary = os.environ['PYTHON_EXE'] if 'PYTHON_EXE' in os.environ else sys.executable
metadata_start_script = format("{metadata_bin}/atlas_start.py")
metadata_stop_script = format("{metadata_bin}/atlas_stop.py")

# metadata local directory structure
log_dir = config['configurations']['atlas-env']['metadata_log_dir']

# service locations
hadoop_conf_dir = os.path.join(os.environ["HADOOP_HOME"], "conf") if 'HADOOP_HOME' in os.environ else '/etc/hadoop/conf'

# some commands may need to supply the JAAS location when running as atlas
atlas_jaas_file = format("{conf_dir}/atlas_jaas.conf")

# user
user_group = config['configurations']['cluster-env']['user_group']

# metadata env
java64_home = config['hostLevelParams']['java_home']
env_sh_template = config['configurations']['atlas-env']['content']

# credential provider
credential_provider = format( "jceks://file@{conf_dir}/atlas-site.jceks")

# command line args
ssl_enabled = default("/configurations/application-properties/atlas.enableTLS", False)
http_port = default("/configurations/application-properties/atlas.server.http.port", "21000")
https_port = default("/configurations/application-properties/atlas.server.https.port", "21443")
if ssl_enabled:
  metadata_port = https_port
  metadata_protocol = 'https'
else:
  metadata_port = http_port
  metadata_protocol = 'http'

metadata_host = config['hostname']

atlas_hosts = sorted(default('/clusterHostInfo/atlas_server_hosts', []))
metadata_server_host = atlas_hosts[0] if len(atlas_hosts) > 0 else "UNKNOWN_HOST"

# application properties
application_properties = dict(config['configurations']['application-properties'])
application_properties["atlas.server.bind.address"] = metadata_host

# trimming knox_key
if 'atlas.sso.knox.publicKey' in application_properties:
  knox_key = application_properties['atlas.sso.knox.publicKey']
  knox_key_without_new_line = knox_key.replace("\n","")
  application_properties['atlas.sso.knox.publicKey'] = knox_key_without_new_line

if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, version_for_stack_feature_checks):
  metadata_server_url = application_properties["atlas.rest.address"]
else:
  # In HDP 2.3 and 2.4 the property was computed and saved to the local config but did not exist in the database.
  metadata_server_url = format('{metadata_protocol}://{metadata_server_host}:{metadata_port}')
  application_properties["atlas.rest.address"] = metadata_server_url

# Atlas HA should populate
# atlas.server.ids = id1,id2,...,idn
# atlas.server.address.id# = host#:port
# User should not have to modify this property, but still allow overriding it to False if multiple Atlas servers exist
# This can be None, True, or False
is_atlas_ha_enabled = default("/configurations/application-properties/atlas.server.ha.enabled", None)
additional_ha_props = configs_for_ha(atlas_hosts, metadata_port, is_atlas_ha_enabled)
for k,v in additional_ha_props.iteritems():
  application_properties[k] = v


metadata_env_content = config['configurations']['atlas-env']['content']

metadata_opts = config['configurations']['atlas-env']['metadata_opts']
metadata_classpath = config['configurations']['atlas-env']['metadata_classpath']
data_dir = format("{stack_root}/current/atlas-server/data")
expanded_war_dir = os.environ['METADATA_EXPANDED_WEBAPP_DIR'] if 'METADATA_EXPANDED_WEBAPP_DIR' in os.environ else format("{stack_root}/current/atlas-server/server/webapp")

metadata_log4j_content = config['configurations']['atlas-log4j']['content']

metadata_solrconfig_content = default("/configurations/atlas-solrconfig/content", None)

atlas_log_level = config['configurations']['atlas-log4j']['atlas_log_level']
audit_log_level = config['configurations']['atlas-log4j']['audit_log_level']
atlas_log_max_backup_size = default("/configurations/atlas-log4j/atlas_log_max_backup_size", 256)
atlas_log_number_of_backup_files = default("/configurations/atlas-log4j/atlas_log_number_of_backup_files", 20)

# smoke test
smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smoke_test_password = 'smoke'
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']


security_check_status_file = format('{log_dir}/security_check.status')

# hbase
hbase_conf_dir = "/etc/hbase/conf"

atlas_search_backend = default("/configurations/application-properties/atlas.graph.index.search.backend", "")
search_backend_solr = atlas_search_backend.startswith('solr')

# infra solr
infra_solr_znode = default("/configurations/infra-solr-env/infra_solr_znode", None)
infra_solr_hosts = default("/clusterHostInfo/infra_solr_hosts", [])
infra_solr_replication_factor = 2 if len(infra_solr_hosts) > 1 else 1
atlas_solr_shards = default("/configurations/atlas-env/atlas_solr-shards", 1)
has_infra_solr = len(infra_solr_hosts) > 0

# zookeeper
zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_port = default('/configurations/zoo.cfg/clientPort', None)

# get comma separated lists of zookeeper hosts from clusterHostInfo
index = 0
zookeeper_quorum = ""
for host in zookeeper_hosts:
  zookeeper_host = host
  if zookeeper_port is not None:
    zookeeper_host = host + ":" + str(zookeeper_port)

  zookeeper_quorum += zookeeper_host
  index += 1
  if index < len(zookeeper_hosts):
    zookeeper_quorum += ","

stack_supports_atlas_hdfs_site_on_namenode_ha = check_stack_feature(StackFeature.ATLAS_HDFS_SITE_ON_NAMENODE_HA, version_for_stack_feature_checks)

atlas_server_xmx = default("configurations/atlas-env/atlas_server_xmx", 2048)
atlas_server_max_new_size = default("configurations/atlas-env/atlas_server_max_new_size", 614)

hbase_master_hosts = default('/clusterHostInfo/hbase_master_hosts', [])
has_hbase_master = not len(hbase_master_hosts) == 0

atlas_hbase_setup = format("{exec_tmp_dir}/atlas_hbase_setup.rb")
atlas_kafka_setup = format("{exec_tmp_dir}/atlas_kafka_acl.sh")
atlas_graph_storage_hbase_table = default('/configurations/application-properties/atlas.graph.storage.hbase.table', None)
atlas_audit_hbase_tablename = default('/configurations/application-properties/atlas.audit.hbase.tablename', None)

hbase_user_keytab = default('/configurations/hbase-env/hbase_user_keytab', None)
hbase_principal_name = default('/configurations/hbase-env/hbase_principal_name', None)

# ToDo: Kafka port to Atlas
# Used while upgrading the stack in a kerberized cluster and running kafka-acls.sh
hosts_with_kafka = default('/clusterHostInfo/kafka_broker_hosts', [])
host_with_kafka = hostname in hosts_with_kafka

ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
has_ranger_tagsync = len(ranger_tagsync_hosts) > 0
rangertagsync_user = "rangertagsync"

kafka_keytab = default('/configurations/kafka-env/kafka_keytab', None)
kafka_principal_name = default('/configurations/kafka-env/kafka_principal_name', None)
default_replication_factor = default('/configurations/application-properties/atlas.notification.replicas', None)

if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, version_for_stack_feature_checks):
  default_replication_factor = default('/configurations/application-properties/atlas.notification.replicas', None)

  kafka_env_sh_template = config['configurations']['kafka-env']['content']
  kafka_home = os.path.join(stack_root,  "current", "kafka-broker")
  kafka_conf_dir = os.path.join(kafka_home, "config")

  kafka_zk_endpoint = default("/configurations/kafka-broker/zookeeper.connect", None)
  kafka_kerberos_enabled = (('security.inter.broker.protocol' in config['configurations']['kafka-broker']) and
                            ((config['configurations']['kafka-broker']['security.inter.broker.protocol'] == "PLAINTEXTSASL") or
                             (config['configurations']['kafka-broker']['security.inter.broker.protocol'] == "SASL_PLAINTEXT")))
  if security_enabled and stack_version_formatted != "" and 'kafka_principal_name' in config['configurations']['kafka-env'] \
    and check_stack_feature(StackFeature.KAFKA_KERBEROS, stack_version_formatted):
    _hostname_lowercase = config['hostname'].lower()
    _kafka_principal_name = config['configurations']['kafka-env']['kafka_principal_name']
    kafka_jaas_principal = _kafka_principal_name.replace('_HOST', _hostname_lowercase)
    kafka_keytab_path = config['configurations']['kafka-env']['kafka_keytab']
    kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)
    kafka_kerberos_params = "-Djava.security.auth.login.config={0}/kafka_jaas.conf".format(kafka_conf_dir)
  else:
    kafka_kerberos_params = ''
    kafka_jaas_principal = None
    kafka_keytab_path = None

namenode_host = set(default("/clusterHostInfo/namenode_host", []))
has_namenode = not len(namenode_host) == 0

# ranger altas plugin section start

# ranger host
ranger_admin_hosts = default("/clusterHostInfo/ranger_admin_hosts", [])
has_ranger_admin = not len(ranger_admin_hosts) == 0

retry_enabled = default("/commandParams/command_retry_enabled", False)

stack_supports_atlas_ranger_plugin = check_stack_feature(StackFeature.ATLAS_RANGER_PLUGIN_SUPPORT, version_for_stack_feature_checks)
stack_supports_ranger_kerberos = check_stack_feature(StackFeature.RANGER_KERBEROS_SUPPORT, version_for_stack_feature_checks)

# ranger support xml_configuration flag, instead of depending on ranger xml_configurations_supported/ranger-env, using stack feature
xml_configurations_supported = check_stack_feature(StackFeature.RANGER_XML_CONFIGURATION, version_for_stack_feature_checks)

# ranger atlas plugin enabled property
enable_ranger_atlas = default("/configurations/ranger-atlas-plugin-properties/ranger-atlas-plugin-enabled", "No")
enable_ranger_atlas = True if enable_ranger_atlas.lower() == "yes" else False

# ranger hbase plugin enabled property
enable_ranger_hbase = default("/configurations/ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled", "No")
enable_ranger_hbase = True if enable_ranger_hbase.lower() == 'yes' else False

if stack_supports_atlas_ranger_plugin and enable_ranger_atlas:
  # for create_hdfs_directory
  hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
  hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']  if has_namenode else None
  hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
  hdfs_site = config['configurations']['hdfs-site']
  default_fs = config['configurations']['core-site']['fs.defaultFS']
  dfs_type = default("/commandParams/dfs_type", "")

  import functools
  from resource_management.libraries.resources.hdfs_resource import HdfsResource
  from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
  #create partial functions with common arguments for every HdfsResource call
  #to create hdfs directory we need to call params.HdfsResource in code

  HdfsResource = functools.partial(
    HdfsResource,
    user = hdfs_user,
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

  # ranger atlas service/repository name
  repo_name = str(config['clusterName']) + '_atlas'
  repo_name_value = config['configurations']['ranger-atlas-security']['ranger.plugin.atlas.service.name']
  if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
    repo_name = repo_name_value

  ssl_keystore_password = config['configurations']['ranger-atlas-policymgr-ssl']['xasecure.policymgr.clientssl.keystore.password']
  ssl_truststore_password = config['configurations']['ranger-atlas-policymgr-ssl']['xasecure.policymgr.clientssl.truststore.password']
  credential_file = format('/etc/ranger/{repo_name}/cred.jceks')
  xa_audit_hdfs_is_enabled = default('/configurations/ranger-atlas-audit/xasecure.audit.destination.hdfs', False)

  # get ranger policy url
  policymgr_mgr_url = config['configurations']['ranger-atlas-security']['ranger.plugin.atlas.policy.rest.url']

  if not is_empty(policymgr_mgr_url) and policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')

  downloaded_custom_connector = None
  driver_curl_source = None
  driver_curl_target = None

  ranger_env = config['configurations']['ranger-env']

  # create ranger-env config having external ranger credential properties
  if not has_ranger_admin and enable_ranger_atlas:
    external_admin_username = default('/configurations/ranger-atlas-plugin-properties/external_admin_username', 'admin')
    external_admin_password = default('/configurations/ranger-atlas-plugin-properties/external_admin_password', 'admin')
    external_ranger_admin_username = default('/configurations/ranger-atlas-plugin-properties/external_ranger_admin_username', 'amb_ranger_admin')
    external_ranger_admin_password = default('/configurations/ranger-atlas-plugin-properties/external_ranger_admin_password', 'amb_ranger_admin')
    ranger_env = {}
    ranger_env['admin_username'] = external_admin_username
    ranger_env['admin_password'] = external_admin_password
    ranger_env['ranger_admin_username'] = external_ranger_admin_username
    ranger_env['ranger_admin_password'] = external_ranger_admin_password

  ranger_plugin_properties = config['configurations']['ranger-atlas-plugin-properties']
  ranger_atlas_audit = config['configurations']['ranger-atlas-audit']
  ranger_atlas_audit_attrs = config['configuration_attributes']['ranger-atlas-audit']
  ranger_atlas_security = config['configurations']['ranger-atlas-security']
  ranger_atlas_security_attrs = config['configuration_attributes']['ranger-atlas-security']
  ranger_atlas_policymgr_ssl = config['configurations']['ranger-atlas-policymgr-ssl']
  ranger_atlas_policymgr_ssl_attrs = config['configuration_attributes']['ranger-atlas-policymgr-ssl']

  policy_user = config['configurations']['ranger-atlas-plugin-properties']['policy_user']

  atlas_repository_configuration = {
    'username' : config['configurations']['ranger-atlas-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
    'password' : unicode(config['configurations']['ranger-atlas-plugin-properties']['REPOSITORY_CONFIG_PASSWORD']),
    'atlas.rest.address' : metadata_server_url,
    'commonNameForCertificate' : config['configurations']['ranger-atlas-plugin-properties']['common.name.for.certificate'],
    'ambari.service.check.user' : policy_user
  }

  if security_enabled:
    atlas_repository_configuration['policy.download.auth.users'] = metadata_user
    atlas_repository_configuration['tag.download.auth.users'] = metadata_user

  atlas_ranger_plugin_repo = {
    'isEnabled': 'true',
    'configs': atlas_repository_configuration,
    'description': 'atlas repo',
    'name': repo_name,
    'type': 'atlas',
    }
# ranger atlas plugin section end
