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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from resource_management.libraries.functions.decorator import retry
from resource_management.core.resources.system import File, Execute
from resource_management.core.source import Template
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.providers.hdfs_resource import WebHDFSUtil
from resource_management.libraries.providers.hdfs_resource import HdfsResourceProvider
from resource_management import is_empty
from resource_management import shell
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.libraries.functions import namenode_ha_utils

from yarn import yarn
from service import service
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from setup_ranger_yarn import setup_ranger_yarn


class Resourcemanager(Script):
  def install(self, env):
    self.install_packages(env)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service('resourcemanager', action='stop')

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name='resourcemanager')

  def refreshqueues(self, env):
    pass



@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ResourcemanagerWindows(Resourcemanager):
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    service('resourcemanager', action='start')

  def status(self, env):
    service('resourcemanager', action='status')

  def decommission(self, env):
    import params

    env.set_params(params)
    yarn_user = params.yarn_user

    yarn_refresh_cmd = format("cmd /c yarn rmadmin -refreshNodes")

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=yarn_user,
         mode="f"
    )

    if params.include_hosts:
      File(params.include_file_path,
         content=Template("include_hosts_list.j2"),
         owner=yarn_user,
         mode="f"
    )

    if params.update_files_only == False:
      Execute(yarn_refresh_cmd, user=yarn_user)



@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ResourcemanagerDefault(Resourcemanager):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade post-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env) # FOR SECURITY
    if params.enable_ranger_yarn and params.is_supported_yarn_ranger:
      setup_ranger_yarn() #Ranger Yarn Plugin related calls

    # wait for active-dir and done-dir to be created by ATS if needed
    if params.has_ats:
      Logger.info("Verifying DFS directories where ATS stores time line data for active and completed applications.")
      self.wait_for_dfs_directories_created(params.entity_groupfs_store_dir, params.entity_groupfs_active_dir)

    service('resourcemanager', action='start')

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.resourcemanager_pid_file)
    pass

  def refreshqueues(self, env):
    import params

    self.configure(env)
    env.set_params(params)

    service('resourcemanager',
            action='refreshQueues'
    )

  def decommission(self, env):
    import params

    env.set_params(params)
    rm_kinit_cmd = params.rm_kinit_cmd
    yarn_user = params.yarn_user
    conf_dir = params.hadoop_conf_dir
    user_group = params.user_group

    yarn_refresh_cmd = format("{rm_kinit_cmd} yarn --config {conf_dir} rmadmin -refreshNodes")

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=yarn_user,
         group=user_group
    )

    if params.include_hosts:
      File(params.include_file_path,
           content=Template("include_hosts_list.j2"),
           owner=yarn_user,
           group=user_group
           )

    if params.update_files_only == False:
      Execute(yarn_refresh_cmd,
            environment= {'PATH' : params.execute_path },
            user=yarn_user)
      pass
    pass

  def disable_security(self, env):
    import params
    if not params.stack_supports_zk_security:
      Logger.info("Stack doesn't support zookeeper security")
      return
    if not params.rm_zk_address:
      Logger.info("No zookeeper connection string. Skipping reverting ACL")
      return
    zkmigrator = ZkMigrator(
      params.rm_zk_address, \
      params.java_exec, \
      params.java64_home, \
      params.yarn_jaas_file, \
      params.yarn_user)
    zkmigrator.set_acls(params.rm_zk_znode, 'world:anyone:crdwa')
    zkmigrator.set_acls(params.hadoop_registry_zk_root, 'world:anyone:crdwa')
    zkmigrator.delete_node(params.rm_zk_failover_znode)

  def wait_for_dfs_directories_created(self, *dirs):
    import params

    ignored_dfs_dirs = HdfsResourceProvider.get_ignored_resources_list(params.hdfs_resource_ignore_file)

    if params.security_enabled:
      Execute(params.rm_kinit_cmd,
              user=params.yarn_user
      )
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
        user=params.hdfs_user
      )

    for dir_path in dirs:
      self.wait_for_dfs_directory_created(dir_path, ignored_dfs_dirs)


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

      nameservices = namenode_ha_utils.get_nameservices(params.hdfs_site)
      nameservice = None if not nameservices else nameservices[-1]
      
      if WebHDFSUtil.is_webhdfs_available(params.is_webhdfs_enabled, params.dfs_type):
        # check with webhdfs is much faster than executing hdfs dfs -test
        util = WebHDFSUtil(params.hdfs_site, nameservice, params.hdfs_user, params.security_enabled)
        list_status = util.run_command(dir_path, 'GETFILESTATUS', method='GET', ignore_status_codes=['404'], assertable_result=False)
        dir_exists = ('FileStatus' in list_status)
      else:
        # have to do time expensive hdfs dfs -d check.
        dfs_ret_code = shell.call(format("hdfs --config {hadoop_conf_dir} dfs -test -d " + dir_path), user=params.yarn_user)[0]
        dir_exists = not dfs_ret_code #dfs -test -d returns 0 in case the dir exists

      if not dir_exists:
        raise Fail("DFS directory '" + dir_path + "' does not exist !")
      else:
        Logger.info("DFS directory '" + dir_path + "' exists.")

  def get_log_folder(self):
    import params
    return params.yarn_log_dir
  
  def get_user(self):
    import params
    return params.yarn_user

  def get_pid_files(self):
    import status_params
    return [status_params.resourcemanager_pid_file]
  
if __name__ == "__main__":
  Resourcemanager().execute()
