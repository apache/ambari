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

from resource_management import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.show_logs import show_logs
import kafka_process
from setup_ranger_kafka import setup_ranger_kafka
from kafka import kafka


class KafkaBroker(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    kafka(upgrade_type=upgrade_type)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if params.version and check_stack_feature(
      StackFeature.ROLLING_UPGRADE, params.version
    ):
      stack_select.select_packages(params.version)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)

    if params.is_supported_kafka_ranger:
      setup_ranger_kafka()  # Ranger Kafka Plugin related call

    identity = kafka_process.read_or_recover_process(
      params.kafka_pid_file,
      params.kafka_user,
      params.user_group,
      params.kafka_server_properties,
    )
    if identity is not None:
      Logger.info(f"Kafka Broker is already running with pid {identity.pid}")
      return

    start_command = (
      "/bin/bash",
      "-c",
      'source "$1" && exec "$2" -daemon "$3"',
      "ambari-kafka-start",
      params.kafka_env_file,
      params.kafka_server_start,
      params.kafka_server_properties,
    )
    try:
      Execute(start_command, user=params.kafka_user)
      kafka_process.wait_for_started_process(
        params.kafka_pid_file,
        params.kafka_user,
        params.user_group,
        params.kafka_server_properties,
      )
    except Exception:
      show_logs(params.kafka_log_dir, params.kafka_user)
      raise

  def stop(self, env, upgrade_type=None):
    import status_params

    env.set_params(status_params)

    stopped = kafka_process.stop_process(
      status_params.kafka_pid_file,
      status_params.kafka_user,
      status_params.user_group,
      status_params.kafka_server_properties,
    )
    if not stopped:
      Logger.info("Kafka Broker is not running")

  def disable_security(self, env):
    import params

    env.set_params(params)

    if not params.zookeeper_connect:
      Logger.info("No zookeeper connection string. Skipping reverting ACL")
      return
    if not params.secure_acls:
      Logger.info("The zookeeper.set.acl is false. Skipping reverting ACL")
      return
    if not params.kafka_kerberos_params:
      from resource_management.core.exceptions import Fail

      raise Fail("Kafka ZooKeeper ACL migration requires a JAAS configuration")
    Execute(
      (
        params.kafka_security_migrator,
        "--zookeeper.connect",
        params.zookeeper_connect,
        "--zookeeper.acl=unsecure",
      ),
      user=params.kafka_user,
      environment={
        "JAVA_HOME": params.java64_home,
        "KAFKA_OPTS": (
          "-Djavax.security.auth.useSubjectCredsOnly=false "
          + params.kafka_kerberos_params
        ),
      },
      logoutput=True,
      tries=3,
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    identity = kafka_process.read_or_recover_process(
      status_params.kafka_pid_file,
      status_params.kafka_user,
      status_params.user_group,
      status_params.kafka_server_properties,
    )
    if identity is None:
      from resource_management.core.exceptions import ComponentIsNotRunning

      raise ComponentIsNotRunning()

  def get_log_folder(self):
    import params

    return params.kafka_log_dir

  def get_user(self):
    import params

    return params.kafka_user

  def get_pid_files(self):
    import status_params

    return [status_params.kafka_pid_file]


if __name__ == "__main__":
  KafkaBroker().execute()
