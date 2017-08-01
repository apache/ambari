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


from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from ambari_commons import OSCheck, OSConst
if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
from setup_ranger_hive import setup_ranger_hive
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.logger import Logger

import hive_server_upgrade
from hive import hive
from hive_service import hive_service
from resource_management.core.resources.zkmigrator import ZkMigrator


class HiveServer(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hive(name='hiveserver2')


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerWindows(HiveServer):
  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    hive_service('hiveserver2', action='start')

  def stop(self, env):
    import params
    env.set_params(params)
    hive_service('hiveserver2', action='stop')

  def status(self, env):
    import status_params
    check_windows_service_status(status_params.hive_server_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerDefault(HiveServer):
  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY

    setup_ranger_hive(upgrade_type=upgrade_type)
    hive_service('hiveserver2', action = 'start', upgrade_type=upgrade_type)


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # always de-register the old hive instance so that ZK can route clients
    # to the newly created hive server
    try:
      if upgrade_type is not None:
        hive_server_upgrade.deregister()
    except Exception as exception:
      Logger.exception(str(exception))

    # even during rolling upgrades, Hive Server will be stopped - this is because Ambari will
    # not support the "port-change/deregister" workflow as it would impact Hive clients
    # which do not use ZK discovery.
    hive_service( 'hiveserver2', action = 'stop' )


  def status(self, env):
    import status_params
    env.set_params(status_params)

    # Recursively check all existing gmetad pid files
    check_process_status(status_params.hive_pid)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Hive Server Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

      # Copy mapreduce.tar.gz and tez.tar.gz to HDFS
      resource_created = copy_to_hdfs(
        "mapreduce",
        params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs)

      resource_created = copy_to_hdfs(
        "tez",
        params.user_group,
        params.hdfs_user,
        skip=params.sysprep_skip_copy_tarballs_hdfs) or resource_created

      if resource_created:
        params.HdfsResource(None, action="execute")

  def _base_node(self, path):
    if not path.startswith('/'):
      path = '/' + path
    try:
      return '/' + path.split('/')[1]
    except IndexError:
      return path

  def disable_security(self, env):
    import params
    zkmigrator = ZkMigrator(params.hive_zookeeper_quorum, params.java_exec, params.java64_home, params.jaas_file, params.hive_user)
    if params.hive_cluster_token_zkstore:
      zkmigrator.set_acls(self._base_node(params.hive_cluster_token_zkstore), 'world:anyone:crdwa')
    if params.hive_zk_namespace:
      zkmigrator.set_acls(
        params.hive_zk_namespace if params.hive_zk_namespace.startswith('/') else '/' + params.hive_zk_namespace,
        'world:anyone:crdwa')

  def get_log_folder(self):
    import params
    return params.hive_log_dir
  
  def get_user(self):
    import params
    return params.hive_user

  def get_pid_files(self):
    import status_params
    return [status_params.hive_pid]

if __name__ == "__main__":
  HiveServer().execute()
