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

# Python Imports
import sys

# Local Imports
from hive import hive

# Ambari Commons & Resource Management Imports
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.logger import Logger
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature


class HiveClient(Script):
  def install(self, env):
    import params
    self.install_packages(env)
    self.configure(env)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def configure(self, env):
    import params
    env.set_params(params)
    hive(name='client')

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Hive client Stack Upgrade pre-restart")

    import params
    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select_packages(params.version)


if __name__ == "__main__":
  HiveClient().execute()
