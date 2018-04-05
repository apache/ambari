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

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.script import Script
from resource_management.libraries.script.script import get_config_lock_file
from resource_management.libraries.functions import default
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.format_jvm_option import format_jvm_option_value
from resource_management.libraries.functions.version import format_stack_version, get_major_version
from resource_management.libraries.functions.cluster_settings import get_cluster_setting_value
from resource_management.libraries.execution_command import execution_command
from resource_management.libraries.execution_command import module_configs
from string import lower

execution_command = Script.get_execution_command()
module_configs = Script.get_module_configs()
module_name = execution_command.get_module_name()
tmp_dir = Script.get_tmp_dir()

dfs_type = execution_command.get_dfs_type()

is_parallel_execution_enabled = execution_command.check_agent_config_execute_in_parallel() == 1
host_sys_prepped = execution_command.is_host_system_prepared()

sudo = AMBARI_SUDO_BINARY

stack_version_formatted = execution_command.get_mpack_version() 
major_stack_version = get_major_version(stack_version_formatted)

# service name
service_name = execution_command.get_module_name()

# logsearch configuration
logsearch_logfeeder_conf = "/usr/lib/ambari-logsearch-logfeeder/conf"

agent_cache_dir = execution_command.get_agent_cache_dir()
service_package_folder = execution_command.get_module_package_folder()
logsearch_service_name = service_name.lower().replace("_", "-")
logsearch_config_file_name = 'input.config-' + logsearch_service_name + ".json"
logsearch_config_file_path = agent_cache_dir + "/" + service_package_folder + "/templates/" + logsearch_config_file_name + ".j2"
logsearch_config_file_exists = os.path.isfile(logsearch_config_file_path)

# default hadoop params
hadoop_libexec_dir = stack_select.get_hadoop_dir("libexec")

mapreduce_libs_path = "/usr/hdp/current/hadoop-mapreduce-client/*"

versioned_stack_root = '/usr/hdp/current'

#security params
security_enabled = get_cluster_setting_value('security_enabled')

#java params
java_home = execution_command.get_java_home()

#hadoop params
hdfs_log_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_log_dir_prefix')
hadoop_pid_dir_prefix = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_pid_dir_prefix')
hadoop_root_logger = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_root_logger')

jsvc_path = "/usr/lib/bigtop-utils"

hadoop_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'hadoop_heapsize')
namenode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_heapsize')
namenode_opt_newsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_newsize')
namenode_opt_maxnewsize = module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxnewsize')
namenode_opt_permsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_permsize', '128m'))
namenode_opt_maxpermsize = format_jvm_option_value(module_configs.get_property_value(module_name, 'hadoop-env', 'namenode_opt_maxpermsize', '256m'))

jtnode_opt_newsize = "200m"
jtnode_opt_maxnewsize = "200m"
jtnode_heapsize =  "1024m"
ttnode_heapsize = "1024m"

dtnode_heapsize = module_configs.get_property_value(module_name, 'hadoop-env', 'dtnode_heapsize')
mapred_pid_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_pid_dir_prefix', '/var/run/hadoop-mapreduce')
mapred_log_dir_prefix = module_configs.get_property_value(module_name, 'mapred-env', 'mapred_log_dir_prefix', '/var/log/hadoop-mapreduce')

#users and groups
hdfs_user = module_configs.get_property_value(module_name, 'hadoop-env', 'hdfs_user')
user_group = get_cluster_setting_value('user_group')

namenode_host = execution_command.get_value("clusterHostInfo/namenode_hosts", [])
has_namenode = not len(namenode_host) == 0

if has_namenode or dfs_type == 'HCFS':
  hadoop_conf_dir = conf_select.get_hadoop_conf_dir()

link_configs_lock_file = get_config_lock_file()
stack_select_lock_file = os.path.join(tmp_dir, "stack_select_lock_file")

upgrade_suspended = execution_command.is_upgrade_suspended()
