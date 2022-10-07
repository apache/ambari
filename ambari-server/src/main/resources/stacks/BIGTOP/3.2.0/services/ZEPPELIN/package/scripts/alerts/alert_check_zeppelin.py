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

from resource_management.core.exceptions import ComponentIsNotRunning
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.functions import get_kinit_path
from resource_management.core.resources import Execute
from resource_management.libraries.functions import format

ZEPPELIN_PORT_KEY = '{{zeppelin-site/zeppelin.server.port}}'
ZEPPELIN_PORT_SSL_KEY = '{{zeppelin-site/zeppelin.server.ssl.port}}'

SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
ZEPPELIN_KEYTAB_KEY = '{{zeppelin-site/zeppelin.server.kerberos.keytab}}'
ZEPPELIN_PRINCIPAL_KEY = '{{zeppelin-site/zeppelin.server.kerberos.principal}}'
ZEPPELIN_USER_KEY = '{{zeppelin-env/zeppelin_user}}'

UI_SSL_ENABLED = '{{zeppelin-site/zeppelin.ssl}}'

KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (ZEPPELIN_USER_KEY, UI_SSL_ENABLED, SECURITY_ENABLED_KEY, ZEPPELIN_KEYTAB_KEY, ZEPPELIN_PRINCIPAL_KEY,
          KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, ZEPPELIN_PORT_KEY, ZEPPELIN_PORT_SSL_KEY)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):

  if configurations is None:
    return ('UNKNOWN', ['There were no configurations supplied to the script.'])

  zeppelin_user = configurations[ZEPPELIN_USER_KEY]

  ui_ssl_enabled = False
  if UI_SSL_ENABLED in configurations:
    ui_ssl_enabled = str(configurations[UI_SSL_ENABLED]).upper() == 'TRUE'

  zeppelin_port = 9995
  if UI_SSL_ENABLED in configurations:
    zeppelin_port = configurations[ZEPPELIN_PORT_SSL_KEY]
  else:
    zeppelin_port = configurations[ZEPPELIN_PORT_KEY]

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  if host_name is None:
    host_name = socket.getfqdn()

  zeppelin_kerberos_keytab = None
  if ZEPPELIN_KEYTAB_KEY in configurations:
    zeppelin_kerberos_keytab = configurations[ZEPPELIN_KEYTAB_KEY]

  zeppelin_principal = None
  if ZEPPELIN_PRINCIPAL_KEY in configurations:
    zeppelin_principal = configurations[ZEPPELIN_PRINCIPAL_KEY]
    zeppelin_principal = zeppelin_principal.replace('_HOST',host_name.lower())

  if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
    kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
  else:
    kerberos_executable_search_paths = None

  kinit_path_local = get_kinit_path(kerberos_executable_search_paths)

  try:
    if security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_principal}; ")
      Execute(kinit_cmd, user=zeppelin_user)

    scheme = "https" if ui_ssl_enabled else "http"
    command = format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {scheme}://{host_name}:{zeppelin_port}/api/version | grep 200")
    Execute(command, tries = 10, try_sleep=3, user=zeppelin_user, logoutput=True)
  except ComponentIsNotRunning as ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])
  except:
    return (RESULT_CODE_CRITICAL, ["Zeppelin is not running=" + str(command)])

  return (RESULT_CODE_OK, ["Successful connection to Zeppelin"])
