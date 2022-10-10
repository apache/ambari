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

CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

class SparkServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
      Execute(spark_kinit_cmd, user=params.smoke_user)


    Execute(format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {spark_history_scheme}://{spark_history_server_host}:{spark_history_ui_port} | grep 200"),
            tries=5,
            try_sleep=3,
            logoutput=True,
            user=params.smoke_user
            )

    if params.has_spark_thriftserver:
      healthy_spark_thrift_host = ""
      for spark_thrift_host in params.spark_thriftserver_hosts:
        if params.security_enabled:
          kerberos_principal = params.default_hive_kerberos_principal.replace('_HOST', spark_thrift_host)
          beeline_url = ["jdbc:hive2://{spark_thrift_host}:{spark_thrift_port}/default;principal={kerberos_principal}","transportMode={spark_transport_mode}"]
        else:
          beeline_url = ["jdbc:hive2://{spark_thrift_host}:{spark_thrift_port}/default","transportMode={spark_transport_mode}"]
        # append url according to used transport
        if params.spark_transport_mode == "http":
          beeline_url.append("httpPath={spark_thrift_endpoint}")
          if params.spark_thrift_ssl_enabled:
            beeline_url.append("ssl=true")

        beeline_cmd = os.path.join(params.spark_home, "bin", "beeline")
        cmd = "! %s -u '%s'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL' -e 'Error: Could not open'" % \
              (beeline_cmd, format(";".join(beeline_url)))

        try:
          Execute(cmd, user=params.smoke_user, path=[beeline_cmd], timeout=CHECK_COMMAND_TIMEOUT_DEFAULT)
          healthy_spark_thrift_host = spark_thrift_host
          break
        except:
          pass

      if len(params.spark_thriftserver_hosts) > 0 and healthy_spark_thrift_host == "":
        raise Fail("Connection to all Spark thrift servers failed.")

if __name__ == "__main__":
  SparkServiceCheck().execute()

