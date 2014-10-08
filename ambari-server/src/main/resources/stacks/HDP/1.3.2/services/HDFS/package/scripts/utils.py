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


def service(action=None, name=None, user=None, create_pid_dir=False,
            create_log_dir=False):
  import params

  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  log_dir = format("{hdfs_log_dir_prefix}/{user}")
  check_process = format(
    "ls {pid_file} >/dev/null 2>&1 &&"
    " ps `cat {pid_file}` >/dev/null 2>&1")
  hadoop_daemon = format(
    "export HADOOP_LIBEXEC_DIR={hadoop_libexec_dir} && "
    "{hadoop_bin}/hadoop-daemon.sh")
  cmd = format("{hadoop_daemon} --config {hadoop_conf_dir}")

  if create_pid_dir:
    Directory(pid_dir,
              owner=user,
              recursive=True)
  if create_log_dir:
    Directory(log_dir,
              owner=user,
              recursive=True)

  if params.security_enabled and name == "datanode":
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")

  daemon_cmd = format("{ulimit_cmd} su -s /bin/bash - {user} -c '{cmd} {action} {name}'")

  service_is_up = check_process if action == "start" else None
  #remove pid file from dead process
  File(pid_file,
       action="delete",
       not_if=check_process,
  )
  Execute(daemon_cmd,
          not_if=service_is_up
  )
  if action == "stop":
    File(pid_file,
         action="delete",
    )