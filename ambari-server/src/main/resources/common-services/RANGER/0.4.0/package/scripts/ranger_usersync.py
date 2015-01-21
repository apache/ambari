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
import sys
from resource_management import *
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
from setup_ranger import setup_usersync

class RangerUsersync(Script):
    def install(self, env):
        self.install_packages(env)
        setup_usersync(env)        

    def stop(self, env):
        import params
        Execute(format('{params.usersync_stop}'))

    def start(self, env):
        import params
        setup_usersync(env)
        Execute(format('{params.usersync_start}'))
     
    def status(self, env):
        cmd = 'ps -ef | grep proc_rangerusersync | grep -v grep'
        code, output = shell.call(cmd, timeout=20)        

        if code != 0:
            Logger.debug('Ranger usersync process not running')
            raise ComponentIsNotRunning()
        pass

    def configure(self, env):
        import params
        env.set_params(params)


if __name__ == "__main__":
  RangerUsersync().execute()
