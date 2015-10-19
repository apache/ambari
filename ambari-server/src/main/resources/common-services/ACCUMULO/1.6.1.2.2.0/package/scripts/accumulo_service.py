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
      Execute(as_sudo(['chown','-R',params.accumulo_user+":"+params.user_group,
                       format("$(getent passwd {accumulo_user} | cut -d: -f6)")],
                      auto_escape=False),
              ignore_failures=True)
      if name != 'tserver':
        Execute(format("{daemon_script} org.apache.accumulo.master.state.SetGoalState NORMAL"),
                not_if=as_user(pid_exists, params.accumulo_user),
                user=params.accumulo_user
        )
      address = params.hostname
      if name == 'monitor' and params.accumulo_monitor_bind_all:
        address = '0.0.0.0'
      daemon_cmd = format("{daemon_script} {role} --address {address} > {log_dir}/accumulo-{role}.out 2>{log_dir}/accumulo-{role}.err & echo $! > {pid_file}")
      Execute ( daemon_cmd,
        not_if=as_user(pid_exists, params.accumulo_user),
        user=params.accumulo_user
      )

    elif action == 'stop':
      no_pid_exists = format("! ({pid_exists})")

      pid = format("`cat {pid_file}` >/dev/null 2>&1")
      Execute(format("kill {pid}"),
        not_if=as_user(no_pid_exists, params.accumulo_user),
        user=params.accumulo_user
      )
      Execute(format("kill -9 {pid}"),
        not_if=as_user(format("sleep 2; {no_pid_exists} || sleep 20; {no_pid_exists}"), params.accumulo_user),
        ignore_failures=True,
        user=params.accumulo_user
      )
      Execute(format("rm -f {pid_file}"),
        user=params.accumulo_user)
