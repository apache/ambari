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
from os import path
from resource_management import *
from ganglia import generate_daemon
import ganglia
import functions
import ganglia_monitor_service


class GangliaMonitor(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)
    self.configure(env)
    
    functions.turn_off_autostart(params.gmond_service_name)
    functions.turn_off_autostart("gmetad") # since the package is installed as well

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    ganglia_monitor_service.monitor("start")

  def stop(self, env):
    ganglia_monitor_service.monitor("stop")


  def status(self, env):
    import status_params
    pid_file_name = 'gmond.pid'
    pid_file_count = 0
    pid_dir = status_params.pid_dir
    # Recursively check all existing gmond pid files
    for cur_dir, subdirs, files in os.walk(pid_dir):
      for file_name in files:
        if file_name == pid_file_name:
          pid_file = os.path.join(cur_dir, file_name)
          check_process_status(pid_file)
          pid_file_count += 1
    if pid_file_count == 0: # If no any pid file is present
      raise ComponentIsNotRunning()


  def configure(self, env):
    import params

    ganglia.groups_and_users()

    Directory(params.ganglia_conf_dir,
              owner="root",
              group=params.user_group,
              recursive=True
    )

    ganglia.config()
    
    self.generate_slave_configs()

    Directory(path.join(params.ganglia_dir, "conf.d"),
              owner="root",
              group=params.user_group
    )

    File(path.join(params.ganglia_dir, "conf.d/modgstatus.conf"),
         owner="root",
         group=params.user_group
    )
    File(path.join(params.ganglia_dir, "conf.d/multicpu.conf"),
         owner="root",
         group=params.user_group
    )
    File(path.join(params.ganglia_dir, "gmond.conf"),
         owner="root",
         group=params.user_group
    )

    if params.is_ganglia_server_host:
      self.generate_master_configs()

      if len(params.gmond_apps) != 0:
        self.generate_app_configs()
        pass
      pass


  def generate_app_configs(self):
    import params

    for gmond_app in params.gmond_apps:
      generate_daemon("gmond",
                      name=gmond_app[0],
                      role="server",
                      owner="root",
                      group=params.user_group)
      generate_daemon("gmond",
                      name = gmond_app[0],
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    pass

  def generate_slave_configs(self):
    import params

    generate_daemon("gmond",
                    name = "HDPSlaves",
                    role = "monitor",
                    owner = "root",
                    group = params.user_group)


  def generate_master_configs(self):
    import params
     
    if params.has_namenodes:
      generate_daemon("gmond",
                      name = "HDPNameNode",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_jobtracker:
      generate_daemon("gmond",
                      name = "HDPJobTracker",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_hbase_masters:
      generate_daemon("gmond",
                      name = "HDPHBaseMaster",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_resourcemanager:
      generate_daemon("gmond",
                      name = "HDPResourceManager",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_nodemanager:
      generate_daemon("gmond",
                      name = "HDPNodeManager",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_historyserver:
      generate_daemon("gmond",
                      name = "HDPHistoryServer",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_slaves:
      generate_daemon("gmond",
                      name = "HDPDataNode",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_tasktracker:
      generate_daemon("gmond",
                      name = "HDPTaskTracker",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_hbase_rs:
      generate_daemon("gmond",
                      name = "HDPHBaseRegionServer",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_nimbus_server:
      generate_daemon("gmond",
                      name = "HDPNimbus",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_supervisor_server:
      generate_daemon("gmond",
                      name = "HDPSupervisor",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_flume:
      generate_daemon("gmond",
                      name = "HDPFlumeServer",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    if params.has_journalnode:
      generate_daemon("gmond",
                      name = "HDPJournalNode",
                      role = "server",
                      owner = "root",
                      group = params.user_group)

    generate_daemon("gmond",
                    name = "HDPSlaves",
                    role = "server",
                    owner = "root",
                    group = params.user_group)


if __name__ == "__main__":
  GangliaMonitor().execute()
