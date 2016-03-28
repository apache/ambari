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
import time
import sys
import shutil
import subprocess

from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import File, Execute
from resource_management.core.resources.service import Service
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_user
from resource_management.libraries.functions.hive_check import check_thrift_port_sasl
from resource_management.libraries.functions import get_user_call_output

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from hive_service import check_fs_root



@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive_service_interactive(name, action='start', upgrade_type=None):
  pass


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive_service_interactive(name, action='start', upgrade_type=None):
  import params

  pid_file = format("{hive_pid_dir}/{hive_interactive_pid}")
  cmd = format("{start_hiveserver2_interactive_path} {hive_pid_dir}/hive-server2-interactive.out {hive_log_dir}/hive-server2-interactive.err {pid_file} {hive_server_interactive_conf_dir} {hive_log_dir}")

  # TODO : Kerberos work for Hive2

  pid = get_user_call_output.get_user_call_output(format("cat {pid_file}"), user=params.hive_user, is_checked_call=False)[1]
  process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

  if action == 'start':
    check_fs_root(params.hive_server_interactive_conf_dir, params.execute_path_hive_interactive)
    daemon_cmd = cmd
    hadoop_home = params.hadoop_home
    hive_interactive_bin = "hive2"

    # TODO : Upgrade checks required here.

    Execute(daemon_cmd,
            user = params.hive_user,
            environment = { 'HADOOP_HOME': hadoop_home, 'JAVA_HOME': params.java64_home, 'HIVE_BIN': hive_interactive_bin },
            path = params.execute_path,
            not_if = process_id_exists_command)

    if params.hive_jdbc_driver == "com.mysql.jdbc.Driver" or \
        params.hive_jdbc_driver == "org.postgresql.Driver" or \
        params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
      db_connection_check_command = format(
        "{java64_home}/bin/java -cp {check_db_connection_jar}:{target} org.apache.ambari.server.DBConnectionVerification '{hive_jdbc_connection_url}' {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_jdbc_driver}")
      Execute(db_connection_check_command,
              path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10)
  elif action == 'stop':

    daemon_kill_cmd = format("{sudo} kill {pid}")
    daemon_hard_kill_cmd = format("{sudo} kill -9 {pid}")

    Execute(daemon_kill_cmd,
            not_if = format("! ({process_id_exists_command})")
            )

    wait_time = 5
    Execute(daemon_hard_kill_cmd,
            not_if = format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )")
            )

    # check if stopped the process, else fail the task
    Execute(format("! ({process_id_exists_command})"),
            tries=20,
            try_sleep=3,
            )

    File(pid_file,
         action = "delete"
         )


def start_llap(self):
  import params
  abs_dir_path = ''
  try:
    # TODO : Currently hardcoded the params. Need to read the suggested values from hive2/hive-site.xml.
    cmd = ["/usr/hdp/current/hive-server2-hive2/bin/hive", "--service", "llap", "--instances", "1",
           "-slider-am-container-mb", "341", "--loglevel", "INFO"]
    code, output, error = shell.checked_call(cmd, user=params.hive_user, stderr=subprocess.PIPE)

    # TODO : ideally we should check error status, but currently LLAP package creation throws 'ClassNotFoundException'
    # for 'JsonSerDe', but still goes ahead and creates the package which works.
    if output is not None:
      # Expected 'output' string is of the form : "Prepared llap-slider-[DDMMYYYY]/run.sh for running LLAP on Slider"
      Logger.info("LLAP package creation output : {0}".format(output))
      splits = output.split()
      if len(splits) > 2:
        if "llap-slider-" in splits[1]:
          llap_dir, llap_run_file = (splits[1]).split("/")
          abs_dir_path = os.path.join(params.hive_user_home_dir, llap_dir)
          run_file_abs_path = os.path.join(abs_dir_path, llap_run_file)
          file_exists = os.path.isfile(run_file_abs_path)
          if file_exists:
            Execute(run_file_abs_path, user=params.hive_user)
            # TODO : Sleep below is not a good idea. We need to check the status of LLAP app to figure out it got
            # launched properly and is in running state. Then go ahead with Hive Interactive Server start.
            time.sleep(30)
            Logger.info("LLAP app deployed successfully.")
            return True
          else:
            Logger.error("LLAP slider package : {0} , not present in path : {1}. Exiting ... ".format(llap_dir,
                                                                                                      params.hive_user_home_dir))
            return False
      else:
        # Looks like assumption of successful/correct output string being "Prepared llap-slider-[DDMMYYYY]/run.sh
        # for running LLAP on Slider" has changed.
        Logger.error("Couldn't parse the message {0} for LLAP slider package. Exiting ... ".format(output))
        return False
    else:
      Logger.error(
        "Error while creating the LLAP slider package. \n Error Code : {0} \n Output : {1}".format(error, output))
      return False

  except:
    Logger.error("Error: {0}".format(sys.exc_info()))
    return False
  finally:
    # Do the cleanup
    dir_exists = os.path.isdir(abs_dir_path)
    if dir_exists:
      shutil.rmtree(abs_dir_path)


def stop_llap(self):
  import params
  try:
    stop_cmd = ("slider", "stop", "llap0")
    print "STOP cmd  = ",stop_cmd
    Execute(stop_cmd, user=params.hive_user, timeout=30, wait_for_finish=True)

    # TODO : Check status of LLAP app as STOPPED/FINSIHED, before destroying.
    destroy_cmd = ['slider', 'destroy', 'llap0']
    code, output, error = shell.checked_call(destroy_cmd, user=params.hive_user, stderr=subprocess.PIPE)
    if error is None or not error:
      Logger.info("Removed slider app : llap0.")
    else:
      Logger.error("Problem removing slider app : llap0. Exiting ....")

  except:
    Logger.info("Error: {0}".format(sys.exc_info()))
