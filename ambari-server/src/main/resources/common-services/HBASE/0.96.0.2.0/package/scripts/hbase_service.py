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
from resource_management.core.shell import as_sudo
from resource_management.core.resources.system import Execute, File

def hbase_service(
  name,
  action = 'start'): # 'start' or 'stop' or 'status'
    
    import params
  
    role = name
    cmd = format("{daemon_script} --config {hbase_conf_dir}")
    pid_file = format("{pid_dir}/hbase-{hbase_user}-{role}.pid")
    pid_expression = as_sudo(["cat", pid_file])
    no_op_test = as_sudo(["test", "-f", pid_file]) + format(" && ps -p `{pid_expression}` >/dev/null 2>&1")
    
    if action == 'start':
      daemon_cmd = format("{cmd} start {role}")
      
      try:
        Execute ( daemon_cmd,
          not_if = no_op_test,
          user = params.hbase_user
        )
      except:
        show_logs(params.log_dir, params.hbase_user)
        raise
    elif action == 'stop':
      daemon_cmd = format("{cmd} stop {role}")

      try:
        Execute ( daemon_cmd,
          user = params.hbase_user,
          only_if = no_op_test,
          # BUGFIX: hbase regionserver sometimes hangs when nn is in safemode
          timeout = params.hbase_regionserver_shutdown_timeout,
          on_timeout = format("! ( {no_op_test} ) || {sudo} -H -E kill -9 `{pid_expression}`"),
        )
      except:
        show_logs(params.log_dir, params.hbase_user)
        raise
      
      File(pid_file,
           action = "delete",
      )
