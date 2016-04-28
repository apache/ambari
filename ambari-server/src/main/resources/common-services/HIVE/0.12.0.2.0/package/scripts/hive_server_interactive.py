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
import json

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

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.decorator import retry

# Local Imports
from setup_ranger_hive import setup_ranger_hive
from hive_service_interactive import hive_service_interactive
from hive_interactive import hive_interactive
from hive_server import HiveServerDefault


class HiveServerInteractive(Script):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerInteractiveDefault(HiveServerInteractive):

    def get_component_name(self):
      return "hive-server2-hive2"

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
        raise Fail("Skipping START of Hive Server Interactive since LLAP app couldn't be STARTED.")

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

      # TODO : LLAP app status check

      pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")
      # Recursively check all existing gmetad pid files
      check_process_status(pid_file)

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
      LLAP_APP_NAME = 'llap0'

      # TODO, start only if not already running.
      # TODO : Currently hardcoded the params. Need to read the suggested values from hive2/hive-site.xml.
      # TODO, ensure that script works as hive from cmd when not cd'ed in /home/hive
      # Needs permission to write to hive home dir.

      unique_name = "llap-slider%s" % datetime.utcnow().strftime('%Y-%m-%d_%H-%M-%S')

      cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llap --instances {params.num_llap_nodes}"
                   " --slider-am-container-mb {params.slider_am_container_mb} --size {params.llap_daemon_container_size}m "
                   " --cache {params.hive_llap_io_mem_size}m --xmx {params.llap_heap_size}m --loglevel {params.llap_log_level}"
                   " --output {unique_name}")
      if params.security_enabled:
        llap_keytab_splits = params.hive_llap_keytab_file.split("/")
        Logger.debug("llap_keytab_splits : {0}".format(llap_keytab_splits))
        cmd += format(" --slider-keytab-dir .slider/keytabs/{params.hive_user}/ --slider-keytab "
                      "{llap_keytab_splits[4]} --slider-principal {hive_headless_keytab}")

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
          Logger.info("Submitted LLAP app name : {0}".format(LLAP_APP_NAME))

          status = self.check_llap_app_status(LLAP_APP_NAME, params.num_retries_for_checking_llap_status)
          if status:
            Logger.info("LLAP app '{0}' deployed successfully.".format(LLAP_APP_NAME))
            return True
          else:
            return False
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

      hive_interactive_kinit_cmd = format("{kinit_path_local} -kt {params.hive_server2_keytab} {params.hive_principal}; ")
      Execute(hive_interactive_kinit_cmd, user=params.hive_user)

      llap_kinit_cmd = format("{kinit_path_local} -kt {params.hive_llap_keytab_file} {params.hive_headless_keytab}; ")
      Execute(llap_kinit_cmd, user=params.hive_user)

    """
    Get llap app status data.
    """
    def _get_llap_app_status_info(self, app_name):
      import status_params

      llap_status_cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llapstatus --name {app_name}")
      code, output, error = shell.checked_call(llap_status_cmd, user=status_params.hive_user, stderr=subprocess.PIPE,
                                               logoutput=False)
      llap_app_info = json.loads(output)
      return llap_app_info


    """
    Checks llap app status. The states can be : 'COMPLETE', 'APP_NOT_FOUND', 'RUNNING_PARTIAL', 'RUNNING_ALL' & 'LAUNCHING'.

    if app is in 'APP_NOT_FOUND', 'RUNNING_PARTIAL' and 'LAUNCHING' state:
       we wait for 'num_times_to_wait' to have app in (1). 'RUNNING_ALL' or (2). 'RUNNING_PARTIAL'
       state with 80% or more 'desiredInstances' running and Return True
    else :
       Return False

    Parameters: llap_app_name : deployed llap app name.
                num_retries :   Number of retries to check the LLAP app status.
    """
    def check_llap_app_status(self, llap_app_name, num_retries):
      # counters based on various states.
      curr_time = time.time()

      if num_retries <= 0:
        num_retries = 2
      if num_retries > 20:
        num_retries = 20

      @retry(times=num_retries, sleep_time=15, err_class=Fail)
      def do_retries():
        live_instances = 0
        desired_instances = 0

        percent_desired_instances_to_be_up = 80 # Used in 'RUNNING_PARTIAL' state.
        llap_app_info = self._get_llap_app_status_info(llap_app_name)

        if llap_app_info is None or 'state' not in llap_app_info:
          Logger.error("Malformed JSON data received for LLAP app. Exiting ....")
          return False

        if llap_app_info['state'].upper() == 'RUNNING_ALL':
          Logger.info(
            "LLAP app '{0}' in '{1}' state.".format(llap_app_name, llap_app_info['state']))
          return True
        elif llap_app_info['state'].upper() == 'RUNNING_PARTIAL':
          # Check how many instances were up.
          if 'liveInstances' in llap_app_info and 'desiredInstances' in llap_app_info:
            live_instances = llap_app_info['liveInstances']
            desired_instances = llap_app_info['desiredInstances']
          else:
            Logger.info(
              "LLAP app '{0}' is in '{1}' state, but 'instances' information not available in JSON received. " \
              "Exiting ....".format(llap_app_name, llap_app_info['state']))
            Logger.info(llap_app_info)
            return False
          if desired_instances == 0:
            Logger.info("LLAP app '{0}' desired instance are set to 0. Exiting ....".format(llap_app_name))
            return False

          percentInstancesUp = 0
          if live_instances > 0:
            percentInstancesUp = float(live_instances) / desired_instances * 100
          if percentInstancesUp >= percent_desired_instances_to_be_up:
            Logger.info("Slider app '{0}' in '{1}' state. Live Instances : '{2}'  >= {3}% of Desired Instances : " \
                        "'{4}'".format(llap_app_name, llap_app_info['state'],
                                       llap_app_info['liveInstances'],
                                       percent_desired_instances_to_be_up,
                                       llap_app_info['desiredInstances']))
            return True
          else:
            Logger.info("Slider app '{0}' in '{1}' state. Live Instances : '{2}'. Desired Instances : " \
                        "'{3}' after {4} secs.".format(llap_app_name, llap_app_info['state'],
                                                       llap_app_info['liveInstances'],
                                                       llap_app_info['desiredInstances'],
                                                       time.time() - curr_time))
            raise Fail("App state is RUNNING_PARTIAL. Live Instances : '{0}', Desired Instance : '{1}'".format(llap_app_info['liveInstances'],
                                                                                                           llap_app_info['desiredInstances']))
        elif llap_app_info['state'].upper() in ['APP_NOT_FOUND', 'LAUNCHING']:
          status_str = format("Slider app '{0}' current state is {1}.".format(llap_app_name, llap_app_info['state']))
          Logger.info(status_str)
          raise Fail(status_str)
        else:  # Covers state "COMPLETE" and any unknown that we get.
          Logger.info(
            "Slider app '{0}' current state is '{1}'. Expected : 'RUNNING'".format(llap_app_name, llap_app_info['state']))
          return False

      try:
        status = do_retries()
        return status
      except Exception, e:
        Logger.info("App '{0}' did not come up after a wait of {1} seconds".format(llap_app_name,
                                                                                          time.time() - curr_time))
        return False
      
    def get_log_folder(self):
      import params
      return params.hive_log_dir
    
    def get_user(self):
      import params
      return params.hive_user

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerInteractiveWindows(HiveServerInteractive):

  def status(self, env):
    pass

if __name__ == "__main__":
  HiveServerInteractive().execute()