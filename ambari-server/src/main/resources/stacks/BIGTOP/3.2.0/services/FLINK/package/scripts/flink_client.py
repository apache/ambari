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
import os

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.logger import Logger
from resource_management.libraries.script import Script

from setup_flink import *

class FlinkClient(Script):

  def pre_install(self, env):
    import params
    env.set_params(params)

  def configure(self, env, config_dir=None, upgrade_type=None):
    import params
    env.set_params(params)
    setup_flink(env,"client",upgrade_type=upgrade_type, action = 'config')

  def install(self, env):
    import params
    self.install_packages(env)
    self.configure(env, config_dir=params.flink_config_dir)

  def status(self, env):
    raise ClientComponentHasNoStatus()

if __name__ == "__main__":
  FlinkClient().execute()
