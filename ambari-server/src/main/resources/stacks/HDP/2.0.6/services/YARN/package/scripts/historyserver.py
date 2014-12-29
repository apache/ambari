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

Ambari Agent

"""

from resource_management import *
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format

from yarn import yarn
from service import service

class HistoryServer(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="historyserver")

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hadoop-mapreduce-historyserver {version}"))

      params.HdfsResource(InlineTemplate(params.mapreduce_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.mapreduce_tar_source,
                          owner=params.hdfs_user,
                          group=params.user_group,
                          mode=0444,
      )
      params.HdfsResource(None, action="execute")

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:

      params.HdfsResource(InlineTemplate(params.mapreduce_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.mapreduce_tar_source,
                          owner=params.hdfs_user,
                          group=params.user_group,
                          mode=0444,
      )
      params.HdfsResource(None, action="execute")
    service('historyserver', action='start', serviceName='mapreduce')

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    service('historyserver', action='stop', serviceName='mapreduce')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.mapred_historyserver_pid_file)

if __name__ == "__main__":
  HistoryServer().execute()
