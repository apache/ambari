#!/usr/bin/env python3

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
import socket

from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.exceptions import ComponentIsNotRunning
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

AMS_MONITOR_PID_DIR = '{{ams-env/metrics_monitor_pid_dir}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (AMS_MONITOR_PID_DIR,)

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def is_monitor_process_live(pid_file=None):
  """
  Gets whether the Metrics Monitor Service is running.
  :param pid_file: ignored
  :return: True if the monitor is running, False otherwise
  """
  try:
    check_windows_service_status("AmbariMetricsHostMonitoring")
    ams_monitor_process_running = True
  except:
    ams_monitor_process_running = False
  return ams_monitor_process_running

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def is_monitor_process_live(pid_file):
  """
  Gets whether the Metrics Monitor represented by the specified file is running.
  :param pid_file: the PID file of the monitor to check
  :return: True if the monitor is running, False otherwise
  """
  live = False

  try:
    check_process_status(pid_file)
    live = True
  except ComponentIsNotRunning:
    pass

  return live


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (RESULT_CODE_UNKNOWN, ['There were no configurations supplied to the script.'])

  if set([AMS_MONITOR_PID_DIR]).issubset(configurations):
    AMS_MONITOR_PID_PATH = os.path.join(configurations[AMS_MONITOR_PID_DIR], 'ambari-metrics-monitor.pid')
  else:
    return (RESULT_CODE_UNKNOWN, ['The ams_monitor_pid_dir is a required parameter.'])

  if host_name is None:
    host_name = socket.getfqdn()

  ams_monitor_process_running = is_monitor_process_live(AMS_MONITOR_PID_PATH)

  alert_state = RESULT_CODE_OK if ams_monitor_process_running else RESULT_CODE_CRITICAL

  alert_label = 'Ambari Monitor is running on {0}' if ams_monitor_process_running else 'Ambari Monitor is NOT running on {0}'
  alert_label = alert_label.format(host_name)

  return (alert_state, [alert_label])
