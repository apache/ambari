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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.logger import Logger
from resource_management.core import shell
from setup_spark import *
from spark_service import spark_service


class JobHistoryServer(Script):

  def install(self, env):
    import params
    env.set_params(params)
    
    self.install_packages(env)
    
  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    setup_spark(env, 'server', upgrade_type=upgrade_type, action = 'config')
    
  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    self.configure(env)
    spark_service('jobhistoryserver', upgrade_type=upgrade_type, action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    spark_service('jobhistoryserver', upgrade_type=upgrade_type, action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.spark_history_server_pid_file)
    

  def get_stack_to_component(self):
     return {"HDP": "spark-historyserver"}

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Logger.info("Executing Spark Job History Server Stack Upgrade pre-restart")
      conf_select.select(params.stack_name, "spark", params.version)
      hdp_select.select("spark-historyserver", params.version)

      # Spark 1.3.1.2.3, and higher, which was included in HDP 2.3, does not have a dependency on Tez, so it does not
      # need to copy the tarball, otherwise, copy it.

      if params.version and compare_versions(format_hdp_stack_version(params.version), '2.3.0.0') < 0:
        resource_created = copy_to_hdfs(
          "tez",
          params.user_group,
          params.hdfs_user,
          host_sys_prepped=params.host_sys_prepped)
        if resource_created:
          params.HdfsResource(None, action="execute")

if __name__ == "__main__":
  JobHistoryServer().execute()
