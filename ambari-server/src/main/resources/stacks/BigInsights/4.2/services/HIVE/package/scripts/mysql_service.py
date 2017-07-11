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


def mysql_service(daemon_name=None, action='start'): 
  cnf = file('/etc/my.cnf')
  for line in cnf:
    if line.strip().startswith('pid-file'):
      pid_file = line.split('=')[1].strip()
      break
  pid_expression = "`" + "cat " + pid_file + "`"
  status_cmd = "ls " + pid_file + " >/dev/null 2>&1 && ps -p " + pid_expression + " >/dev/null 2>&1"  
  cmd = ('service', daemon_name, action)

  if action == 'status':
    try:
      Execute(status_cmd)
    except Fail:
      raise ComponentIsNotRunning()
  elif action == 'stop':
    import params
    Execute(cmd,
            logoutput = True,
            only_if = status_cmd,
            sudo = True,
    )
  elif action == 'start':
    import params   
    Execute(cmd,
      logoutput = True,
      not_if = status_cmd,
      sudo = True,
    )
    Execute(("chmod", "664", pid_file), sudo=True)




