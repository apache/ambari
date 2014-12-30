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


def server(action=None):# 'start' or 'stop'
  command = as_sudo(('service', 'hdp-gmetad', action)) + " >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1"
  Execute(command,
          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
  )
  MonitorWebserver("restart")
