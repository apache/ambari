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

Ambari Agent

"""

import subprocess
import time
import re
import logging

from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger
from resource_management.core.utils import suppress_stdout
from resource_management.core import shell


PACKAGE_MANAGER_LOCK_ACQUIRED_MSG = "Cannot obtain lock for Package manager. Retrying after {0} seconds. Reason: {1}"
PACKAGE_MANAGER_REPO_ERROR_MSG = "Cannot download the package due to repository unavailability. Retrying after {0} seconds. Reason: {1}"


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)   
  
  def install_package(self, name, version):
    raise NotImplementedError()

  def remove_package(self, name):
    raise NotImplementedError()

  def upgrade_package(self, name, version):
    raise NotImplementedError()

  def action_install(self):
    package_name = self.get_package_name_with_version()
    self.install_package(package_name, self.resource.use_repos, self.resource.skip_repos)

  def action_upgrade(self):
    package_name = self.get_package_name_with_version()
    self.upgrade_package(package_name, self.resource.use_repos, self.resource.skip_repos)

  def action_remove(self):
    package_name = self.get_package_name_with_version()
    self.remove_package(package_name)

  def get_package_name_with_version(self):
    if self.resource.version:
      return self.resource.package_name + '-' + self.resource.version
    else:
      return self.resource.package_name

  def get_repo_update_cmd(self):
    raise NotImplementedError()

  def is_locked_output(self, out):
    return False

  def is_repo_error_output(self, out):
    return False

  def get_logoutput(self):
    return self.resource.logoutput==True and Logger.logger.isEnabledFor(logging.INFO) or self.resource.logoutput==None and Logger.logger.isEnabledFor(logging.DEBUG)

  def call_with_retries(self, cmd, **kwargs):
    return self._call_with_retries(cmd, is_checked=False, **kwargs)
  
  def checked_call_with_retries(self, cmd, **kwargs):
    return self._call_with_retries(cmd, is_checked=True, **kwargs)

  def _call_with_retries(self, cmd, is_checked=True, **kwargs):
    func = shell.checked_call if is_checked else shell.call
    # at least do one retry, to run after repository is cleaned
    try_count = 2 if self.resource.retry_count < 2 else self.resource.retry_count

    for i in range(try_count):
      is_first_time = (i == 0)
      is_last_time = (i == try_count - 1)

      try:
        code, out = func(cmd, **kwargs)
      except ExecutionFailed as ex:
        should_stop_retries = self._handle_retries(cmd, ex.code, ex.out, is_first_time, is_last_time)
        if should_stop_retries:
          raise
      else:
        should_stop_retries = self._handle_retries(cmd, code, out, is_first_time, is_last_time)
        if should_stop_retries:
          break

      time.sleep(self.resource.retry_sleep)

    return code, out

  def _handle_retries(self, cmd, code, out, is_first_time, is_last_time):
    # handle first failure in a special way (update repo metadata after it, so next try has a better chance to succeed)
    if is_first_time and code and not self.is_locked_output(out):
      self._update_repo_metadata_after_bad_try(cmd, code, out)
      return False

    handled_error_log_message = None
    if self.resource.retry_on_locked and self.is_locked_output(out):
      handled_error_log_message = PACKAGE_MANAGER_LOCK_ACQUIRED_MSG.format(self.resource.retry_sleep, out)
    elif self.resource.retry_on_repo_unavailability and self.is_repo_error_output(out):
      handled_error_log_message = PACKAGE_MANAGER_REPO_ERROR_MSG.format(self.resource.retry_sleep, out)

    is_handled_error = (handled_error_log_message is not None)
    if is_handled_error and not is_last_time:
      Logger.info(handled_error_log_message)

    return (is_last_time or not code or not is_handled_error)

  def _update_repo_metadata_after_bad_try(self, cmd, code, out):
    name = self.get_package_name_with_version()
    repo_update_cmd = self.get_repo_update_cmd()

    Logger.info("Execution of '%s' returned %d. %s" % (shell.string_cmd_from_args_list(cmd), code, out))
    Logger.info("Failed to install package %s. Executing '%s'" % (name, shell.string_cmd_from_args_list(repo_update_cmd)))
    code, out = shell.call(repo_update_cmd, sudo=True, logoutput=self.get_logoutput())

    if code:
      Logger.info("Execution of '%s' returned %d. %s" % (repo_update_cmd, code, out))

    Logger.info("Retrying to install package %s after %d seconds" % (name, self.resource.retry_sleep))

  def yum_check_package_available(self, name):
    """
    Does the same as rpm_check_package_avaiable, but faster.
    However need root permissions.
    """
    import yum # Python Yum API is much faster then other check methods. (even then "import rpm")
    yb = yum.YumBase()
    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)
    
    with suppress_stdout():
      package_list = yb.rpmdb.simplePkgList()
    
    for package in package_list:
      if regex.match(package[0]):
        return True
    
    return False
  
  def rpm_check_package_available(self, name):
    import rpm # this is faster then calling 'rpm'-binary externally.
    ts = rpm.TransactionSet()
    packages = ts.dbMatch()
    
    name_regex = re.escape(name).replace("\\?", ".").replace("\\*", ".*") + '$'
    regex = re.compile(name_regex)
    
    for package in packages:
      if regex.match(package['name']):
        return True
    return False
