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
from ambari_server.serverConfiguration import configDefaults, PID_NAME, get_resources_location, get_stack_location, \
  CLIENT_API_PORT, CLIENT_API_PORT_PROPERTY, SSL_API, DEFAULT_SSL_API_PORT, SSL_API_PORT


# Ambari server API properties
SERVER_API_HOST = '127.0.0.1'
SERVER_API_PROTOCOL = 'http'
SERVER_API_SSL_PROTOCOL = 'https'

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
  resources_location = get_resources_location(properties)
  stacks_location = get_stack_location(properties)
  resource_files_keeper = ResourceFilesKeeper(resources_location, stacks_location)

  try:
    print "Organizing resource files at {0}...".format(resources_location,
                                                       verbose=get_verbose())
    resource_files_keeper.perform_housekeeping()
  except KeeperException, ex:
    msg = "Can not organize resource files at {0}: {1}".format(
      resources_location, str(ex))
    raise FatalException(-1, msg)


#
# Builds ambari-server API base url
# Reads server protocol/port from configuration
# And returns something like
# http://127.0.0.1:8080/api/v1/
#
def get_ambari_server_api_base(properties):
  api_protocol = SERVER_API_PROTOCOL
  api_port = CLIENT_API_PORT
  api_port_prop = properties.get_property(CLIENT_API_PORT_PROPERTY)
  if api_port_prop is not None and api_port_prop != '':
    api_port = api_port_prop

  api_ssl = False
  api_ssl_prop = properties.get_property(SSL_API)
  if api_ssl_prop is not None:
    api_ssl = api_ssl_prop.lower() == "true"

  if api_ssl:
    api_protocol = SERVER_API_SSL_PROTOCOL
    api_port = DEFAULT_SSL_API_PORT
    api_port_prop = properties.get_property(SSL_API_PORT)
    if api_port_prop is not None:
      api_port = api_port_prop
  return '{0}://{1}:{2!s}/api/v1/'.format(api_protocol, SERVER_API_HOST, api_port)
