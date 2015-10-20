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

from resource_management import Script
from resource_management.core.resources.system import File
from resource_management.core.exceptions import Fail

import utils
import common
import constants


def get_pid_file():
  """
  Fetches the pid file, which will be used to get the status of the HAWQ Master, Standby
  or Segments
  """

  config = Script.get_config()
  
  component_name = config['componentName']
  component = "master" if component_name in ["HAWQMASTER", "HAWQSTANDBY"] else "segment"
  hawq_pid_file = os.path.join(constants.hawq_pid_dir, "hawq-{0}.pid".format(component))

  File(hawq_pid_file, action='delete')
  utils.create_dir_as_hawq_user(constants.hawq_pid_dir)

  #Get hawq_master_directory or hawq_segment_directory value from hawq-site.xml depending 
  #on the component
  hawq_site_directory_property = "hawq_{0}_directory".format(component)
  
  #hawq-site content from Ambari server will not be available when the 
  #command type is STATUS_COMMAND. Hence, reading it directly from the local file
  postmaster_pid_file = os.path.join(common.get_local_hawq_site_property(
      hawq_site_directory_property), constants.postmaster_pid_filename)

  pid = ""
  if os.path.exists(postmaster_pid_file):
    with open(postmaster_pid_file, 'r') as fh:
      pid = fh.readline().strip()

  if not pid:
    raise Fail("Failed to fetch pid from {0}".format(postmaster_pid_file))

  File(hawq_pid_file, content=pid, owner=constants.gpadmin_user, group=constants.gpadmin_user)

  return hawq_pid_file

