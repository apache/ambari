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

from resource_management import *

class PigServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)
    
    input_file = 'passwd'
    output_file = "pigsmoke.out"
  
    cleanup_cmd = format("dfs -rmr {output_file} {input_file}")
    #cleanup put below to handle retries; if retrying there wil be a stale file that needs cleanup; exit code is fn of second command
    create_file_cmd = format("{cleanup_cmd}; hadoop dfs -put /etc/passwd {input_file} ") #TODO: inconsistent that second command needs hadoop
    test_cmd = format("fs -test -e {output_file}")
  
    ExecuteHadoop( create_file_cmd,
      tries     = 3,
      try_sleep = 5,
      user      = params.smokeuser,
      conf_dir = params.hadoop_conf_dir
    )
  
    File( '/tmp/pigSmoke.sh',
      content = StaticFile("pigSmoke.sh"),
      mode = 0755
    )
  
    Execute( "pig /tmp/pigSmoke.sh",
      tries     = 3,
      try_sleep = 5,
      path      = '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      user      = params.smokeuser,
      logoutput = True
    )
  
    ExecuteHadoop( test_cmd,
      user      = params.smokeuser,
      conf_dir = params.hadoop_conf_dir
    )
    
def main():
  import sys
  command_type = 'service_check'
  command_data_file = '/root/workspace/Pig/input.json'
  basedir = '/root/workspace/Pig/main'
  sys.argv = ["", command_type, command_data_file, basedir]
  
  PigServiceCheck().execute()
  
if __name__ == "__main__":
  #main()
  PigServiceCheck().execute()
  
