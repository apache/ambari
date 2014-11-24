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

from postgresql_service import postgresql_service

class PostgreSQLServer(Script):

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)

    # init the database, the ':' makes the command always return 0 in case the database has
    # already been initialized when the postgresql server colocates with ambari server
    Execute(format("service {postgresql_daemon_name} initdb || :"))

    # update the configuration files
    self.update_pghda_conf(env)
    self.update_postgresql_conf(env)

    # Reload the settings and start the postgresql server for the changes to take effect
    # Note: Don't restart the postgresql server because when Ambari server and the hive metastore on the same machine,
    # they will share the same postgresql server instance. Restarting the postgresql database may cause the ambari server database connection lost
    postgresql_service(postgresql_daemon_name=params.postgresql_daemon_name, action = 'reload')

    # ensure the postgresql server is started because the add hive metastore user requires the server is running.
    self.start(env)

    # create the database and hive_metastore_user
    File(params.postgresql_adduser_path,
         mode=0755,
         content=StaticFile(format("{postgresql_adduser_file}"))
    )

    cmd = format("bash -x {postgresql_adduser_path} {postgresql_daemon_name} {hive_metastore_user_name} {hive_metastore_user_passwd!p} {db_name}")

    Execute(cmd,
            tries=3,
            try_sleep=5,
            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    )

  def start(self, env):
    import params
    env.set_params(params)

    postgresql_service(postgresql_daemon_name=params.postgresql_daemon_name, action = 'start')

  def stop(self, env):
    import params
    env.set_params(params)

    postgresql_service(postgresql_daemon_name=params.postgresql_daemon_name, action = 'stop')

  def status(self, env):
    import status_params
    postgresql_service(postgresql_daemon_name=status_params.postgresql_daemon_name, action = 'status')

  def update_postgresql_conf(self, env):
    import params
    env.set_params(params)

    # change the listen_address to *
    Execute(format("sed -i '/^[[:space:]]*listen_addresses[[:space:]]*=.*/d' {postgresql_conf_path}"))
    Execute(format("echo \"listen_addresses = '*'\" | tee -a {postgresql_conf_path}"))

    # change the standard_conforming_string to off
    Execute(format("sed -i '/^[[:space:]]*standard_conforming_strings[[:space:]]*=.*/d' {postgresql_conf_path}"))
    Execute(format("echo \"standard_conforming_strings = off\" | tee -a {postgresql_conf_path}"))

  def update_pghda_conf(self, env):
    import params
    env.set_params(params)

    # trust hive_metastore_user and postgres locally
    Execute(format("sed -i '/^[[:space:]]*local[[:space:]]*all[[:space:]]*all.*$/s/^/#/' {postgresql_pghba_conf_path}"))
    Execute(format("sed -i '/^[[:space:]]*local[[:space:]]*all[[:space:]]*postgres.*$/d' {postgresql_pghba_conf_path}"))
    Execute(format("sed -i '/^[[:space:]]*local[[:space:]]*all[[:space:]]*\"{hive_metastore_user_name}\".*$/d' {postgresql_pghba_conf_path}"))
    Execute(format("echo \"local   all   postgres   trust\" | tee -a {postgresql_pghba_conf_path}"))
    Execute(format("echo \"local   all   \\\"{hive_metastore_user_name}\\\" trust\" | tee -a {postgresql_pghba_conf_path}"))

    # trust hive_metastore_user and postgres via local interface
    Execute(format("sed -i '/^[[:space:]]*host[[:space:]]*all[[:space:]]*all.*$/s/^/#/' {postgresql_pghba_conf_path}"))
    Execute(format("sed -i '/^[[:space:]]*host[[:space:]]*all[[:space:]]*postgres.*$/d' {postgresql_pghba_conf_path}"))
    Execute(format("sed -i '/^[[:space:]]*host[[:space:]]*all[[:space:]]*\"{hive_metastore_user_name}\".*$/d' {postgresql_pghba_conf_path}"))
    Execute(format("echo \"host    all   postgres         0.0.0.0/0       trust\" | tee -a {postgresql_pghba_conf_path}"))
    Execute(format("echo \"host    all   \\\"{hive_metastore_user_name}\\\"         0.0.0.0/0       trust\" | tee -a {postgresql_pghba_conf_path}"))

if __name__ == "__main__":
  PostgreSQLServer().execute()
