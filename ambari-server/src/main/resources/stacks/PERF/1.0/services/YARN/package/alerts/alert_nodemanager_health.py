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

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OK_MESSAGE = 'NodeManager Healthy'

NODEMANAGER_HTTP_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.address}}'
NODEMANAGER_HTTPS_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.https.address}}'
YARN_HTTP_POLICY_KEY = '{{yarn-site/yarn.http.policy}}'

KERBEROS_KEYTAB = '{{yarn-site/yarn.nodemanager.webapp.spnego-keytab-file}}'
KERBEROS_PRINCIPAL = '{{yarn-site/yarn.nodemanager.webapp.spnego-principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'

logger = logging.getLogger('ambari_alerts')


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NODEMANAGER_HTTP_ADDRESS_KEY,NODEMANAGER_HTTPS_ADDRESS_KEY, EXECUTABLE_SEARCH_PATHS,
  YARN_HTTP_POLICY_KEY, SMOKEUSER_KEY, KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY)
  

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  result_code = RESULT_CODE_UNKNOWN

  if configurations is None:
    return (result_code, ['There were no configurations supplied to the script.'])

  result_code = RESULT_CODE_OK
  label = OK_MESSAGE
  return (result_code, [label])
