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
import socket
from resource_management.core.exceptions import Fail

def check_thrift_port_sasl(address, port, timeout = 5, security_enabled = False):
  """
  Hive thrift SASL port check
  """

  #Authentification mechanism
  mechanism = "PLAIN"
  #Anonymous username
  usr = "ANONYMOUS"
  start_byte = 0x01 #START communication
  ok_byte = 0x02 #OK
  bad_byte = 0x03 #BAD
  error_byte = 0x04 #ERROR
  complete_byte = 0x05 #COMPLETE communication
  
  msg = bytearray()

  msg.append(start_byte)
  msg.append(0)
  msg.append(0)
  msg.append(0)
  msg.append(len(mechanism))
  for elem in mechanism:
    msg.append(ord(elem))

  msg.append(ok_byte)
  msg.append(0)
  msg.append(0)
  msg.append(0)
  msg.append(len(usr)*2+2)
  
  #Adding anonymous user name
  msg.append(0)
  for elem in usr:
    msg.append(ord(elem))

  #Adding anonymous user password
  msg.append(0)
  for elem in usr:
    msg.append(ord(elem))

  msg.append(complete_byte)
  msg.append(0)
  msg.append(0)
  msg.append(0)
  msg.append(0)

  is_service_socket_valid = False
  s = socket.socket()
  s.settimeout(timeout)

  try:
    s.connect((address, port))
    #Successfull connection, port check passed
    is_service_socket_valid = True

    # Try to send anonymous plain auth message to thrift to prevent errors in hive log
    # Plain mechanism is not supported in security mode
    if not security_enabled:
      s.send(msg)
  except socket.error, e:
    #Expected if service unreachable
    pass
  finally:
    s.close()
    return is_service_socket_valid