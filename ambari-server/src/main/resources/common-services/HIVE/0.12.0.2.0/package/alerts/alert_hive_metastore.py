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
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.core.resources import Execute

OK_MESSAGE = "Metastore OK - %.4f response"
CRITICAL_MESSAGE = "Connection to metastore failed on host {0}"

SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
HIVE_METASTORE_URIS_KEY = '{{hive-site/hive.metastore.uris}}'

PERCENT_WARNING = 200
PERCENT_CRITICAL = 200

SMOKEUSER_KEYTAB_DEFAULT = '/etc/security/keytabs/smokeuser.headless.keytab'
SMOKEUSER_DEFAULT = 'ambari-qa'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (SECURITY_ENABLED_KEY,SMOKEUSER_KEYTAB_KEY,SMOKEUSER_KEY,HIVE_METASTORE_URIS_KEY)


def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

  if not HIVE_METASTORE_URIS_KEY in parameters:
    return (('UNKNOWN', ['Hive metastore uris were not supplied to the script.']))
  metastore_uris = parameters[HIVE_METASTORE_URIS_KEY].split(',')

  security_enabled = False
  if SECURITY_ENABLED_KEY in parameters:
    security_enabled = str(parameters[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  smokeuser = SMOKEUSER_DEFAULT
  if SMOKEUSER_KEY in parameters:
    smokeuser = parameters[SMOKEUSER_KEY]

  result_code = None

  if security_enabled:
    smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
    if SMOKEUSER_KEYTAB_KEY in parameters:
      smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_KEY]
    kinit_path_local = get_kinit_path()
    kinitcmd=format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser}; ")
    Execute(kinitcmd,
            user=smokeuser,
            path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
    )

  try:
    if host_name is None:
      host_name = socket.getfqdn()

    for uri in metastore_uris:
      if host_name in uri:
        metastore_uri = uri

    cmd = format("hive --hiveconf hive.metastore.uris={metastore_uri} -e 'show databases;'")
    start_time = time.time()
    try:
      Execute(cmd,
              user=smokeuser,
              path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
              timeout=240
      )
      is_metastore_ok = True
    except:
      is_metastore_ok = False

    if is_metastore_ok == True:
      result_code = 'OK'
      total_time = time.time() - start_time
      label = OK_MESSAGE % (total_time)
    else:
      result_code = 'CRITICAL'
      label = CRITICAL_MESSAGE.format(host_name)

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'

  return ((result_code, [label]))
