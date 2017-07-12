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
from resource_management.core.logger import Logger

def hbase_service(
  name,
  action = 'start'): # 'start' or 'stop' or 'status'
    
    import params
  
    role = name
    cmd = format("{daemon_script} --config {hbase_conf_dir}")
    pid_file = format("{pid_dir}/hbase-{hbase_user}-{role}.pid")
    pid_expression = as_sudo(["cat", pid_file])
    no_op_test = as_sudo(["test", "-f", pid_file]) + format(" && ps -p `{pid_expression}` >/dev/null 2>&1")
    
    # delete wal log if HBase version has moved down
    if params.to_backup_wal_dir:
      wal_directory = params.wal_directory
      timestamp = datetime.datetime.now()
      timestamp_format = '%Y%m%d%H%M%S'
      wal_directory_backup = '%s_%s' % (wal_directory, timestamp.strftime(timestamp_format))

      rm_cmd = format("hadoop fs -mv {wal_directory} {wal_directory_backup}")
      try:
        Execute ( rm_cmd,
          user = params.hbase_user
        )
      except Exception, e:
        #Should still allow HBase Start/Stop to proceed
        Logger.error("Failed to backup HBase WAL directory, command: {0} . Exception: {1}".format(rm_cmd, e.message))

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
