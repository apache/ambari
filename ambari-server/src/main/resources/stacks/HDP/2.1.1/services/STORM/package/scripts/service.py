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
from resource_management.core.shell import call
import subprocess


def service(
    name,
    action='start'):
  import params
  import status_params

  pid_file = status_params.pid_files[name]
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")

  if action == "start":
    cmd = ["/usr/bin/storm", name]
    if name == "ui":
      crt_pid_cmd = format("pgrep -f \"^java.+backtype.storm.ui.core$\" > {pid_file}")
    else :
      crt_pid_cmd = format("pgrep -f \"^java.+backtype.storm.daemon.{name}$\" > {pid_file}")

    #Execute(cmd,
    #        not_if=no_op_test,
    #        user=params.storm_user
    #)

    #TODO run from storm user

    if call(no_op_test)[0]:
      subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env={"PATH":format("{java64_home}/bin:/bin")})

    Execute(crt_pid_cmd,
            logoutput=True,
            tries=6,
            try_sleep=10
    )

  elif action == "stop":
    cmd = format("kill `cat {pid_file}` >/dev/null 2>&1")
    Execute(cmd)

    Execute(format("! ({no_op_test})"),
            tries=5,
            try_sleep=3
    )
    Execute(format("rm -f {pid_file}"))
