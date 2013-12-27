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

Ambari Agent

"""

from resource_management import *

def nagios_service(action='start'): # start or stop
  import params

  if action == 'start':
   command = "service nagios start"
  elif action == 'stop':
   command = format("service nagios stop && rm -f {nagios_pid_file}")

  Execute( command,
     path    = "/usr/local/bin/:/bin/:/sbin/"      
  )
  MonitorWebserver("restart")