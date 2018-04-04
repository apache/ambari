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
# this is needed to avoid a circular dependency since utils.py calls this class
import utils
from hdfs import hdfs

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory
from resource_management.core.resources.service import Service
from resource_management.core import shell
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script import Script
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import Fail, ComponentIsNotRunning
from resource_management.core.resources.system import Execute


class ZkfcSlave(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    
  def configure(env):
    ZkfcSlave.configure_static(env)
    
  @staticmethod
  def configure_static(env):
    import params
    env.set_params(params)
    hdfs("zkfc_slave")
    utils.set_up_zkfc_security(params)
    pass

  def format(self, env):
    import params
    env.set_params(params)

    utils.set_up_zkfc_security(params)

    Execute("hdfs zkfc -formatZK -nonInteractive",
            returns=[0, 2], # Returns 0 on success ; Returns 2 if zkfc is already formatted
            user=params.hdfs_user,
            logoutput=True
    )

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZkfcSlaveDefault(ZkfcSlave):

  def start(self, env, upgrade_type=None):
    ZkfcSlaveDefault.start_static(env, upgrade_type)
    
  @staticmethod
  def start_static(env, upgrade_type=None):
    import params

    env.set_params(params)
    ZkfcSlave.configure_static(env)
    Directory(params.hadoop_pid_dir_prefix,
              mode=0755,
              owner=params.hdfs_user,
              group=params.user_group
    )

    # format the znode for this HA setup
    # only run this format command if the active namenode hostname is set
    # The Ambari UI HA Wizard prompts the user to run this command
    # manually, so this guarantees it is only run in the Blueprints case
    if params.dfs_ha_enabled and \
       params.dfs_ha_namenode_active is not None:
      success =  initialize_ha_zookeeper(params)
      if not success:
        raise Fail("Could not initialize HA state in zookeeper")

    utils.service(
      action="start", name="zkfc", user=params.hdfs_user, create_pid_dir=True,
      create_log_dir=True
    )
  
  def stop(self, env, upgrade_type=None):
    ZkfcSlaveDefault.stop_static(env, upgrade_type)

  @staticmethod
  def stop_static(env, upgrade_type=None):
    import params

    env.set_params(params)
    utils.service(
      action="stop", name="zkfc", user=params.hdfs_user, create_pid_dir=True,
      create_log_dir=True
    )


  def status(self, env):
    ZkfcSlaveDefault.status_static(env)
    
  @staticmethod
  def status_static(env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.zkfc_pid_file)

  def disable_security(self, env):
    import params

    if not params.stack_supports_zk_security:
      return

    zkmigrator = ZkMigrator(params.ha_zookeeper_quorum, params.java_exec, params.java_home, params.jaas_file, params.hdfs_user)
    zkmigrator.set_acls(params.zk_namespace if params.zk_namespace.startswith('/') else '/' + params.zk_namespace, 'world:anyone:crdwa')

  def get_log_folder(self):
    import params
    return params.hdfs_log_dir
  
  def get_user(self):
    import params
    return params.hdfs_user

  def get_pid_files(self):
    import status_params
    return [status_params.zkfc_pid_file]

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)
    if check_stack_feature(StackFeature.ZKFC_VERSION_ADVERTISED, params.version_for_stack_feature_checks):
      stack_select.select_packages(params.version)

def initialize_ha_zookeeper(params):
  try:
    iterations = 10
    formatZK_cmd = "hdfs zkfc -formatZK -nonInteractive"
    Logger.info("Initialize HA state in ZooKeeper: %s" % (formatZK_cmd))
    for i in range(iterations):
      Logger.info('Try %d out of %d' % (i+1, iterations))
      code, out = shell.call(formatZK_cmd, logoutput=False, user=params.hdfs_user)
      if code == 0:
        Logger.info("HA state initialized in ZooKeeper successfully")
        return True
      elif code == 2:
        Logger.info("HA state already initialized in ZooKeeper")
        return True
      else:
        Logger.warning('HA state initialization in ZooKeeper failed with %d error code. Will retry' % (code))
  except Exception as ex:
    Logger.error('HA state initialization in ZooKeeper threw an exception. Reason %s' %(str(ex)))
  return False


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ZkfcSlaveWindows(ZkfcSlave):
  def start(self, env):
    import params
    self.configure(env)
    Service(params.zkfc_win_service_name, action="start")

  def stop(self, env):
    import params
    Service(params.zkfc_win_service_name, action="stop")

  def status(self, env):
    import status_params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

    env.set_params(status_params)
    check_windows_service_status(status_params.zkfc_win_service_name)

if __name__ == "__main__":
  ZkfcSlave().execute()
