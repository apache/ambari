#!/usr/bin/env python3
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

import grp
import pwd

from resource_management import *
from resource_management.libraries.resources.xml_config import XmlConfig


class HbaseRestGateway(Script):
  def configure(self, env):
    import params
    Logger.info("Configure Hbase Http service")

    Directory(params.pid_dir,
              owner=params.hbase_user,
              group=params.user_group,
              create_parents=True,
              mode=0o755,
              )

    Directory(params.log_dir,
              owner=params.hbase_user,
              group=params.user_group,
              create_parents=True,
              mode=0o755,
              )

    XmlConfig("hbase-site.xml",
              conf_dir=params.hbase_conf_dir,
              configurations=params.config['configurations']['hbase-site'],
              configuration_attributes=params.config['configurationAttributes']['hbase-site']
              )

  def install(self, env):
    import params
    self.install_packages(env)

  def stop(self, env):
    import params
    Logger.info("Stopping HttpFS service")
    command = format("{params.daemon_script} stop rest")
    Execute(
      command,
      user=params.hbase_user,
      logoutput=True)

    File(params.hbase_rest_pid_file,
         action="delete",
         owner=params.hbase_user
         )

  def start(self, env):
    import params
    self.configure(env)
    command = format("{params.daemon_script} start rest")
    Execute(
      command,
      user=params.hbase_user,
      logoutput=True)
    Logger.info("Starting HttpFS service")

  def status(self, env):
    import params
    check_process_status(params.hbase_rest_pid_file)


if __name__ == "__main__":
  HbaseRestGateway().execute()
