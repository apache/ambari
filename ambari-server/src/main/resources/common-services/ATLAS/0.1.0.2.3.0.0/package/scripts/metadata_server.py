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

from metadata import metadata
from resource_management import Execute, check_process_status, Script
from resource_management.libraries.functions import format

class MetadataServer(Script):

  def get_stack_to_component(self):
    return {"HDP": "atlas-server"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    metadata()

  # def pre_rolling_restart(self, env):
  #   import params
  #   env.set_params(params)
  #   upgrade.prestart(env, "metadata-server")
  #
  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    daemon_cmd = format('source {params.conf_dir}/metadata-env.sh ; {params.metadata_start_script} --port {params.metadata_port}')
    no_op_test = format('ls {params.pid_file} >/dev/null 2>&1 && ps -p `cat {params.pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.metadata_user,
            not_if=no_op_test
    )

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    daemon_cmd = format('source {params.conf_dir}/metadata-env.sh; {params.metadata_stop_script}')
    Execute(daemon_cmd,
            user=params.metadata_user,
    )
    Execute (format("rm -f {params.pid_file}"))


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_file)

if __name__ == "__main__":
  MetadataServer().execute()
