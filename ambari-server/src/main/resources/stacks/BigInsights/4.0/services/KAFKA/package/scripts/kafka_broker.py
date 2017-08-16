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

from resource_management import *
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from kafka import ensure_base_directories

import sys
import upgrade

from kafka import kafka

class KafkaBroker(Script):

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    kafka(upgrade_type=upgrade_type)

  '''def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.prestart(env, "kafka-broker")'''

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_stack_version(params.version), '4.1.0.0') >= 0:
      stack_select.select_packages(params.version)

    # This is extremely important since it should only be called if crossing the IOP 4.2 boundary.
    # This is extremely important since it should only be called if crossing the HDP 2.3.4.0 boundary.
    if params.version and params.upgrade_direction:
      src_version = dst_version = None
      if params.upgrade_direction == Direction.UPGRADE:
        src_version = upgrade_summary.get_source_version("KAFKA", default_version = params.version)
        dst_version = upgrade_summary.get_target_version("KAFKA", default_version = params.version)
      else:
        # These represent the original values during the UPGRADE direction
        src_version = upgrade_summary.get_target_version("KAFKA", default_version = params.version)
        dst_version = upgrade_summary.get_source_version("KAFKA", default_version = params.version)

      if compare_versions(src_version, '4.2.0.0') < 0 and compare_versions(dst_version, '4.2.0.0') >= 0:
        # Upgrade from IOP 4.1 to 4.2, Calling the acl migration script requires the configs to be present.
        self.configure(env, upgrade_type=upgrade_type)
        upgrade.run_migration(env, upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    daemon_cmd = format('source {params.conf_dir}/kafka-env.sh ; {params.kafka_bin} start')
    no_op_test = format('ls {params.kafka_pid_file} >/dev/null 2>&1 && ps -p `cat {params.kafka_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.kafka_user,
            not_if=no_op_test
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    ensure_base_directories()
    daemon_cmd = format('source {params.conf_dir}/kafka-env.sh; {params.kafka_bin} stop')
    Execute(daemon_cmd,
            user=params.kafka_user,
    )
    File (params.kafka_pid_file,
          action = "delete"
    )


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.kafka_pid_file)

if __name__ == "__main__":
  KafkaBroker().execute()
