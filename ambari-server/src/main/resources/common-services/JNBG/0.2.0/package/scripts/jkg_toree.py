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

import os, errno
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File, Directory
from resource_management.core.source import StaticFile, InlineTemplate, Template
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.check_process_status import check_process_status
import jnbg_helpers as helpers

class GatewayKernels(Script):
  def install(self, env):
    import jkg_toree_params as params
    self.install_packages(env)

    # Create user and group if they don't exist
    helpers.create_linux_user(params.user, params.group)

    # Create directories used by the service and service user
    Directory([params.home_dir, params.jkg_pid_dir, params.log_dir, params.spark_config_dir],
              mode=0755,
              create_parents=True,
              owner=params.user,
              group=params.group,
              recursive_ownership=True
             )

    if os.path.exists(params.py_venv_pathprefix):
      Logger.warning("Virtualenv path prefix {0} to be used for JNBG service might already exist."
                     "This is unexpected if the service or service component is being installed on the node for the first time."
                     "It could indicate remnants from a prior installation.".format(params.py_venv_pathprefix))

    # Setup bash scripts for execution
    for sh_script in params.sh_scripts:
      File(params.sh_scripts_dir + os.sep + sh_script,
           content=StaticFile(sh_script),
           mode=0750
          )
    for sh_script in params.sh_scripts_user:
      File(params.sh_scripts_dir + os.sep + sh_script,
           content=StaticFile(sh_script),
           mode=0755
          )

    # Run install commands for JKG defined in params
    for command in params.jkg_commands: Execute(command, logoutput=True)

    # Run install commands for Toree defined in params
    for command in params.toree_commands: Execute(command, logoutput=True)

    # Run setup commands for log4j
    for command in params.log4j_setup_commands: Execute(command, logoutput=True)

    # Note that configure is done during startup

  def stop(self, env):
    import status_params as params
    import jkg_toree_params as jkgparams
    env.set_params(params)

    helpers.stop_process(params.jkg_pid_file, jkgparams.user, jkgparams.log_dir)

  def start(self, env):
    import os, sys, time
    import jkg_toree_params as params
    env.set_params(params)
    self.configure(env)
    delay_checks = 8

    # Need HDFS started for the next step
    helpers.create_hdfs_dirs(params.user, params.group, params.dirs)

    Execute(params.start_command, user=params.user, logoutput=True)
    check_process_status(params.jkg_pid_file)

    time.sleep(delay_checks)

    with open(params.jkg_pid_file, 'r') as fp:
      try:
        os.kill(int(fp.read().strip()), 0)
      except OSError as ose:
        if ose.errno != errno.EPERM:
          raise Fail("Error starting Jupyter Kernel Gateway. Check {0} for the possible cause.".format(params.log_dir + "/jupyter_kernel_gateway.log"))
        else:
          # non-root install might have to resort to status check but
          # with the side-effect that any error might only reflected during
          # the status check after a minute rather than immediately 
          check_process_status(params.jkg_pid_file)

  def status(self, env):
    import status_params as params
    env.set_params(params)
    check_process_status(params.jkg_pid_file)

  def configure(self, env):
    import jkg_toree_params as params
    env.set_params(params)

    # Create directories used by the service and service user
    # if they were updated
    Directory([params.home_dir, params.jkg_pid_dir, params.log_dir],
              mode=0755,
              create_parents=True,
              owner=params.user,
              group=params.group,
              recursive_ownership=True)

    # Run commands to configure Toree and PySpark
    for command in params.toree_configure_commands: Execute(command, logoutput=True)
    for command in params.pyspark_configure_commands: Execute(command, logoutput=True)

if __name__ == "__main__":
  GatewayKernels().execute()
