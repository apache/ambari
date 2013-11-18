#!/usr/bin/env python2.6
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

from hbase import hbase
from hbase_service import hbase_service

         
class HbaseMaster(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    hbase(type='master')
    
  def start(self, env):
    import params
    env.set_params(params)

    hbase_service( 'master',
      action = 'start'
    )
    
  def stop(self, env):
    import params
    env.set_params(params)

    hbase_service( 'master',
      action = 'stop'
    )
    
def main():
  command_type = sys.argv[1] if len(sys.argv)>1 else "start"
  print "Running "+command_type
  command_data_file = '/root/workspace/HBase/input.json'
  basedir = '/root/workspace/HBase/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  HbaseMaster().execute()
  
if __name__ == "__main__":
  HbaseMaster().execute()
