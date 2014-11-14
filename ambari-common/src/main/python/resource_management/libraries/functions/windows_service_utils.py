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

Ambari Agent

"""

from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
__all__ = ['check_windows_service_status', 'check_windows_service_exists']

import win32service

_schSCManager = win32service.OpenSCManager(None, None, win32service.SC_MANAGER_ALL_ACCESS)

def check_windows_service_status(service_name):
  _service_handle = win32service.OpenService(_schSCManager, service_name, win32service.SERVICE_ALL_ACCESS)
  if win32service.QueryServiceStatusEx(_service_handle)["CurrentState"] == win32service.SERVICE_STOPPED:
      raise ComponentIsNotRunning()

def check_windows_service_exists(service_name):
  typeFilter = win32service.SERVICE_WIN32
  stateFilter = win32service.SERVICE_STATE_ALL
  statuses = win32service.EnumServicesStatus(_schSCManager, typeFilter, stateFilter)
  for (short_name, desc, status) in statuses:
    if short_name == service_name:
      return True
  return False
