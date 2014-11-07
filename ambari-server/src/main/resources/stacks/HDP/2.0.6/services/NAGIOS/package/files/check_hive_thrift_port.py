#!/usr/bin/env python
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

import os
import optparse
import json
import traceback
from resource_management import *
from time import time


OK_MESSAGE = "TCP OK - %.3f second response time on port %s"
CRITICAL_MESSAGE = "Connection to %s on port %s failed"

def main():

  parser = optparse.OptionParser()

  parser.add_option("-H", "--host", dest="address", help="Hive thrift host")
  parser.add_option("-p", "--port", type="int", dest="port", help="Hive thrift port")
  parser.add_option("-a", "--hive-auth", dest="hive_auth")
  parser.add_option("-k", "--hive-principal", dest="key")
  parser.add_option("-c", "--cmd-kinit", dest="kinitcmd")

  (options, args) = parser.parse_args()

  if options.address is None:
    print "Specify hive thrift host (--host or -H)"
    exit(-1)

  if options.port is None:
    print "Specify hive thrift port (--port or -p)"
    exit(-1)

  address = options.address
  port = options.port

  starttime = time()
  try:
    with Environment() as env:
      check_thrift_port_sasl(address, port, options.hive_auth, options.key, options.kinitcmd)
    timetaken = time() - starttime
    print OK_MESSAGE % (timetaken, port)
  except:
    print CRITICAL_MESSAGE % (address, port)
    exit(2)

  exit(0)

if __name__ == "__main__":
  main()

