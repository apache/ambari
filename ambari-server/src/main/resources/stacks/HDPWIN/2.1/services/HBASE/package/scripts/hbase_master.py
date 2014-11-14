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

"""

import sys
from resource_management import *
from hbase import hbase
import service_mapping
from hbase_decommission import *

class HbaseMaster(Script):
  def install(self, env):
    if not check_windows_service_exists(service_mapping.hbase_master_win_service_name):
      self.install_packages(env)

  def configure(self, env):
    hbase()

  def start(self, env):
    import params
    self.configure(env) # for security
    Service(service_mapping.hbase_master_win_service_name, action="start")

  def stop(self, env):
    Service(service_mapping.hbase_master_win_service_name, action="stop")

  def status(self, env):
    check_windows_service_status(service_mapping.hbase_master_win_service_name)

  def decommission(self, env):
    import params
    env.set_params(params)
    hbase_decomission()

if __name__ == "__main__":
  HbaseMaster().execute()
