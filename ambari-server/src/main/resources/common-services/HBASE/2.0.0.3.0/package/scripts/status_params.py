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
from ambari_commons.os_check import OSCheck

from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'HBASE_MASTER' : 'hbase-master',
  'HBASE_REGIONSERVER' : 'hbase-regionserver',
  'HBASE_CLIENT' : 'hbase-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "HBASE_CLIENT")

config = Script.get_config()

if OSCheck.is_windows_family():
  hbase_master_win_service_name = "master"
  hbase_regionserver_win_service_name = "regionserver"
else:
  pid_dir = config['configurations']['hbase-env']['hbase_pid_dir']
  hbase_user = config['configurations']['hbase-env']['hbase_user']

  hbase_master_pid_file = format("{pid_dir}/hbase-{hbase_user}-master.pid")
  regionserver_pid_file = format("{pid_dir}/hbase-{hbase_user}-regionserver.pid")
  phoenix_pid_file = format("{pid_dir}/phoenix-{hbase_user}-queryserver.pid")

  # Security related/required params
  hostname = config['hostname']
  security_enabled = config['configurations']['cluster-env']['security_enabled']
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  tmp_dir = Script.get_tmp_dir()
  
  stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
  stack_version_formatted = format_stack_version(stack_version_unformatted)
  stack_root = Script.get_stack_root()

  hbase_conf_dir = "/etc/hbase/conf"
  limits_conf_dir = "/etc/security/limits.d"
  if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
    hbase_conf_dir = format("{stack_root}/current/{component_directory}/conf")
    
stack_name = default("/hostLevelParams/stack_name", None)
