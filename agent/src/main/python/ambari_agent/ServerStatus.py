#!/usr/bin/env python

'''
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
'''

from shell import shellRunner
import logging
import logging.handlers

logger = logging.getLogger()
global serverTracker

class ServerStatus:
  def build(self):
    sh = shellRunner()
    list = []
    servers = sh.getServerTracker()
    for server in servers:
      (component, role) = server.split(".")
      result = {
                 'component' : component,
                 'role' : role,
                 'state' : 'STARTED'
               }
      list.append(result)
    return list

def main(argv=None):
  serverStatus = ServerStatus()
  print serverStatus.build()

if __name__ == '__main__':
  main()
