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
from resource_management import *
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
from setup_spark import *


class JobHistoryServer(Script):

  def get_stack_to_component(self):
     return {"HDP": "spark-historyserver"}

  def pre_rolling_restart(self, env):
    import params

    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set spark-historyserver {version}"))

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)

  def stop(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    daemon_cmd = format('{spark_history_server_stop}')
    Execute(daemon_cmd,
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home}
    )
    if os.path.isfile(params.spark_history_server_pid_file):
      os.remove(params.spark_history_server_pid_file)


  def start(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    setup_spark(env, 'server', action = 'start')

    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {spark_kerberos_keytab} {spark_principal}; ")
      Execute(spark_kinit_cmd, user=params.spark_user)

    daemon_cmd = format('{spark_history_server_start}')
    no_op_test = format(
      'ls {spark_history_server_pid_file} >/dev/null 2>&1 && ps -p `cat {spark_history_server_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    pid_file = format("{spark_history_server_pid_file}")
    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)

  # Note: This function is not called from start()/install()
  def configure(self, env):
    import params

    env.set_params(params)
    setup_spark(env, 'server', action = 'config')

if __name__ == "__main__":
  JobHistoryServer().execute()
