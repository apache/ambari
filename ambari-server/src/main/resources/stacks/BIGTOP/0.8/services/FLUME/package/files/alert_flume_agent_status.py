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

import socket

from resource_management.libraries.functions.flume_agent_helper import find_expected_agent_names
from resource_management.libraries.functions.flume_agent_helper import get_flume_status

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

FLUME_CONF_DIR_KEY = '{{flume-env/flume_conf_dir}}'

FLUME_RUN_DIR_KEY = "run.directory"
FLUME_RUN_DIR_DEFAULT = '/var/run/flume'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (FLUME_CONF_DIR_KEY,)


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

  flume_conf_directory = None
  if FLUME_CONF_DIR_KEY in configurations:
    flume_conf_directory = configurations[FLUME_CONF_DIR_KEY]

  if flume_conf_directory is None:
    return (RESULT_CODE_UNKNOWN, ['The Flume configuration directory is a required parameter.'])

  if host_name is None:
    host_name = socket.getfqdn()

  # parse script arguments
  flume_run_directory = FLUME_RUN_DIR_DEFAULT
  if FLUME_RUN_DIR_KEY in parameters:
    flume_run_directory = parameters[FLUME_RUN_DIR_KEY]

  processes = get_flume_status(flume_conf_directory, flume_run_directory)
  expected_agents = find_expected_agent_names(flume_conf_directory)

  alert_label = ''
  alert_state = RESULT_CODE_OK

  if len(processes) == 0 and len(expected_agents) == 0:
    alert_label = 'No agents defined on {0}'.format(host_name)
  else:
    ok = []
    critical = []
    text_arr = []

    for process in processes:
      if not process.has_key('status') or process['status'] == 'NOT_RUNNING':
        critical.append(process['name'])
      else:
        ok.append(process['name'])

    if len(critical) > 0:
      text_arr.append("{0} {1} NOT running".format(", ".join(critical),
        "is" if len(critical) == 1 else "are"))

    if len(ok) > 0:
      text_arr.append("{0} {1} running".format(", ".join(ok),
        "is" if len(ok) == 1 else "are"))

    plural = len(critical) > 1 or len(ok) > 1
    alert_label = "Agent{0} {1} {2}".format(
      "s" if plural else "",
      " and ".join(text_arr),
      "on " + host_name)

    alert_state = RESULT_CODE_CRITICAL if len(critical) > 0 else RESULT_CODE_OK

  return (alert_state, [alert_label])