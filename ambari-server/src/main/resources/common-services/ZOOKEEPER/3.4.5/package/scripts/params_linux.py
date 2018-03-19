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
import os

from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.expect import expect

# server configurations
config_object = Script.get_config_object()
tmp_dir = Script.get_tmp_dir()

stack_version_formatted = status_params.stack_version_formatted
stack_root = status_params.stack_root

stack_name = status_params.stack_name
component_directory = status_params.component_directory

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = config_object.get_config_object("commandParams/version")

# default parameters
zk_home = "/usr"
zk_bin = "/usr/lib/zookeeper/bin"
zk_cli_shell = "/usr/lib/zookeeper/bin/zkCli.sh"
config_dir = "/etc/zookeeper/conf"
zk_smoke_out = os.path.join(tmp_dir, "zkSmoke.out")

# hadoop parameters for stacks that support rolling_upgrade
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  zk_home = format("{stack_root}/current/{component_directory}")
  zk_bin = format("{stack_root}/current/{component_directory}/bin")
  zk_cli_shell = format("{stack_root}/current/{component_directory}/bin/zkCli.sh")
  config_dir = status_params.config_dir


zk_user = config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'zk_user')
hostname = config_object.get_host_name()
user_group = config_object.get_service_config_property_value('zookeeper', 'cluster-settings', 'user_group')
zk_env_sh_template = config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'content')

zk_log_dir = config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'zk_log_dir')
zk_data_dir = config_object.get_service_config_property_value('zookeeper', 'zoo.cfg', 'dataDir')
zk_pid_dir = status_params.zk_pid_dir
zk_pid_file = status_params.zk_pid_file
zk_server_heapsize_value = str(config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'zk_server_heapsize', '1024')).strip()
if len(zk_server_heapsize_value) > 0 and zk_server_heapsize_value[-1].isdigit():
  zk_server_heapsize_value = zk_server_heapsize_value + "m"
zk_server_heapsize = format("-Xmx{zk_server_heapsize_value}")

client_port = config_object.get_service_config_property_value('zookeeper', 'zoo.cfg', 'clientPort')
# Do not use {} as default parameter here
zoo_cfg_properties_map = config_object.get_service_config_property_value('zookeeper', 'zoo.cfg', '').value()
if not zoo_cfg_properties_map:
  zoo_cfg_properties_map = {}

zoo_cfg_properties_map_length = len(zoo_cfg_properties_map)

zk_principal_name = config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'zookeeper_principal_name', 'zookeeper@EXAMPLE.COM')
zk_principal = zk_principal_name.replace('_HOST', hostname.lower())

java64_home = config_object.get_host_java_home()
java_version = expect("/hostLevelParams/java_version", int)

zookeeper_hosts = config_object.get_service_config_property_value('zookeeper', 'clusterHostInfo', 'zookeeper_hosts')
zookeeper_hosts.sort()

zk_keytab_path = config_object.get_service_config_property_value('zookeeper', 'zookeeper-env', 'zookeeper_keytab_path')
zk_server_jaas_file = format("{config_dir}/zookeeper_jaas.conf")
zk_client_jaas_file = format("{config_dir}/zookeeper_client_jaas.conf")
security_enabled = config_object.check_security_enabled()

smoke_user_keytab = config_object.get_smokeuser_keytab_path()
smokeuser = config_object.config_object.get_smokeuser()
smokeuser_principal = config_object.config_object.get_smokeuser_principal_name()
kinit_path_local = get_kinit_path(config_object.get_kinit_path())

# Zookeeper log4j settings
zookeeper_log_max_backup_size = config_object.get_service_config_property_value('zookeeper', 'zookeeper-log4j', 'zookeeper_log_max_backup_size', 10)
zookeeper_log_number_of_backup_files = config_object.get_service_config_property_value('zookeeper', 'zookeeper-log4j', 'zookeeper_log_number_of_backup_files', 10)

#log4j.properties
log4j_props = config_object.get_service_config_property_value('zookeeper', 'zookeeper-log4j', 'content')
