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

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
import os
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

config = Script.get_config()
stack_root = status_params.stack_root
stack_name = status_params.stack_name

agent_stack_retry_on_unavailability = config['hostLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/hostLevelParams/agent_stack_retry_count", int)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted = status_params.stack_version_formatted
etc_prefix_dir = "/etc/falcon"

# hadoop params
hadoop_home_dir = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  # if this is a server action, then use the server binaries; smoke tests
  # use the client binaries
  server_role_dir_mapping = { 'FALCON_SERVER' : 'falcon-server',
    'FALCON_SERVICE_CHECK' : 'falcon-client' }

  command_role = default("/role", "")
  if command_role not in server_role_dir_mapping:
    command_role = 'FALCON_SERVICE_CHECK'

  falcon_root = server_role_dir_mapping[command_role]
  falcon_webapp_dir = format('{stack_root}/current/{falcon_root}/webapp')
  falcon_home = format('{stack_root}/current/{falcon_root}')
else:
  falcon_webapp_dir = '/var/lib/falcon/webapp'
  falcon_home = '/usr/lib/falcon'

falcon_webinf_lib = falcon_home + "/server/webapp/falcon/WEB-INF/lib"

hadoop_conf_dir = status_params.hadoop_conf_dir
falcon_conf_dir = status_params.falcon_conf_dir
oozie_user = config['configurations']['oozie-env']['oozie_user']
falcon_user = config['configurations']['falcon-env']['falcon_user']
smoke_user = config['configurations']['cluster-env']['smokeuser']

server_pid_file = status_params.server_pid_file

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

java_home = config['hostLevelParams']['java_home']
falcon_local_dir = config['configurations']['falcon-env']['falcon_local_dir']
falcon_store_uri = config['configurations']['falcon-env']['falcon_store_uri']
falcon_log_dir = config['configurations']['falcon-env']['falcon_log_dir']

# falcon-startup.properties
store_uri = config['configurations']['falcon-startup.properties']['*.config.store.uri']
# If these properties are present, the directories need to be created.
falcon_graph_storage_directory = default("/configurations/falcon-startup.properties/*.falcon.graph.storage.directory", None)  # explicitly set in HDP 2.2 and higher
falcon_graph_serialize_path = default("/configurations/falcon-startup.properties/*.falcon.graph.serialize.path", None)        # explicitly set in HDP 2.2 and higher

falcon_embeddedmq_data = config['configurations']['falcon-env']['falcon.embeddedmq.data']
falcon_embeddedmq_enabled = config['configurations']['falcon-env']['falcon.embeddedmq']
falcon_emeddedmq_port = config['configurations']['falcon-env']['falcon.emeddedmq.port']

falcon_host = config['clusterHostInfo']['falcon_server_hosts'][0]
falcon_port = config['configurations']['falcon-env']['falcon_port']
falcon_runtime_properties = config['configurations']['falcon-runtime.properties']
falcon_startup_properties = config['configurations']['falcon-startup.properties']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
falcon_env_sh_template = config['configurations']['falcon-env']['content']

falcon_apps_dir = config['configurations']['falcon-env']['falcon_apps_hdfs_dir']
#for create_hdfs_directory
security_enabled = config['configurations']['cluster-env']['security_enabled']
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

supports_hive_dr = config['configurations']['falcon-env']['supports_hive_dr']
local_data_mirroring_dir = format('{stack_root}/current/falcon-server/data-mirroring')
dfs_data_mirroring_dir = "/apps/data-mirroring"

atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
has_atlas = len(atlas_hosts) > 0
atlas_plugin_package = "atlas-metadata*-hive-plugin"
atlas_ubuntu_plugin_package = "atlas-metadata.*-hive-plugin"

if has_atlas:
  atlas_conf_file = config['configurations']['atlas-env']['metadata_conf_file']
  atlas_conf_dir = os.environ['METADATA_CONF'] if 'METADATA_CONF' in os.environ else '/etc/atlas/conf'
  atlas_home_dir = os.environ['METADATA_HOME_DIR'] if 'METADATA_HOME_DIR' in os.environ else format('{stack_root}/current/atlas-server')
  atlas_hook_cp = atlas_conf_dir + os.pathsep + os.path.join(atlas_home_dir, "hook", "falcon", "*") + os.pathsep

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

