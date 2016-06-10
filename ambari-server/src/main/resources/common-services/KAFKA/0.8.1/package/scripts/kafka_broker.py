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
from resource_management.core.resources.system import Execute, File, Directory
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.default import default
from kafka import ensure_base_directories

import upgrade
from kafka import kafka
from setup_ranger_kafka import setup_ranger_kafka

class KafkaBroker(Script):

  def get_component_name(self):
    return "kafka-broker"

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    kafka(upgrade_type=upgrade_type)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select("kafka-broker", params.version)

    if params.version and check_stack_feature(StackFeature.CONFIG_VERSIONING, params.version):
      conf_select.select(params.stack_name, "kafka", params.version)

    # This is extremely important since it should only be called if crossing the HDP 2.3.4.0 boundary. 
    if params.current_version and params.version and params.upgrade_direction:
      src_version = dst_version = None
      if params.upgrade_direction == Direction.UPGRADE:
        src_version = format_stack_version(params.current_version)
        dst_version = format_stack_version(params.version)
      else:
        # These represent the original values during the UPGRADE direction
        src_version = format_stack_version(params.version)
        dst_version = format_stack_version(params.downgrade_from_version)

      # TODO: How to handle the case of crossing stack version boundary in a stack agnostic way?
      if compare_versions(src_version, '2.3.4.0') < 0 and compare_versions(dst_version, '2.3.4.0') >= 0:
        # Calling the acl migration script requires the configs to be present.
        self.configure(env, upgrade_type=upgrade_type)
        upgrade.run_migration(env, upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    if params.is_supported_kafka_ranger:
      setup_ranger_kafka() #Ranger Kafka Plugin related call 
    daemon_cmd = format('source {params.conf_dir}/kafka-env.sh ; {params.kafka_bin} start')
    no_op_test = format('ls {params.kafka_pid_file} >/dev/null 2>&1 && ps -p `cat {params.kafka_pid_file}` >/dev/null 2>&1')
    try:
      Execute(daemon_cmd,
              user=params.kafka_user,
              not_if=no_op_test
      )
    except:
      show_logs(params.kafka_log_dir, params.kafka_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    # Kafka package scripts change permissions on folders, so we have to
    # restore permissions after installing repo version bits
    # before attempting to stop Kafka Broker
    ensure_base_directories()
    daemon_cmd = format('source {params.conf_dir}/kafka-env.sh; {params.kafka_bin} stop')
    try:
      Execute(daemon_cmd,
              user=params.kafka_user,
      )
    except:
      show_logs(params.kafka_log_dir, params.kafka_user)
      raise
    File(params.kafka_pid_file,
          action = "delete"
    )


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.kafka_pid_file)
    
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
