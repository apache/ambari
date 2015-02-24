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
from setup_ranger import setup_ranger
from ranger_service import ranger_service
import upgrade

class RangerAdmin(Script):

  def get_stack_to_component(self):
    return {"HDP": "ranger-admin"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def stop(self, env, rolling_restart=False):
    import params

    env.set_params(params)
    Execute(format('{params.ranger_stop}'), user=params.unix_user)

  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)
    upgrade.prestart(env, "ranger-admin")

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    ranger_service('ranger_admin')


  def status(self, env):
    cmd = 'ps -ef | grep proc_rangeradmin | grep -v grep'
    code, output = shell.call(cmd, timeout=20)

    if code != 0:
      Logger.debug('Ranger admin process not running')
      raise ComponentIsNotRunning()
    pass

  def configure(self, env):
    import params
    env.set_params(params)
    
    setup_ranger()


if __name__ == "__main__":
  RangerAdmin().execute()
