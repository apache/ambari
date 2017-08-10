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
# Python imports
import os
import sys

# Local imports
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.exceptions import ClientComponentHasNoStatus
from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger
from setup_spark import setup_spark


class SparkClient(Script):
  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env, config_dir=None, upgrade_type=None):
    """
    :param env: Python environment
    :param config_dir: During rolling upgrade, which config directory to save configs to.
    :param upgrade_type:  If in the middle of a stack upgrade, whether rolling or non-rolling
    """
    import params
    env.set_params(params)
    
    setup_spark(env, 'client', upgrade_type=upgrade_type, action='config', config_dir=config_dir)

  def status(self, env):
    raise ClientComponentHasNoStatus()
  
  def stack_upgrade_save_new_config(self, env):
    """
    Because this gets called during a Rolling Upgrade, the new configs have already been saved, so we must be
    careful to only call configure() on the directory with the new version.
    """
    import params
    env.set_params(params)

    conf_select_name = "spark"
    base_dir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
    config_dir = self.get_config_dir_during_stack_upgrade(env, base_dir, conf_select_name)

    if config_dir:
      Logger.info("stack_upgrade_save_new_config(): Calling conf-select on %s using version %s" % (conf_select_name, str(params.version)))

      # Because this script was called from ru_execute_tasks.py which already enters an Environment with its own basedir,
      # must change it now so this function can find the Jinja Templates for the service.
      env.config.basedir = base_dir
      self.configure(env, config_dir=config_dir, upgrade_type=UPGRADE_TYPE_ROLLING)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      Logger.info("Executing Spark Client Stack Upgrade pre-restart")
      stack_select.select_packages(params.version)

if __name__ == "__main__":
  SparkClient().execute()

