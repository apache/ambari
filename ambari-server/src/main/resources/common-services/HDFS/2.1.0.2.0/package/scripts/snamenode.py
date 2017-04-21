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

from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from hdfs_snamenode import snamenode
from hdfs import hdfs
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

from resource_management.core.logger import Logger

class SNameNode(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("secondarynamenode")
    snamenode(action="configure")

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    snamenode(action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    snamenode(action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    snamenode(action="status")

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class SNameNodeDefault(SNameNode):

  def get_component_name(self):
    return "hadoop-hdfs-secondarynamenode"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "hadoop", params.version)
      stack_select.select("hadoop-hdfs-secondarynamenode", params.version)
      
  def get_log_folder(self):
    import params
    return params.hdfs_log_dir
  
  def get_user(self):
    import params
    return params.hdfs_user

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class SNameNodeWindows(SNameNode):
  pass

if __name__ == "__main__":
  SNameNode().execute()
