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
import time
import urllib2

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OK_MESSAGE = 'TCP OK - {0:.4f} response on port {1}'
CRITICAL_CONNECTION_MESSAGE = 'Connection failed on host {0}:{1}'
CRITICAL_TEMPLETON_STATUS_MESSAGE = 'WebHCat returned an unexpected status of "{0}"'
CRITICAL_TEMPLETON_UNKNOWN_JSON_MESSAGE = 'Unable to determine WebHCat health from unexpected JSON response'

TEMPLETON_PORT_KEY = '{{webhcat-site/templeton.port}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

TEMPLETON_OK_RESPONSE = 'ok'
TEMPLETON_PORT_DEFAULT = 50111

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (TEMPLETON_PORT_KEY,SECURITY_ENABLED_KEY)      
  

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

  templeton_port = TEMPLETON_PORT_DEFAULT
  if TEMPLETON_PORT_KEY in parameters:
    templeton_port = int(parameters[TEMPLETON_PORT_KEY])  

  security_enabled = False
  if SECURITY_ENABLED_KEY in parameters:
    security_enabled = parameters[SECURITY_ENABLED_KEY].lower() == 'true'

  scheme = 'http'
  if security_enabled is True:
    scheme = 'https'

  label = ''
  url_response = None
  templeton_status = ''
  total_time = 0

  try:
    # the alert will always run on the webhcat host
    if host_name is None:
      host_name = socket.getfqdn()
    
    query = "{0}://{1}:{2}/templeton/v1/status".format(scheme, host_name,
        templeton_port)
    
    # execute the query for the JSON that includes templeton status
    start_time = time.time()
    url_response = urllib2.urlopen(query)
    total_time = time.time() - start_time
  except:
    label = CRITICAL_CONNECTION_MESSAGE.format(host_name,templeton_port)
    return (RESULT_CODE_CRITICAL, [label])

  # URL response received, parse it
  try:
    json_response = json.loads(url_response.read())
    templeton_status = json_response['status']
  except:
    return (RESULT_CODE_CRITICAL, [CRITICAL_TEMPLETON_UNKNOWN_JSON_MESSAGE])

  # proper JSON received, compare against known value
  if templeton_status.lower() == TEMPLETON_OK_RESPONSE:
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE.format(total_time, templeton_port)
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_TEMPLETON_STATUS_MESSAGE.format(templeton_status)

  return (result_code, [label])