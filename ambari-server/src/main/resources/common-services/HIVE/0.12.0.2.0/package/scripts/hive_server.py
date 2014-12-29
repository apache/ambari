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
import hive_server_upgrade

from resource_management import *
from hive import hive
from hive_service import hive_service
from install_jars import install_tez_jars
from resource_management.libraries.functions.version import compare_versions

class HiveServer(Script):

  def install(self, env):
    import params
    self.install_packages(env, exclude_packages=params.hive_exclude_packages)


  def configure(self, env):
    import params
    env.set_params(params)
    
    if not (params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, '2.2') >=0):
      install_tez_jars()

    hive(name='hiveserver2')


  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY

    hive_service( 'hiveserver2', action = 'start',
      rolling_restart=rolling_restart )


  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)

    if rolling_restart:
      hive_server_upgrade.pre_upgrade_deregister()
    else:
      hive_service( 'hiveserver2', action = 'stop' )


  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = format("{hive_pid_dir}/{hive_pid}")

    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)


  def pre_rolling_restart(self, env):
    Logger.info("Executing HiveServer2 Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hive-server2 {version}"))


if __name__ == "__main__":
  HiveServer().execute()
