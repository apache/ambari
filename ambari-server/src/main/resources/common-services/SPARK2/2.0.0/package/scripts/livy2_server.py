#!/usr/bin/python
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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.libraries.providers.hdfs_resource import WebHDFSUtil
from resource_management.libraries.providers.hdfs_resource import HdfsResourceProvider
from resource_management import is_empty
from resource_management import shell
from resource_management.libraries.functions.decorator import retry
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import conf_select, stack_select

from livy2_service import livy2_service
from setup_livy2 import setup_livy

class LivyServer(Script):

  def install(self, env):
    import params
    env.set_params(params)

    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_livy(env, 'server', upgrade_type=upgrade_type, action = 'config')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.has_ats and params.has_livyserver:
      Logger.info("Verifying DFS directories where ATS stores time line data for active and completed applications.")
      self.wait_for_dfs_directories_created([params.entity_groupfs_store_dir, params.entity_groupfs_active_dir])

    self.configure(env)
    livy2_service('server', upgrade_type=upgrade_type, action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    livy2_service('server', upgrade_type=upgrade_type, action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.livy2_server_pid_file)

  #  TODO move out and compose with similar method in resourcemanager.py
  def wait_for_dfs_directories_created(self, dirs):
    import params

    ignored_dfs_dirs = HdfsResourceProvider.get_ignored_resources_list(params.hdfs_resource_ignore_file)

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {livy_kerberos_keytab} {livy2_principal}"),
              user=params.livy2_user
              )
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
              user=params.hdfs_user
              )

    for dir_path in dirs:
        self.wait_for_dfs_directory_created(dir_path, ignored_dfs_dirs)

  def get_pid_files(self):
    import status_params
    return [status_params.livy2_server_pid_file]


  @retry(times=8, sleep_time=20, backoff_factor=1, err_class=Fail)
  def wait_for_dfs_directory_created(self, dir_path, ignored_dfs_dirs):
    import params

    if not is_empty(dir_path):
      dir_path = HdfsResourceProvider.parse_path(dir_path)

      if dir_path in ignored_dfs_dirs:
        Logger.info("Skipping DFS directory '" + dir_path + "' as it's marked to be ignored.")
        return

      Logger.info("Verifying if DFS directory '" + dir_path + "' exists.")

      dir_exists = None

      if WebHDFSUtil.is_webhdfs_available(params.is_webhdfs_enabled, params.default_fs):
        # check with webhdfs is much faster than executing hdfs dfs -test
        util = WebHDFSUtil(params.hdfs_site, params.hdfs_user, params.security_enabled)
        list_status = util.run_command(dir_path, 'GETFILESTATUS', method='GET', ignore_status_codes=['404'], assertable_result=False)
        dir_exists = ('FileStatus' in list_status)
      else:
        # have to do time expensive hdfs dfs -d check.
        dfs_ret_code = shell.call(format("hdfs --config {hadoop_conf_dir} dfs -test -d " + dir_path), user=params.livy_user)[0]
        dir_exists = not dfs_ret_code #dfs -test -d returns 0 in case the dir exists

      if not dir_exists:
        raise Fail("DFS directory '" + dir_path + "' does not exist !")
      else:
        Logger.info("DFS directory '" + dir_path + "' exists.")

  def get_component_name(self):
    return "livy2-server"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      Logger.info("Executing Livy2 Server Stack Upgrade pre-restart")
      conf_select.select(params.stack_name, "spark2", params.version)
      stack_select.select("livy2-server", params.version)

  def get_log_folder(self):
    import params
    return params.livy2_log_dir

  def get_user(self):
    import params
    return params.livy2_user
if __name__ == "__main__":
    LivyServer().execute()

