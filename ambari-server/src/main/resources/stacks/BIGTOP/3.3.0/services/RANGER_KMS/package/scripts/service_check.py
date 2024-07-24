#!/usr/bin/env python3
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
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.core.exceptions import ComponentIsNotRunning


class KmsServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    cmd = 'ps -ef | grep proc_rangerkms | grep -v grep'
    code, output = shell.call(cmd, timeout=20)
    if code == 0:
      Logger.info('KMS process up and running')
    else:
      Logger.debug('KMS process not running')
      raise ComponentIsNotRunning()

if __name__ == "__main__":
  KmsServiceCheck().execute()
