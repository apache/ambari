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
import utils

def assert_component_running(component_name):
  """
  Based on the port and process identifies the status of the component
  """
  port_number = common.get_local_hawq_site_property_value(hawq_constants.COMPONENT_ATTRIBUTES_MAP[component_name]['port_property'])
  return_code, _ = call(utils.generate_hawq_process_status_cmd(component_name, port_number))
  if return_code:
    raise ComponentIsNotRunning()