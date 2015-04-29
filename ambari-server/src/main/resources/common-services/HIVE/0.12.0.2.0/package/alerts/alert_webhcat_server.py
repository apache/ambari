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
import subprocess
import socket
import time
import urllib2

from resource_management.core.environment import Environment
from resource_management.core.resources import Execute
from resource_management.core.shell import call
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_klist_path
from os import getpid, sep

RESULT_CODE_OK = "OK"
RESULT_CODE_CRITICAL = "CRITICAL"
RESULT_CODE_UNKNOWN = "UNKNOWN"

OK_MESSAGE = "WebHCat status was OK ({0:.3f}s response from {1})"
CRITICAL_CONNECTION_MESSAGE = "Connection failed to {0}"
CRITICAL_HTTP_MESSAGE = "HTTP {0} response from {1}"
CRITICAL_WEBHCAT_STATUS_MESSAGE = 'WebHCat returned an unexpected status of "{0}"'
CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE = "Unable to determine WebHCat health from unexpected JSON response"

TEMPLETON_PORT_KEY = '{{webhcat-site/templeton.port}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
WEBHCAT_PRINCIPAL_KEY = '{{webhcat-site/templeton.kerberos.principal}}'
WEBHCAT_KEYTAB_KEY = '{{webhcat-site/templeton.kerberos.keytab}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

WEBHCAT_OK_RESPONSE = 'ok'
WEBHCAT_PORT_DEFAULT = 50111

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0
CURL_CONNECTION_TIMEOUT_DEFAULT = str(int(CONNECTION_TIMEOUT_DEFAULT))


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (TEMPLETON_PORT_KEY, SECURITY_ENABLED_KEY, WEBHCAT_KEYTAB_KEY, WEBHCAT_PRINCIPAL_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY)
  

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

  webhcat_port = WEBHCAT_PORT_DEFAULT
  if TEMPLETON_PORT_KEY in configurations:
    webhcat_port = int(configurations[TEMPLETON_PORT_KEY])

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = configurations[SECURITY_ENABLED_KEY].lower() == 'true'

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  curl_connection_timeout = CURL_CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])
    curl_connection_timeout = str(int(connection_timeout))


  # the alert will always run on the webhcat host
  if host_name is None:
    host_name = socket.getfqdn()

  # webhcat always uses http, never SSL
  query_url = "http://{0}:{1}/templeton/v1/status".format(host_name, webhcat_port)

  # initialize
  total_time = 0
  json_response = {}

  if security_enabled:
    if WEBHCAT_KEYTAB_KEY not in configurations or WEBHCAT_PRINCIPAL_KEY not in configurations:
      return (RESULT_CODE_UNKNOWN, [str(configurations)])

    try:
      webhcat_keytab = configurations[WEBHCAT_KEYTAB_KEY]
      webhcat_principal = configurations[WEBHCAT_PRINCIPAL_KEY]

      # substitute _HOST in kerberos principal with actual fqdn
      webhcat_principal = webhcat_principal.replace('_HOST', host_name)

      # Create the kerberos credentials cache (ccache) file and set it in the environment to use
      # when executing curl
      env = Environment.get_instance()
      ccache_file = "{0}{1}webhcat_alert_cc_{2}".format(env.tmp_dir, sep, getpid())
      kerberos_env = {'KRB5CCNAME': ccache_file}

      # Get the configured Kerberos executable search paths, if any
      if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
      else:
        kerberos_executable_search_paths = None

      klist_path_local = get_klist_path(kerberos_executable_search_paths)
      klist_command = format("{klist_path_local} -s {ccache_file}")

      # Determine if we need to kinit by testing to see if the relevant cache exists and has
      # non-expired tickets.  Tickets are marked to expire after 5 minutes to help reduce the number
      # it kinits we do but recover quickly when keytabs are regenerated
      return_code, _ = call(klist_command)
      if return_code != 0:
        kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
        kinit_command = format("{kinit_path_local} -l 5m -c {ccache_file} -kt {webhcat_keytab} {webhcat_principal}; ")

        # kinit so that curl will work with --negotiate
        Execute(kinit_command)

      # make a single curl call to get just the http code
      curl = subprocess.Popen(['curl', '--negotiate', '-u', ':', '-sL', '-w',
        '%{http_code}', '--connect-timeout', curl_connection_timeout,
        '-o', '/dev/null', query_url], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=kerberos_env)

      stdout, stderr = curl.communicate()

      if stderr != '':
        raise Exception(stderr)

      # check the response code
      response_code = int(stdout)

      # 0 indicates no connection
      if response_code == 0:
        label = CRITICAL_CONNECTION_MESSAGE.format(query_url)
        return (RESULT_CODE_CRITICAL, [label])

      # any other response aside from 200 is a problem
      if response_code != 200:
        label = CRITICAL_HTTP_MESSAGE.format(response_code, query_url)
        return (RESULT_CODE_CRITICAL, [label])

      # now that we have the http status and it was 200, get the content
      start_time = time.time()
      curl = subprocess.Popen(['curl', '--negotiate', '-u', ':', '-sL',
        '--connect-timeout', curl_connection_timeout, query_url, ],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=kerberos_env)

      stdout, stderr = curl.communicate()
      total_time = time.time() - start_time

      if stderr != '':
        raise Exception(stderr)

      json_response = json.loads(stdout)
    except Exception, exception:
      return (RESULT_CODE_CRITICAL, [str(exception)])
  else:
    url_response = None
    
    try:
      # execute the query for the JSON that includes WebHCat status
      start_time = time.time()
      url_response = urllib2.urlopen(query_url, timeout=connection_timeout)
      total_time = time.time() - start_time

      json_response = json.loads(url_response.read())
    except urllib2.HTTPError as httpError:
      label = CRITICAL_HTTP_MESSAGE.format(httpError.code, query_url)
      return (RESULT_CODE_CRITICAL, [label])
    except:
      label = CRITICAL_CONNECTION_MESSAGE.format(query_url)
      return (RESULT_CODE_CRITICAL, [label])
    finally:
      if url_response is not None:
        try:
          url_response.close()
        except:
          pass


  # if status is not in the response, we can't do any check; return CRIT
  if 'status' not in json_response:
    return (RESULT_CODE_CRITICAL, [CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE])


  # URL response received, parse it
  try:
    webhcat_status = json_response['status']
  except:
    return (RESULT_CODE_CRITICAL, [CRITICAL_WEBHCAT_UNKNOWN_JSON_MESSAGE])


  # proper JSON received, compare against known value
  if webhcat_status.lower() == WEBHCAT_OK_RESPONSE:
    result_code = RESULT_CODE_OK
    label = OK_MESSAGE.format(total_time, query_url)
  else:
    result_code = RESULT_CODE_CRITICAL
    label = CRITICAL_WEBHCAT_STATUS_MESSAGE.format(webhcat_status)

  return (result_code, [label])
