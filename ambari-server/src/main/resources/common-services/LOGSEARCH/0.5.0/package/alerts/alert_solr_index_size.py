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

import traceback
import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

RESULT_CODE_OK = 'OK'
RESULT_CODE_WARNING = 'WARNING'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

SOLR_PORT = '{{logsearch-solr-env/logsearch_solr_port}}'

MESSAGE = 'Solr index size is {0:.1f}GB'

SOLR_INDEX_SIZE_WARNING_KEY = 'solr.index.size.warning'
SOLR_INDEX_SIZE_WARNING_DEFAULT = 50

SOLR_INDEX_SIZE_CRITICAL_KEY = 'solr.index.size.critical'
SOLR_INDEX_SIZE_CRITICAL_DEFAULT = 100

CRITICAL_CONNECTION_MESSAGE = 'Could not execute query {0}\n{1}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (SOLR_PORT,)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (RESULT_CODE_UNKNOWN, ['There were no configurations supplied to the script.'])

  if SOLR_PORT in configurations:
    solr_port = configurations[SOLR_PORT]
  else:
    return (RESULT_CODE_UNKNOWN, ['No Solr port specified'])

  # parse script arguments
  solr_index_size_warning = SOLR_INDEX_SIZE_WARNING_DEFAULT
  if SOLR_INDEX_SIZE_WARNING_KEY in parameters:
    solr_index_size_warning = float(parameters[SOLR_INDEX_SIZE_WARNING_KEY])

  solr_index_size_critical = SOLR_INDEX_SIZE_CRITICAL_DEFAULT
  if SOLR_INDEX_SIZE_CRITICAL_KEY in parameters:
    solr_index_size_critical = float(parameters[SOLR_INDEX_SIZE_CRITICAL_KEY])


  try:
    query = "http://localhost:" + str(solr_port) + "/solr/admin/cores?action=STATUS&wt=json"
    response = urllib2.urlopen(query)
    raw_data = response.read()
    json_data = json.loads(raw_data)

    size_in_bytes = 0
    for shard_data in json_data["status"].itervalues():
      size_in_bytes += shard_data["index"]["sizeInBytes"]
  except:
    label = CRITICAL_CONNECTION_MESSAGE.format(query, traceback.format_exc())
    return (RESULT_CODE_CRITICAL, [label])

  size_in_gb = float(size_in_bytes) / float(1024 * 1024 * 1024)
  label = MESSAGE.format(size_in_gb)
  if size_in_gb <= solr_index_size_warning:
    result_code = RESULT_CODE_OK
  elif size_in_gb <= solr_index_size_critical:
    result_code = RESULT_CODE_WARNING
  else:
    result_code = RESULT_CODE_CRITICAL

  return (result_code, [label])
