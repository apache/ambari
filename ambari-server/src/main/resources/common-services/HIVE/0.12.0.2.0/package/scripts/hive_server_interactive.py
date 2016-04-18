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
# Python Imports
import subprocess
import os
import re
import time
import shutil
from datetime import datetime

# Ambari Commons & Resource Management imports
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.resources.system import Execute

# Imports needed for Rolling/Express Upgrade
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from pwd import getpwnam

# Local Imports
from setup_ranger_hive import setup_ranger_hive
from hive_service_interactive import hive_service_interactive
from hive_interactive import hive_interactive
from hive_server import HiveServerDefault


class HiveServerInteractive(Script):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerInteractiveDefault(HiveServerInteractive):

    def get_stack_to_component(self):
      import params
      return {params.stack_name: "hive-server2-hive2"}

    def install(self, env):
      import params
      self.install_packages(env)

    def configure(self, env):
      import params
      env.set_params(params)
      hive_interactive(name='hiveserver2')

    def pre_upgrade_restart(self, env, upgrade_type=None):
      Logger.info("Executing Hive Server Interactive Stack Upgrade pre-restart")
      import params
      env.set_params(params)

      if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
        stack_select.select("hive-server2-hive2", params.version)
        conf_select.select(params.stack_name, "hive2", params.version)

        # Copy hive.tar.gz and tez.tar.gz used by Hive Interactive to HDFS
        resource_created = copy_to_hdfs(
          "hive2",
          params.user_group,
          params.hdfs_user,
          host_sys_prepped=params.host_sys_prepped)

        resource_created = copy_to_hdfs(
          "tez_hive2",
          params.user_group,
          params.hdfs_user,
          host_sys_prepped=params.host_sys_prepped) or resource_created

        if resource_created:
          params.HdfsResource(None, action="execute")

    def start(self, env, upgrade_type=None):
      import params
      env.set_params(params)
      self.configure(env)

      if params.security_enabled:
        # Do the security setup, internally calls do_kinit()
        self.setup_security()

      # TODO : We need have conditional [re]start of LLAP once "status check command" for LLAP is ready.
      # Check status and based on that decide on [re]starting.

      # Start LLAP before Hive Server Interactive start.
      # TODO, why does LLAP have to be started before Hive Server Interactive???
      status = self._llap_start(env)
      if not status:
        raise Fail("Skipping start of Hive Server Interactive since could not start LLAP.")

      # TODO : test the workability of Ranger and Hive2 during upgrade
      # setup_ranger_hive(upgrade_type=upgrade_type)
      hive_service_interactive('hiveserver2', action='start', upgrade_type=upgrade_type)


    def stop(self, env, upgrade_type=None):
      import params
      env.set_params(params)

      if params.security_enabled:
        self.do_kinit()

      # TODO, why must Hive Server Interactive be stopped before LLAP???

      # Stop Hive Interactive Server first
      # TODO : Upgrade check comes here.
      hive_service_interactive('hiveserver2', action='stop')

      self._llap_stop(env)

    def status(self, env):
      import status_params
      env.set_params(status_params)
      pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")

      # Recursively check all existing gmetad pid files
      check_process_status(pid_file)
      # TODO : Check the LLAP app status as well.

    def security_status(self, env):
      HiveServerDefault.security_status(env)

    def restart_llap(self, env):
      """
      Custom command to Restart LLAP
      """
      Logger.info("Custom Command to retart LLAP")
      import params
      env.set_params(params)

      if params.security_enabled:
        self.do_kinit()

      self._llap_stop(env)
      self._llap_start(env)

    def _llap_stop(self, env):
      import params
      Logger.info("Stopping LLAP")
      SLIDER_APP_NAME = "llap0"

      stop_cmd = ["slider", "stop", SLIDER_APP_NAME]
      Logger.info(format("Command: {stop_cmd}"))

      code, output, error = shell.call(stop_cmd, user=params.hive_user, stderr=subprocess.PIPE, logoutput=True)
      if code == 0:
        Logger.info(format("Stopped {SLIDER_APP_NAME} application on Slider successfully"))
      elif code == 69 and output is not None and "Unknown application instance" in output:
        Logger.info(format("Application {SLIDER_APP_NAME} was already stopped on Slider"))
      else:
        raise Fail(format("Could not stop application {SLIDER_APP_NAME} on Slider"))

      # Will exit with code 4 if need to run with "--force" to delete directories and registries.
      destroy_cmd = ['slider', 'destroy', SLIDER_APP_NAME, "--force"]
      code, output, error = shell.call(destroy_cmd, user=params.hive_user, stderr=subprocess.PIPE)
      if code == 0:
        Logger.info(format("Successfully removed slider app {SLIDER_APP_NAME}."))
      else:
        message = format("Could not remove slider app {SLIDER_APP_NAME}. Please retry this task.")
        if error is not None:
          message += " " + error
        raise Fail(message)

    """
    Controls the start of LLAP.
    """
    def _llap_start(self, env, cleanup=False):
      import params
      env.set_params(params)
      Logger.info("Starting LLAP")

      # TODO, start only if not already running.
      # TODO : Currently hardcoded the params. Need to read the suggested values from hive2/hive-site.xml.
      # TODO, ensure that script works as hive from cmd when not cd'ed in /home/hive
      # Needs permission to write to hive home dir.

      unique_name = "llap-slider%s" % datetime.utcnow().strftime('%Y-%m-%d_%H-%M-%S')

      cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llap --instances 1 "
                   "-slider-am-container-mb {slider_am_container_mb} --loglevel INFO --output {unique_name}")

      if params.security_enabled:
        cmd += format(" --slider-keytab-dir .slider/keytabs/{params.hive_user}/ --slider-keytab "
                      "{hive_llap_keytab_file} --slider-principal {hive_headless_keytab}")

      run_file_path = None
      try:
        Logger.info(format("Command: {cmd}"))
        cmd = cmd.split()
        code, output, error = shell.checked_call(cmd, user=params.hive_user, stderr=subprocess.PIPE, logoutput=True)

        if code != 0 or output is None:
          raise Fail("Command failed with either non-zero return code or no output.")

        # E.g., output:
        # Prepared llap-slider-05Apr2016/run.sh for running LLAP on Slider
        exp = r"Prepared (.*?run.sh) for running LLAP"
        m = re.match(exp, output, re.I)
        if m and len(m.groups()) == 1:
          run_file_name = m.group(1)
          run_file_path = os.path.join(params.hive_user_home_dir, run_file_name)
        else:
          raise Fail("Did not find run.sh file in output: " + str(output))

        Logger.info(format("Run file path: {run_file_path}"))
        if os.path.isfile(run_file_path):
          Execute(run_file_path, user=params.hive_user)

          # TODO : Sleep below is not a good idea. We need to check the status of LLAP app to figure out it got
          # launched properly and is in running state. Then go ahead with Hive Interactive Server start.
          Logger.info("Sleeping for 30 secs")
          # Important to mock this sleep call during unit tests.
          time.sleep(30)
          Logger.info("LLAP app deployed successfully.")
          return True
        else:
          raise Fail(format("Did not find run file {run_file_path}"))
      except:
        # Attempt to clean up the packaged application, or potentially rename it with a .bak
        if run_file_path is not None and cleanup:
          try:
            parent_dir = os.path.dirname(run_file_path)
            if os.path.isdir(parent_dir):
              shutil.rmtree(parent_dir)
          except Exception, e:
            Logger.error("Could not cleanup LLAP app package. Error: " + str(e))

        # throw the original exception
        raise
      return False

    """
    Does kinit and copies keytab for Hive/LLAP to HDFS.
    """
    def setup_security(self):
      import params

      self.do_kinit()

      # Copy params.hive_llap_keytab_file to hdfs://<host>:<port>/user/<hive_user>/.slider/keytabs/<hive_user> , required by LLAP
      slider_keytab_install_cmd = format("slider install-keytab --keytab {params.hive_llap_keytab_file} --folder {params.hive_user} --overwrite")
      Execute(slider_keytab_install_cmd, user=params.hive_user)

    def do_kinit(self):
      import params

      hive_interactive_kinit_cmd = format("{kinit_path_local} -kt {hive_server2_keytab} {hive_principal}; ")
      Execute(hive_interactive_kinit_cmd, user=params.hive_user)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerInteractiveWindows(HiveServerInteractive):

  def status(self, env):
    pass

if __name__ == "__main__":
  HiveServerInteractive().execute()