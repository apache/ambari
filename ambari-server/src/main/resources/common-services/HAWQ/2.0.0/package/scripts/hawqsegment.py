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

import common
import hawq_constants

class HawqSegment(Script):
  """
  Contains the interface definitions for methods like install, 
  start, stop, status, etc. for the HAWQ Segment
  """

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params

    env.set_params(params)
    env.set_params(hawq_constants)
    common.setup_user()
    common.setup_common_configurations()
    common.create_master_dir(params.hawq_segment_dir)
    # temp directories are stateless and they should be recreated when configured (started)
    common.create_temp_dirs(params.hawq_segment_temp_dirs)

  def start(self, env):
    import params
    self.configure(env)
    common.validate_configuration()
    common.start_component(hawq_constants.SEGMENT, params.hawq_segment_address_port, params.hawq_segment_dir)

  def stop(self, env, mode=hawq_constants.FAST):
    import params
    common.stop_component(hawq_constants.SEGMENT, mode)

  def status(self, env):
    from hawqstatus import assert_component_running
    assert_component_running(hawq_constants.SEGMENT)

  def immediate_stop_hawq_segment(self, env):
    self.stop(env, hawq_constants.IMMEDIATE)

if __name__ == "__main__":
  HawqSegment().execute()
