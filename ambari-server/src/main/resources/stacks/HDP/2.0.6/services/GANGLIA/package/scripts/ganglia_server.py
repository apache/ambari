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
import ganglia_server_service


class GangliaServer(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)
    self.configure(env)
    self.chkconfigOff()

  def start(self, env):
    import params
    env.set_params(params)
    ganglia_server_service.server("start")

  def stop(self, env):
    import params

    env.set_params(params)
    ganglia_server_service.server("stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = format("{pid_dir}/gmetad.pid")
    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)

  def configure(self, env):
    import params

    ganglia.groups_and_users()
    ganglia.config()

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
                                
    if params.ganglia_server_host == params.hostname:
      generate_daemon("gmetad",
                      name = "gmetad",
                      role = "server",
                      owner = "root",
                      group = params.user_group)
                      
      generate_daemon("gmond",
          name = "HDPSlaves",
          role = "server",
          owner = "root",
          group = params.user_group)

    change_permission()
    server_files()
    File(path.join(params.ganglia_dir, "gmetad.conf"),
         owner="root",
         group=params.user_group
    )


  def chkconfigOff(self):
    Execute("chkconfig gmetad off",
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin')


def change_permission():
  import params

  Directory('/var/lib/ganglia/dwoo',
            mode=0777,
            owner=params.gmetad_user,
            recursive=True
  )


def server_files():
  import params

  rrd_py_path = params.rrd_py_path
  Directory(rrd_py_path,
            recursive=True
  )
  rrd_py_file_path = path.join(rrd_py_path, "rrd.py")
  File(rrd_py_file_path,
       content=StaticFile("rrd.py"),
       mode=0755
  )
  rrd_file_owner = params.gmetad_user
  if params.rrdcached_default_base_dir != params.rrdcached_base_dir:
    Directory(params.rrdcached_base_dir,
              owner=rrd_file_owner,
              group=rrd_file_owner,
              mode=0755,
              recursive=True
    )
    Directory(params.rrdcached_default_base_dir,
              action = "delete"
    )
    Link(params.rrdcached_default_base_dir,
         to=params.rrdcached_base_dir
    )
  elif rrd_file_owner != 'nobody':
    Directory(params.rrdcached_default_base_dir,
              owner=rrd_file_owner,
              group=rrd_file_owner,
              recursive=True
    )


if __name__ == "__main__":
  GangliaServer().execute()
