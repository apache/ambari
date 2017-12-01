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
from resource_management.core.source import InlineTemplate
from resource_management.core.resources.system import Execute, Directory

# Imports needed for Rolling/Express Upgrade
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

# Local Imports
from setup_ranger_hive import setup_ranger_hive
from hive_service_interactive import hive_service_interactive
from hive_interactive import hive_interactive
from hive_server import HiveServerDefault
from setup_ranger_hive_interactive import setup_ranger_hive_interactive

import traceback

class HiveServerInteractive(Script):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServerInteractiveDefault(HiveServerInteractive):
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
        stack_select.select_packages(params.version)

        # Copy hive.tar.gz and tez.tar.gz used by Hive Interactive to HDFS
        resource_created = copy_to_hdfs(
          "hive2",
          params.user_group,
          params.hdfs_user,
          skip=params.sysprep_skip_copy_tarballs_hdfs)

        resource_created = copy_to_hdfs(
          "tez_hive2",
          params.user_group,
          params.hdfs_user,
          skip=params.sysprep_skip_copy_tarballs_hdfs) or resource_created

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
      status = self._llap_start(env)
      if not status:
        # if we couldnt get LLAP in RUNNING or RUNNING_ALL state, stop LLAP process before bailing out.
        self._llap_stop(env)
        raise Fail("Skipping START of Hive Server Interactive since LLAP app couldn't be STARTED.")

      # TODO : test the workability of Ranger and Hive2 during upgrade
      setup_ranger_hive_interactive(upgrade_type=upgrade_type)
      hive_service_interactive('hiveserver2', action='start', upgrade_type=upgrade_type)


    def stop(self, env, upgrade_type=None):
      import params
      env.set_params(params)

      if params.security_enabled:
        self.do_kinit()

      # Stop Hive Interactive Server first
      hive_service_interactive('hiveserver2', action='stop')

      if not params.is_restart_command:
        self._llap_stop(env)
      else:
        Logger.info("LLAP stop is skipped as its a restart command")

    def status(self, env):
      import status_params
      env.set_params(status_params)

      # We are not doing 'llap' status check done here as part of status check for 'HSI', as 'llap' status
      # check is a heavy weight operation.

      # Recursively check all existing gmetad pid files
      check_process_status(status_params.hive_interactive_pid)

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

      stop_cmd = ["slider", "stop", params.llap_app_name]

      code, output, error = shell.call(stop_cmd, user=params.hive_user, stderr=subprocess.PIPE, logoutput=True)
      if code == 0:
        Logger.info(format("Stopped {params.llap_app_name} application on Slider successfully"))
      elif code == 69 and output is not None and "Unknown application instance" in output:
        Logger.info(format("Application {params.llap_app_name} was already stopped on Slider"))
      else:
        raise Fail(format("Could not stop application {params.llap_app_name} on Slider. {error}\n{output}"))

      # Will exit with code 4 if need to run with "--force" to delete directories and registries.
      Execute(('slider', 'destroy', params.llap_app_name, "--force"),
              user=params.hive_user,
              timeout=30,
              ignore_failures=True,
      )

    """
    Controls the start of LLAP.
    """
    def _llap_start(self, env, cleanup=False):
      import params
      env.set_params(params)

      if params.hive_server_interactive_ha:
        """
        Check llap app state
        """
        Logger.info("HSI HA is enabled. Checking if LLAP is already running ...")
        if params.stack_supports_hive_interactive_ga:
          status = self.check_llap_app_status_in_llap_ga(params.llap_app_name, 2, params.hive_server_interactive_ha)
        else:
          status = self.check_llap_app_status_in_llap_tp(params.llap_app_name, 2, params.hive_server_interactive_ha)

        if status:
          Logger.info("LLAP app '{0}' is already running.".format(params.llap_app_name))
          return True
        else:
          Logger.info("LLAP app '{0}' is not running. llap will be started.".format(params.llap_app_name))
        pass

      # Call for cleaning up the earlier run(s) LLAP package folders.
      self._cleanup_past_llap_package_dirs()

      Logger.info("Starting LLAP")
      LLAP_PACKAGE_CREATION_PATH = Script.get_tmp_dir()

      unique_name = "llap-slider%s" % datetime.utcnow().strftime('%Y-%m-%d_%H-%M-%S')

      cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llap --slider-am-container-mb {params.slider_am_container_mb} "
                   "--size {params.llap_daemon_container_size}m --cache {params.hive_llap_io_mem_size}m --xmx {params.llap_heap_size}m "
                   "--loglevel {params.llap_log_level} {params.llap_extra_slider_opts} --output {LLAP_PACKAGE_CREATION_PATH}/{unique_name}")

      # Append params that are supported from Hive llap GA version.
      if params.stack_supports_hive_interactive_ga:
        # Figure out the Slider Anti-affinity to be used.
        # YARN does not support anti-affinity, and therefore Slider implements AA by the means of exclusion lists, i.e, it
        # starts containers one by one and excludes the nodes it gets (adding a delay of ~2sec./machine). When the LLAP
        # container memory size configuration is more than half of YARN node memory, AA is implicit and should be avoided.
        slider_placement = 4
        if long(params.llap_daemon_container_size) > (0.5 * long(params.yarn_nm_mem)):
          slider_placement = 0
          Logger.info("Setting slider_placement : 0, as llap_daemon_container_size : {0} > 0.5 * "
                      "YARN NodeManager Memory({1})".format(params.llap_daemon_container_size, params.yarn_nm_mem))
        else:
          Logger.info("Setting slider_placement: 4, as llap_daemon_container_size : {0} <= 0.5 * "
                     "YARN NodeManager Memory({1})".format(params.llap_daemon_container_size, params.yarn_nm_mem))
        cmd += format(" --slider-placement {slider_placement} --skiphadoopversion --skiphbasecp --instances {params.num_llap_daemon_running_nodes}")

        # Setup the logger for the ga version only
        cmd += format(" --logger {params.llap_logger}")
      else:
        cmd += format(" --instances {params.num_llap_nodes}")
      if params.security_enabled:
        llap_keytab_splits = params.hive_llap_keytab_file.split("/")
        Logger.debug("llap_keytab_splits : {0}".format(llap_keytab_splits))
        cmd += format(" --slider-keytab-dir .slider/keytabs/{params.hive_user}/ --slider-keytab "
                      "{llap_keytab_splits[4]} --slider-principal {params.hive_llap_principal}")

      # Add the aux jars if they are specified. If empty, dont need to add this param.
      if params.hive_aux_jars:
        cmd+= format(" --auxjars {params.hive_aux_jars}")

      # Append args.
      llap_java_args = InlineTemplate(params.llap_app_java_opts).get_content()
      cmd += format(" --args \" {llap_java_args}\"")
      # Append metaspace size to args.
      if params.java_version > 7 and params.llap_daemon_container_size > 4096:
        if params.llap_daemon_container_size <= 32768:
          metaspaceSize = "256m"
        else:
          metaspaceSize = "1024m"
        cmd = cmd[:-1] + " -XX:MetaspaceSize="+metaspaceSize+ "\""

      run_file_path = None
      try:
        Logger.info(format("LLAP start command: {cmd}"))
        code, output, error = shell.checked_call(cmd, user=params.hive_user, quiet = True, stderr=subprocess.PIPE, logoutput=True)

        if code != 0 or output is None:
          raise Fail("Command failed with either non-zero return code or no output.")

        # E.g., output:
        # Prepared llap-slider-05Apr2016/run.sh for running LLAP on Slider
        exp = r"Prepared (.*?run.sh) for running LLAP"
        run_file_path = None
        out_splits = output.split("\n")
        for line in out_splits:
          line = line.strip()
          m = re.match(exp, line, re.I)
          if m and len(m.groups()) == 1:
            run_file_name = m.group(1)
            run_file_path = os.path.join(params.hive_user_home_dir, run_file_name)
            break
        if not run_file_path:
          raise Fail("Did not find run.sh file in output: " + str(output))

        Logger.info(format("Run file path: {run_file_path}"))
        Execute(run_file_path, user=params.hive_user, logoutput=True)
        Logger.info("Submitted LLAP app name : {0}".format(params.llap_app_name))

        # We need to check the status of LLAP app to figure out it got
        # launched properly and is in running state. Then go ahead with Hive Interactive Server start.
        if params.stack_supports_hive_interactive_ga:
          status = self.check_llap_app_status_in_llap_ga(params.llap_app_name, params.num_retries_for_checking_llap_status)
        else:
          status = self.check_llap_app_status_in_llap_tp(params.llap_app_name, params.num_retries_for_checking_llap_status)
        if status:
          Logger.info("LLAP app '{0}' deployed successfully.".format(params.llap_app_name))
          return True
        else:
          Logger.error("LLAP app '{0}' deployment unsuccessful.".format(params.llap_app_name))
          return False
      except:
        # Attempt to clean up the packaged application, or potentially rename it with a .bak
        if run_file_path is not None and cleanup:
          parent_dir = os.path.dirname(run_file_path)
          Directory(parent_dir,
                    action = "delete",
                    ignore_failures = True,
          )

        # throw the original exception
        raise

    """
    Checks and deletes previous run 'LLAP package' folders, ignoring three latest packages.
    Last three are are ignore for debugging/reference purposes.
    Helps in keeping check on disk space used.
    """
    def _cleanup_past_llap_package_dirs(self):
      try:
        import params
        Logger.info("Determining previous run 'LLAP package' folder(s) to be deleted ....")
        llap_package_folder_name_prefix = "llap-slider" # Package name is like : llap-sliderYYYY-MM-DD-HH:MM:SS
        num_folders_to_retain = 3  # Hardcoding it as of now, as no considerable use was found to provide an env param.
        file_names = [dir_name for dir_name in os.listdir(Script.get_tmp_dir())
                      if dir_name.startswith(llap_package_folder_name_prefix)]

        file_names.sort()
        del file_names[-num_folders_to_retain:] # Ignore 'num_folders_to_retain' latest package folders.
        Logger.info("Previous run 'LLAP package' folder(s) to be deleted = {0}".format(file_names))

        if file_names:
          for path in file_names:
            abs_path = Script.get_tmp_dir()+"/"+path
            Directory(abs_path,
                      action = "delete",
                      ignore_failures = True
            )
        else:
          Logger.info("No '{0}*' folder deleted.".format(llap_package_folder_name_prefix))
      except:
        Logger.exception("Exception while doing cleanup for past 'LLAP package(s)':")



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

    """
    Get llap app status data for LLAP Tech Preview code base.
    """
    def _get_llap_app_status_info_in_llap_tp(self, app_name):
      import status_params
      LLAP_APP_STATUS_CMD_TIMEOUT = 0

      llap_status_cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llapstatus --name {app_name} --findAppTimeout {LLAP_APP_STATUS_CMD_TIMEOUT}")
      code, output, error = shell.checked_call(llap_status_cmd, user=status_params.hive_user, stderr=subprocess.PIPE,
                                               logoutput=False)
      Logger.info("Received 'llapstatus' command 'output' : {0}".format(output))
      if code == 0:
        return self._make_valid_json(output)
      else:
        Logger.info("'LLAP status command' output : ", output)
        Logger.info("'LLAP status command' error : ", error)
        Logger.info("'LLAP status command' exit code : ", code)
        raise Fail("Error getting LLAP app status. ")

    """
    Get llap app status data for LLAP GA code base.

    Parameters: 'percent_desired_instances_to_be_up' : A value b/w 0.0 and 1.0.
                'total_timeout' : Total wait time while checking the status via llapstatus command
                'refresh_rate' : Frequency of polling for llapstatus.
    """
    def _get_llap_app_status_info_in_llap_ga(self, percent_desired_instances_to_be_up, total_timeout, refresh_rate):
      import status_params

      # llapstatus comamnd : llapstatus -w -r <percent containers to wait for to be Up> -i <refresh_rate> -t <total timeout for this comand>
      # -w : Watch mode waits until all LLAP daemons are running or subset of the nodes are running (threshold can be specified via -r option) (Default wait until all nodes are running)
      # -r : When watch mode is enabled (-w), wait until the specified threshold of nodes are running (Default 1.0 which means 100% nodes are running)
      # -i : Amount of time in seconds to wait until subsequent status checks in watch mode (Default: 1sec)
      # -t : Exit watch mode if the desired state is not attained until the specified timeout (Default: 300sec)
      #
      #            example : llapstatus -w -r 0.8 -i 2 -t 150
      llap_status_cmd = format("{stack_root}/current/hive-server2-hive2/bin/hive --service llapstatus -w -r {percent_desired_instances_to_be_up} -i {refresh_rate} -t {total_timeout}")
      Logger.info("\n\n\n\n\n");
      Logger.info("LLAP status command : {0}".format(llap_status_cmd))
      code, output, error = shell.checked_call(llap_status_cmd, user=status_params.hive_user, quiet=True, stderr=subprocess.PIPE,
                                               logoutput=True)

      if code == 0:
        return self._make_valid_json(output)
      else:
        Logger.info("'LLAP status command' output : ", output)
        Logger.info("'LLAP status command' error : ", error)
        Logger.info("'LLAP status command' exit code : ", code)
        raise Fail("Error getting LLAP app status. ")




    """
    Remove extra lines (begginning/end) from 'llapstatus' status output (eg: because of MOTD logging) so as to have 
    a valid JSON data to be passed in to JSON converter.
    """
    def _make_valid_json(self, output):
      '''

      Note: Extra lines (eg: because of MOTD) may be at the start or the end (some other logging getting appended)
      of the passed-in data.

      Sample expected JSON to be passed for 'loads' is either of the form :

      Case 'A':
      {
          "amInfo" : {
          "appName" : "llap0",
          "appType" : "org-apache-slider",
          "appId" : "APP1",
          "containerId" : "container_1466036628595_0010_01_000001",
          "hostname" : "hostName",
          "amWebUrl" : "http://hostName:port/"
        },
        "state" : "LAUNCHING",
        ....
        "desiredInstances" : 1,
        "liveInstances" : 0,
        ....
        ....
      }

      or

      Case 'B':
      {
        "state" : "APP_NOT_FOUND"
      }

      '''
      splits = output.split("\n")

      len_splits = len(splits)
      if (len_splits < 3):
        raise Fail ("Malformed JSON data received from 'llapstatus' command. Exiting ....")

      # Firstly, remove extra lines from the END.
      updated_splits = []
      for itr, line in enumerate(reversed(splits)):
        if line == "}": # Our assumption of end of JSON data.
          updated_splits = splits[:-itr]
          break

      if len(updated_splits) > 0:
        splits = updated_splits
        len_splits = len(splits)


      # Secondly, remove extra lines from the BEGGINNING.
      marker_idx = None # To detect where from to start reading for JSON data
      for idx, split in enumerate(splits):
        curr_elem = split.strip()
        if idx+2 > len_splits:
          raise Fail("Iterated over the received 'llapstatus' comamnd. Couldn't validate the received output for JSON parsing.")
        next_elem = (splits[(idx + 1)]).strip()
        if curr_elem == "{":
          if next_elem == "\"amInfo\" : {" and (splits[len_splits-1]).strip() == '}':
            # For Case 'A'
            marker_idx = idx
            break;
          elif idx+3 == len_splits and next_elem.startswith('"state" : ') and (splits[idx + 2]).strip() == '}':
              # For Case 'B'
              marker_idx = idx
              break;


      # Remove extra logging from possible JSON output
      if marker_idx is None:
        raise Fail("Couldn't validate the received output for JSON parsing.")
      else:
        if marker_idx != 0:
          del splits[0:marker_idx]

      scanned_output = '\n'.join(splits)
      llap_app_info = json.loads(scanned_output)
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
    def check_llap_app_status_in_llap_tp(self, llap_app_name, num_retries, return_immediately_if_stopped=False):
      curr_time = time.time()
      num_retries = int(num_retries)
      if num_retries <= 0:
        Logger.info("Read 'num_retries' as : {0}. Setting it to : {1}".format(num_retries, 2))
        num_retries = 2
      if num_retries > 20:
        Logger.info("Read 'num_retries' as : {0}. Setting it to : {1}".format(num_retries, 20))
        num_retries = 20

      @retry(times=num_retries, sleep_time=2, err_class=Fail)
      def do_retries():
        llap_app_info = self._get_llap_app_status_info_in_llap_tp(llap_app_name)
        return self._verify_llap_app_status(llap_app_info, llap_app_name, return_immediately_if_stopped, curr_time)

      try:
        status = do_retries()
        return status
      except Exception, e:
        Logger.info("LLAP app '{0}' did not come up after a wait of {1} seconds.".format(llap_app_name,
                                                                                          time.time() - curr_time))
        traceback.print_exc()
        return False

    def check_llap_app_status_in_llap_ga(self, llap_app_name, num_retries, return_immediately_if_stopped=False):
      curr_time = time.time()
      total_timeout = int(num_retries) * 20; # Total wait time while checking the status via llapstatus command
      Logger.debug("Calculated 'total_timeout' : {0} using config 'num_retries_for_checking_llap_status' : {1}".format(total_timeout, num_retries))
      refresh_rate = 2 # Frequency of checking the llapstatus
      percent_desired_instances_to_be_up = 80 # Out of 100.
      llap_app_info = self._get_llap_app_status_info_in_llap_ga(percent_desired_instances_to_be_up/100.0, total_timeout, refresh_rate)

      try:
        return self._verify_llap_app_status(llap_app_info, llap_app_name, return_immediately_if_stopped, curr_time)
      except Exception as e:
        Logger.info(e.message)
        return False

    def get_log_folder(self):
      import params
      return params.hive_log_dir

    def get_user(self):
      import params
      return params.hive_user

    def _verify_llap_app_status(self, llap_app_info, llap_app_name, return_immediately_if_stopped, curr_time):
      if llap_app_info is None or 'state' not in llap_app_info:
        Logger.error("Malformed JSON data received for LLAP app. Exiting ....")
        return False

      # counters based on various states.
      live_instances = 0
      desired_instances = 0
      percent_desired_instances_to_be_up = 80 # Used in 'RUNNING_PARTIAL' state.
      if return_immediately_if_stopped and (llap_app_info['state'].upper() in ('APP_NOT_FOUND', 'COMPLETE')):
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
          Logger.info("LLAP app '{0}' in '{1}' state. Live Instances : '{2}'  >= {3}% of Desired Instances : " \
                      "'{4}'.".format(llap_app_name, llap_app_info['state'],
                                      llap_app_info['liveInstances'],
                                      percent_desired_instances_to_be_up,
                                      llap_app_info['desiredInstances']))
          return True
        else:
          Logger.info("LLAP app '{0}' in '{1}' state. Live Instances : '{2}'. Desired Instances : " \
                      "'{3}' after {4} secs.".format(llap_app_name, llap_app_info['state'],
                                                     llap_app_info['liveInstances'],
                                                     llap_app_info['desiredInstances'],
                                                     time.time() - curr_time))
          raise Fail("App state is RUNNING_PARTIAL. Live Instances : '{0}', Desired Instance : '{1}'".format(llap_app_info['liveInstances'],
                                                                                                           llap_app_info['desiredInstances']))
      elif llap_app_info['state'].upper() in ['APP_NOT_FOUND', 'LAUNCHING', 'COMPLETE']:
        status_str = format("LLAP app '{0}' current state is {1}.".format(llap_app_name, llap_app_info['state']))
        Logger.info(status_str)
        raise Fail(status_str)
      else:  # Covers any unknown that we get.
        Logger.info(
          "LLAP app '{0}' current state is '{1}'. Expected : 'RUNNING'.".format(llap_app_name, llap_app_info['state']))
        return False

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServerInteractiveWindows(HiveServerInteractive):

  def status(self, env):
    pass

if __name__ == "__main__":
  HiveServerInteractive().execute()
