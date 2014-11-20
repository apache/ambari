#!/usr/bin/env python
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

import json
import sys
import traceback
from resource_management import *
from resource_management.libraries.functions.list_ambari_managed_repos import *
from ambari_commons.os_check import OSCheck, OSConst


class ExecuteUpgradeTasks(Script):
  """
  This script is a part of Rolling Upgrade workflow and is described at
  appropriate design doc.

  It executes tasks used for rolling upgrades.
  """

  def actionexecute(self, env):

    # Parse parameters
    config = Script.get_config()
    #tasks = json.loads(config['roleParams']['tasks'])

    print str(config)


if __name__ == "__main__":
  ExecuteUpgradeTasks().execute()
