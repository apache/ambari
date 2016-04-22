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

MESSAGE = 'Memory usage is {0:.1%}'

SOLR_MEMORY_USAGE_WARNING_KEY = 'solr.memory.usage.warning'
SOLR_MEMORY_USAGE_WARNING_DEFAULT = 200

SOLR_MEMORY_USAGE_CRITICAL_KEY = 'solr.memory.usage.critical'
SOLR_MEMORY_USAGE_CRITICAL_DEFAULT = 250

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
  solr_memory_usage_warning = SOLR_MEMORY_USAGE_WARNING_DEFAULT
  if SOLR_MEMORY_USAGE_WARNING_KEY in parameters:
    solr_memory_usage_warning = float(parameters[SOLR_MEMORY_USAGE_WARNING_KEY])

  solr_memory_usage_critical = SOLR_MEMORY_USAGE_CRITICAL_DEFAULT
  if SOLR_MEMORY_USAGE_CRITICAL_KEY in parameters:
    solr_memory_usage_critical = float(parameters[SOLR_MEMORY_USAGE_CRITICAL_KEY])


  try:
    query = "http://localhost:" + str(solr_port) + "/solr/admin/cores?action=STATUS&indexInfo=false&wt=json"
    shard_response = urllib2.urlopen(query)
    shard_raw_data = shard_response.read()
    shard_json_data = json.loads(shard_raw_data)

    shard_name = shard_json_data["status"].keys()[0]
    query = "http://localhost:" + str(solr_port) + "/solr/" + shard_name + "/admin/system?wt=json"
    shard_details_response = urllib2.urlopen(query)
    shard_details_raw_data = shard_details_response.read()
    shard_details_json_data = json.loads(shard_details_raw_data)
    memory_percent = shard_details_json_data["jvm"]["memory"]["raw"]["used%"]
  except:
    label = CRITICAL_CONNECTION_MESSAGE.format(query, traceback.format_exc())
    return (RESULT_CODE_CRITICAL, [label])

  memory_load = memory_percent / 100.0
  label = MESSAGE.format(memory_load)
  if memory_percent <= solr_memory_usage_warning:
    result_code = RESULT_CODE_OK
  elif memory_percent <= solr_memory_usage_critical:
    result_code = RESULT_CODE_WARNING
  else:
    result_code = RESULT_CODE_CRITICAL

  return (result_code, [label])
