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
import os

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.logger import Logger
from resource_management.core import shell
from setup_spark import setup_spark
from spark_service import spark_service


class SparkThriftServer(Script):

  def install(self, env):
    import params
    env.set_params(params)

    self.install_packages(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    setup_spark(env, 'server', upgrade_type = upgrade_type, action = 'config')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    self.configure(env)
    spark_service('sparkthriftserver', upgrade_type=upgrade_type, action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    spark_service('sparkthriftserver', upgrade_type=upgrade_type, action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.spark_thrift_server_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    Logger.info("Executing Spark2 Thrift Server Stack Upgrade pre-restart")
    stack_select.select_packages(params.version)
      
  def get_log_folder(self):
    import params
    return params.spark_log_dir
  
  def get_user(self):
    import params
    return params.hive_user

  def get_pid_files(self):
    import status_params
    return [status_params.spark_thrift_server_pid_file]

if __name__ == "__main__":
  SparkThriftServer().execute()
