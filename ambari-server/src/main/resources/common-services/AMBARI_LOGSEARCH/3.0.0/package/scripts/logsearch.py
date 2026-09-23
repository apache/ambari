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

from ambari_commons.repo_manager import ManagerFactory
from ambari_commons.shell import RepoCallContext
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from setup_logsearch import setup_logsearch

class LogSearch(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_logsearch()

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    Execute(format("{logsearch_dir}/bin/logsearch.sh start"),
            user=params.logsearch_user
            )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    Execute(format("{logsearch_dir}/bin/logsearch.sh stop"),
            user=params.logsearch_user
            )

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.logsearch_pid_file)

  def upgrade_logsearch_portal(self, env):
    pkg_provider = ManagerFactory.get()
    context = RepoCallContext()
    context.log_output = True
    pkg_provider.remove_package('ambari-logsearch-portal', context, ignore_dependencies=True)
    pkg_provider.upgrade_package('ambari-logsearch-portal', context)

if __name__ == "__main__":
  LogSearch().execute()
