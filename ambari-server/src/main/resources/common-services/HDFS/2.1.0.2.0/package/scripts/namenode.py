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
import os
import json
import subprocess
from datetime import datetime

from resource_management import *
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status

from hdfs_namenode import namenode
from hdfs import hdfs
import hdfs_rebalance
from utils import failover_namenode


class NameNode(Script):
  def install(self, env):
    import params

    self.install_packages(env, params.exclude_packages)
    env.set_params(params)
    #TODO we need this for HA because of manual steps
    self.configure(env)

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hadoop-hdfs-namenode {version}"))

  def start(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    self.configure(env)
    namenode(action="start", rolling_restart=rolling_restart, env=env)

  def post_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    Execute("hdfs dfsadmin -report -live",
            user=params.hdfs_principal_name if params.security_enabled else params.hdfs_user
    )

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)

    if rolling_restart and params.dfs_ha_enabled:
      if params.dfs_ha_automatic_failover_enabled:
        failover_namenode()
      else:
        raise Fail("Rolling Upgrade - dfs.ha.automatic-failover.enabled must be enabled to perform a rolling restart")

    namenode(action="stop", rolling_restart=rolling_restart, env=env)

  def configure(self, env):
    import params

    env.set_params(params)
    hdfs()
    namenode(action="configure", env=env)
    pass

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.namenode_pid_file)
    pass

  def decommission(self, env):
    import params

    env.set_params(params)
    namenode(action="decommission")
    pass
  
    
  def rebalancehdfs(self, env):
    import params
    env.set_params(params)

    name_node_parameters = json.loads( params.name_node_params )
    threshold = name_node_parameters['threshold']
    _print("Starting balancer with threshold = %s\n" % threshold)
    
    def calculateCompletePercent(first, current):
      return 1.0 - current.bytesLeftToMove/first.bytesLeftToMove
    
    
    def startRebalancingProcess(threshold):
      rebalanceCommand = format('hdfs --config {hadoop_conf_dir} balancer -threshold {threshold}')
      return as_user(rebalanceCommand, params.hdfs_user, env={'PATH': params.hadoop_bin_dir})
    
    command = startRebalancingProcess(threshold)
    
    basedir = os.path.join(env.config.basedir, 'scripts')
    if(threshold == 'DEBUG'): #FIXME TODO remove this on PROD
      basedir = os.path.join(env.config.basedir, 'scripts', 'balancer-emulator')
      command = ['python','hdfs-command.py']
    
    _print("Executing command %s\n" % command)
    
    parser = hdfs_rebalance.HdfsParser()
    proc = subprocess.Popen(
                            command, 
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            shell=True,
                            close_fds=True,
                            cwd=basedir
                           )
    for line in iter(proc.stdout.readline, ''):
      _print('[balancer] %s %s' % (str(datetime.now()), line ))
      pl = parser.parseLine(line)
      if pl:
        res = pl.toJson()
        res['completePercent'] = calculateCompletePercent(parser.initialLine, pl) 
        
        self.put_structured_out(res)
      elif parser.state == 'PROCESS_FINISED' : 
        _print('[balancer] %s %s' % (str(datetime.now()), 'Process is finished' ))
        self.put_structured_out({'completePercent' : 1})
        break
    
    proc.stdout.close()
    proc.wait()
    if proc.returncode != None and proc.returncode != 0:
      raise Fail('Hdfs rebalance process exited with error. See the log output')
      
def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()

if __name__ == "__main__":
  NameNode().execute()
