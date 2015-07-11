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
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script

# a map of the Ambari role to the component name
# for use with /usr/hdp/current/<component>
SERVER_ROLE_DIRECTORY_MAP = {
  'OOZIE_SERVER' : 'oozie-server',
  'OOZIE_CLIENT' : 'oozie-client',
  'OOZIE_SERVICE_CHECK' : 'oozie-client',
  'ru_execute_tasks' : 'oozie-server'
}

component_directory = Script.get_component_from_role(SERVER_ROLE_DIRECTORY_MAP, "OOZIE_CLIENT")

config = Script.get_config()

if OSCheck.is_windows_family():
  # windows service mapping
  oozie_server_win_service_name = "oozieservice"
else:
  oozie_pid_dir = config['configurations']['oozie-env']['oozie_pid_dir']
  pid_file = format("{oozie_pid_dir}/oozie.pid")

  security_enabled = config['configurations']['cluster-env']['security_enabled']
  kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))

  conf_dir = "/etc/oozie/conf"
  if Script.is_hdp_stack_greater_or_equal("2.2"):
    conf_dir = format("/usr/hdp/current/{component_directory}/conf")

  tmp_dir = Script.get_tmp_dir()
  oozie_user = config['configurations']['oozie-env']['oozie_user']
  hostname = config["hostname"]
