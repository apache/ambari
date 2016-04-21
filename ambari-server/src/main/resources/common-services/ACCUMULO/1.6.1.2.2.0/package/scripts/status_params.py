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

# a map of the Ambari role to the component name
# for use with <stack-root>/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'ACCUMULO_MASTER' : 'accumulo-master',
  'ACCUMULO_MONITOR' : 'accumulo-monitor',
  'ACCUMULO_GC' : 'accumulo-gc',
  'ACCUMULO_TRACER' : 'accumulo-tracer',
  'ACCUMULO_TSERVER' : 'accumulo-tablet',
  'ACCUMULO_CLIENT' : 'accumulo-client'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "ACCUMULO_CLIENT")

config = Script.get_config()
stack_root = Script.get_stack_root()

conf_dir = format('{stack_root}/current/{component_directory}/conf')
server_conf_dir = format('{conf_dir}/server')
pid_dir = config['configurations']['accumulo-env']['accumulo_pid_dir']
accumulo_user = config['configurations']['accumulo-env']['accumulo_user']

# Security related/required params
hostname = config['hostname']
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
tmp_dir = Script.get_tmp_dir()

# stack name
stack_name = default("/hostLevelParams/stack_name", None)
