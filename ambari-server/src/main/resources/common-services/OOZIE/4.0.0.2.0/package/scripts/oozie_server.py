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

from resource_management.core import Logger
from resource_management.libraries.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import default
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING

from oozie import oozie
from oozie_service import oozie_service
from oozie_server_upgrade import OozieUpgrade

from check_oozie_server_status import check_oozie_server_status
from resource_management.core.resources.zkmigrator import ZkMigrator

class OozieServer(Script):

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params

    # The configure command doesn't actually receive the upgrade_type from Script.py, so get it from the config dictionary
    if upgrade_type is None:
      upgrade_type = Script.get_upgrade_type(default("/commandParams/upgrade_type", ""))

    if upgrade_type is not None and params.upgrade_direction == Direction.UPGRADE and params.version is not None:
      Logger.info(format("Configuring Oozie during upgrade type: {upgrade_type}, direction: {params.upgrade_direction}, and version {params.version}"))
      if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
        # In order for the "<stack-root>/current/oozie-<client/server>" point to the new version of
        # oozie, we need to create the symlinks both for server and client.
        # This is required as both need to be pointing to new installed oozie version.

        # Sets the symlink : eg: <stack-root>/current/oozie-client -> <stack-root>/a.b.c.d-<version>/oozie
        # Sets the symlink : eg: <stack-root>/current/oozie-server -> <stack-root>/a.b.c.d-<version>/oozie
        stack_select.select_packages(params.version)

    env.set_params(params)
    oozie(is_server=True)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    self.configure(env)

    # preparing the WAR file must run after configure since configure writes out
    # oozie-env.sh which is needed to have the right environment directories setup!
    if upgrade_type is not None:
      OozieUpgrade.prepare_warfile()

    oozie_service(action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    oozie_service(action='stop', upgrade_type=upgrade_type)


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_oozie_server_status()


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class OozieServerDefault(OozieServer):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    """
    Performs the tasks that should be done before an upgrade of oozie. This includes:
      - backing up configurations
      - running <stack-selector-tool> and <conf-selector-tool>
      - restoring configurations
      - preparing the libext directory
    :param env:
    :return:
    """
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version)):
      return

    Logger.info("Executing Oozie Server Stack Upgrade pre-restart")

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

    OozieUpgrade.prepare_libext_directory()

  def disable_security(self, env):
    import params
    if not params.stack_supports_zk_security:
      Logger.info("Stack doesn't support zookeeper security")
      return
    if not params.zk_connection_string:
      Logger.info("No zookeeper connection string. Skipping reverting ACL")
      return
    zkmigrator = ZkMigrator(params.zk_connection_string, params.java_exec, params.java64_home, params.jaas_file, params.oozie_user)
    zkmigrator.set_acls(params.zk_namespace if params.zk_namespace.startswith('/') else '/' + params.zk_namespace, 'world:anyone:crdwa')

  def get_log_folder(self):
    import params
    return params.oozie_log_dir
  
  def get_user(self):
    import params
    return params.oozie_user

  def get_pid_files(self):
    import status_params
    return [status_params.pid_file]


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class OozieServerWindows(OozieServer):
  pass

if __name__ == "__main__":
  OozieServer().execute()
