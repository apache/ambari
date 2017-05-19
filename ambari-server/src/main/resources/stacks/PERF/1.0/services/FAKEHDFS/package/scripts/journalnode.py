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

# Python Imports

# Local Imports
from resource_management.libraries.script.dummy import Dummy
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core.logger import Logger


class FAKEJournalNode(Dummy):
  """
  Dummy script that simulates a master component.
  """

  def __init__(self):
    super(FAKEJournalNode, self).__init__()
    self.component_name = "FAKEJOURNALNODE"
    self.principal_conf_name = "hdfs-site"
    self.principal_name = "dfs.journalnode.kerberos.principal"
    self.keytab_conf_name = "hdfs-site"
    self.keytab_name = "dfs.journalnode.keytab.file"

  def get_component_name(self):
    return "hadoop-hdfs-journalnode"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing FAKEJournalNode Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      stack_select.select(self.get_component_name(), params.version)

if __name__ == "__main__":
  FAKEJournalNode().execute()
