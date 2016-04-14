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
from resource_management.core.exceptions import ComponentIsNotRunning

import common
import hawq_constants

PROCESS_INFO = {
  hawq_constants.MASTER: {
    'port_property': 'hawq_master_address_port',
    'process_name': 'postgres'
  },
  hawq_constants.STANDBY: {
    'port_property': 'hawq_master_address_port',
    'process_name': 'gpsyncmaster'
  },
  hawq_constants.SEGMENT: {
    'port_property': 'hawq_segment_address_port',
    'process_name': 'postgres'
  }
}

def assert_component_running(component):
  """
  Based on the port and process identifies the status of the component
  """
  port = common.get_local_hawq_site_property(PROCESS_INFO[component]['port_property'])
  process = PROCESS_INFO[component]['process_name']

  netstat_cmd = "netstat -tupln | egrep ':{0}\s' | egrep {1}".format(port, process)
  return_code, _ = call(netstat_cmd)
  if return_code:
    raise ComponentIsNotRunning()