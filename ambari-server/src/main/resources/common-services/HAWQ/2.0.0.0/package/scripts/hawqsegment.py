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
import os
from resource_management import Script
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.check_process_status import check_process_status

import utils
import common
import constants

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
    env.set_params(constants)
    common.setup_user()
    common.setup_common_configurations()
    common.update_bashrc(constants.hawq_greenplum_path_file, constants.hawq_user_bashrc_file)


  def __start_segment(self):
    import params
    return utils.exec_hawq_operation(
          constants.START, 
          "{0} -a".format(constants.SEGMENT), 
          not_if=utils.chk_postgres_status_cmd(params.hawq_segment_address_port))

  def start(self, env):
    self.configure(env)
    common.validate_configuration()

    if self.__is_segment_initialized():
      self.__start_segment()
      return

    # Initialization also starts process.
    self.__init_segment()


  def stop(self, env):
    import params

    utils.exec_hawq_operation(constants.STOP, "{0} -a".format(constants.SEGMENT), only_if=utils.chk_postgres_status_cmd(
                                params.hawq_segment_address_port))


  def status(self, env):
    from hawqstatus import get_pid_file
    check_process_status(get_pid_file())


  @staticmethod
  def __init_segment():
    import params

    # Create segment directories
    utils.create_dir_as_hawq_user(params.hawq_segment_dir)
    utils.create_dir_as_hawq_user(params.hawq_segment_temp_dir.split(','))

    Execute("chown {0}:{1} {2}".format(constants.hawq_user, constants.hawq_group, os.path.dirname(params.hawq_segment_dir)),
            user=constants.root_user, timeout=constants.default_exec_timeout)

    # Initialize hawq segment
    utils.exec_hawq_operation(constants.INIT, "{0} -a -v".format(constants.SEGMENT))

  def __is_segment_initialized(self):
    """
    Check whether the HAWQ Segment is initialized
    """
    import params
    return os.path.exists(os.path.join(params.hawq_segment_dir, constants.postmaster_opts_filename))


if __name__ == "__main__":
  HawqSegment().execute()
