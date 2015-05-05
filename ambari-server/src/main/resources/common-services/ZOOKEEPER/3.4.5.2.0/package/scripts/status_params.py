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
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'ZOOKEEPER_SERVER' : 'zookeeper-server',
  'ZOOKEEPER_CLIENT' : 'zookeeper-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "ZOOKEEPER_CLIENT")

config = Script.get_config()

if OSCheck.is_windows_family():
  zookeeper_win_service_name = "zkServer"
else:
  zk_pid_dir = config['configurations']['zookeeper-env']['zk_pid_dir']
  zk_pid_file = format("{zk_pid_dir}/zookeeper_server.pid")

  # Security related/required params
  hostname = config['hostname']
  security_enabled = config['configurations']['cluster-env']['security_enabled']
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  tmp_dir = Script.get_tmp_dir()
  zk_user =  config['configurations']['zookeeper-env']['zk_user']

  config_dir = "/etc/zookeeper/conf"
  if Script.is_hdp_stack_greater_or_equal("2.2"):
    config_dir = format("/usr/hdp/current/{component_directory}/conf")
