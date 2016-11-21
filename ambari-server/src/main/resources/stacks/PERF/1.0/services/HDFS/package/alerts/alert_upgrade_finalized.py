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

RESULT_STATE_OK = 'OK'

OK_MESSAGE = 'Ok'

NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
NN_HTTP_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'

HDFS_SITE_KEY = '{{hdfs-site}}'
KERBEROS_KEYTAB = '{{hdfs-site/dfs.web.authentication.kerberos.keytab}}'
KERBEROS_PRINCIPAL = '{{hdfs-site/dfs.web.authentication.kerberos.principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'

logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute

  :rtype tuple
  """
  return (HDFS_SITE_KEY, NN_HTTP_ADDRESS_KEY, NN_HTTPS_ADDRESS_KEY, NN_HTTP_POLICY_KEY, EXECUTABLE_SEARCH_PATHS,
          KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY, SMOKEUSER_KEY)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations : a mapping of configuration key to value
  parameters : a mapping of script parameter key to value
  host_name : the name of this host where the alert is running

  :type configurations dict
  :type parameters dict
  :type host_name str
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))

  # hdfs-site is required
  if not HDFS_SITE_KEY in configurations:
    return 'SKIPPED', ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)]

  result_code = RESULT_STATE_OK
  label = OK_MESSAGE
  return (result_code, [label])