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
from resource_management import Script
from resource_management.libraries.functions.check_process_status import check_process_status

import master_helper
import common
import constants

class HawqStandby(Script):
  """
  Contains the interface definitions for methods like install, 
  start, stop, status, etc. for the HAWQ Standby Master
  """

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    env.set_params(constants)
    master_helper.configure_master()

  def start(self, env):
    self.configure(env)
    common.validate_configuration()
    master_helper.start_master()

  def stop(self, env):
    master_helper.stop_master()

  def status(self, env):
    from hawqstatus import get_pid_file
    check_process_status(get_pid_file())

  def activatestandby(self, env):
    pass

if __name__ == "__main__":
    HawqStandby().execute()
