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

from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Execute

def ranger_service(name, action=None):
  import params

  env_dict = {'JAVA_HOME': params.java_home}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'JAVA_HOME': params.java_home, 'LD_LIBRARY_PATH': params.ld_lib_path}

  if name == 'ranger_admin':
    no_op_test = format('ps -ef | grep proc_rangeradmin | grep -v grep')
    try:
      Execute(params.ranger_start, environment=env_dict, user=params.unix_user, not_if=no_op_test)
    except:
      show_logs(params.admin_log_dir, params.unix_user)
      raise
  elif name == 'ranger_usersync':
    no_op_test = format('ps -ef | grep proc_rangerusersync | grep -v grep')
    if params.stack_supports_usersync_non_root:
      try:
        Execute(params.usersync_start,
                environment=env_dict,
                not_if=no_op_test,
                user=params.unix_user
        )
      except:
        show_logs(params.usersync_log_dir, params.unix_user)
        raise
    else:
      # Usersync requires to be run as root for 2.2
      Execute((params.usersync_start,),
              environment={'JAVA_HOME': params.java_home},
              not_if=no_op_test,
              sudo=True
      )
  elif name == 'ranger_tagsync' and params.stack_supports_ranger_tagsync:
    no_op_test = format('ps -ef | grep proc_rangertagsync | grep -v grep')
    cmd = format('{tagsync_services_file} start')
    try:
      Execute(cmd,
        environment=env_dict,
        user=params.unix_user,
        not_if=no_op_test
      )
    except:
      show_logs(params.tagsync_log_dir, params.unix_user)
      raise