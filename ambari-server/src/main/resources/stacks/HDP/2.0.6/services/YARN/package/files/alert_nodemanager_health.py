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

import json
import socket
import urllib2

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

NODEMANAGER_HTTP_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.address}}'
NODEMANAGER_HTTPS_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.https.address}}'
YARN_HTTP_POLICY_KEY = '{{yarn-site/yarn.http.policy}}'

OK_MESSAGE = 'NodeManager Healthy'
CRITICAL_CONNECTION_MESSAGE = 'Connection failed to {0}'
CRITICAL_NODEMANAGER_STATUS_MESSAGE = 'NodeManager returned an unexpected status of "{0}"'
CRITICAL_NODEMANAGER_UNKNOWN_JSON_MESSAGE = 'Unable to determine NodeManager health from unexpected JSON response'

NODEMANAGER_DEFAULT_PORT = 8042

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NODEMANAGER_HTTP_ADDRESS_KEY,NODEMANAGER_HTTPS_ADDRESS_KEY,
  YARN_HTTP_POLICY_KEY)
  

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  result_code = RESULT_CODE_UNKNOWN

  if parameters is None:
    return (result_code, ['There were no parameters supplied to the script.'])

  scheme = 'http'
  http_uri = None
  https_uri = None
  http_policy = 'HTTP_ONLY'

  if NODEMANAGER_HTTP_ADDRESS_KEY in parameters:
    http_uri = parameters[NODEMANAGER_HTTP_ADDRESS_KEY]

  if NODEMANAGER_HTTPS_ADDRESS_KEY in parameters:
    https_uri = parameters[NODEMANAGER_HTTPS_ADDRESS_KEY]

  if YARN_HTTP_POLICY_KEY in parameters:
    http_policy = parameters[YARN_HTTP_POLICY_KEY]

  # determine the right URI and whether to use SSL
  uri = http_uri
  if http_policy == 'HTTPS_ONLY':
    scheme = 'https'

    if https_uri is not None:
      uri = https_uri

  label = ''
  url_response = None
  node_healthy = 'false'
  total_time = 0

  # some yarn-site structures don't have the web ui address
  if uri is None:
    if host_name is None:
      host_name = socket.getfqdn()

    uri = '{0}:{1}'.format(host_name, NODEMANAGER_DEFAULT_PORT)

  try:
    query = "{0}://{1}/ws/v1/node/info".format(scheme,uri)
    
    # execute the query for the JSON that includes templeton status
    url_response = urllib2.urlopen(query)
  except:
    label = CRITICAL_CONNECTION_MESSAGE.format(uri)
    return (RESULT_CODE_CRITICAL, [label])

  # URL response received, parse it
  try:
    json_response = json.loads(url_response.read())
    node_healthy = json_response['nodeInfo']['nodeHealthy']

    # convert boolean to string
    node_healthy = str(node_healthy)
  except:
    return (RESULT_CODE_CRITICAL, [query])

  # proper JSON received, compare against known value
  if node_healthy.lower() == 'true':
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_NODEMANAGER_STATUS_MESSAGE.format(node_healthy)

  return (result_code, [label])
