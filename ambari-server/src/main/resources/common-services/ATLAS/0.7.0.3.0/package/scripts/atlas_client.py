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

import sys
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import ClientComponentHasNoStatus

from metadata import metadata


class AtlasClient(Script):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, params.version):
      stack_select.select_packages(params.version)

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    metadata('client')

  def status(self, env):
    raise ClientComponentHasNoStatus()

if __name__ == "__main__":
  AtlasClient().execute()
