#!/usr/bin/python
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
from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
from setup_spark import setup_spark


class SparkClient(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    setup_spark(env, 'client', upgrade_type=upgrade_type, action = 'config')

  def status(self, env):
    raise ClientComponentHasNoStatus()
  
  def get_stack_to_component(self):
    return {"HDP": "spark-client"}

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Logger.info("Executing Spark Client Stack Upgrade pre-restart")
      conf_select.select(params.stack_name, "spark", params.version)
      hdp_select.select("spark-client", params.version)

if __name__ == "__main__":
  SparkClient().execute()

