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

import socket
import sys
import time
import subprocess

from hcat_service_check import hcat_service_check
from webhcat_service_check import webhcat_service_check
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.hive_check import check_thrift_port_sasl
from resource_management.core.resources.system import Execute, File
from resource_management.core.exceptions import Fail
from resource_management.core.source import StaticFile


class HiveServiceCheck(Script):
  pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServiceCheckWindows(HiveServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.stack_root,"Run-SmokeTests.cmd")
    service = "HIVE"
    Execute(format("cmd /C {smoke_cmd} {service}"), user=params.hive_user, logoutput=True)

    hcat_service_check()
    webhcat_service_check()


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServiceCheckDefault(HiveServiceCheck):

  def __init__(self):
    super(HiveServiceCheckDefault, self).__init__()
    Logger.initialize_logger()

  def service_check(self, env):
    import params
    env.set_params(params)

    if params.security_enabled:
      kinit_cmd = format(
        "{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
    else:
      kinit_cmd = ""

    # Check HiveServer
    Logger.info("Running Hive Server checks")
    Logger.info("--------------------------\n")
    self.check_hive_server(env, 'Hive Server', kinit_cmd, params.hive_server_hosts,
                           int(format("{hive_server_port}")))


    if params.has_hive_interactive  and params.hive_interactive_enabled:
      Logger.info("Running Hive Server2 checks")
      Logger.info("--------------------------\n")

      self.check_hive_server(env, 'Hive Server2', kinit_cmd, params.hive_interactive_hosts,
                             int(format("{hive_server_interactive_port}")))

      Logger.info("Running LLAP checks")
      Logger.info("-------------------\n")
      self.check_llap(env, kinit_cmd)


    Logger.info("Running HCAT checks")
    Logger.info("-------------------\n")
    hcat_service_check()

    Logger.info("Running WEBHCAT checks")
    Logger.info("---------------------\n")
    webhcat_service_check()

  def check_hive_server(self, env, server_component_name, kinit_cmd, address_list, server_port):
    import params
    env.set_params(params)
    Logger.info("Server Address List : {0}, Port : {1}".format(address_list, server_port))

    if not address_list:
      raise Fail("Can not find any "+server_component_name+" ,host. Please check configuration.")

    SOCKET_WAIT_SECONDS = 290

    start_time = time.time()
    end_time = start_time + SOCKET_WAIT_SECONDS

    Logger.info("Waiting for the {0} to start...".format(server_component_name))

    workable_server_available = False
    i = 0
    while time.time() < end_time and not workable_server_available:
      address = address_list[i]
      try:
        check_thrift_port_sasl(address, server_port, params.hive_server2_authentication,
                               params.hive_server_principal, kinit_cmd, params.smokeuser,
                               transport_mode=params.hive_transport_mode, http_endpoint=params.hive_http_endpoint,
                               ssl=params.hive_ssl, ssl_keystore=params.hive_ssl_keystore_path,
                               ssl_password=params.hive_ssl_keystore_password)
        Logger.info("Successfully connected to {0} on port {1}".format(address, server_port))
        workable_server_available = True
      except:
        Logger.info("Connection to {0} on port {1} failed".format(address, server_port))
        time.sleep(5)

      i += 1
      if i == len(address_list):
        i = 0

    elapsed_time = time.time() - start_time

    if not workable_server_available:
      raise Fail("Connection to '{0}' on host: {1} and port {2} failed after {3} seconds"
                 .format(server_component_name, params.hostname, server_port, elapsed_time))

    Logger.info("Successfully stayed connected to '{0}' on host: {1} and port {2} after {3} seconds"
                .format(server_component_name, params.hostname, server_port, elapsed_time))

  def check_llap(self, env, kinit_cmd):
    import params
    env.set_params(params)

    File(format("{tmp_dir}/hiveLlapSmoke.sh"),
         content=StaticFile("hiveLlapSmoke.sh"),
         mode=0755
         )
    unique_id = get_unique_id_and_date()
    llap_cmd = format("{kinit_cmd}env JAVA_HOME={java64_home} {tmp_dir}/hiveLlapSmoke.sh {stack_root} llap_smoke_{unique_id} prepare")

    exec_path = params.execute_path
    if params.version and params.stack_root:
      upgrade_hive_bin = format("{stack_root}/{version}/hive2/bin")
      exec_path =  os.environ['PATH'] + os.pathsep + params.hadoop_bin_dir + os.pathsep + upgrade_hive_bin

    Execute(llap_cmd,
            user=params.hive_user,
            path=['/usr/sbin', '/usr/local/bin', '/bin', '/usr/bin', exec_path],
            tries=1,
            try_sleep=5,
            wait_for_finish=True,
            stderr=subprocess.PIPE,
            logoutput=True)

if __name__ == "__main__":
  HiveServiceCheck().execute()