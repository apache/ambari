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
import os
from resource_management.core import sudo
from resource_management import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.show_logs import show_logs
from druid import druid, get_daemon_cmd, getPid


class DruidBase(Script):
  def __init__(self, nodeType=None):
    Script.__init__(self)
    self.nodeType = nodeType

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    druid(upgrade_type=upgrade_type, nodeType=self.nodeType)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    node_type_lower = self.nodeType.lower()
    Logger.info(format("Executing druid-{node_type_lower} Upgrade pre-restart"))
    import params

    env.set_params(params)

    if params.stack_version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version):
      stack_select.select_packages(params.stack_version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    daemon_cmd = get_daemon_cmd(params, self.nodeType, "start")
    # Verify Database connection on Druid start
    if params.metadata_storage_type == 'mysql':
      if not params.jdbc_driver_jar or not os.path.isfile(params.connector_download_dir + os.path.sep + params.jdbc_driver_jar):
        path_to_jdbc =  params.connector_download_dir + os.path.sep + "*"
        error_message = "Error! Sorry, but we can't find jdbc driver for mysql.So, db connection check can fail." + \
                        "Please run 'ambari-server setup --jdbc-db=mysql --jdbc-driver={path_to_jdbc} on server host.'"
        Logger.error(error_message)
      else:
        path_to_jdbc = params.connector_download_dir + os.path.sep + params.jdbc_driver_jar
      db_connection_check_command = format("{params.java8_home}/bin/java -cp {params.check_db_connection_jar}:{path_to_jdbc} org.apache.ambari.server.DBConnectionVerification '{params.metadata_storage_url}' {params.metadata_storage_user} {params.metadata_storage_password!p} com.mysql.jdbc.Driver")
    else:
      db_connection_check_command = None

    if db_connection_check_command:
      sudo.chmod(params.check_db_connection_jar, 0755)
      Execute( db_connection_check_command,
               tries=5,
               try_sleep=10,
               user=params.druid_user
               )

    try:
      Execute(daemon_cmd,
              user=params.druid_user
              )
    except:
      show_logs(params.druid_log_dir, params.druid_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    daemon_cmd = get_daemon_cmd(params, self.nodeType, "stop")
    try:
      Execute(daemon_cmd,
              user=params.druid_user
              )
    except:
      show_logs(params.druid_log_dir, params.druid_user)
      raise

  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = getPid(status_params, self.nodeType)
    check_process_status(pid_file)

  def get_log_folder(self):
    import params
    return params.druid_log_dir

  def get_user(self):
    import params
    return params.druid_user
