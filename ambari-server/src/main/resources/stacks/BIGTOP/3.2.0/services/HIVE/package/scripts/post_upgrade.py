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
# Python Imports
import os
import shutil


# Local Imports
from hive import create_hive_hdfs_dirs


# Ambari Commons & Resource Management Imports
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.format import format
from resource_management.libraries.script import Script

class HivePostUpgrade(Script):
  def move_tables(self, env):
    import params
    env.set_params(params)
    
    create_hive_hdfs_dirs()
    
    target_version = upgrade_summary.get_target_version(service_name = "HIVE")
    
    hive_script = format("/usr/bigtop/{target_version}/usr/lib/hive/bin/hive")
    cmd = format("{hive_script} --config /etc/hive/conf --service  strictmanagedmigration --hiveconf hive.strict.managed.tables=true  -m automatic  --modifyManagedTables --oldWarehouseRoot /apps/hive/warehouse")
    Execute(cmd,
            environment = { 'JAVA_HOME': params.java64_home },
            user = params.hdfs_user)

if __name__ == "__main__":
  HivePostUpgrade().execute()
