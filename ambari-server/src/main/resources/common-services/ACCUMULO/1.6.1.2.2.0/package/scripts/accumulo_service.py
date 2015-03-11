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

def accumulo_service( name,
                      action = 'start'): # 'start' or 'stop' or 'status'
    import params

    role = name
    pid_file = format("{pid_dir}/accumulo-{accumulo_user}-{role}.pid")

    pid_exists = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")

    if action == 'start':
      daemon_cmd = format("{daemon_script} {role} --address {params.hostname} > {log_dir}/accumulo-{role}.out 2>{log_dir}/accumulo-{role}.err & echo $! > {pid_file}")
      Execute ( daemon_cmd,
        not_if=pid_exists,
        user=params.accumulo_user
      )

    elif action == 'stop':
      no_pid_exists = format("! ({pid_exists})")
      try:
        if name == 'master':
          Execute(format("{daemon_script} admin stopMaster"),
                  not_if=no_pid_exists,
                  timeout=30,
                  user=params.accumulo_user
          )
        elif name == 'tserver':
          Execute(format("{daemon_script} admin stop {hostname}"),
                  not_if=no_pid_exists,
                  timeout=30,
                  user=params.accumulo_user
          )
      except:
        pass

      time.sleep(5)

      pid = format("`cat {pid_file}` >/dev/null 2>&1")
      Execute(format("kill {pid}"),
        not_if=no_pid_exists,
        user=params.accumulo_user
      )
      Execute(format("kill -9 {pid}"),
        not_if=format("sleep 2; {no_pid_exists} || sleep 20; {no_pid_exists}"),
        ignore_failures=True,
        user=params.accumulo_user
      )
      Execute(format("rm -f {pid_file}"),
        user=params.accumulo_user)
