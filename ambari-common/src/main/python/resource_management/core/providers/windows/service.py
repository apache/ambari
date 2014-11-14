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
from resource_management.core.providers import Provider
from resource_management.core.base import Fail
import win32service
import time


_schSCManager = win32service.OpenSCManager(None, None, win32service.SC_MANAGER_ALL_ACCESS)


class ServiceProvider(Provider):
  def action_start(self):
    self._service_handle = self._service_handle if hasattr(self, "_service_handle") else \
      win32service.OpenService(_schSCManager, self.resource.service_name, win32service.SERVICE_ALL_ACCESS)
    if not self.status():
      win32service.StartService(self._service_handle, None)
      self.wait_status(win32service.SERVICE_RUNNING)

  def action_stop(self):
    self._service_handle = self._service_handle if hasattr(self, "_service_handle") else \
      win32service.OpenService(_schSCManager, self.resource.service_name, win32service.SERVICE_ALL_ACCESS)
    if self.status():
      win32service.ControlService(self._service_handle, win32service.SERVICE_CONTROL_STOP)
      self.wait_status(win32service.SERVICE_STOPPED)

  def action_restart(self):
    self._service_handle = win32service.OpenService(_schSCManager, self.resource.service_name,
                                                    win32service.SERVICE_ALL_ACCESS)
    self.action_stop()
    self.action_start()

  def action_reload(self):
    raise Fail("Reload for Service resource not supported on windows")

  def status(self):
    if win32service.QueryServiceStatusEx(self._service_handle)["CurrentState"] == win32service.SERVICE_RUNNING:
      return True
    return False

  def get_current_status(self):
    return win32service.QueryServiceStatusEx(self._service_handle)["CurrentState"]

  def wait_status(self, status, timeout=5):
    begin = time.time()
    while self.get_current_status() != status and (timeout == 0 or time.time() - begin < timeout):
      time.sleep(1)