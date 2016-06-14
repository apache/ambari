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
from resource_management.core.logger import Logger

import common
import hawq_constants
import master_helper

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
    env.set_params(hawq_constants)
    master_helper.configure_master()

  def start(self, env):
    import params
    self.configure(env)
    common.validate_configuration()
    common.start_component(hawq_constants.STANDBY, params.hawq_master_address_port, params.hawq_master_dir)

  def stop(self, env):
    common.stop_component(hawq_constants.STANDBY, hawq_constants.FAST)

  def status(self, env):
    from hawqstatus import assert_component_running
    assert_component_running(hawq_constants.STANDBY)

  def activate_hawq_standby(self, env):
    import params
    import utils
    Logger.info("Activating HAWQ standby...")
    params.XmlConfig("hawq-site.xml",
                     configurations=params.hawq_site,
                     configuration_attributes=params.config_attrs['hawq-site'])
    utils.exec_hawq_operation(hawq_constants.ACTIVATE,
                              "{0} -a -M {1} -v --ignore-bad-hosts".format(hawq_constants.STANDBY, hawq_constants.FAST),
                              host_name=params.hawqstandby_host)

    # Stop the new HAWQMASTER as the process might be running at an old port,
    # which might cause a failure in Start HAWQ Service step in the Activate HAWQ Standby Master Wizard
    common.stop_component(hawq_constants.MASTER, hawq_constants.FAST)

if __name__ == "__main__":
  HawqStandby().execute()
