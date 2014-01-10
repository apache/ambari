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

import sys
from resource_management import *
from yaml_config import yaml_config

         
class Supervisor(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)
    
  def configure(self, env):
    import params
    env.set_params(params)
    
    # example
    #yaml_config( "storm.yaml",
    #        #conf_dir = params.conf_dir,
    #        conf_dir = "/etc/storm/conf",
    #        configurations = params.config['configurations']['storm-site'],
    #        #owner = params.storm_user,
    #        #group = params.user_group
    #)
    
    print "Configure."
    
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)

    print "Start."
    
  def stop(self, env):
    import params
    env.set_params(params)

    print "Stop."

  def status(self, env):
    import status_params
    env.set_params(status_params)
    
    #pid_file = format("{pid_dir}/?.pid")
    #check_process_status(pid_file)

# for testing
def main():
  command_type = "install"
  command_data_file = '/root/storm.json'
  basedir = '/root/ambari/ambari-server/src/main/resources/stacks/HDP/2.0.8/services/STORM/package'
  stroutputf = '/1.txt'
  sys.argv = ["", command_type, command_data_file, basedir, stroutputf]
  
  Supervisor().execute()
  
if __name__ == "__main__":
  Supervisor().execute()
  #main()
