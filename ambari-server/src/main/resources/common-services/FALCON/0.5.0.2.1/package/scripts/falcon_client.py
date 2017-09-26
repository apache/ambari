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
from falcon import falcon
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.logger import Logger
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import ClientComponentHasNoStatus

class FalconClient(Script):
  def configure(self, env):
    import params
    env.set_params(params)
    falcon('client', action='config')

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class FalconClientLinux(FalconClient):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version)):
      return

    Logger.info("Executing Falcon Client Stack Upgrade pre-restart")
    stack_select.select_packages(params.version)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class FalconClientWindows(FalconClient):
  def install(self, env):
    import params
    if params.falcon_home is None:
      self.install_packages(env)
    self.configure(env)

if __name__ == "__main__":
  FalconClient().execute()
