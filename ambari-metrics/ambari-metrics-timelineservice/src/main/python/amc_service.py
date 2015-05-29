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
from ambari_commons.exceptions import FatalException
from ambari_commons.os_utils import remove_file
from ambari_commons.os_windows import SvcStatusCallback, WinServiceController, SERVICE_STATUS_RUNNING
from ambari_metrics_collector.serviceConfiguration import get_properties, get_value_from_properties, DEBUG_MODE_KEY, \
  SUSPEND_START_MODE_KEY, PID_OUT_FILE, SERVER_OUT_FILE_KEY, SERVER_OUT_FILE, SERVICE_USERNAME_KEY, SERVICE_PASSWORD_KEY, \
  DEFAULT_CONF_DIR, EMBEDDED_HBASE_MASTER_SERVICE
from embedded_hbase_service import EmbeddedHBaseService
from main import server_process_main


class AMCollectorService(AmbariService):
  AmbariService._svc_name_ = "AmbariMetricsCollector"
  AmbariService._svc_display_name_ = "Ambari Metrics Collector"
  AmbariService._svc_description_ = "Ambari Metrics Collector Service"

  # Adds the necessary script dir(s) to the Python's modules path.
  # Modify this as the deployed product's dir structure changes.
  def _adjustPythonPath(self, current_dir):
    python_path = os.path.join(current_dir, "sbin")
    sys.path.insert(0, python_path)
    pass

  @classmethod
  def Install(cls, startupMode = "auto", username = None, password = None, interactive = False,
              perfMonIni = None, perfMonDll = None):
    script_path = os.path.dirname(__file__.replace('/', os.sep))
    classPath = os.path.join(script_path, cls.__module__) + "." + cls.__name__

    return AmbariService.Install(classPath, startupMode, username, password, interactive,
                                                    perfMonIni, perfMonDll)

  def SvcDoRun(self):
    scmStatus = SvcStatusCallback(self)

    properties = get_properties()
    self.options.debug = get_value_from_properties(properties, DEBUG_MODE_KEY, self.options.debug)
    self.options.suspend_start = get_value_from_properties(properties, SUSPEND_START_MODE_KEY, self.options.suspend_start)

    self.redirect_output_streams()

    childProc = server_process_main(self.options, scmStatus)

    if not self._StopOrWaitForChildProcessToFinish(childProc):
      return

    remove_file(PID_OUT_FILE)
    pass

  def _InitOptionsParser(self):
    return init_options_parser()

  def redirect_output_streams(self):
    properties = get_properties()

    outFilePath = properties[SERVER_OUT_FILE_KEY]
    if (outFilePath is None or outFilePath == ""):
      outFilePath = SERVER_OUT_FILE

    self._RedirectOutputStreamsToFile(outFilePath)
    pass

def ctrlHandler(ctrlType):
  AMCollectorService.DefCtrlCHandler()
  return True

def svcsetup():
  AMCollectorService.set_ctrl_c_handler(ctrlHandler)

  # we don't save password between 'setup' runs, so we can't run Install every time. We run 'setup' only if user and
  # password provided or if service not installed
  if (SERVICE_USERNAME_KEY in os.environ and SERVICE_PASSWORD_KEY in os.environ):
    EmbeddedHBaseService.Install(username=os.environ[SERVICE_USERNAME_KEY], password=os.environ[SERVICE_PASSWORD_KEY])
    AMCollectorService.Install(username=os.environ[SERVICE_USERNAME_KEY], password=os.environ[SERVICE_PASSWORD_KEY])
  else:
    EmbeddedHBaseService.Install()
    AMCollectorService.Install()
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
  AMCollectorService.set_ctrl_c_handler(ctrlHandler)

  if options.is_process:
    #Run as a normal process. Invoke the ServiceMain directly.
    childProc = server_process_main(options)

    childProc.wait()

    remove_file(PID_OUT_FILE)
  else:
    AMCollectorService.Start()

#
# Stops the Ambari Metrics Collector. Ineffective when the server is started as a standalone process.
#
def stop():
  AMCollectorService.Stop()

#
# Prints the Ambari Metrics Collector service status.
#
def svcstatus(options):
  options.exit_message = None

  statusStr = AMCollectorService.QueryStatus()
  print "Ambari Metrics Collector is " + statusStr


def setup(options):
  svcsetup()

def init_options_parser():
  parser = optparse.OptionParser(usage="usage: %prog action [options]", )
  parser.add_option('--config', dest="conf_dir",
                    default=DEFAULT_CONF_DIR,
                    help="Configuration files directory")
  parser.add_option('--debug', action="store_true", dest='debug', default=False,
                    help="Start ambari-metrics-collector in debug mode")
  parser.add_option('--suspend-start', action="store_true", dest='suspend_start', default=False,
                    help="Freeze ambari-metrics-collector Java process at startup in debug mode")
  parser.add_option('--process', action="store_true", dest='is_process', default=False,
                    help="Start ambari-metrics-collector as a process, not as a service")
  parser.add_option('--noembedded', action="store_true", dest='no_embedded_hbase', default=False,
                    help="Don't attempt to start the HBASE services. Expect them to be already installed and running.")

  # --help reserved for help
  return parser


def init_service_debug(options):
  if options.debug:
    sys.frozen = 'windows_exe'  # Fake py2exe so we can debug


def ensure_hdp_service_soft_dependencies():
  if SERVICE_STATUS_RUNNING != WinServiceController.QueryStatus(EMBEDDED_HBASE_MASTER_SERVICE):
    err = 'ERROR: Service "{0}" was not started.'.format(EMBEDDED_HBASE_MASTER_SERVICE)
    raise FatalException(1, err)
