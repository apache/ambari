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


def service(componentName, action='start', serviceName='yarn'):

  import params

  if serviceName == 'mapreduce' and componentName == 'historyserver':
    daemon = format("{mapred_bin}/mr-jobhistory-daemon.sh")
    pid_file = format("{mapred_pid_dir}/mapred-{mapred_user}-{componentName}.pid")
    usr = params.mapred_user
  else:
    daemon = format("{yarn_bin}/yarn-daemon.sh")
    pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-{componentName}.pid")
    usr = params.yarn_user

  cmd = format("export HADOOP_LIBEXEC_DIR={hadoop_libexec_dir} && {daemon} --config {hadoop_conf_dir}")

  if action == 'start':
    daemon_cmd = format("{ulimit_cmd} {cmd} start {componentName}")
    check_process = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")

    # Remove the pid file if its corresponding process is not running.
    File(pid_file,
         action="delete",
         not_if=check_process)

    # Attempt to start the process. Internally, this is skipped if the process is already running.
    Execute(daemon_cmd,
            user=usr,
            not_if=check_process
    )

    # Ensure that the process with the expected PID exists.
    Execute(check_process,
            user=usr,
            not_if=check_process,
            initial_wait=5
    )

  elif action == 'stop':
    daemon_cmd = format("{cmd} stop {componentName}")
    Execute(daemon_cmd,
            user=usr)

    File(pid_file,
         action="delete")

  elif action == 'refreshQueues':
    refresh_cmd = format("export HADOOP_LIBEXEC_DIR={hadoop_libexec_dir} && {yarn_container_bin}/yarn rmadmin -refreshQueues")

    Execute(refresh_cmd,
            user=usr,
    )
