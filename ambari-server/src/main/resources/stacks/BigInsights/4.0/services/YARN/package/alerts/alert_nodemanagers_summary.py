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

import urllib2
import json

from ambari_commons.urllib_handlers import RefreshHeaderProcessor

ERROR_LABEL = '{0} NodeManager{1} {2} unhealthy.'
OK_LABEL = 'All NodeManagers are healthy'

NODEMANAGER_HTTP_ADDRESS_KEY = '{{yarn-site/yarn.resourcemanager.webapp.address}}'
NODEMANAGER_HTTPS_ADDRESS_KEY = '{{yarn-site/yarn.resourcemanager.webapp.https.address}}'
YARN_HTTP_POLICY_KEY = '{{yarn-site/yarn.http.policy}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return NODEMANAGER_HTTP_ADDRESS_KEY, NODEMANAGER_HTTPS_ADDRESS_KEY, \
    YARN_HTTP_POLICY_KEY


def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

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

  live_nodemanagers_qry = "{0}://{1}/jmx?qry=Hadoop:service=ResourceManager,name=RMNMInfo".format(scheme, uri)

  try:
    live_nodemanagers = json.loads(get_value_from_jmx(live_nodemanagers_qry, "LiveNodeManagers"))

    unhealthy_count = 0

    for nodemanager in live_nodemanagers:
      health_report = nodemanager['State']
      if health_report == 'UNHEALTHY':
        unhealthy_count += 1

    if unhealthy_count == 0:
      result_code = 'OK'
      label = OK_LABEL
    else:
      result_code = 'CRITICAL'
      if unhealthy_count == 1:
        label = ERROR_LABEL.format(unhealthy_count, '', 'is')
      else:
        label = ERROR_LABEL.format(unhealthy_count, 's', 'are')

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'

  return ((result_code, [label]))


def get_value_from_jmx(query, jmx_property):
  response = None

  try:
    # use a customer header process that will look for the non-standard
    # "Refresh" header and attempt to follow the redirect
    url_opener = urllib2.build_opener(RefreshHeaderProcessor())
    response = url_opener.open(query)

    data = response.read()
    data_dict = json.loads(data)
    return data_dict["beans"][0][jmx_property]
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass
