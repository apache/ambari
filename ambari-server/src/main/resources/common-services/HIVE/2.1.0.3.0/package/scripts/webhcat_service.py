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

Ambari Agent

"""
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.core.resources.service import Service
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Execute, File
import traceback


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def webhcat_service(action='start', rolling_restart=False):
  import params
  if action == 'start' or action == 'stop':
    Service(params.webhcat_server_win_service_name, action=action)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def webhcat_service(action='start', upgrade_type=None):
  import params

  environ = {
    'HADOOP_HOME': params.hadoop_home
  }

  cmd = format('{webhcat_bin_dir}/webhcat_server.sh')

  if action == 'start':
    if upgrade_type is not None and params.version and params.stack_root:
      environ['HADOOP_HOME'] = format("{stack_root}/{version}/hadoop")

    daemon_cmd = format('cd {hcat_pid_dir} ; {cmd} start')
    no_op_test = format('ls {webhcat_pid_file} >/dev/null 2>&1 && ps -p `cat {webhcat_pid_file}` >/dev/null 2>&1')
    try:
      Execute(daemon_cmd,
              user=params.webhcat_user,
              not_if=no_op_test,
              environment = environ)
    except:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      raise
  elif action == 'stop':
    try:
      # try stopping WebHCat using its own script
      graceful_stop(cmd, environ)
    except Fail:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      Logger.info(traceback.format_exc())

    # this will retrieve the PID
    pid_expression = format("`cat {webhcat_pid_file}`")

    # the PID must exist AND'd with the process must be alive
    # the return code here is going to be 0 IFF both conditions are met correctly
    process_id_exists_command = format("ls {webhcat_pid_file} >/dev/null 2>&1 && ps -p {pid_expression} >/dev/null 2>&1")

    # kill command to run
    daemon_hard_kill_cmd = format("{sudo} kill -9 {pid_expression}")

    # check to ensure that it has stopped by looking for the running PID and then killing
    # it forcefully if it exists - the behavior of not-if/only-if is as follows:
    #   not_if return code IS 0
    #   only_if return code is NOT 0
    Execute(daemon_hard_kill_cmd,
      only_if = process_id_exists_command,
      ignore_failures = True)

    try:
      # check if stopped the process, else fail the task
      Execute(format("! ({process_id_exists_command})"))
    except:
      show_logs(params.hcat_log_dir, params.webhcat_user)
      raise

    File(params.webhcat_pid_file, action="delete")

def graceful_stop(cmd, environ):
  """
  Attemps to stop WebHCat using its own shell script. On some versions this may not correctly
  stop the daemon.
  :param cmd: the command to run to stop the daemon
  :param environ: the environment variables to execute the command with
  :return:
  """
  import params
  daemon_cmd = format('{cmd} stop')

  Execute(daemon_cmd,
          user = params.webhcat_user,
          environment = environ)
