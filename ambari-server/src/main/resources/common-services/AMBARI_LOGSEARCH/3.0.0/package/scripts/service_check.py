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

from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute

class LogSearchServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    try:
      if params.logsearch_server_host:
        Execute(params.smoke_logsearch_cmd, user=params.logsearch_user,
                tries=15, try_sleep=5, timeout=10)
        Logger.info('Log Search Server up and running')
      else:
        Logger.info('No portal is installed on the cluster thus no service check is required')
    except:
      Logger.error('Log Search Server not running')
      raise

if __name__ == "__main__":
  LogSearchServiceCheck().execute()