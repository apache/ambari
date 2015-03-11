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
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path

OK_MESSAGE = "TCP OK - %.4f response on port %s"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1}"

HIVE_SERVER_THRIFT_PORT_KEY = '{{hive-site/hive.server2.thrift.port}}'
HIVE_SERVER_THRIFT_HTTP_PORT_KEY = '{{hive-site/hive.server2.thrift.http.port}}'
HIVE_SERVER_TRANSPORT_MODE_KEY = '{{hive-site/hive.server2.transport.mode}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
HIVE_SERVER2_AUTHENTICATION_KEY = '{{hive-site/hive.server2.authentication}}'
HIVE_SERVER_PRINCIPAL_KEY = '{{hive-site/hive.server2.authentication.kerberos.principal}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'

PERCENT_WARNING = 200
PERCENT_CRITICAL = 200

THRIFT_PORT_DEFAULT = 10000
HIVE_SERVER_TRANSPORT_MODE_DEFAULT = 'binary'
HIVE_SERVER_PRINCIPAL_DEFAULT = 'hive/_HOST@EXAMPLE.COM'
HIVE_SERVER2_AUTHENTICATION_DEFAULT = 'NOSASL'
SMOKEUSER_KEYTAB_DEFAULT = '/etc/security/keytabs/smokeuser.headless.keytab'
SMOKEUSER_DEFAULT = 'ambari-qa'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HIVE_SERVER_THRIFT_PORT_KEY,SECURITY_ENABLED_KEY,HIVE_SERVER2_AUTHENTICATION_KEY,HIVE_SERVER_PRINCIPAL_KEY,
          SMOKEUSER_KEYTAB_KEY,SMOKEUSER_KEY,HIVE_SERVER_THRIFT_HTTP_PORT_KEY,HIVE_SERVER_TRANSPORT_MODE_KEY)


def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

  transport_mode = HIVE_SERVER_TRANSPORT_MODE_DEFAULT
  if HIVE_SERVER_TRANSPORT_MODE_KEY in parameters:
    transport_mode = parameters[HIVE_SERVER_TRANSPORT_MODE_KEY]

  port = THRIFT_PORT_DEFAULT
  if transport_mode.lower() == 'binary' and HIVE_SERVER_THRIFT_PORT_KEY in parameters:
    port = int(parameters[HIVE_SERVER_THRIFT_PORT_KEY])
  elif  transport_mode.lower() == 'http' and HIVE_SERVER_THRIFT_HTTP_PORT_KEY in parameters:
    port = int(parameters[HIVE_SERVER_THRIFT_HTTP_PORT_KEY])

  security_enabled = False
  if SECURITY_ENABLED_KEY in parameters:
    security_enabled = str(parameters[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  hive_server2_authentication = HIVE_SERVER2_AUTHENTICATION_DEFAULT
  if HIVE_SERVER2_AUTHENTICATION_KEY in parameters:
    hive_server2_authentication = parameters[HIVE_SERVER2_AUTHENTICATION_KEY]

  smokeuser = SMOKEUSER_DEFAULT
  if SMOKEUSER_KEY in parameters:
    smokeuser = parameters[SMOKEUSER_KEY]

  result_code = None

  if security_enabled:
    hive_server_principal = HIVE_SERVER_PRINCIPAL_DEFAULT
    if HIVE_SERVER_PRINCIPAL_KEY in parameters:
      hive_server_principal = parameters[HIVE_SERVER_PRINCIPAL_KEY]
    smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
    if SMOKEUSER_KEYTAB_KEY in parameters:
      smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_KEY]
    kinit_path_local = get_kinit_path()
    kinitcmd=format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser}; ")
  else:
    hive_server_principal = None
    kinitcmd=None

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    start_time = time.time()
    try:
      hive_check.check_thrift_port_sasl(host_name, port, hive_server2_authentication,
                                        hive_server_principal, kinitcmd, smokeuser, transport_mode = transport_mode)
      is_thrift_port_ok = True
    except:
      is_thrift_port_ok = False

    if is_thrift_port_ok == True:
      result_code = 'OK'
      total_time = time.time() - start_time
      label = OK_MESSAGE % (total_time, port)
    else:
      result_code = 'CRITICAL'
      label = CRITICAL_MESSAGE.format(host_name,port)

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'

  return ((result_code, [label]))
