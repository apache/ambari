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
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.core.environment import Environment

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OOZIE_URL_KEY = '{{oozie-site/oozie.base.url}}'
SECURITY_ENABLED = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (OOZIE_URL_KEY, SMOKEUSER_KEY, SECURITY_ENABLED,SMOKEUSER_KEYTAB_KEY)

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (RESULT_CODE_UNKNOWN, ['There were no parameters supplied to the script.'])

  security_enabled = False
  if set([OOZIE_URL_KEY, SMOKEUSER_KEY, SECURITY_ENABLED]).issubset(parameters):
    oozie_url = parameters[OOZIE_URL_KEY]
    smokeuser = parameters[SMOKEUSER_KEY]
    security_enabled = str(parameters[SECURITY_ENABLED]).upper() == 'TRUE'
  else:
    return (RESULT_CODE_UNKNOWN, ['The Oozie URL and Smokeuser are a required parameters.'])

  try:
    if security_enabled:
      if set([SMOKEUSER_KEYTAB_KEY]).issubset(parameters):
        smokeuser_keytab = parameters[SMOKEUSER_KEYTAB_KEY]
      else:
        return (RESULT_CODE_UNKNOWN, ['The Smokeuser keytab is required when security is enabled.'])
      kinit_path_local = get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
      kinitcmd = format("{kinit_path_local} -kt {smokeuser_keytab} {smokeuser}; ")

      Execute(kinitcmd,
              user=smokeuser,
              )

    Execute(format("source /etc/oozie/conf/oozie-env.sh ; oozie admin -oozie {oozie_url} -status"),
            user=smokeuser,
            )
    return (RESULT_CODE_OK, ["Oozie check success"])

  except Exception, ex:
    return (RESULT_CODE_CRITICAL, [str(ex)])
