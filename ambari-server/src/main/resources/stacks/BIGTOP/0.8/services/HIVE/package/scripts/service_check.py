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

from hcat_service_check import hcat_service_check

class HiveServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    address=format("{hive_server_host}")
    port=int(format("{hive_server_port}"))
    s = socket.socket()
    print "Test connectivity to hive server"
    try:
      s.connect((address, port))
      print "Successfully connected to %s on port %s" % (address, port)
      s.close()
    except socket.error, e:
      print "Connection to %s on port %s failed: %s" % (address, port, e)
      sys.exit(1)

    hcat_service_check()

if __name__ == "__main__":
  HiveServiceCheck().execute()
