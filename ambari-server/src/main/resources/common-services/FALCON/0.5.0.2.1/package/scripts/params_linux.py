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
from resource_management.libraries.functions import stack_features
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

config = Script.get_config()
stack_root = status_params.stack_root
stack_name = status_params.stack_name

agent_stack_retry_on_unavailability = config['ambariLevelParams']['agent_stack_retry_on_unavailability']
agent_stack_retry_count = expect("/ambariLevelParams/agent_stack_retry_count", int)

version = stack_features.get_stack_feature_version(config)

stack_version_unformatted = status_params.stack_version_unformatted
stack_version_formatted = status_params.stack_version_formatted
upgrade_direction = default("/commandParams/upgrade_direction", None)
jdk_location = config['ambariLevelParams']['jdk_location']


etc_prefix_dir = "/etc/falcon"

# hadoop params
hadoop_home_dir = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

if check_stack_feature(StackFeature.ROLLING_UPGRADE, version):
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

  # Extensions dir is only available in HDP 2.5 and higher
  falcon_extensions_source_dir = os.path.join(stack_root, "current", falcon_root, "extensions")
  # Dir in HDFS
  falcon_extensions_dest_dir = default("/configurations/falcon-startup.properties/*.extension.store.uri", "/apps/falcon/extensions")
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

java_home = config['ambariLevelParams']['java_home']
falcon_local_dir = config['configurations']['falcon-env']['falcon_local_dir']
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
falcon_client_properties = config['configurations']['falcon-client.properties']
smokeuser_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
falcon_env_sh_template = config['configurations']['falcon-env']['content']

#Log4j properties
falcon_log_maxfilesize = default('/configurations/falcon-log4j/falcon_log_maxfilesize',256)
falcon_log_maxbackupindex =  default('/configurations/falcon-log4j/falcon_log_maxbackupindex',20)
falcon_security_log_maxfilesize = default('/configurations/falcon-log4j/falcon_security_log_maxfilesize',256)
falcon_security_log_maxbackupindex = default('/configurations/falcon-log4j/falcon_security_log_maxbackupindex',20)

falcon_log4j=config['configurations']['falcon-log4j']['content']

falcon_apps_dir = config['configurations']['falcon-env']['falcon_apps_hdfs_dir']
#for create_hdfs_directory
security_enabled = config['configurations']['cluster-env']['security_enabled']
hostname = config['agentLevelParams']['hostname']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
smokeuser_principal =  config['configurations']['cluster-env']['smokeuser_principal_name']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

supports_hive_dr = config['configurations']['falcon-env']['supports_hive_dr']
# HDP 2.4 still supported the /usr/$STACK/$VERSION/falcon/data-mirroring folder, which had to be copied to HDFS
# In HDP 2.5, an empty data-mirroring folder has to be created, and the extensions folder has to be uploaded to HDFS.
supports_data_mirroring = supports_hive_dr and not check_stack_feature(StackFeature.FALCON_EXTENSIONS, version)

local_data_mirroring_dir = format('{stack_root}/current/falcon-server/data-mirroring')
dfs_data_mirroring_dir = "/apps/data-mirroring"


########################################################
############# Atlas related params #####################
########################################################
#region Atlas Hooks
falcon_atlas_application_properties = default('/configurations/falcon-atlas-application.properties', {})
atlas_hook_filename = default('/configurations/atlas-env/metadata_conf_file', 'atlas-application.properties')
enable_atlas_hook = default('/configurations/falcon-env/falcon.atlas.hook', False)

# Calculate atlas_hook_cp to add to FALCON_EXTRA_CLASS_PATH
falcon_atlas_support = False

# Path to add to environment variable
atlas_hook_cp = ""
if enable_atlas_hook:

  # stack_version doesn't contain a minor number of the stack (only first two numbers: 2.3). Get it from the command version
  falcon_atlas_support = check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT_2_3, version) \
      or check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT, version)

  if check_stack_feature(StackFeature.ATLAS_CONF_DIR_IN_PATH, version):
    atlas_conf_dir = format('{stack_root}/current/atlas-server/conf')
    atlas_home_dir = format('{stack_root}/current/atlas-server')
    atlas_hook_cp = atlas_conf_dir + os.pathsep + os.path.join(atlas_home_dir, "hook", "falcon", "*") + os.pathsep
  elif check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, version):
    atlas_hook_cp = format('{stack_root}/current/atlas-client/hook/falcon/*') + os.pathsep

atlas_application_class_addition = ""
if falcon_atlas_support:
  # Some stack versions do not support Atlas Falcon hook. See stack_features.json
  # Packaging was different in older versions.
  if check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT_2_3, version):
    atlas_application_class_addition = ",\\\norg.apache.falcon.atlas.service.AtlasService"
    atlas_plugin_package = "atlas-metadata*-falcon-plugin"
    atlas_ubuntu_plugin_package = "atlas-metadata.*-falcon-plugin"
  else:
    atlas_application_class_addition = ",\\\norg.apache.atlas.falcon.service.AtlasService"
    atlas_plugin_package = "atlas-metadata*-hive-plugin"
    atlas_ubuntu_plugin_package = "atlas-metadata.*-hive-plugin"

#endregion

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/clusterLevelParams/dfs_type", "")

bdb_jar_name = "je-5.0.73.jar"
bdb_resource_name = format("{jdk_location}/{bdb_jar_name}")
target_jar_file = os.path.join(falcon_webinf_lib, bdb_jar_name)


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

