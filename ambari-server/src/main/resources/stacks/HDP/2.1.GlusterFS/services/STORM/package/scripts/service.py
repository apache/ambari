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
import time


def service(
    name,
    action='start'):
  import params
  import status_params

  pid_file = status_params.pid_files[name]
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")

  if name == "logviewer":
    tries_count = 12
  else:
    tries_count = 6

  if name == 'ui':
    process_cmd = "^java.+backtype.storm.ui.core$"
  elif name == "rest_api":
    process_cmd = format("{java64_home}/bin/java -jar {rest_lib_dir}/`ls {rest_lib_dir} | grep -wE storm-rest-[0-9.-]+\.jar` server")
  else:
    process_cmd = format("^java.+backtype.storm.daemon.{name}$")

  crt_pid_cmd = format("pgrep -f \"{process_cmd}\" && pgrep -f \"{process_cmd}\" > {pid_file}")

  if action == "start":
    if name == "rest_api":
      cmd = format("{process_cmd} {rest_api_conf_file} > {log_dir}/restapi.log")
    else:
      cmd = format("env JAVA_HOME={java64_home} PATH=$PATH:{java64_home}/bin /usr/bin/storm {name} > {log_dir}/{name}.out 2>&1")

    Execute(cmd,
           not_if=no_op_test,
           user=params.storm_user,
           wait_for_finish=False
    )
    Execute(crt_pid_cmd,
            user=params.storm_user,
            logoutput=True,
            tries=tries_count,
            try_sleep=10
    )

  elif action == "stop":
    process_dont_exist = format("! ({no_op_test})")
    pid = format("`cat {pid_file}` >/dev/null 2>&1")
    Execute(format("kill {pid}"),
            not_if=process_dont_exist
    )
    Execute(format("kill -9 {pid}"),
            not_if=format("sleep 2; {process_dont_exist} || sleep 20; {process_dont_exist}"),
            ignore_failures=True
    )
    Execute(format("rm -f {pid_file}"))
