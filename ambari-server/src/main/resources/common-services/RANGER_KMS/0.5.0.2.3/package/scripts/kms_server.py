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
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core import shell
from kms import kms, setup_kms_db, setup_java_patch, enable_kms_plugin
from kms_service import kms_service
import upgrade

class KmsServer(Script):

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)

    setup_kms_db()
    self.configure(env)
    setup_java_patch()

  def stop(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    kms_service(action = 'stop')

  def start(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    self.configure(env)
    enable_kms_plugin()
    kms_service(action = 'start')

  def status(self, env):    
    kms_service(action = 'status')

  def configure(self, env):
    import params

    env.set_params(params)
    kms()

  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)

    upgrade.prestart(env, "ranger-kms")
    setup_kms_db()
    kms()
    setup_java_patch()

if __name__ == "__main__":
  KmsServer().execute()
