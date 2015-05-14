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

import optparse
import os
import sys

from ambari_commons.ambari_service import AmbariService
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import print_warning_msg, print_error_msg
from ambari_commons.os_windows import SvcStatusCallback
from core.config_reader import SERVER_OUT_FILE, SERVICE_USERNAME_KEY, SERVICE_PASSWORD_KEY, \
  SETUP_ACTION, START_ACTION, STOP_ACTION, RESTART_ACTION, STATUS_ACTION
from core.stop_handler import bind_signal_handlers, StopHandler
from main import server_process_main


#
# Windows-specific service implementation. This class will be instantiated directly by pythonservice.exe.
#
class AMHostMonitoringService(AmbariService):
  AmbariService._svc_name_ = "AmbariMetricsHostMonitoring"
  AmbariService._svc_display_name_ = "Ambari Metrics Host Monitoring"
  AmbariService._svc_description_ = "Ambari Metrics Host Monitoring Service"

  # Adds the necessary script dir to the Python's modules path.
  # Modify this as the deployed product's dir structure changes.
  def _adjustPythonPath(self, current_dir):
    python_path = os.path.join(current_dir, "sbin")
    sys.path.insert(0, python_path)
    pass

  def SvcDoRun(self):
    scmStatus = SvcStatusCallback(self)

    self.redirect_output_streams()

    stopHandler = StopHandler(AMHostMonitoringService._heventSvcStop)
    bind_signal_handlers(stopHandler)

    AMHostMonitoringService.set_ctrl_c_handler(ctrlHandler)

    server_process_main(stopHandler, scmStatus)
    pass

  def _InitOptionsParser(self):
    return init_options_parser()

  def redirect_output_streams(self):
    self._RedirectOutputStreamsToFile(SERVER_OUT_FILE)
    pass


def ctrlHandler(ctrlType):
  AMHostMonitoringService.DefCtrlCHandler()
  return True


def svcsetup():
  AMHostMonitoringService.set_ctrl_c_handler(ctrlHandler)
  # we don't save password between 'setup' runs, so we can't run Install every time. We run 'setup' only if user and
  # password provided or if service not installed
  if (SERVICE_USERNAME_KEY in os.environ and SERVICE_PASSWORD_KEY in os.environ):
    AMHostMonitoringService.Install(username=os.environ[SERVICE_USERNAME_KEY], password=os.environ[SERVICE_PASSWORD_KEY])
  elif AMHostMonitoringService.QueryStatus() == "not installed":
    AMHostMonitoringService.Install()
  pass


#
# Starts the Ambari Metrics Collector. The server can start as a service or standalone process.
# args:
#  options.is_process = True - start the server as a process. For now, there is no restrictions for the number of
#     server instances that can run like this.
#  options.is_process = False - start the server in normal mode, as a Windows service. If the Ambari Metrics Collector
#     is not registered as a service, the function fails. By default, only one instance of the service can
#     possibly run.
#
def start(options):
  AMHostMonitoringService.set_ctrl_c_handler(ctrlHandler)

  if options.is_process:
    #Run as a normal process. Invoke the ServiceMain directly.
    stopHandler = StopHandler(AMHostMonitoringService._heventSvcStop)
    bind_signal_handlers(stopHandler)
    server_process_main(stopHandler)
  else:
    AMHostMonitoringService.Start()

#
# Stops the Ambari Metrics Collector. Ineffective when the server is started as a standalone process.
#
def stop():
  AMHostMonitoringService.Stop()

#
# Prints the Ambari Metrics Collector service status.
#
def svcstatus(options):
  options.exit_message = None

  statusStr = AMHostMonitoringService.QueryStatus()
  print "Ambari Metrics Collector is " + statusStr


def init_options_parser():
  parser = optparse.OptionParser(usage="usage: %prog action [options]", )
  parser.add_option('-d', '--debug', action="store_true", dest='debug', default=False,
                    help="Start Ambari Metrics Host Monitoring in debug mode")
  parser.add_option('-p', '--process', action="store_true", dest='is_process', default=False,
                    help="Start Ambari Metrics Host Monitoring as a normal process, not as a service")

  # --help reserved for help
  return parser

def win_main():
  parser = init_options_parser()
  (options, args) = parser.parse_args()

  options.warnings = []
  options.exit_message = None

  if options.debug:
    sys.frozen = 'windows_exe' # Fake py2exe so we can debug

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  try:
    if action == SETUP_ACTION:
      svcsetup()
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
      options.exit_message = "Ambari Metrics Host Monitoring '%s' completed with warnings." % action
      pass
  except FatalException as e:
    if e.reason is not None:
      print_error_msg("Exiting with exit code {0}. \nREASON: {1}".format(e.code, e.reason))
    sys.exit(e.code)
  except NonFatalException as e:
    options.exit_message = "Ambari Metrics Host Monitoring '%s' completed with warnings." % action
    if e.reason is not None:
      print_warning_msg(e.reason)

  if options.exit_message is not None:
    print options.exit_message

  sys.exit(0)

if __name__ == "__main__":
  try:
    win_main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
