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
from utils import service


class JournalNode(Script):
  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)

  def start(self, env):
    import params

    env.set_params(params)
    self.config(env)
    service(
      action="start", name="journalnode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True,
      keytab=params.dfs_journalnode_keytab_file,
      principal=params.dfs_journalnode_kerberos_principal
    )

  def stop(self, env):
    import params

    env.set_params(params)
    service(
      action="stop", name="journalnode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True,
      keytab=params.dfs_journalnode_keytab_file,
      principal=params.dfs_journalnode_kerberos_principal
    )

  def config(self, env):
    import params

    Directory(params.jn_edits_dir,
              recursive=True,
              owner=params.hdfs_user,
              group=params.user_group
    )
    pass

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.journalnode_pid_file)


if __name__ == "__main__":
  JournalNode().execute()
