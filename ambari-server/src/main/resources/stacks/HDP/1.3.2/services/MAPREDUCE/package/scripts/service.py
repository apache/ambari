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


def service(
    name,
    action='start'):

  import params

  pid_file = format("{mapred_pid_dir}/hadoop-{mapred_user}-{name}.pid")
  hadoop_daemon = format("export HADOOP_LIBEXEC_DIR={hadoop_libexec_dir} && {hadoop_bin}/hadoop-daemon.sh")
  cmd = format("{hadoop_daemon} --config {conf_dir}")

  if action == 'start':
    daemon_cmd = format("{cmd} start {name}")
    no_op = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
    Execute(daemon_cmd,
            user=params.mapred_user,
            not_if=no_op
    )

    Execute(no_op,
            user=params.mapred_user,
            not_if=no_op,
            initial_wait=5
    )
  elif action == 'stop':
    daemon_cmd = format("{cmd} stop {name}")
    rm_pid =  format("rm -f {pid_file}")

    Execute(daemon_cmd,
            user=params.mapred_user
    )
    Execute(rm_pid)
