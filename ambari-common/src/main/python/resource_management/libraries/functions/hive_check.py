#!/usr/bin/env python

'''
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
'''
from resource_management.core.exceptions import Fail
from resource_management.core.resources import Execute
from resource_management.libraries.functions import format
import socket

def check_thrift_port_sasl(address, port, hive_auth = "NOSASL", key = None, kinitcmd = None):
  """
  Hive thrift SASL port check
  """
  BEELINE_CHECK_TIMEOUT = 30

  if kinitcmd:
    url = format("jdbc:hive2://{address}:{port}/;principal={key}")
    Execute(kinitcmd)
  else:
    url = format("jdbc:hive2://{address}:{port}")

  if hive_auth != "NOSASL":
    cmd = format("! beeline -u '{url}' -e '' ") + "2>&1| awk '{print}'|grep -i 'Connection refused'"
    Execute(cmd,
            path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
            timeout=BEELINE_CHECK_TIMEOUT
    )
  else:
    s = socket.socket()
    s.settimeout(1)
    try:
      s.connect((address, port))
    except socket.error, e:
      raise
    finally:
      s.close()

