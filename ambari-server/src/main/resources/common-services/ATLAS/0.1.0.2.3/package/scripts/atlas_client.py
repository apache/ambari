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
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import ClientComponentHasNoStatus

from metadata import metadata

# todo: support rolling upgrade
class AtlasClient(Script):

  def get_component_name(self):
    return "atlas-client"

  # ToDo: currently <stack-selector-tool> doesn't contain atlas-client, uncomment this block when
  # ToDo: atlas-client will be available
  # def pre_upgrade_restart(self, env, upgrade_type=None):
  #   import params
  #   env.set_params(params)
  #
  # TODO: Add ATLAS_CONFIG_VERSIONING stack feature and uncomment this code when config versioning for Atlas is supported
  #   if params.version and check_stack_feature(StackFeature.ATLAS_CONFIG_VERSIONING, params.version):
  #     conf_select.select(params.stack_name, "atlas", params.version)
  # TODO: Add ATLAS_CLIENT_ROLLING_UPGRADE stack feature and uncomment this code when rolling upgrade for Atlas client is supported
  #   if params.version and check_stack_feature(StackFeature.ATLAS_CLIENT_ROLLING_UPGRADE, params.version):
  #     stack_select.select("atlas-client", params.version)

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    metadata()

  def status(self, env):
    raise ClientComponentHasNoStatus()

if __name__ == "__main__":
  AtlasClient().execute()
