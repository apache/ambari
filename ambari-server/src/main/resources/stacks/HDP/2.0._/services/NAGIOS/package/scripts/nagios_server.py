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

import sys
from resource_management import *
from nagios import nagios
from nagios_service import nagios_service

         
class NagiosServer(Script):
  def install(self, env):
    remove_conflicting_packages()
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)
    nagios()

    
  def start(self, env):
    import params
    env.set_params(params)
    
    nagios_service(action='start')

    
  def stop(self, env):
    import params
    env.set_params(params)
    
    nagios_service(action='stop')
    
def remove_conflicting_packages():  
  Package( 'hdp_mon_nagios_addons',
    action = "remove"
  )

  Package( 'nagios-plugins',
    action = "remove"
  )

  Execute( "rpm -e --allmatches --nopostun nagios",
    path    = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
    ignore_failures = True 
  )

def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "stop"
  print "Running "+command_type
  command_data_file = '/root/workspace/Nagios/input.json'
  basedir = '/root/workspace/Nagios/main'
  stroutfile = '/1.txt'
  sys.argv = ["", command_type, command_data_file, basedir, stroutfile]
  
  NagiosServer().execute()
  
if __name__ == "__main__":
  #main()
  NagiosServer().execute()