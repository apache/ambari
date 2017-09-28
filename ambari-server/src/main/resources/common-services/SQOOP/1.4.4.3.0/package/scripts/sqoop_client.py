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

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from sqoop import sqoop
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

class SqoopClient(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    sqoop(type='client')

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class SqoopClientDefault(SqoopClient):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class SqoopClientWindows(SqoopClient):
  pass

if __name__ == "__main__":
  SqoopClient().execute()
