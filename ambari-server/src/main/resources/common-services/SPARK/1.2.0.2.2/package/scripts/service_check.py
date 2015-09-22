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

from resource_management import *
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute
from resource_management.core.logger import Logger

class SparkServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {spark_kerberos_keytab} {spark_principal}; ")
      Execute(spark_kinit_cmd, user=params.spark_user)

    Execute(format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k http://{spark_history_server_host}:{spark_history_ui_port} | grep 200"),
      tries = 10,
      try_sleep=3,
      logoutput=True
    )

if __name__ == "__main__":
  SparkServiceCheck().execute()
