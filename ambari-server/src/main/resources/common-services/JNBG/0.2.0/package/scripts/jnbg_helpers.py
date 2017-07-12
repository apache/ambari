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

import os, pwd, grp
from resource_management.core.resources.system import Execute, File
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.show_logs import show_logs
#from resource_management.libraries.resources.hdfs_resource import HdfsResource
from ambari_commons.constants import AMBARI_SUDO_BINARY

def package_dir():
  return os.path.realpath(__file__).split('/package')[0] + '/package/'

def create_linux_user(user, group):
  sudo = AMBARI_SUDO_BINARY

  try: pwd.getpwnam(user)
  except KeyError: Execute(format("{sudo} useradd ") + user, logoutput=True)
  try: grp.getgrnam(group)
  except KeyError: Execute(format("{sudo} groupadd ") + group, logoutput=True)

def create_hdfs_dirs(user, group, dirs):
  import jnbg_params as params
  for dir, perms in dirs:
    params.HdfsResource(dir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = user,
                        group = group,
                        mode = int(perms, 8)
                       )
  params.HdfsResource(None, action="execute")
 
def stop_process(pid_file, user, log_dir):
  """
  Kill the process by pid file, then check the process is running or not.
  If the process is still running after the kill command, try to kill
  with -9 option (hard kill)
  """

  sudo = AMBARI_SUDO_BINARY
  pid = get_user_call_output(format("cat {pid_file}"), user=user, is_checked_call=False)[1]
  process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

  kill_cmd = format("{sudo} kill {pid}")
  Execute(kill_cmd, not_if=format("! ({process_id_exists_command})"))

  wait_time = 5
  hard_kill_cmd = format("{sudo} kill -9 {pid}")
  Execute(hard_kill_cmd,
          not_if=format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )"),
          ignore_failures = True)

  try:
    Execute(format("! ({process_id_exists_command})"),
            tries=20,
            try_sleep=3)
  except:
    show_logs(log_dir, user)
    raise

  File(pid_file, action="delete")
