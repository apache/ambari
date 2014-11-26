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

Ambari Agent

"""

from resource_management import *

def zookeeper_service(action='start'):
  import params

  cmd = format("env ZOOCFGDIR={config_dir} ZOOCFG=zoo.cfg {zk_bin}/zkServer.sh")

  if action == 'start':
    daemon_cmd = format("source {config_dir}/zookeeper-env.sh ; {cmd} start")
    no_op_test = format("ls {zk_pid_file} >/dev/null 2>&1 && ps -p `cat {zk_pid_file}` >/dev/null 2>&1")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.zk_user
    )

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser};")

      Execute(kinit_cmd,
              user=params.smokeuser
      )

    # Ensure that a quorum is still formed.
    create_command = format("echo 'create /zk_test mydata' | {zk_cli_shell}")
    list_command = format("echo 'ls /' | {zk_cli_shell}")
    delete_command = format("echo 'delete /zk_test ' | {zk_cli_shell}")
    Execute(create_command,
            user=params.smokeuser)
    Execute(list_command,
            user=params.smokeuser)
    Execute(delete_command,
            user=params.smokeuser)

  elif action == 'stop':
    daemon_cmd = format("source {config_dir}/zookeeper-env.sh ; {cmd} stop")
    rm_pid = format("rm -f {zk_pid_file}")
    Execute(daemon_cmd,
            user=params.zk_user
    )
    Execute(rm_pid)
