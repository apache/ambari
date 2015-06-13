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

from resource_management import *
import socket
import sys
import time
from hcat_service_check import hcat_service_check
from webhcat_service_check import webhcat_service_check
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HiveServiceCheck(Script):
  pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HiveServiceCheckWindows(HiveServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)
    smoke_cmd = os.path.join(params.hdp_root,"Run-SmokeTests.cmd")
    service = "HIVE"
    Execute(format("cmd /C {smoke_cmd} {service}"), user=params.hive_user, logoutput=True)

    hcat_service_check()
    webhcat_service_check()


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HiveServiceCheckDefault(HiveServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    address_list = params.hive_server_hosts

    if not address_list:
      raise Fail("Can not find any Hive Server host. Please check configuration.")

    port = int(format("{hive_server_port}"))
    print "Test connectivity to hive server"
    if params.security_enabled:
      kinitcmd=format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
    else:
      kinitcmd=None

    SOCKET_WAIT_SECONDS = 290

    start_time = time.time()
    end_time = start_time + SOCKET_WAIT_SECONDS

    print "Waiting for the Hive server to start..."
      
    workable_server_available = False
    i = 0
    while time.time() < end_time and not workable_server_available:
      address = address_list[i]
      try:
        check_thrift_port_sasl(address, port, params.hive_server2_authentication,
                               params.hive_server_principal, kinitcmd, params.smokeuser,
                               transport_mode=params.hive_transport_mode, http_endpoint=params.hive_http_endpoint,
                               ssl=params.hive_ssl, ssl_keystore=params.hive_ssl_keystore_path,
                               ssl_password=params.hive_ssl_keystore_password)
        print "Successfully connected to %s on port %s" % (address, port)
        workable_server_available = True
      except:
        print "Connection to %s on port %s failed" % (address, port)
        time.sleep(5)
        
      i += 1
      if i == len(address_list):
        i = 0
          
    elapsed_time = time.time() - start_time
    
    if not workable_server_available:
      raise Fail("Connection to Hive server %s on port %s failed after %d seconds" %
                 (params.hostname, params.hive_server_port, elapsed_time))
    
    print "Successfully connected to Hive at %s on port %s after %d seconds" %\
          (params.hostname, params.hive_server_port, elapsed_time)

    hcat_service_check()
    webhcat_service_check()

if __name__ == "__main__":
  HiveServiceCheck().execute()
