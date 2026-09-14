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

from resource_management.libraries.script import Script
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from ams import ams
from ams_service import ams_service
from hbase import hbase
from status import check_service_status
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

class AmsCollector(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, action = None):
    import params
    env.set_params(params)
    if action == 'start' and params.embedded_mode_multiple_instances:
      raise Fail("AMS in embedded mode cannot have more than 1 instance. Delete all but 1 instances or switch to Distributed mode ")
    hbase('master', action)
    hbase('regionserver', action)
    ams(name='collector')

  def start(self, env, upgrade_type=None):
    self.configure(env, action = 'start') # for security
    # stop hanging components before start
    ams_service('collector', action = 'stop')
    ams_service('collector', action = 'start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    # Sometimes, stop() may be called before start(), in case restart() is initiated right after installation
    self.configure(env, action = 'stop') # for security
    ams_service('collector', action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(env, name='collector')
    
  def get_log_folder(self):
    import params
    return params.ams_collector_log_dir
  
  def get_user(self):
    import params
    return params.ams_user

  def get_pid_files(self):
    import status
    return status.get_collector_pid_files()


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class AmsCollectorDefault(AmsCollector):
  pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class AmsCollectorWindows(AmsCollector):
  def install(self, env):
    self.install_packages(env)
    self.configure(env) # for security

if __name__ == "__main__":
  AmsCollector().execute()
