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
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from ambari_commons import OSCheck
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature


config = Script.get_config()
stack_root = Script.get_stack_root()
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

if OSCheck.is_windows_family():
  knox_gateway_win_service_name = "gateway"
  knox_ldap_win_service_name = "ldap"
else:
  knox_conf_dir = '/etc/knox/conf'
  if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
    knox_conf_dir = format('{stack_root}/current/knox-server/conf')
  knox_pid_dir = config['configurations']['knox-env']['knox_pid_dir']
  knox_pid_file = format("{knox_pid_dir}/gateway.pid")
  ldap_pid_file = format("{knox_pid_dir}/ldap.pid")

  security_enabled = config['configurations']['cluster-env']['security_enabled']
  if security_enabled:
      knox_keytab_path = config['configurations']['knox-env']['knox_keytab_path']
      knox_principal_name = config['configurations']['knox-env']['knox_principal_name']
  else:
      knox_keytab_path = None
      knox_principal_name = None

  hostname = config['hostname'].lower()
  knox_user = default("/configurations/knox-env/knox_user", "knox")
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  temp_dir = Script.get_tmp_dir()
  
stack_name = default("/hostLevelParams/stack_name", None)