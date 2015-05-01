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
import time
import win32api
import win32service
import win32serviceutil
import winerror

from ambari_commons.os_windows import WinServiceController

from resource_management.core.base import Fail
from resource_management.core.providers import Provider


def safe_open_scmanager():
  try:
    _schSCManager = win32service.OpenSCManager(None, None, win32service.SC_MANAGER_ALL_ACCESS)
  except win32api.error, details:
    raise Fail("Error opening Service Control Manager on the local machine: {0}".format(details.winerror))

  return _schSCManager

def safe_open_service(hSCM, service_name):
  try:
    hSvc = win32serviceutil.SmartOpenService(hSCM, service_name,
                                             win32service.SERVICE_ALL_ACCESS)
  except win32api.error, details:
    if details.winerror == winerror.ERROR_SERVICE_DOES_NOT_EXIST:
      err_msg = "Invalid service name: {0}".format(service_name)
    else:
      err_msg = "Error configuring service {0}: {1}".format(service_name, details.winerror)
    raise Fail(err_msg)

  return hSvc

class ServiceProvider(Provider):
  def action_start(self):
    WinServiceController.Start(self.resource.service_name, 5)

  def action_stop(self):
    WinServiceController.Stop(self.resource.service_name, 5)

  def action_restart(self):
    self.action_stop()
    self.action_start()

  def action_reload(self):
    raise Fail("Reload for Service resource not supported on windows")

  def status(self):
    svcStatus = WinServiceController.QueryStatus(self.resource.service_name)
    if svcStatus == win32service.SERVICE_RUNNING:
      return True
    return False

  def enable(self):
    hSCM = safe_open_scmanager()

    try:
      hSvc = safe_open_service(hSCM, self.resource.service_name)

      if win32service.QueryServiceConfig(hSvc)[1] == win32service.SERVICE_DISABLED:
        win32service.ChangeServiceConfig(hSvc,
                                         win32service.SERVICE_NO_CHANGE,
                                         win32service.SERVICE_DEMAND_START,
                                         win32service.SERVICE_NO_CHANGE,
                                         None,
                                         None,
                                         0,
                                         None,
                                         None,
                                         None,
                                         None)
      win32service.CloseServiceHandle(hSvc)
    except win32api.error, details:
      raise Fail("Error enabling service {0}: {1}".format(self.resource.service_name, details.winerror))
    finally:
      win32service.CloseServiceHandle(hSCM)

  def get_current_status(self):
    return win32service.QueryServiceStatusEx(self._service_handle)["CurrentState"]

  def wait_status(self, status, timeout=5):
    begin = time.time()
    while self.get_current_status() != status and (timeout == 0 or time.time() - begin < timeout):
      time.sleep(1)


class ServiceConfigProvider(Provider):
  def action_install(self):
    hSCM = safe_open_scmanager()

    self._fix_system_user_name()

    try:
      hSvc = win32service.CreateService(hSCM,
                                        self.resource.service_name,
                                        self.resource.display_name,
                                        win32service.SERVICE_ALL_ACCESS,         # desired access
                                        win32service.SERVICE_WIN32_OWN_PROCESS,  # service type
                                        self.resource.start_type,
                                        win32service.SERVICE_ERROR_NORMAL,       # error control type
                                        self.resource.exe_path,
                                        None,
                                        0,
                                        None,
                                        self.resource.userName,
                                        self.resource.password)
      if self.resource.description:
        try:
          win32service.ChangeServiceConfig2(hSvc, win32service.SERVICE_CONFIG_DESCRIPTION, self.description)
        except NotImplementedError:
          pass    ## ChangeServiceConfig2 and description do not exist on NT

      win32service.CloseServiceHandle(hSvc)
    except win32api.error, details:
      raise Fail("Error creating service {0}: {1}".format(self.resource.service_name, details.winerror))
    finally:
      win32service.CloseServiceHandle(hSCM)

  def action_configure(self):
    hSCM = safe_open_scmanager()

    try:
      hSvc = safe_open_service(hSCM, self.resource.service_name)

      self._fix_system_user_name()

      try:
        win32service.ChangeServiceConfig(hSvc,
                                         win32service.SERVICE_NO_CHANGE,
                                         self.resource.startType,
                                         win32service.SERVICE_NO_CHANGE,
                                         None,
                                         None,
                                         0,
                                         None,
                                         self.resource.username,
                                         self.resource.password,
                                         self.resource.displayName)
        if self.resource.description:
          try:
            win32service.ChangeServiceConfig2(hSvc, win32service.SERVICE_CONFIG_DESCRIPTION, self.resource.description)
          except NotImplementedError:
            pass    ## ChangeServiceConfig2 and description do not exist on NT
      except win32api.error, details:
        raise Fail("Error configuring service {0}: {1}".format(self.resource.service_name, details.winerror))
      finally:
        win32service.CloseServiceHandle(hSvc)
    finally:
      win32service.CloseServiceHandle(hSCM)

  def action_change_user(self):
    hSCM = safe_open_scmanager()

    try:
      hSvc = safe_open_service(hSCM, self.resource.service_name)

      self._fix_system_user_name()

      try:
        win32service.ChangeServiceConfig(hSvc,
                                         win32service.SERVICE_NO_CHANGE,
                                         win32service.SERVICE_NO_CHANGE,
                                         win32service.SERVICE_NO_CHANGE,
                                         None,
                                         None,
                                         0,
                                         None,
                                         self.resource.username,
                                         self.resource.password,
                                         None)
      except win32api.error, details:
        raise Fail("Error changing user for service {0}: {1}".format(self.resource.service_name, details.winerror))
      finally:
        win32service.CloseServiceHandle(hSvc)
    finally:
      win32service.CloseServiceHandle(hSCM)

  def action_uninstall(self):
    hSCM = safe_open_scmanager()

    try:
      try:
        hSvc = win32serviceutil.SmartOpenService(hSCM, self.resource.service_name,
                                                 win32service.SERVICE_ALL_ACCESS)
      except win32api.error, details:
        if details.winerror == winerror.ERROR_SERVICE_DOES_NOT_EXIST:
          # Nothing to do
          return
        else:
          raise Fail("Error removing service {0}: {1}".format(self.resource.service_name, details.winerror))

      try:
        win32service.DeleteService(hSvc)
      except win32api.error:
        # Error mostly means the service is running and its removal is delayed until the next opportunity
        pass
      finally:
        win32service.CloseServiceHandle(hSvc)
    finally:
      win32service.CloseServiceHandle(hSCM)

  def _fix_system_user_name(self):
    if self.resource.username.upper() == "NT AUTHORITY\\SYSTEM":
      self.resource.username = None

  def _is_system_user(self):
    if self.resource.username in ["NT AUTHORITY\\SYSTEM", "NT AUTHORITY\\NetworkService", "NT AUTHORITY\\LocalService"]:
      return True
    return False