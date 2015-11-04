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
from resource_management.libraries.script import Script
from storm import storm
from supervisord_service import supervisord_service, supervisord_check_status
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import format
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version

class Nimbus(Script):

  def get_stack_to_component(self):
    return {"HDP": "storm-nimbus"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)

    storm()

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "storm", params.version)
      hdp_select.select("storm-client", params.version)
      hdp_select.select("storm-nimbus", params.version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    supervisord_service("nimbus", action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    supervisord_service("nimbus", action="stop")

  def status(self, env):
    supervisord_check_status("nimbus")

if __name__ == "__main__":
  Nimbus().execute()
