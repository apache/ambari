#!/usr/bin/env python3
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

from resource_management.libraries.script.script import Script
from resource_management.core.exceptions import ComponentIsNotRunning
from setup_spark import setup_spark
from spark_service import spark_service
import spark_process


class JobHistoryServer(Script):
  def install(self, env):
    import params

    env.set_params(params)

    self.install_packages(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params

    env.set_params(params)

    setup_spark(env, "historyserver", upgrade_type=upgrade_type, action="config")

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    self.configure(env)
    spark_service("jobhistoryserver", upgrade_type=upgrade_type, action="start")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    spark_service("jobhistoryserver", upgrade_type=upgrade_type, action="stop")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    identity = spark_process.read_or_recover_process(
      "jobhistoryserver",
      status_params.spark_history_server_pid_file,
      status_params.spark_user,
      status_params.user_group,
      status_params.spark_defaults_file,
    )
    if identity is None:
      raise ComponentIsNotRunning("Spark History Server is not running")

  def get_log_folder(self):
    import params

    return params.spark_log_dir

  def get_user(self):
    import params

    return params.spark_user

  def get_pid_files(self):
    import status_params

    return [status_params.spark_history_server_pid_file]


if __name__ == "__main__":
  JobHistoryServer().execute()
