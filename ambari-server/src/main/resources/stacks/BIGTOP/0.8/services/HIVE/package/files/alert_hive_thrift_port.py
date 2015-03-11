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
import time
from resource_management.libraries.functions import hive_check

OK_MESSAGE = "TCP OK - %.4f response on port %s"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1}"

HIVE_SERVER_THRIFT_PORT_KEY = '{{hive-site/hive.server2.thrift.port}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

PERCENT_WARNING = 200
PERCENT_CRITICAL = 200

THRIFT_PORT_DEFAULT = 10000

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HIVE_SERVER_THRIFT_PORT_KEY,SECURITY_ENABLED_KEY)      
  

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

  thrift_port = THRIFT_PORT_DEFAULT
  if HIVE_SERVER_THRIFT_PORT_KEY in parameters:
    thrift_port = int(parameters[HIVE_SERVER_THRIFT_PORT_KEY])  

  security_enabled = False
  if SECURITY_ENABLED_KEY in parameters:
    security_enabled = bool(parameters[SECURITY_ENABLED_KEY])  

  result_code = None

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    start_time = time.time()
    is_thrift_port_ok = hive_check.check_thrift_port_sasl(host_name,
        thrift_port, security_enabled=security_enabled)
     
    if is_thrift_port_ok == True:
      result_code = 'OK'
      total_time = time.time() - start_time
      label = OK_MESSAGE % (total_time, thrift_port)
    else:
      result_code = 'CRITICAL'
      label = CRITICAL_MESSAGE.format(host_name,thrift_port)

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'
        
  return ((result_code, [label]))