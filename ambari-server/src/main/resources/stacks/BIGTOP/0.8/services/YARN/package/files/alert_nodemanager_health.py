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
from ambari_commons import OSCheck
from ambari_commons.inet_utils import resolve_address

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

NODEMANAGER_HTTP_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.address}}'
NODEMANAGER_HTTPS_ADDRESS_KEY = '{{yarn-site/yarn.nodemanager.webapp.https.address}}'
YARN_HTTP_POLICY_KEY = '{{yarn-site/yarn.http.policy}}'

OK_MESSAGE = 'NodeManager Healthy'
CRITICAL_CONNECTION_MESSAGE = 'Connection failed to {0} ({1})'
CRITICAL_HTTP_STATUS_MESSAGE = 'HTTP {0} returned from {1} ({2})'
CRITICAL_NODEMANAGER_STATUS_MESSAGE = 'NodeManager returned an unexpected status of "{0}"'
CRITICAL_NODEMANAGER_UNKNOWN_JSON_MESSAGE = 'Unable to determine NodeManager health from unexpected JSON response'

NODEMANAGER_DEFAULT_PORT = 8042

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NODEMANAGER_HTTP_ADDRESS_KEY,NODEMANAGER_HTTPS_ADDRESS_KEY,
  YARN_HTTP_POLICY_KEY)
  

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

  scheme = 'http'
  http_uri = None
  https_uri = None
  http_policy = 'HTTP_ONLY'

  if NODEMANAGER_HTTP_ADDRESS_KEY in configurations:
    http_uri = configurations[NODEMANAGER_HTTP_ADDRESS_KEY]

  if NODEMANAGER_HTTPS_ADDRESS_KEY in configurations:
    https_uri = configurations[NODEMANAGER_HTTPS_ADDRESS_KEY]

  if YARN_HTTP_POLICY_KEY in configurations:
    http_policy = configurations[YARN_HTTP_POLICY_KEY]


  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])


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
    
  if OSCheck.is_windows_family():
    uri_host, uri_port = uri.split(':')
    # on windows 0.0.0.0 is invalid address to connect but on linux it resolved to 127.0.0.1
    uri_host = resolve_address(uri_host)
    uri = '{0}:{1}'.format(uri_host, uri_port)

  query = "{0}://{1}/ws/v1/node/info".format(scheme,uri)

  try:
    # execute the query for the JSON that includes templeton status
    url_response = urllib2.urlopen(query, timeout=connection_timeout)
  except urllib2.HTTPError, httpError:
    label = CRITICAL_HTTP_STATUS_MESSAGE.format(str(httpError.code), query,
      str(httpError))

    return (RESULT_CODE_CRITICAL, [label])
  except Exception, exception:
    label = CRITICAL_CONNECTION_MESSAGE.format(query, str(exception))
    return (RESULT_CODE_CRITICAL, [label])

  # URL response received, parse it
  try:
    json_response = json.loads(url_response.read())
    node_healthy = json_response['nodeInfo']['nodeHealthy']
    node_healthy_report = json_response['nodeInfo']['healthReport']

    # convert boolean to string
    node_healthy = str(node_healthy)
  except:
    return (RESULT_CODE_CRITICAL, [query])
  finally:
    if url_response is not None:
      try:
        url_response.close()
      except:
        pass

  # proper JSON received, compare against known value
  if node_healthy.lower() == 'true':
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE
  elif node_healthy.lower() == 'false':
    result_code = RESULT_CODE_CRITICAL
    label = node_healthy_report
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_NODEMANAGER_STATUS_MESSAGE.format(node_healthy)

  return (result_code, [label])
