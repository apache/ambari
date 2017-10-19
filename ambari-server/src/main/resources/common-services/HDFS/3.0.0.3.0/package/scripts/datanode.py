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
import datanode_upgrade
from hdfs_datanode import datanode
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, FILE_TYPE_XML
from resource_management.core.logger import Logger
from hdfs import hdfs, reconfig
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from utils import get_hdfs_binary

class DataNode(Script):

  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the component name.
    """
    return get_hdfs_binary("hadoop-hdfs-datanode")


  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("datanode")
    datanode(action="configure")

  def save_configs(self, env):
    import params
    env.set_params(params)
    hdfs("datanode")

  def reload_configs(self, env):
    import params
    env.set_params(params)
    Logger.info("RELOAD CONFIGS")
    reconfig("datanode", params.dfs_dn_ipc_address)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    datanode(action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    # pre-upgrade steps shutdown the datanode, so there's no need to call

    hdfs_binary = self.get_hdfs_binary()
    if upgrade_type == "rolling":
      stopped = datanode_upgrade.pre_rolling_upgrade_shutdown(hdfs_binary)
      if not stopped:
        datanode(action="stop")
    else:
      datanode(action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    datanode(action = "status")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class DataNodeDefault(DataNode):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade pre-restart")
    import params
    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade post-restart")
    import params
    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    # ensure the DataNode has started and rejoined the cluster
    datanode_upgrade.post_upgrade_check(hdfs_binary)

  def get_log_folder(self):
    import params
    return params.hdfs_log_dir
  
  def get_user(self):
    import params
    return params.hdfs_user

  def get_pid_files(self):
    import status_params
    return [status_params.datanode_pid_file]

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class DataNodeWindows(DataNode):
  def install(self, env):
    import install_params
    self.install_packages(env)

if __name__ == "__main__":
  DataNode().execute()
