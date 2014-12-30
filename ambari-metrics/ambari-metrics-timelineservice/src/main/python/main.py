#!/usr/bin/env python

'''
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
'''

import os
import subprocess
import sys

from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg, print_error_msg
from ambari_metrics_collector.serviceConfiguration import get_java_exe_path, get_java_cp, build_jvm_args, \
  SETUP_ACTION, START_ACTION, STOP_ACTION, RESTART_ACTION, STATUS_ACTION, PID_DIR, EXITCODE_OUT_FILE, \
  SERVER_OUT_FILE, PID_OUT_FILE, SERVER_LOG_FILE


# debug settings
SERVER_START_DEBUG = False
SUSPEND_START_MODE = False

AMS_ENV_CMD = "ams-env.cmd"

SERVER_START_CMD = \
  "-cp {0} {1} " + \
  "-Djava.net.preferIPv4Stack=true " \
  "-Dproc_timelineserver " + \
  "org.apache.hadoop.yarn.server.applicationhistoryservice.ApplicationHistoryServer"
SERVER_START_CMD_DEBUG = \
  "-cp {0} {1} " + \
  "-Djava.net.preferIPv4Stack=true " \
  "-Dproc_timelineserver " + \
  " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend={2} " + \
  "org.apache.hadoop.yarn.server.applicationhistoryservice.ApplicationHistoryServer"

AMC_DIE_MSG = "Ambari Metrics Collector java process died with exitcode {0}. Check {1} for more information."


def save_pid(pid, pidfile):
  """
    Save pid to pidfile.
  """
  try:
    pfile = open(pidfile, "w")
    pfile.write("%s\n" % pid)
  except IOError:
    pass
  finally:
    try:
      pfile.close()
    except:
      pass


def exec_ams_env_cmd(options):
  ams_env_cmd = os.path.join(options.conf_dir, AMS_ENV_CMD)
  if os.path.exists(ams_env_cmd):
    cmds = ["cmd.exe", "/C", ams_env_cmd]
    procAms = subprocess.Popen(cmds, env=os.environ)
    out, err = procAms.communicate()
    if err is not None and err is not "":
      print_warning_msg(AMS_ENV_CMD + " error output: " + err)
    if out is not None and out is not "":
      print_info_msg(AMS_ENV_CMD + " output: " + out)
  else:
    err = 'ERROR: Cannot execute "{0}"'.format(ams_env_cmd)
    raise FatalException(1, err)


def server_process_main(options, scmStatus=None):
  if scmStatus is not None:
    scmStatus.reportStartPending()

  # debug mode
  try:
    global DEBUG_MODE
    DEBUG_MODE = options.debug
  except AttributeError:
    pass

  # stop Java process at startup?
  try:
    global SUSPEND_START_MODE
    SUSPEND_START_MODE = options.suspend_start
  except AttributeError:
    pass

  #options.conf_dir <= --config
  if not os.path.isdir(options.conf_dir):
    err = 'ERROR: Cannot find configuration directory "{0}"'.format(options.conf_dir)
    raise FatalException(1, err)

  #execute ams-env.cmd
  exec_ams_env_cmd(options)

  #Ensure the 3 Hadoop services required are started on the local machine
  if not options.no_embedded_hbase:
    from amc_service import ensure_hdp_service_soft_dependencies
    ensure_hdp_service_soft_dependencies()

  if scmStatus is not None:
    scmStatus.reportStartPending()

  java_exe = get_java_exe_path()
  java_class_path = get_java_cp()
  java_heap_max = build_jvm_args()
  command_base = SERVER_START_CMD_DEBUG if (DEBUG_MODE or SERVER_START_DEBUG) else SERVER_START_CMD
  suspend_mode = 'y' if SUSPEND_START_MODE else 'n'
  command = command_base.format(java_class_path, java_heap_max, suspend_mode)
  if not os.path.exists(PID_DIR):
    os.makedirs(PID_DIR, 0755)

  #Ignore the requirement to run as root. In Windows, by default the child process inherits the security context
  # and the environment from the parent process.
  param_list = java_exe + " " + command

  print_info_msg("Running server: " + str(param_list))
  procJava = subprocess.Popen(param_list, env=os.environ)

  #wait for server process for SERVER_START_TIMEOUT seconds
  print "Waiting for server start..."

  pidJava = procJava.pid
  if pidJava <= 0:
    procJava.terminate()
    exitcode = procJava.returncode
    save_pid(exitcode, EXITCODE_OUT_FILE)

    if scmStatus is not None:
      scmStatus.reportStopPending()

    raise FatalException(-1, AMC_DIE_MSG.format(exitcode, SERVER_OUT_FILE))
  else:
    save_pid(pidJava, PID_OUT_FILE)
    print "Server PID at: " + PID_OUT_FILE
    print "Server out at: " + SERVER_OUT_FILE
    print "Server log at: " + SERVER_LOG_FILE

  if scmStatus is not None:
    scmStatus.reportStarted()

  return procJava

def main():
  from amc_service import init_options_parser, init_service_debug, setup, start, stop, svcstatus

  parser = init_options_parser()
  (options, args) = parser.parse_args()

  options.warnings = []
  options.exit_message = None

  init_service_debug(options)

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  try:
    if action == SETUP_ACTION:
      setup(options)
    elif action == START_ACTION:
      start(options)
    elif action == STOP_ACTION:
      stop()
    elif action == RESTART_ACTION:
      stop()
      start(options)
    elif action == STATUS_ACTION:
      svcstatus(options)
    else:
      parser.error("Invalid action")

    if options.warnings:
      for warning in options.warnings:
        print_warning_msg(warning)
        pass
      options.exit_message = "Ambari Metrics Collector '%s' completed with warnings." % action
      pass
  except FatalException as e:
    if e.reason is not None:
      print_error_msg("Exiting with exit code {0}. \nREASON: {1}".format(e.code, e.reason))
    sys.exit(e.code)
  except NonFatalException as e:
    options.exit_message = "Ambari Metrics Collector '%s' completed with warnings." % action
    if e.reason is not None:
      print_warning_msg(e.reason)

  if options.exit_message is not None:
    print options.exit_message


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
