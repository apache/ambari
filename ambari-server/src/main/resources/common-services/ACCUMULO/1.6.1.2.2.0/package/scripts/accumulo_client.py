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

from resource_management.core.logger import Logger
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.script.script import Script

from accumulo_configuration import setup_conf_dir


class AccumuloClient(Script):
  def get_stack_to_component(self):
    return {"HDP": "accumulo-client"}


  def install(self, env):
    self.install_packages(env)
    self.configure(env)


  def configure(self, env):
    import params
    env.set_params(params)

    setup_conf_dir(name='client')


  def status(self, env):
    raise ClientComponentHasNoStatus()


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # is not at least HDP 2.2.0.0
    if Script.is_hdp_stack_less_than("2.2"):
      return

    Logger.info("Executing Accumulo Client Upgrade pre-restart")
    conf_select.select(params.stack_name, "accumulo", params.version)
    hdp_select.select("accumulo-client", params.version)

if __name__ == "__main__":
  AccumuloClient().execute()
