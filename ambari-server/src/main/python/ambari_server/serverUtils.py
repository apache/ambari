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
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_verbose
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_check import OSConst
from ambari_commons.os_utils import run_os_command
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
from ambari_server.serverConfiguration import configDefaults, PID_NAME, get_ambari_properties, get_stack_location


@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def is_server_runing():
  pid_file_path = os.path.join(configDefaults.PID_DIR, PID_NAME)

  if os.path.exists(pid_file_path):
    try:
      f = open(pid_file_path, "r")
    except IOError, ex:
      raise FatalException(1, str(ex))

    pid = f.readline().strip()

    if not pid.isdigit():
      err = "%s is corrupt. Removing" % (pid_file_path)
      f.close()
      run_os_command("rm -f " + pid_file_path)
      raise NonFatalException(err)

    f.close()
    retcode, out, err = run_os_command("ps -p " + pid)
    if retcode == 0:
      return True, int(pid)
    else:
      return False, None
  else:
    return False, None


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def is_server_runing():
  from ambari_commons.os_windows import SERVICE_STATUS_STARTING, SERVICE_STATUS_RUNNING, SERVICE_STATUS_STOPPING, \
    SERVICE_STATUS_STOPPED, SERVICE_STATUS_NOT_INSTALLED
  from ambari_windows_service import AmbariServerService

  statusStr = AmbariServerService.QueryStatus()
  if statusStr in(SERVICE_STATUS_STARTING, SERVICE_STATUS_RUNNING, SERVICE_STATUS_STOPPING):
    return True, ""
  elif statusStr == SERVICE_STATUS_STOPPED:
    return False, SERVICE_STATUS_STOPPED
  elif statusStr == SERVICE_STATUS_NOT_INSTALLED:
    return False, SERVICE_STATUS_NOT_INSTALLED
  else:
    return False, None


#
# Performs HDP stack housekeeping
#
def refresh_stack_hash(properties):
  stack_location = get_stack_location(properties)
  # Hack: we determine resource dir as a parent dir for stack_location
  resources_location = os.path.dirname(stack_location)
  resource_files_keeper = ResourceFilesKeeper(resources_location)

  try:
    print "Organizing resource files at {0}...".format(resources_location,
                                                       verbose=get_verbose())
    resource_files_keeper.perform_housekeeping()
  except KeeperException, ex:
    msg = "Can not organize resource files at {0}: {1}".format(
      resources_location, str(ex))
    raise FatalException(-1, msg)
