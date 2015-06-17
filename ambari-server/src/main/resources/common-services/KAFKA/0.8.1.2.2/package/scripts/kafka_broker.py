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
import sys
import upgrade

from kafka import kafka
from setup_ranger_kafka import setup_ranger_kafka

class KafkaBroker(Script):

  def get_stack_to_component(self):
    return {"HDP": "kafka-broker"}

  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    kafka()

  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)
    upgrade.prestart(env, "kafka-broker")

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    if params.is_supported_kafka_ranger:
      setup_ranger_kafka() #Ranger Kafka Plugin related call 
    daemon_cmd = format('source {params.conf_dir}/kafka-env.sh ; {params.kafka_bin} start')
    no_op_test = format('ls {params.kafka_pid_file} >/dev/null 2>&1 && ps -p `cat {params.kafka_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.kafka_user,
            not_if=no_op_test
    )

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
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
