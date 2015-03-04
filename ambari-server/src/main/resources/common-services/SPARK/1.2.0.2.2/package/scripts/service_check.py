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

    command = "curl"
    httpGssnegotiate = "--negotiate"
    userpswd = "-u:"
    insecure = "-k"
    silent = "-s"
    out = "-o /dev/null"
    head = "-w'%{http_code}'"
    url = 'http://' + params.spark_history_server_host + ':' + str(params.spark_history_ui_port)

    command_with_flags = [command, silent, out, head, httpGssnegotiate, userpswd, insecure, url]

    is_running = False
    for i in range(1,11):
      proc = subprocess.Popen(command_with_flags, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
      Logger.info("Try %d, command: %s" % (i, " ".join(command_with_flags)))
      (stdout, stderr) = proc.communicate()
      response = stdout
      if '200' in response:
        is_running = True
        Logger.info('Spark Job History Server up and running')
        break
      Logger.info("Response: %s" % str(response))
      time.sleep(5)

    if is_running == False :
      Logger.info('Spark Job History Server not running.')
      raise ComponentIsNotRunning()

if __name__ == "__main__":
  SparkServiceCheck().execute()
