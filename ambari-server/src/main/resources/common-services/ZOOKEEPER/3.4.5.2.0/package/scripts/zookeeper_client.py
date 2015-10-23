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

import sys
from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

from zookeeper import zookeeper

class ZookeeperClient(Script):
  def configure(self, env):
    import params
    env.set_params(params)
    zookeeper(type='client')
    pass

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    pass

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    pass

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZookeeperClientLinux(ZookeeperClient):
  def get_stack_to_component(self):
    return {"HDP": "zookeeper-client"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "zookeeper", params.version)
      hdp_select.select("zookeeper-client", params.version)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ZookeeperClientWindows(ZookeeperClient):
  def install(self, env):
    # client checks env var to determine if it is installed
    if not os.environ.has_key("ZOOKEEPER_HOME"):
      self.install_packages(env)
    self.configure(env)

if __name__ == "__main__":
  ZookeeperClient().execute()
