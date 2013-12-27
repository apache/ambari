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
import ganglia_monitor_service


class GangliaMonitor(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)
    self.config(env)

  def start(self, env):
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


  def config(self, env):
    import params

    ganglia.groups_and_users()

    Directory(params.ganglia_conf_dir,
              owner="root",
              group=params.user_group,
              recursive=True
    )

    ganglia.config()

    if params.is_namenode_master:
      generate_daemon("gmond",
                      name = "HDPNameNode",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    if params.is_jtnode_master:
      generate_daemon("gmond",
                      name = "HDPJobTracker",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    if params.is_rmnode_master:
      generate_daemon("gmond",
                      name = "HDPResourceManager",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    if params.is_hsnode_master:
      generate_daemon("gmond",
                      name = "HDPHistoryServer",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    if params.is_hbase_master:
      generate_daemon("gmond",
                      name = "HDPHBaseMaster",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)
    not_any_master = params.is_namenode_master == False and \
                     params.is_jtnode_master == False and \
                     params.is_rmnode_master == False and \
                     params.is_hsnode_master == False and \
                     params.is_hbase_master == False
    if params.is_slave or not_any_master:
      generate_daemon("gmond",
                      name = "HDPSlaves",
                      role = "monitor",
                      owner = "root",
                      group = params.user_group)

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


if __name__ == "__main__":
  GangliaMonitor().execute()
