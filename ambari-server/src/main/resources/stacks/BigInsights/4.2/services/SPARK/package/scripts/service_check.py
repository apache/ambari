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

from resource_management import *
from ambari_commons import subprocess32
import time

class SparkServiceCheck(Script):
  def service_check(self, env):
    import params
    import spark_check

    env.set_params(params)

    # smoke_cmd = params.spark_service_check_cmd
    # code, output = shell.call(smoke_cmd, timeout=100)
    # if code == 0:
    #   Logger.info('Spark-on-Yarn Job submitted successfully')
    # else:
    #   Logger.info('Spark-on-Yarn Job cannot be submitted')
    #   raise ComponentIsNotRunning()

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
      proc = subprocess32.Popen(command_with_flags, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE)
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


    Logger.info('Checking for Spark Thriftserver now')
    if params.hive_server2_authentication == "KERBEROS" or params.hive_server2_authentication == "NONE":

      address_list = params.spark_thriftserver_hosts

      if not address_list:
        raise Fail('Can not find any Spark Thriftserver host. Please check configuration.')

      port = int(format("{spark_thriftserver_port}"))
      Logger.info("Test connectivity to hive server")
      if params.security_enabled:
        kinitcmd=format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
      else:
        kinitcmd=None

      SOCKET_WAIT_SECONDS = 290

      start_time = time.time()
      end_time = start_time + SOCKET_WAIT_SECONDS

      Logger.info('Waiting for the Spark Thriftserver to start...')

      workable_server_available = False
      i = 0
      while time.time() < end_time and not workable_server_available:
        address = address_list[i]
        try:
          spark_check.check_thrift_port_sasl(address, port, params.hive_server2_authentication,
                                             params.hive_server_principal, kinitcmd=kinitcmd, smokeuser=params.smokeuser,
                                             transport_mode=params.hive_transport_mode, http_endpoint=params.hive_http_endpoint,
                                             ssl=params.hive_ssl, ssl_keystore=params.hive_ssl_keystore_path,
                                             ssl_password=params.hive_ssl_keystore_password)
          Logger.info("Successfully connected to %s on port %s" % (address, port))
          workable_server_available = True
        except Exception, e:
          Logger.info(str(e))
          Logger.info("Connection to %s on port %s failed" % (address, port))
          time.sleep(5)

        i += 1
        if i == len(address_list):
          i = 0

      elapsed_time = time.time() - start_time

      if not workable_server_available:
        raise Fail("Connection to Spark Thriftserver %s on port %s failed after %d seconds" %
                   (params.hostname, params.spark_thriftserver_port, elapsed_time))

      Logger.info("Successfully connected to Spark Thriftserver at %s on port %s after %d seconds" % \
            (params.hostname, params.spark_thriftserver_port, elapsed_time))



    #command_with_flags = [command, silent, out, head, httpGssnegotiate, userpswd, insecure, url]
    # proc = subprocess32.Popen(command_with_flags, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE)
    # (stdout, stderr) = proc.communicate()
    # response = stdout
    # if '200' in response:
    #   Logger.info('Spark Job History Server up and running')
    # else:
    #   Logger.info('Spark Job History Server not running.')
    #   raise ComponentIsNotRunning()

if __name__ == "__main__":
  SparkServiceCheck().execute()
