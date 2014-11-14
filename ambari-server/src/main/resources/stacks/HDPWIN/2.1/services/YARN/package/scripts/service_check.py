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
from ambari_commons import os_utils

from resource_management import *
import sys
import os

class ServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    yarn_exe = os_utils.quote_path(os.path.join(params.yarn_home, "bin", "yarn.cmd"))

    run_yarn_check_cmd = "cmd /C %s node -list" % yarn_exe

    component_type = 'rm'
    if params.hadoop_ssl_enabled:
      component_address = params.rm_webui_https_address
    else:
      component_address = params.rm_webui_address

    #temp_dir = os.path.abspath(os.path.join(params.hadoop_home, os.pardir)), "/tmp"
    temp_dir = os.path.join(os.path.dirname(params.hadoop_home), "temp")
    validateStatusFileName = "validateYarnComponentStatus.py"
    validateStatusFilePath = os.path.join(temp_dir, validateStatusFileName)
    python_executable = sys.executable
    validateStatusCmd = "%s %s %s -p %s -s %s" % (python_executable, validateStatusFilePath, component_type, component_address, params.hadoop_ssl_enabled)

    if params.security_enabled:
      kinit_cmd = "%s -kt %s %s;" % (params.kinit_path_local, params.smoke_user_keytab, params.smokeuser)
      smoke_cmd = kinit_cmd + ' ' + validateStatusCmd
    else:
      smoke_cmd = validateStatusCmd

    File(validateStatusFilePath,
         content=StaticFile(validateStatusFileName)
    )

    Execute(smoke_cmd,
            tries=3,
            try_sleep=5,
            logoutput=True
    )

    Execute(run_yarn_check_cmd, logoutput=True)

if __name__ == "__main__":
  ServiceCheck().execute()
