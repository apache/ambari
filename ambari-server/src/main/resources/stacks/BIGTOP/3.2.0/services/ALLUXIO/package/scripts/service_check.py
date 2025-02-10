"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agree in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import subprocess
import time
import os

from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger


CHECK_COMMAND_TIMEOUT_DEFAULT = 300.0


class AlluxioServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)

    Execute(params.alluxio_test_cmd, user=params.alluxio_user)


if __name__ == "__main__":
  AlluxioServiceCheck().execute()
