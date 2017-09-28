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
      if params.has_livyserver:
        livy_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
        Execute(livy_kinit_cmd, user=params.livy2_user)

    Execute(format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {spark_history_scheme}://{spark_history_server_host}:{spark_history_ui_port} | grep 200"),
            tries=5,
            try_sleep=3,
            logoutput=True,
            user=params.spark_user
            )
    if params.has_livyserver:
      live_livyserver_host = ""
      for livyserver_host in params.livy2_livyserver_hosts:
        try:
          Execute(format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {livy2_http_scheme}://{livyserver_host}:{livy2_livyserver_port}/sessions | grep 200"),
                  tries=3,
                  try_sleep=1,
                  logoutput=True,
                  user=params.livy2_user
                  )
          live_livyserver_host = livyserver_host
          break
        except:
          pass
      if len(params.livy2_livyserver_hosts) > 0 and live_livyserver_host == "":
        raise Fail(format("Connection to all Livy servers failed"))

if __name__ == "__main__":
  SparkServiceCheck().execute()

