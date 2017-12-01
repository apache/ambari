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
from ambari_commons import OSCheck
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.lzo_utils import should_install_lzo
from ambari_commons.constants import AMBARI_SUDO_BINARY

import status_params

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'DRUID_BROKER': 'druid-broker',
  'DRUID_COORDINATOR': 'druid-coordinator',
  'DRUID_HISTORICAL': 'druid-historical',
  'DRUID_MIDDLEMANAGER': 'druid-middlemanager',
  'DRUID_OVERLORD': 'druid-overlord',
  'DRUID_ROUTER': 'druid-router'
}

# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()
tmp_dir = Script.get_tmp_dir()

stack_name = default("/hostLevelParams/stack_name", None)

# stack version
stack_version = default("/commandParams/version", None)

# un-formatted stack version
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])

# default role to coordinator needed for service checks
component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "DRUID_COORDINATOR")

hostname = config['hostname']
sudo = AMBARI_SUDO_BINARY

# default druid parameters
druid_home = format("{stack_root}/current/{component_directory}")
druid_conf_dir = format("{stack_root}/current/{component_directory}/conf")

druid_common_conf_dir = druid_conf_dir + "/_common"
druid_coordinator_conf_dir = druid_conf_dir + "/coordinator"
druid_overlord_conf_dir = druid_conf_dir + "/overlord"
druid_broker_conf_dir = druid_conf_dir + "/broker"
druid_historical_conf_dir = druid_conf_dir + "/historical"
druid_middlemanager_conf_dir = druid_conf_dir + "/middleManager"
druid_router_conf_dir = druid_conf_dir + "/router"
druid_extensions_dir = druid_home + "/extensions"
druid_hadoop_dependencies_dir = druid_home + "/hadoop-dependencies"
druid_segment_infoDir = config['configurations']['druid-historical']['druid.segmentCache.infoDir']
druid_segment_cache_locations = config['configurations']['druid-historical']['druid.segmentCache.locations']
druid_tasks_dir = config['configurations']['druid-middlemanager']['druid.indexer.task.baseTaskDir']
druid_user = config['configurations']['druid-env']['druid_user']
druid_log_dir = config['configurations']['druid-env']['druid_log_dir']
druid_classpath = config['configurations']['druid-env']['druid_classpath']
druid_extensions = config['configurations']['druid-common']['druid.extensions.pullList']
druid_repo_list = config['configurations']['druid-common']['druid.extensions.repositoryList']
druid_extensions_load_list = config['configurations']['druid-common']['druid.extensions.loadList']
druid_security_extensions_load_list = config['configurations']['druid-common']['druid.security.extensions.loadList']


# status params
druid_pid_dir = status_params.druid_pid_dir
user_group = config['configurations']['cluster-env']['user_group']
java8_home = config['hostLevelParams']['java_home']
druid_env_sh_template = config['configurations']['druid-env']['content']

# log4j params
log4j_props = config['configurations']['druid-log4j']['content']
druid_log_level = config['configurations']['druid-log4j']['druid_log_level']
metamx_log_level = config['configurations']['druid-log4j']['metamx_log_level']
root_log_level = config['configurations']['druid-log4j']['root_log_level']

druid_log_maxbackupindex = default('/configurations/druid-logrotate/druid_log_maxbackupindex', 7)
druid_log_maxfilesize = default('/configurations/druid-logrotate/druid_log_maxfilesize', 256)
logrotate_props = config['configurations']['druid-logrotate']['content']

# Metadata storage
metadata_storage_user = config['configurations']['druid-common']['druid.metadata.storage.connector.user']
metadata_storage_password = config['configurations']['druid-common']['druid.metadata.storage.connector.password']
metadata_storage_db_name = config['configurations']['druid-common']['database_name']
metadata_storage_db_name = config['configurations']['druid-common']['database_name']
metadata_storage_type = config['configurations']['druid-common']['druid.metadata.storage.type']
metadata_storage_url = config['configurations']['druid-common']['druid.metadata.storage.connector.connectURI']
jdk_location = config['hostLevelParams']['jdk_location']
if 'mysql' == metadata_storage_type:
  jdbc_driver_jar = default("/hostLevelParams/custom_mysql_jdbc_name", None)
  connector_curl_source = format("{jdk_location}/{jdbc_driver_jar}")
  connector_download_dir=format("{druid_extensions_dir}/mysql-metadata-storage")
  downloaded_custom_connector = format("{tmp_dir}/{jdbc_driver_jar}")

check_db_connection_jar_name = "DBConnectionVerification.jar"
check_db_connection_jar = format("/usr/lib/ambari-agent/{check_db_connection_jar_name}")

# HDFS
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', 'missing_principal').replace("_HOST",
                                                                                                             hostname)
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
dfs_type = default("/commandParams/dfs_type", "")
hdfs_tmp_dir = config['configurations']['hadoop-env']['hdfs_tmp_dir']

# Kerberos
druid_principal_name = default('/configurations/druid-common/druid.hadoop.security.kerberos.principal',
                               'missing_principal')
druid_user_keytab = default('/configurations/druid-common/druid.hadoop.security.kerberos.keytab', 'missing_keytab')

import functools

# create partial functions with common arguments for every HdfsResource call
# to create hdfs directory we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs,
  immutable_paths=get_not_managed_resources(),
  dfs_type=dfs_type
)

# Ambari Metrics
metric_emitter_type = "noop"
metric_collector_host = ""
metric_collector_port = ""
metric_collector_protocol = ""
metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0

if has_metric_collector:
    metric_emitter_type = "ambari-metrics"
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
    pass

# Create current Hadoop Clients  Libs
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
lzo_enabled = should_install_lzo()
hadoop_lib_home = stack_root + '/' + stack_version + '/hadoop/lib'
