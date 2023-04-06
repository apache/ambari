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
from resource_management.core.shell import call

HAWQMASTER_PORT = '{{hawq-site/hawq_master_address_port}}'
HAWQSEGMENT_PORT = '{{hawq-site/hawq_segment_address_port}}'
HAWQSTANDBY_ADDRESS = '{{hawq-site/hawq_standby_address_host}}'

RESULT_STATE_OK = 'OK'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'
RESULT_STATE_CRITICAL = 'CRITICAL'

COMPONENT_PROCESS_MAP = {
                         "segment": "postgres",
                         "master": "postgres",
                         "standby": "gpsyncmaster"
                        }



def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used to build the dictionary passed into execute
  """
  return (HAWQMASTER_PORT, HAWQSEGMENT_PORT, HAWQSTANDBY_ADDRESS)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  component = parameters['component_name']
  # Identify port of the process
  port = configurations[HAWQSEGMENT_PORT] if component == "segment" else configurations[HAWQMASTER_PORT]

  component_name = component.capitalize()
  is_running = is_component_running(port, COMPONENT_PROCESS_MAP[component])
  if is_running:
    return (RESULT_STATE_OK, ['HAWQ {0} is running'.format(component_name)])
  else:
    return (RESULT_STATE_CRITICAL, ['HAWQ {0} is not running'.format(component_name)])

def is_component_running(port, process):
  """
  Check if the process is running on the specified port
  """
  cmd = "netstat -tupln | egrep ':{0}\s' | egrep {1}".format(port, process)
  rc, op= call(cmd, timeout=60)
  return rc == 0
