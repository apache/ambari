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
from resource_management import *

def ranger_service(name, action=None):
  import params

  env_dict = {'JAVA_HOME': params.java_home}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'JAVA_HOME': params.java_home, 'LD_LIBRARY_PATH': params.ld_lib_path}
  
  if name == 'ranger_admin':
    no_op_test = format('ps -ef | grep proc_rangeradmin | grep -v grep')
    Execute(params.ranger_start, environment=env_dict, user=params.unix_user, not_if=no_op_test)
  elif name == 'ranger_usersync':
    no_op_test = format('ps -ef | grep proc_rangerusersync | grep -v grep')


    if params.stack_is_hdp23_or_further:
      Execute(('chown','-R', format('{unix_user}:{unix_group}'), format('{usersync_log_dir}/')), sudo=True)
      Execute(params.usersync_start,
              environment=env_dict,
              not_if=no_op_test,
              user=params.unix_user,
      )
    else:
      # Usersync requires to be run as root for 2.2
      Execute((params.usersync_start,),
              environment={'JAVA_HOME': params.java_home},
              not_if=no_op_test,
              sudo=True,
      )
