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
from resource_management.libraries.script import Script
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import default, format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from ambari_commons import OSCheck

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'NIMBUS' : 'storm-nimbus',
  'SUPERVISOR' : 'storm-supervisor',
  'STORM_UI_SERVER' : 'storm-client',
  'DRPC_SERVER' : 'storm-client',
  'STORM_SERVICE_CHECK' : 'storm-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "STORM_SERVICE_CHECK")

config = Script.get_config()
stack_root = Script.get_stack_root()
stack_version_unformatted = str(config['clusterLevelParams']['stack_version'])
stack_version_formatted = format_stack_version(stack_version_unformatted)

if OSCheck.is_windows_family():
  nimbus_win_service_name = "nimbus"
  supervisor_win_service_name = "supervisor"
  ui_win_service_name = "ui"
else:
  pid_dir = config['configurations']['storm-env']['storm_pid_dir']
  pid_nimbus = format("{pid_dir}/nimbus.pid")
  pid_supervisor = format("{pid_dir}/supervisor.pid")
  pid_drpc = format("{pid_dir}/drpc.pid")
  pid_ui = format("{pid_dir}/ui.pid")
  pid_logviewer = format("{pid_dir}/logviewer.pid")
  pid_rest_api = format("{pid_dir}/restapi.pid")

  pid_files = {
    "logviewer":pid_logviewer,
    "ui": pid_ui,
    "nimbus": pid_nimbus,
    "supervisor": pid_supervisor,
    "drpc": pid_drpc,
    "rest_api": pid_rest_api
  }

  # Security related/required params
  hostname = config['agentLevelParams']['hostname']
  security_enabled = config['configurations']['cluster-env']['security_enabled']
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  tmp_dir = Script.get_tmp_dir()

  storm_component_home_dir = "/usr/lib/storm"
  conf_dir = "/etc/storm/conf"
  if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
    storm_component_home_dir = format("{stack_root}/current/{component_directory}")
    conf_dir = format("{stack_root}/current/{component_directory}/conf")

  storm_user = config['configurations']['storm-env']['storm_user']
  storm_ui_principal = default('/configurations/storm-env/storm_ui_principal_name', None)
  storm_ui_keytab = default('/configurations/storm-env/storm_ui_keytab', None)

stack_name = default("/clusterLevelParams/stack_name", None)