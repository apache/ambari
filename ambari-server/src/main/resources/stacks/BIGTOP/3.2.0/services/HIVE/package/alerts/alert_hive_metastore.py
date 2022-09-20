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

import logging
import os
import socket
import time
import traceback
from urlparse import urlparse

from resource_management.core import global_lock
from resource_management.core.resources import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path


OK_MESSAGE = "Metastore OK - Hive command took {0:.3f}s"
NOT_LISTENING_MESSAGE = "Metastore on {0} is not listening or port {1}"
CRITICAL_MESSAGE = "Metastore on {0} failed ({1})"
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_PRINCIPAL_KEY = '{{cluster-env/smokeuser_principal_name}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
HIVE_METASTORE_URIS_KEY = '{{hive-site/hive.metastore.uris}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

# default keytab location
SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY = 'default.smoke.keytab'
SMOKEUSER_KEYTAB_DEFAULT = '/etc/security/keytabs/smokeuser.headless.keytab'

# default smoke principal
SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY = 'default.smoke.principal'
SMOKEUSER_PRINCIPAL_DEFAULT = 'ambari-qa@EXAMPLE.COM'

# default smoke user
SMOKEUSER_SCRIPT_PARAM_KEY = 'default.smoke.user'
SMOKEUSER_DEFAULT = 'ambari-qa'

STACK_ROOT = '{{cluster-env/stack_root}}'

CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

HADOOPUSER_KEY = '{{cluster-env/hadoop.user.name}}'
HADOOPUSER_DEFAULT = 'hadoop'

logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (SECURITY_ENABLED_KEY,SMOKEUSER_KEYTAB_KEY,SMOKEUSER_PRINCIPAL_KEY,
    HIVE_METASTORE_URIS_KEY, SMOKEUSER_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
    STACK_ROOT)

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))

  if not HIVE_METASTORE_URIS_KEY in configurations:
    return (('UNKNOWN', ['Hive metastore uris were not supplied to the script.']))

  metastore_uris = configurations[HIVE_METASTORE_URIS_KEY].split(',')

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
  if CHECK_COMMAND_TIMEOUT_KEY in parameters:
    check_command_timeout = float(parameters[CHECK_COMMAND_TIMEOUT_KEY])

  # defaults
  smokeuser_keytab = SMOKEUSER_KEYTAB_DEFAULT
  smokeuser_principal = SMOKEUSER_PRINCIPAL_DEFAULT
  smokeuser = SMOKEUSER_DEFAULT

  # check script params
  if SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY in parameters:
    smokeuser_principal = parameters[SMOKEUSER_PRINCIPAL_SCRIPT_PARAM_KEY]

  if SMOKEUSER_SCRIPT_PARAM_KEY in parameters:
    smokeuser = parameters[SMOKEUSER_SCRIPT_PARAM_KEY]

  if SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY in parameters:
    smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_SCRIPT_PARAM_KEY]


  # check configurations last as they should always take precedence
  if SMOKEUSER_PRINCIPAL_KEY in configurations:
    smokeuser_principal = configurations[SMOKEUSER_PRINCIPAL_KEY]

  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  result_code = None

  try:
    if security_enabled:
      if SMOKEUSER_KEYTAB_KEY in configurations:
        smokeuser_keytab = configurations[SMOKEUSER_KEYTAB_KEY]

      # Get the configured Kerberos executable search paths, if any
      if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
      else:
        kerberos_executable_search_paths = None

      kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
      kinitcmd=format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser_principal}; ")

      # prevent concurrent kinit
      kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
      kinit_lock.acquire()
      try:
        Execute(kinitcmd, user=smokeuser,
          path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
          timeout=10)
      finally:
        kinit_lock.release()

    if host_name is None:
      host_name = socket.getfqdn()

    port = None

    for uri in metastore_uris:
      if host_name in uri:
        parts = urlparse(uri)
        port = parts.port

    start_time = time.time()

    try:
      sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      result = sock.connect_ex((host_name, port))

      total_time = time.time() - start_time

      if result == 0:
        result_code = 'OK'
        label = OK_MESSAGE.format(total_time)
      else:
        result_code = 'CRITICAL'
        label = NOT_LISTENING_MESSAGE.format(host_name, port)
    except:
      result_code = 'CRITICAL'
      label = CRITICAL_MESSAGE.format(host_name, traceback.format_exc())

  except:
    label = traceback.format_exc()
    result_code = 'UNKNOWN'

  return ((result_code, [label]))
