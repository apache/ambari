#!/usr/bin/env python

'''
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
'''

from resource_management.libraries.functions import format
from resource_management.core.resources.system import File, Execute
from resource_management.libraries.functions import get_user_call_output

def livy_service(name, upgrade_type=None, action=None):
  import params

  # use the livy user to get the PID (it is protected on non-root systems)
  livy_server_pid = get_user_call_output.get_user_call_output(format("cat {livy_server_pid_file}"),
    user=params.livy_user, is_checked_call=False)[1]

  livy_server_pid = livy_server_pid.replace("\n", " ")

  process_id_exists_command = format("ls {livy_server_pid_file} >/dev/null 2>&1 && ps -p {livy_server_pid} >/dev/null 2>&1")

  if action == 'start':
    Execute(format('{livy_server_start}'),
            user=params.livy_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=process_id_exists_command
    )

  elif action == 'stop':
    Execute(format('{livy_server_stop}'),
            user=params.livy_user,
            only_if=process_id_exists_command,
            timeout=10,
            on_timeout=format("! ( {process_id_exists_command} ) || {sudo} -H -E kill -9 {livy_server_pid}"),
            environment={'JAVA_HOME': params.java_home}
            )
