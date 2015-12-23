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

import os

from resource_management.core.resources import Execute
from resource_management.core.resources import File
from resource_management.core.shell import as_user
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_user_call_output
import time


def service(name, action = 'start'):
  import params
  import status_params

  pid_file = status_params.pid_files[name]
  no_op_test = as_user(format(
    "ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1"), user=params.storm_user)
  tries_count = 12

  if name == 'ui':
    process_grep = "backtype.storm.ui.core$"
  elif name == "rest_api":
    process_grep = format("{rest_lib_dir}/storm-rest-.*\.jar$")
  else:
    process_grep = format("storm.daemon.{name}$")

  find_proc = format("{jps_binary} -l  | grep {process_grep}")
  write_pid = format("{find_proc} | awk {{'print $1'}} > {pid_file}")
  crt_pid_cmd = format("{find_proc} && {write_pid}")
  storm_env = format(
    "source {conf_dir}/storm-env.sh ; export PATH=$JAVA_HOME/bin:$PATH")

  if action == "start":
    if name == "rest_api":
      process_cmd = format(
        "{storm_env} ; java -jar {rest_lib_dir}/`ls {rest_lib_dir} | grep -wE storm-rest-[0-9.-]+\.jar` server")
      cmd = format(
        "{process_cmd} {rest_api_conf_file} > {log_dir}/restapi.log 2>&1")
    else:
      cmd = format("{storm_env} ; storm {name} > {log_dir}/{name}.out 2>&1")

    Execute(cmd,
      not_if = no_op_test,
      user = params.storm_user,
      wait_for_finish = False,
      path = params.storm_bin_dir)

    Execute(crt_pid_cmd,
      user = params.storm_user,
      logoutput = True,
      tries = tries_count,
      try_sleep = 10,
      path = params.storm_bin_dir)

  elif action == "stop":
    process_dont_exist = format("! ({no_op_test})")
    if os.path.exists(pid_file):
      pid = get_user_call_output.get_user_call_output(format("! test -f {pid_file} ||  cat {pid_file}"), user=params.storm_user)[1]

      # if multiple processes are running (for example user can start logviewer from console)
      # there can be more than one id
      pid = pid.replace("\n", " ")

      Execute(format("{sudo} kill {pid}"),
        not_if = process_dont_exist)

      Execute(format("{sudo} kill -9 {pid}"),
        not_if = format(
          "sleep 2; {process_dont_exist} || sleep 20; {process_dont_exist}"),
        ignore_failures = True)

      File(pid_file, action = "delete")
